package com.livrohub.data.archive

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.room.withTransaction
import com.livrohub.data.local.BookEntity
import com.livrohub.data.local.ChapterEntity
import com.livrohub.data.local.ChapterVersionEntity
import com.livrohub.data.local.CharacterEntity
import com.livrohub.data.local.ImageEntity
import com.livrohub.data.local.LivroHubDatabase
import com.livrohub.data.local.LocationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Resultado de uma importação ou exportação de backup completo de livro. */
sealed interface BookArchiveResult {
    data class Success(
        val message: String,
        val importedBookId: Long? = null
    ) : BookArchiveResult

    data class Failure(val message: String) : BookArchiveResult
}

/**
 * Importa e exporta um livro inteiro em um contêiner ZIP versionado.
 *
 * O arquivo contém `manifest.json`, com os dados estruturados, e opcionalmente
 * `media/`, com imagens locais que ainda possam ser lidas pelo Android. O formato
 * é ZIP padrão, portanto pode usar `.zip` ou uma extensão própria no futuro.
 *
 * O backup preserva capítulos, histórico imutável, personagens, locais e imagens.
 * IDs do banco não são preservados na importação: novos IDs são gerados e as relações
 * internas são reconstruídas, evitando colisões com dados já existentes.
 */
class BookArchiveManager(
    context: Context,
    private val database: LivroHubDatabase
) {
    private val appContext = context.applicationContext

    suspend fun exportBook(bookId: Long, destination: Uri): BookArchiveResult =
        withContext(Dispatchers.IO) {
            val snapshot = runCatching { loadSnapshot(bookId) }
                .getOrElse { error ->
                    return@withContext BookArchiveResult.Failure(
                        error.message ?: "Não foi possível preparar o backup do livro."
                    )
                }

            val staged = stageMedia(snapshot)
            try {
                val output = appContext.contentResolver.openOutputStream(destination)
                    ?: return@withContext BookArchiveResult.Failure(
                        "Não foi possível abrir o arquivo de destino."
                    )

                output.use { stream ->
                    ZipOutputStream(BufferedOutputStream(stream)).use { zip ->
                        val manifest = buildManifest(snapshot, staged.filesBySource)
                        zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
                        zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                        zip.closeEntry()

                        staged.filesBySource.values
                            .distinctBy { it.entryName }
                            .forEach { media ->
                                zip.putNextEntry(ZipEntry(media.entryName))
                                media.file.inputStream().buffered().use { input ->
                                    input.copyTo(zip)
                                }
                                zip.closeEntry()
                            }
                    }
                }

                val detail = if (staged.skippedCount > 0) {
                    " ${staged.skippedCount} imagem(ns) local(is) não puderam ser incorporadas e tiveram apenas a referência preservada."
                } else {
                    ""
                }
                BookArchiveResult.Success("Backup de \"${snapshot.book.title}\" criado.$detail")
            } catch (error: Exception) {
                BookArchiveResult.Failure(
                    error.message ?: "Não foi possível exportar o livro."
                )
            } finally {
                staged.directory.deleteRecursively()
            }
        }

    suspend fun importBook(source: Uri): BookArchiveResult = withContext(Dispatchers.IO) {
        val temporaryArchive = File.createTempFile("freeferbook-import-", ".zip", appContext.cacheDir)
        var importedMediaDirectory: File? = null

        try {
            val input = appContext.contentResolver.openInputStream(source)
                ?: return@withContext BookArchiveResult.Failure(
                    "Não foi possível abrir o arquivo selecionado."
                )

            input.use { stream ->
                FileOutputStream(temporaryArchive).use { output ->
                    copyWithLimit(stream, output, MAX_ARCHIVE_BYTES)
                }
            }

            detectUnsupportedArchive(temporaryArchive)?.let { message ->
                return@withContext BookArchiveResult.Failure(message)
            }

            val zip = runCatching { ZipFile(temporaryArchive) }
                .getOrElse {
                    return@withContext BookArchiveResult.Failure(
                        "Arquivo inválido. Selecione um backup Freeferbook ou ZIP gerado pelo aplicativo."
                    )
                }

            zip.use { archive ->
                val manifestEntry = archive.getEntry(MANIFEST_ENTRY)
                    ?: return@withContext BookArchiveResult.Failure(
                        "Este ZIP não contém um backup Freeferbook reconhecível."
                    )

                if (manifestEntry.size > MAX_MANIFEST_BYTES) {
                    return@withContext BookArchiveResult.Failure("O manifesto do backup é grande demais.")
                }

                val manifestText = archive.getInputStream(manifestEntry).use { manifestInput ->
                    readTextWithLimit(manifestInput, MAX_MANIFEST_BYTES)
                }
                val manifest = runCatching { JSONObject(manifestText) }
                    .getOrElse {
                        return@withContext BookArchiveResult.Failure("O manifesto do backup está corrompido.")
                    }

                validateManifest(manifest)?.let { message ->
                    return@withContext BookArchiveResult.Failure(message)
                }

                importedMediaDirectory = File(
                    appContext.filesDir,
                    "imported_book_media/${UUID.randomUUID()}"
                ).apply { mkdirs() }

                val mediaResolver = ImportedMediaResolver(
                    archive = archive,
                    destinationDirectory = importedMediaDirectory!!
                )
                val snapshot = parseManifest(manifest, mediaResolver)

                val newBookId = database.withTransaction {
                    restoreSnapshot(snapshot)
                }

                val warning = if (mediaResolver.missingCount > 0) {
                    " ${mediaResolver.missingCount} mídia(s) não puderam ser restauradas integralmente."
                } else {
                    ""
                }
                val versionCount = snapshot.chapters.sumOf { it.versions.size }
                return@withContext BookArchiveResult.Success(
                    message = "Livro \"${snapshot.book.title}\" importado com ${snapshot.chapters.size} capítulo(s) e $versionCount versão(ões).$warning",
                    importedBookId = newBookId
                )
            }
        } catch (error: ArchiveTooLargeException) {
            importedMediaDirectory?.deleteRecursively()
            BookArchiveResult.Failure(error.message ?: "O arquivo excede o limite permitido.")
        } catch (error: Exception) {
            importedMediaDirectory?.deleteRecursively()
            BookArchiveResult.Failure(
                error.message ?: "Não foi possível importar o backup."
            )
        } finally {
            temporaryArchive.delete()
        }
    }

    private suspend fun loadSnapshot(bookId: Long): ArchiveSnapshot = database.withTransaction {
        val book = database.bookDao().getBook(bookId)
            ?: error("Livro não encontrado.")
        val chapters = database.chapterDao().getChapters(bookId).map { chapter ->
            ArchiveChapter(
                title = chapter.title,
                orderIndex = chapter.orderIndex,
                createdAt = chapter.createdAt,
                versions = database.chapterVersionDao().getVersions(chapter.id).map { version ->
                    ArchiveVersion(
                        content = version.content,
                        createdAt = version.createdAt,
                        message = version.message,
                        sequenceNumber = version.sequenceNumber,
                        wordCount = version.wordCount,
                        charCount = version.charCount,
                        lineCount = version.lineCount
                    )
                }
            )
        }

        ArchiveSnapshot(
            book = ArchiveBook(book.title, book.createdAt),
            chapters = chapters,
            characters = database.characterDao().getCharactersByBook(bookId).map {
                ArchiveCharacter(it.name, it.surnames, it.chapters, it.imageUri)
            },
            locations = database.locationDao().getLocationsByBookDirect(bookId).map {
                ArchiveLocation(it.name, it.description, it.chapters, it.imageUri)
            },
            images = database.imageDao().getImages(bookId).map {
                ArchiveImage(it.url, it.description, it.createdAt)
            }
        )
    }

    private fun stageMedia(snapshot: ArchiveSnapshot): StagedMediaResult {
        val directory = File(appContext.cacheDir, "book-export-media/${UUID.randomUUID()}")
            .apply { mkdirs() }
        val sources = linkedSetOf<String>()
        snapshot.characters.mapNotNullTo(sources) { it.imageUri }
        snapshot.locations.mapNotNullTo(sources) { it.imageUri }
        snapshot.images.mapTo(sources) { it.url }

        val staged = linkedMapOf<String, StagedMedia>()
        var skipped = 0
        var totalMediaBytes = 0L

        sources.filter(::isLocalMediaReference).forEachIndexed { index, source ->
            val extension = mediaExtension(source)
            val entryName = "media/${index.toString().padStart(4, '0')}.$extension"
            val localFile = File(directory, "media-$index.$extension")

            val copied = runCatching {
                openLocalMedia(source)?.use { input ->
                    FileOutputStream(localFile).use { output ->
                        copyWithLimit(input, output, MAX_MEDIA_ENTRY_BYTES)
                    }
                } ?: error("Mídia inacessível")
                localFile.length() > 0L
            }.getOrDefault(false)

            if (copied && totalMediaBytes + localFile.length() <= MAX_TOTAL_MEDIA_BYTES) {
                staged[source] = StagedMedia(entryName, localFile)
                totalMediaBytes += localFile.length()
            } else {
                localFile.delete()
                skipped++
            }
        }

        return StagedMediaResult(directory, staged, skipped)
    }

    private fun buildManifest(
        snapshot: ArchiveSnapshot,
        media: Map<String, StagedMedia>
    ): JSONObject = JSONObject().apply {
        put("format", FORMAT_ID)
        put("schemaVersion", SCHEMA_VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("book", JSONObject().apply {
            put("title", snapshot.book.title)
            put("createdAt", snapshot.book.createdAt)
        })

        put("chapters", JSONArray().apply {
            snapshot.chapters.forEach { chapter ->
                put(JSONObject().apply {
                    put("title", chapter.title)
                    put("orderIndex", chapter.orderIndex)
                    put("createdAt", chapter.createdAt)
                    put("versions", JSONArray().apply {
                        chapter.versions.forEach { version ->
                            put(JSONObject().apply {
                                put("content", version.content)
                                put("createdAt", version.createdAt)
                                put("message", version.message ?: JSONObject.NULL)
                                put("sequenceNumber", version.sequenceNumber)
                                put("wordCount", version.wordCount)
                                put("charCount", version.charCount)
                                put("lineCount", version.lineCount)
                            })
                        }
                    })
                })
            }
        })

        put("characters", JSONArray().apply {
            snapshot.characters.forEach { character ->
                put(JSONObject().apply {
                    put("name", character.name)
                    put("surnames", character.surnames)
                    put("chapters", character.chapters)
                    putNullable("imageUri", character.imageUri)
                    putNullable("imageEntry", character.imageUri?.let { media[it]?.entryName })
                })
            }
        })

        put("locations", JSONArray().apply {
            snapshot.locations.forEach { location ->
                put(JSONObject().apply {
                    put("name", location.name)
                    put("description", location.description)
                    put("chapters", location.chapters)
                    putNullable("imageUri", location.imageUri)
                    putNullable("imageEntry", location.imageUri?.let { media[it]?.entryName })
                })
            }
        })

        put("images", JSONArray().apply {
            snapshot.images.forEach { image ->
                put(JSONObject().apply {
                    put("url", image.url)
                    put("description", image.description)
                    put("createdAt", image.createdAt)
                    putNullable("mediaEntry", media[image.url]?.entryName)
                })
            }
        })
    }

    private fun parseManifest(
        root: JSONObject,
        mediaResolver: ImportedMediaResolver
    ): ArchiveSnapshot {
        val bookObject = root.getJSONObject("book")
        val chaptersArray = root.optJSONArray("chapters") ?: JSONArray()
        val charactersArray = root.optJSONArray("characters") ?: JSONArray()
        val locationsArray = root.optJSONArray("locations") ?: JSONArray()
        val imagesArray = root.optJSONArray("images") ?: JSONArray()

        val chapters = buildList {
            for (index in 0 until chaptersArray.length()) {
                val chapter = chaptersArray.getJSONObject(index)
                val versionsArray = chapter.optJSONArray("versions") ?: JSONArray()
                val versions = buildList {
                    for (versionIndex in 0 until versionsArray.length()) {
                        val version = versionsArray.getJSONObject(versionIndex)
                        val content = version.optString("content", "")
                        add(
                            ArchiveVersion(
                                content = content,
                                createdAt = version.optLong("createdAt", System.currentTimeMillis()),
                                message = version.nullableString("message"),
                                sequenceNumber = version.optInt("sequenceNumber", versionIndex + 1),
                                wordCount = version.optInt("wordCount", countWords(content)),
                                charCount = version.optInt("charCount", content.length),
                                lineCount = version.optInt(
                                    "lineCount",
                                    if (content.isEmpty()) 0 else content.lines().size
                                )
                            )
                        )
                    }
                }
                add(
                    ArchiveChapter(
                        title = chapter.optString("title", "Capítulo"),
                        orderIndex = chapter.optInt("orderIndex", index),
                        createdAt = chapter.optLong("createdAt", System.currentTimeMillis()),
                        versions = versions
                    )
                )
            }
        }

        val characters = buildList {
            for (index in 0 until charactersArray.length()) {
                val character = charactersArray.getJSONObject(index)
                val original = character.nullableString("imageUri")
                add(
                    ArchiveCharacter(
                        name = character.optString("name", "Personagem"),
                        surnames = character.optString("surnames", ""),
                        chapters = character.optString("chapters", ""),
                        imageUri = mediaResolver.resolve(
                            original = original,
                            entryName = character.nullableString("imageEntry")
                        )
                    )
                )
            }
        }

        val locations = buildList {
            for (index in 0 until locationsArray.length()) {
                val location = locationsArray.getJSONObject(index)
                val original = location.nullableString("imageUri")
                add(
                    ArchiveLocation(
                        name = location.optString("name", "Local"),
                        description = location.optString("description", ""),
                        chapters = location.optString("chapters", ""),
                        imageUri = mediaResolver.resolve(
                            original = original,
                            entryName = location.nullableString("imageEntry")
                        )
                    )
                )
            }
        }

        val images = buildList {
            for (index in 0 until imagesArray.length()) {
                val image = imagesArray.getJSONObject(index)
                val original = image.optString("url", "")
                add(
                    ArchiveImage(
                        url = mediaResolver.resolve(
                            original = original,
                            entryName = image.nullableString("mediaEntry")
                        ).orEmpty(),
                        description = image.optString("description", ""),
                        createdAt = image.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        return ArchiveSnapshot(
            book = ArchiveBook(
                title = bookObject.getString("title").trim().ifBlank { "Livro importado" },
                createdAt = bookObject.optLong("createdAt", System.currentTimeMillis())
            ),
            chapters = chapters,
            characters = characters,
            locations = locations,
            images = images
        )
    }

    private suspend fun restoreSnapshot(snapshot: ArchiveSnapshot): Long {
        val bookId = database.bookDao().insert(
            BookEntity(
                title = snapshot.book.title,
                createdAt = snapshot.book.createdAt
            )
        )

        snapshot.chapters.sortedBy { it.orderIndex }.forEachIndexed { index, chapter ->
            val chapterId = database.chapterDao().insert(
                ChapterEntity(
                    bookId = bookId,
                    title = chapter.title.trim().ifBlank { "Capítulo ${index + 1}" },
                    orderIndex = chapter.orderIndex,
                    createdAt = chapter.createdAt
                )
            )

            val versions = chapter.versions
                .sortedBy { it.sequenceNumber }
                .ifEmpty {
                    listOf(
                        ArchiveVersion(
                            content = "",
                            createdAt = chapter.createdAt,
                            message = "Versão inicial importada",
                            sequenceNumber = 1,
                            wordCount = 0,
                            charCount = 0,
                            lineCount = 0
                        )
                    )
                }

            versions.forEachIndexed { versionIndex, version ->
                database.chapterVersionDao().insert(
                    ChapterVersionEntity(
                        chapterId = chapterId,
                        content = version.content,
                        createdAt = version.createdAt,
                        message = version.message,
                        sequenceNumber = version.sequenceNumber.takeIf { it > 0 } ?: versionIndex + 1,
                        wordCount = version.wordCount,
                        charCount = version.charCount,
                        lineCount = version.lineCount
                    )
                )
            }
        }

        snapshot.characters.forEach { character ->
            database.characterDao().insertCharacter(
                CharacterEntity(
                    bookId = bookId,
                    name = character.name,
                    surnames = character.surnames,
                    chapters = character.chapters,
                    imageUri = character.imageUri
                )
            )
        }

        snapshot.locations.forEach { location ->
            database.locationDao().insertLocation(
                LocationEntity(
                    bookId = bookId,
                    name = location.name,
                    description = location.description,
                    chapters = location.chapters,
                    imageUri = location.imageUri
                )
            )
        }

        snapshot.images.forEach { image ->
            if (image.url.isNotBlank()) {
                database.imageDao().insert(
                    ImageEntity(
                        bookId = bookId,
                        url = image.url,
                        description = image.description,
                        createdAt = image.createdAt
                    )
                )
            }
        }

        return bookId
    }

    private fun validateManifest(root: JSONObject): String? {
        if (root.optString("format") != FORMAT_ID) {
            return "Este arquivo não é um backup de livro do Freeferbook."
        }
        val schemaVersion = root.optInt("schemaVersion", 0)
        if (schemaVersion <= 0) {
            return "O backup não informa uma versão de formato válida."
        }
        if (schemaVersion > SCHEMA_VERSION) {
            return "Este backup foi criado por uma versão mais nova do Freeferbook. Atualize o app antes de importar."
        }
        if (!root.has("book")) {
            return "O backup não contém os dados do livro."
        }
        return null
    }

    private inner class ImportedMediaResolver(
        private val archive: ZipFile,
        private val destinationDirectory: File
    ) {
        private val resolved = mutableMapOf<String, String>()
        var missingCount: Int = 0
            private set

        fun resolve(original: String?, entryName: String?): String? {
            if (entryName.isNullOrBlank()) return original
            if (!entryName.startsWith("media/") || entryName.contains("..")) {
                missingCount++
                return original
            }
            resolved[entryName]?.let { return it }

            val entry = archive.getEntry(entryName)
            if (entry == null || entry.isDirectory || entry.size > MAX_MEDIA_ENTRY_BYTES) {
                missingCount++
                return original
            }

            val extension = entryName.substringAfterLast('.', "bin")
                .filter { it.isLetterOrDigit() }
                .take(8)
                .ifBlank { "bin" }
            val destination = File(
                destinationDirectory,
                "media-${resolved.size}-${UUID.randomUUID()}.$extension"
            )

            val restored = runCatching {
                archive.getInputStream(entry).use { input ->
                    FileOutputStream(destination).use { output ->
                        copyWithLimit(input, output, MAX_MEDIA_ENTRY_BYTES)
                    }
                }
                if (destination.length() <= 0L) error("Mídia vazia")
                Uri.fromFile(destination).toString()
            }.getOrNull()

            return if (restored != null) {
                resolved[entryName] = restored
                restored
            } else {
                destination.delete()
                missingCount++
                original
            }
        }
    }

    private fun isLocalMediaReference(value: String): Boolean {
        val uri = Uri.parse(value)
        return uri.scheme in setOf("content", "file", "android.resource") ||
            (uri.scheme.isNullOrBlank() && File(value).exists())
    }

    private fun openLocalMedia(value: String): InputStream? {
        val uri = Uri.parse(value)
        return when (uri.scheme) {
            "content", "android.resource" -> appContext.contentResolver.openInputStream(uri)
            "file" -> uri.path?.let(::File)?.takeIf { it.isFile }?.inputStream()
            null, "" -> File(value).takeIf { it.isFile }?.inputStream()
            else -> null
        }?.let(::BufferedInputStream)
    }

    private fun mediaExtension(value: String): String {
        val uri = Uri.parse(value)
        val mime = runCatching { appContext.contentResolver.getType(uri) }.getOrNull()
        val byMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        val byPath = uri.lastPathSegment
            ?.substringAfterLast('.', "")
            ?.lowercase()
            ?.filter { it.isLetterOrDigit() }
            ?.take(8)
            ?.takeIf { it.isNotBlank() }
        return byMime ?: byPath ?: "bin"
    }

    private fun detectUnsupportedArchive(file: File): String? {
        val header = ByteArray(8)
        val count = FileInputStream(file).use { it.read(header) }
        if (count >= 7 && header.copyOfRange(0, 7).contentEquals(RAR4_SIGNATURE)) {
            return "Arquivos RAR ainda não são suportados diretamente. Exporte ou extraia o backup como ZIP."
        }
        if (count >= 8 && header.contentEquals(RAR5_SIGNATURE)) {
            return "Arquivos RAR ainda não são suportados diretamente. Exporte ou extraia o backup como ZIP."
        }
        if (count >= 6 && header.copyOfRange(0, 6).contentEquals(SEVEN_Z_SIGNATURE)) {
            return "Arquivos 7z ainda não são suportados diretamente. Use o formato ZIP do Freeferbook."
        }
        return null
    }

    private fun copyWithLimit(input: InputStream, output: java.io.OutputStream, maxBytes: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            if (total > maxBytes) {
                throw ArchiveTooLargeException("O arquivo excede o limite de ${maxBytes / (1024 * 1024)} MB.")
            }
            output.write(buffer, 0, read)
        }
    }

    private fun readTextWithLimit(input: InputStream, maxBytes: Long): String {
        val bytes = java.io.ByteArrayOutputStream()
        copyWithLimit(input, bytes, maxBytes)
        return bytes.toString(Charsets.UTF_8.name())
    }

    private fun countWords(content: String): Int =
        if (content.isBlank()) 0 else content.trim().split(WHITESPACE_REGEX).size

    companion object {
        const val MIME_TYPE = "application/zip"
        private const val FORMAT_ID = "freeferbook-book-backup"
        private const val SCHEMA_VERSION = 1
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_MANIFEST_BYTES = 20L * 1024L * 1024L
        private const val MAX_MEDIA_ENTRY_BYTES = 100L * 1024L * 1024L
        private const val MAX_TOTAL_MEDIA_BYTES = 900L * 1024L * 1024L
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val RAR4_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00)
        private val RAR5_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)
        private val SEVEN_Z_SIGNATURE = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)

        fun suggestedFileName(title: String): String {
            val safeTitle = title
                .trim()
                .replace(Regex("[^A-Za-z0-9À-ÿ_-]+"), "_")
                .trim('_')
                .ifBlank { "livro" }
                .take(80)
            return "$safeTitle-freeferbook.zip"
        }
    }
}

private fun JSONObject.putNullable(key: String, value: String?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private class ArchiveTooLargeException(message: String) : Exception(message)

private data class StagedMedia(
    val entryName: String,
    val file: File
)

private data class StagedMediaResult(
    val directory: File,
    val filesBySource: Map<String, StagedMedia>,
    val skippedCount: Int
)

private data class ArchiveSnapshot(
    val book: ArchiveBook,
    val chapters: List<ArchiveChapter>,
    val characters: List<ArchiveCharacter>,
    val locations: List<ArchiveLocation>,
    val images: List<ArchiveImage>
)

private data class ArchiveBook(
    val title: String,
    val createdAt: Long
)

private data class ArchiveChapter(
    val title: String,
    val orderIndex: Int,
    val createdAt: Long,
    val versions: List<ArchiveVersion>
)

private data class ArchiveVersion(
    val content: String,
    val createdAt: Long,
    val message: String?,
    val sequenceNumber: Int,
    val wordCount: Int,
    val charCount: Int,
    val lineCount: Int
)

private data class ArchiveCharacter(
    val name: String,
    val surnames: String,
    val chapters: String,
    val imageUri: String?
)

private data class ArchiveLocation(
    val name: String,
    val description: String,
    val chapters: String,
    val imageUri: String?
)

private data class ArchiveImage(
    val url: String,
    val description: String,
    val createdAt: Long
)
