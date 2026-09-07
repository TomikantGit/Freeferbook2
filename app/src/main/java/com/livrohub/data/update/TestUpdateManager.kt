package com.livrohub.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Metadados publicados pelo canal automatizado de builds de teste. */
data class TestUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val notes: String
)

/** Resultado de uma consulta ao canal de atualização. */
sealed interface TestUpdateCheckResult {
    data class Available(val update: TestUpdateInfo) : TestUpdateCheckResult
    data object UpToDate : TestUpdateCheckResult
    data class Error(val message: String) : TestUpdateCheckResult
}

/** Resultado da tentativa de abrir o instalador do Android. */
sealed interface TestUpdateInstallResult {
    data object InstallerOpened : TestUpdateInstallResult
    data object PermissionRequired : TestUpdateInstallResult
    data class Error(val message: String) : TestUpdateInstallResult
}

/**
 * Cliente enxuto do canal de atualizações de teste do Freeferbook.
 *
 * A escrita e a biblioteca continuam totalmente offline. Rede só é usada quando o usuário
 * solicita uma verificação ou baixa explicitamente um APK de teste.
 */
class TestUpdateManager(private val context: Context) {

    @Suppress("DEPRECATION")
    val currentVersionCode: Int
        get() {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                packageInfo.versionCode
            }
        }

    @Suppress("DEPRECATION")
    val currentVersionName: String
        get() = context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()

    /** Consulta o manifesto público e compara o versionCode publicado com o instalado. */
    suspend fun checkForUpdate(): TestUpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val manifest = requestText(MANIFEST_URL)
            val json = JSONObject(manifest)
            TestUpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                apkUrl = json.getString("apkUrl"),
                sha256 = json.optString("sha256"),
                notes = json.optString("notes")
            )
        }.fold(
            onSuccess = { update ->
                if (update.versionCode > currentVersionCode) {
                    TestUpdateCheckResult.Available(update)
                } else {
                    TestUpdateCheckResult.UpToDate
                }
            },
            onFailure = { error ->
                TestUpdateCheckResult.Error(
                    error.message ?: "Não foi possível consultar o canal de atualização."
                )
            }
        )
    }

    /** Baixa o APK para o cache privado e valida o SHA-256 publicado, quando presente. */
    suspend fun downloadUpdate(update: TestUpdateInfo): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
            val partialFile = File(updateDirectory, "freeferbook-test.apk.part")
            val apkFile = File(updateDirectory, "freeferbook-test.apk")

            partialFile.delete()

            val connection = openConnection(update.apkUrl)
            try {
                ensureSuccess(connection, "baixar a atualização")
                connection.inputStream.use { input ->
                    FileOutputStream(partialFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                connection.disconnect()
            }

            if (partialFile.length() == 0L) {
                partialFile.delete()
                error("O APK baixado está vazio.")
            }

            if (update.sha256.isNotBlank()) {
                val actualSha256 = sha256(partialFile)
                if (!actualSha256.equals(update.sha256, ignoreCase = true)) {
                    partialFile.delete()
                    error("A assinatura SHA-256 do APK não confere. A instalação foi cancelada.")
                }
            }

            apkFile.delete()
            if (!partialFile.renameTo(apkFile)) {
                partialFile.copyTo(apkFile, overwrite = true)
                partialFile.delete()
            }
            apkFile
        }
    }

    /**
     * Abre o instalador do Android para o APK já validado.
     * O Android continua responsável pela confirmação final da atualização.
     */
    fun requestInstall(apkFile: File): TestUpdateInstallResult {
        if (!apkFile.exists()) {
            return TestUpdateInstallResult.Error("O APK de atualização não foi encontrado.")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            return runCatching {
                context.startActivity(settingsIntent)
                TestUpdateInstallResult.PermissionRequired
            }.getOrElse {
                TestUpdateInstallResult.Error(
                    "Não foi possível abrir a autorização para instalar apps desta fonte."
                )
            }
        }

        return runCatching {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, APK_MIME_TYPE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (installIntent.resolveActivity(context.packageManager) == null) {
                return TestUpdateInstallResult.Error(
                    "Nenhum instalador de pacotes está disponível neste dispositivo."
                )
            }

            context.startActivity(installIntent)
            TestUpdateInstallResult.InstallerOpened
        }.getOrElse { error ->
            TestUpdateInstallResult.Error(
                error.message ?: "Não foi possível abrir o instalador do Android."
            )
        }
    }

    /** Abre a release privada no navegador como rota de contingência. */
    fun openGitHubRelease() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_PAGE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun requestText(url: String): String {
        val connection = openConnection(url)
        return try {
            ensureSuccess(connection, "consultar a atualização")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Freeferbook/${currentVersionName}")
            setRequestProperty("Cache-Control", "no-cache")
        }
    }

    private fun ensureSuccess(connection: HttpURLConnection, action: String) {
        val code = connection.responseCode
        if (code !in 200..299) {
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                error("O canal público de teste ainda não está disponível.")
            }
            error("Falha ao $action (HTTP $code).")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        const val MANIFEST_URL =
            "https://tomikantgit.github.io/Freeferbook2/update.json"
        const val RELEASE_PAGE_URL =
            "https://github.com/TomikantGit/Freeferbook2/releases/tag/test-latest"
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
