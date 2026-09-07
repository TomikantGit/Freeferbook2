package com.livrohub.ui.settings.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.livrohub.data.update.TestUpdateCheckResult
import com.livrohub.data.update.TestUpdateInfo
import com.livrohub.data.update.TestUpdateInstallResult
import com.livrohub.data.update.TestUpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface TestUpdateUiState {
    data object Idle : TestUpdateUiState
    data object Checking : TestUpdateUiState
    data object UpToDate : TestUpdateUiState
    data class Available(val update: TestUpdateInfo) : TestUpdateUiState
    data class Downloading(val update: TestUpdateInfo) : TestUpdateUiState
    data class Ready(val update: TestUpdateInfo, val apkFile: File) : TestUpdateUiState
    data class Error(val message: String) : TestUpdateUiState
}

data class TestUpdateUiModel(
    val state: TestUpdateUiState = TestUpdateUiState.Idle,
    val installNotice: String? = null
)

/**
 * Orquestra o fluxo de atualização de teste fora da camada Compose.
 *
 * O ViewModel preserva downloads/estado entre mudanças de configuração e deixa
 * [TestUpdateSection] responsável apenas por renderização e eventos de UI.
 */
class TestUpdateViewModel(
    private val manager: TestUpdateManager
) : ViewModel() {

    private val _uiModel = MutableStateFlow(TestUpdateUiModel())
    val uiModel: StateFlow<TestUpdateUiModel> = _uiModel.asStateFlow()

    val currentVersionName: String
        get() = manager.currentVersionName

    val currentVersionCode: Int
        get() = manager.currentVersionCode

    fun checkForUpdate() {
        if (_uiModel.value.state is TestUpdateUiState.Checking ||
            _uiModel.value.state is TestUpdateUiState.Downloading
        ) {
            return
        }

        _uiModel.value = TestUpdateUiModel(state = TestUpdateUiState.Checking)
        viewModelScope.launch {
            val nextState = when (val result = manager.checkForUpdate()) {
                is TestUpdateCheckResult.Available -> TestUpdateUiState.Available(result.update)
                TestUpdateCheckResult.UpToDate -> TestUpdateUiState.UpToDate
                is TestUpdateCheckResult.Error -> TestUpdateUiState.Error(result.message)
            }
            _uiModel.update { it.copy(state = nextState) }
        }
    }

    fun downloadAndInstall(update: TestUpdateInfo) {
        if (_uiModel.value.state is TestUpdateUiState.Downloading) return

        _uiModel.value = TestUpdateUiModel(
            state = TestUpdateUiState.Downloading(update)
        )
        viewModelScope.launch {
            manager.downloadUpdate(update)
                .onSuccess { apkFile ->
                    _uiModel.update {
                        it.copy(state = TestUpdateUiState.Ready(update, apkFile))
                    }
                    requestInstall(apkFile)
                }
                .onFailure { error ->
                    _uiModel.update {
                        it.copy(
                            state = TestUpdateUiState.Error(
                                error.message ?: "Não foi possível baixar a atualização."
                            )
                        )
                    }
                }
        }
    }

    fun installReadyUpdate() {
        val ready = _uiModel.value.state as? TestUpdateUiState.Ready ?: return
        requestInstall(ready.apkFile)
    }

    fun openGitHubRelease() = manager.openGitHubRelease()

    private fun requestInstall(apkFile: File) {
        val notice = when (val result = manager.requestInstall(apkFile)) {
            TestUpdateInstallResult.InstallerOpened ->
                "Instalador aberto. Confirme a atualização no Android."

            TestUpdateInstallResult.PermissionRequired ->
                "Autorize o Freeferbook a instalar apps desta fonte. Ao voltar, toque em Instalar atualização."

            is TestUpdateInstallResult.Error -> result.message
        }

        _uiModel.update { it.copy(installNotice = notice) }
    }
}
