package com.livrohub.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livrohub.data.update.TestUpdateCheckResult
import com.livrohub.data.update.TestUpdateInfo
import com.livrohub.data.update.TestUpdateInstallResult
import com.livrohub.data.update.TestUpdateManager
import com.livrohub.ui.common.viewModelFactory
import com.livrohub.ui.theme.LivroHubTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

private sealed interface StartupUpdateState {
    data object Idle : StartupUpdateState
    data object Checking : StartupUpdateState
    data class Available(val update: TestUpdateInfo) : StartupUpdateState
    data class Downloading(val update: TestUpdateInfo) : StartupUpdateState
    data class Ready(
        val update: TestUpdateInfo,
        val apkFile: File,
        val notice: String
    ) : StartupUpdateState

    data class Error(val message: String) : StartupUpdateState
}

private class StartupUpdateViewModel(
    private val manager: TestUpdateManager
) : ViewModel() {

    private val _state = MutableStateFlow<StartupUpdateState>(StartupUpdateState.Idle)
    val state: StateFlow<StartupUpdateState> = _state.asStateFlow()

    private var checkedThisSession = false

    fun checkOnce() {
        if (checkedThisSession) return
        checkedThisSession = true
        _state.value = StartupUpdateState.Checking

        viewModelScope.launch {
            _state.value = when (val result = manager.checkForUpdate()) {
                is TestUpdateCheckResult.Available -> StartupUpdateState.Available(result.update)
                TestUpdateCheckResult.UpToDate -> StartupUpdateState.Idle
                is TestUpdateCheckResult.Error -> StartupUpdateState.Idle
            }
        }
    }

    fun dismiss() {
        _state.value = StartupUpdateState.Idle
    }

    fun updateNow(update: TestUpdateInfo) {
        if (_state.value is StartupUpdateState.Downloading) return
        _state.value = StartupUpdateState.Downloading(update)

        viewModelScope.launch {
            manager.downloadUpdate(update)
                .onSuccess { apkFile -> requestInstall(update, apkFile) }
                .onFailure { error ->
                    _state.value = StartupUpdateState.Error(
                        error.message ?: "Não foi possível baixar a atualização."
                    )
                }
        }
    }

    fun installReady() {
        val ready = _state.value as? StartupUpdateState.Ready ?: return
        requestInstall(ready.update, ready.apkFile)
    }

    private fun requestInstall(update: TestUpdateInfo, apkFile: File) {
        when (val result = manager.requestInstall(apkFile)) {
            TestUpdateInstallResult.InstallerOpened -> _state.value = StartupUpdateState.Idle
            TestUpdateInstallResult.PermissionRequired -> {
                _state.value = StartupUpdateState.Ready(
                    update = update,
                    apkFile = apkFile,
                    notice = "Autorize o Freeferbook a instalar apps desta fonte. Ao voltar, toque em Instalar atualização."
                )
            }

            is TestUpdateInstallResult.Error -> {
                _state.value = StartupUpdateState.Error(result.message)
            }
        }
    }
}

/**
 * Faz uma verificação silenciosa uma única vez por sessão do canal público de testes.
 * Erros de rede no startup não são exibidos; o usuário continua podendo verificar manualmente
 * em Configurações > Extras.
 */
@Composable
fun StartupUpdatePrompt() {
    val context = LocalContext.current
    if (context.packageName != PUBLIC_TEST_PACKAGE) return

    val viewModel: StartupUpdateViewModel = viewModel(
        key = "startup-update",
        factory = viewModelFactory {
            StartupUpdateViewModel(TestUpdateManager(context.applicationContext))
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkOnce()
    }

    when (val current = state) {
        StartupUpdateState.Idle,
        StartupUpdateState.Checking -> Unit

        is StartupUpdateState.Available -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Nova atualização disponível") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Freeferbook ${current.update.versionName}",
                        fontWeight = FontWeight.Bold
                    )
                    if (current.update.notes.isNotBlank()) {
                        Text(
                            text = current.update.notes,
                            color = LivroHubTheme.colors.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Você pode atualizar agora ou continuar usando esta versão e fazer isso depois.",
                        color = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateNow(current.update) }) {
                    Text("Atualizar agora")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) {
                    Text("Depois")
                }
            }
        )

        is StartupUpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text("Baixando atualização") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Baixando e validando ${current.update.versionName}…")
                }
            },
            confirmButton = {}
        )

        is StartupUpdateState.Ready -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Atualização pronta") },
            text = {
                Column {
                    Text(current.notice)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "O APK já foi baixado e validado por SHA-256.",
                        color = LivroHubTheme.colors.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::installReady) {
                    Text("Instalar atualização")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismiss) {
                    Text("Depois")
                }
            }
        )

        is StartupUpdateState.Error -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text("Não foi possível atualizar") },
            text = { Text(current.message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismiss) {
                    Text("Fechar")
                }
            }
        )
    }
}

private const val PUBLIC_TEST_PACKAGE = "com.livrohub.test"
