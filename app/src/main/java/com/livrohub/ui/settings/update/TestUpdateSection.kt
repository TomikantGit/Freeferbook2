package com.livrohub.ui.settings.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livrohub.data.update.TestUpdateManager
import com.livrohub.ui.common.viewModelFactory
import com.livrohub.ui.theme.LivroHubTheme

/** Seção isolada de atualização do canal público de testes. */
@Composable
fun TestUpdateSection() {
    val context = LocalContext.current
    val viewModel: TestUpdateViewModel = viewModel(
        key = "test-update",
        factory = viewModelFactory {
            TestUpdateViewModel(TestUpdateManager(context.applicationContext))
        }
    )
    val uiModel by viewModel.uiModel.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Versão instalada",
            style = LivroHubTheme.typography.bodyLarge,
            color = LivroHubTheme.colors.onBackground
        )
        Text(
            text = "${viewModel.currentVersionName} (código ${viewModel.currentVersionCode})",
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))
        UpdateStatus(uiModel.state)

        uiModel.installNotice?.let { notice ->
            Text(
                text = notice,
                style = LivroHubTheme.typography.bodySmall,
                color = LivroHubTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        UpdateAction(
            state = uiModel.state,
            onCheck = viewModel::checkForUpdate,
            onDownload = viewModel::downloadAndInstall,
            onInstall = viewModel::installReadyUpdate
        )

        if (uiModel.state is TestUpdateUiState.Error) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = viewModel::openGitHubRelease) {
                Text("Abrir release no GitHub")
            }
        }
    }
}

@Composable
private fun UpdateStatus(state: TestUpdateUiState) {
    when (state) {
        TestUpdateUiState.Idle -> Text(
            text = "Verifique manualmente o APK de teste mais recente publicado pelo GitHub Actions.",
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.onSurfaceVariant
        )

        TestUpdateUiState.Checking -> ProgressStatus("Verificando atualização...")

        TestUpdateUiState.UpToDate -> Text(
            text = "Você já está usando a versão de teste mais recente.",
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.onSurfaceVariant
        )

        is TestUpdateUiState.Available -> {
            Text(
                text = "Nova versão: ${state.update.versionName} (código ${state.update.versionCode})",
                style = LivroHubTheme.typography.bodyMedium,
                color = LivroHubTheme.colors.primary,
                fontWeight = FontWeight.Bold
            )
            if (state.update.notes.isNotBlank()) {
                Text(
                    text = state.update.notes,
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        is TestUpdateUiState.Downloading -> ProgressStatus(
            "Baixando ${state.update.versionName}..."
        )

        is TestUpdateUiState.Ready -> Text(
            text = "${state.update.versionName} foi baixada e validada por SHA-256.",
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.onSurfaceVariant
        )

        is TestUpdateUiState.Error -> Text(
            text = state.message,
            style = LivroHubTheme.typography.bodySmall,
            color = LivroHubTheme.colors.error
        )
    }
}

@Composable
private fun ProgressStatus(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text, style = LivroHubTheme.typography.bodySmall)
    }
}

@Composable
private fun UpdateAction(
    state: TestUpdateUiState,
    onCheck: () -> Unit,
    onDownload: (com.livrohub.data.update.TestUpdateInfo) -> Unit,
    onInstall: () -> Unit
) {
    when (state) {
        TestUpdateUiState.Checking,
        is TestUpdateUiState.Downloading -> Button(onClick = {}, enabled = false) {
            Text(if (state is TestUpdateUiState.Downloading) "Baixando..." else "Verificando...")
        }

        is TestUpdateUiState.Available -> Button(
            onClick = { onDownload(state.update) }
        ) {
            Text("Baixar e instalar")
        }

        is TestUpdateUiState.Ready -> Button(onClick = onInstall) {
            Text("Instalar atualização")
        }

        else -> Button(onClick = onCheck) {
            Text("Verificar atualização")
        }
    }
}
