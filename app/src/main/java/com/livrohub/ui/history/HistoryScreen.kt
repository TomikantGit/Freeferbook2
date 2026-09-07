package com.livrohub.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.domain.model.ChapterVersion
import com.livrohub.ui.diff.StaticDiffRequest
import com.livrohub.ui.components.LivroHubButton
import com.livrohub.ui.components.LivroHubCard
import com.livrohub.ui.theme.LivroHubTheme
import java.text.DateFormat
import java.util.Date

private data class ExportRequest(
    val fileName: String,
    val content: String
)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onCompareVersions: (StaticDiffRequest) -> Unit,
    settings: com.livrohub.domain.model.AppSettings,
    onUpdateSettings: ((com.livrohub.domain.model.AppSettings) -> com.livrohub.domain.model.AppSettings) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<ExportRequest?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val export = pendingExport
        pendingExport = null
        if (uri != null && export != null) {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(export.content)
            }
        }
    }

    HistoryContent(
        uiState = uiState,
        onBack = onBack,
        onRestoreVersion = viewModel::restoreVersion,
        onExportVersion = { version, extension ->
            val safeTitle = uiState.bookTitle.ifBlank { "livro" }
                .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                .trim('_')
                .ifBlank { "livro" }
            pendingExport = ExportRequest(
                fileName = "$safeTitle-v${version.sequenceNumber}.$extension",
                content = version.content
            )
            exportLauncher.launch(pendingExport!!.fileName)
        },
        onCompareVersions = onCompareVersions,
        settings = settings,
        onUpdateSettings = onUpdateSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onRestoreVersion: (ChapterVersion) -> Unit,
    onExportVersion: (ChapterVersion, String) -> Unit,
    onCompareVersions: (StaticDiffRequest) -> Unit,
    settings: com.livrohub.domain.model.AppSettings,
    onUpdateSettings: ((com.livrohub.domain.model.AppSettings) -> com.livrohub.domain.model.AppSettings) -> Unit
) {
    var versionToRestore by remember { mutableStateOf<ChapterVersion?>(null) }
    var compareBaseVersion by remember { mutableStateOf<ChapterVersion?>(null) }

    var showTutorial by remember(settings.showHistoryTutorial) { mutableStateOf(settings.showHistoryTutorial) }
    var firstCardBounds by remember { mutableStateOf(Rect.Zero) }
    var optionsMenuBounds by remember { mutableStateOf(Rect.Zero) }

    val tutorialSteps = remember(firstCardBounds, optionsMenuBounds) {
        listOf(
            com.livrohub.ui.components.TutorialStep(
                targetRect = firstCardBounds,
                title = "Histórico de Versões",
                description = "Aqui ficam todas as versões salvas deste capítulo. As mais recentes aparecem no topo."
            ),
            com.livrohub.ui.components.TutorialStep(
                targetRect = optionsMenuBounds,
                title = "Opções da Versão",
                description = "Toque nos três pontos para restaurar uma versão antiga, exportar o texto ou compará-la com outra versão."
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    if (compareBaseVersion != null) {
                        Text(text = "Selecione para comparar", style = LivroHubTheme.typography.titleLarge)
                    } else {
                        Column {
                            Text(text = "Histórico", style = LivroHubTheme.typography.titleLarge)
                            Text(
                                text = uiState.bookTitle.ifBlank { "Livro" },
                                style = LivroHubTheme.typography.bodySmall,
                                color = LivroHubTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (compareBaseVersion != null) {
                            compareBaseVersion = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            imageVector = if (compareBaseVersion != null) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (compareBaseVersion != null) "Cancelar comparação" else "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground,
                    navigationIconContentColor = LivroHubTheme.colors.onBackground,
                    actionIconContentColor = LivroHubTheme.colors.onBackground
                )
            )
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = LivroHubTheme.colors.primary
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(LivroHubTheme.spacing.l),
                        color = LivroHubTheme.colors.error,
                        style = LivroHubTheme.typography.bodyLarge
                    )
                }

                uiState.versions.isEmpty() -> {
                    Text(
                        text = "Nenhuma versão salva.",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(LivroHubTheme.spacing.l),
                        style = LivroHubTheme.typography.bodyLarge,
                        color = LivroHubTheme.colors.onBackground
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m),
                        contentPadding = PaddingValues(LivroHubTheme.spacing.m)
                    ) {
                        itemsIndexed(
                            items = uiState.versions,
                            key = { _, version -> version.id }
                        ) { index, version ->
                            val isSelectingCompareTarget = compareBaseVersion != null
                            val isBaseVersion = compareBaseVersion?.id == version.id
                            
                            VersionRow(
                                version = version,
                                isSelectingCompareTarget = isSelectingCompareTarget,
                                isBaseVersion = isBaseVersion,
                                isFirstItem = index == 0,
                                onBoundsPositioned = { cardBounds, menuBounds ->
                                    if (index == 0) {
                                        firstCardBounds = cardBounds
                                        optionsMenuBounds = menuBounds
                                    }
                                },
                                onRestore = { versionToRestore = version },
                                onExport = { extension -> onExportVersion(version, extension) },
                                onInitiateCompare = {
                                    compareBaseVersion = version
                                },
                                onSelectForCompare = {
                                    compareBaseVersion?.let { base ->
                                        val oldVersion = if (base.sequenceNumber < version.sequenceNumber) base else version
                                        val newVersion = if (base.sequenceNumber < version.sequenceNumber) version else base
                                        onCompareVersions(
                                            StaticDiffRequest(
                                                title = uiState.bookTitle,
                                                oldLabel = "Versão #${oldVersion.sequenceNumber}",
                                                newLabel = "Versão #${newVersion.sequenceNumber}",
                                                oldContent = oldVersion.content,
                                                newContent = newVersion.content
                                            )
                                        )
                                        compareBaseVersion = null
                                    }
                                }
                            )
                        }
                    }
                }
            }

            }
        }

        if (showTutorial && firstCardBounds != Rect.Zero && optionsMenuBounds != Rect.Zero && uiState.versions.isNotEmpty()) {
            com.livrohub.ui.components.TutorialOverlay(
                steps = tutorialSteps,
                isVisible = showTutorial,
                onFinish = { dontShowAgain ->
                    showTutorial = false
                    if (dontShowAgain) {
                        onUpdateSettings { it.copy(showHistoryTutorial = false) }
                    }
                }
            )
        }
    }

    versionToRestore?.let { version ->
        AlertDialog(
            onDismissRequest = { versionToRestore = null },
            title = { Text("Restaurar versão", style = LivroHubTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Restaurar a versão #${version.sequenceNumber} criará uma nova versão com o mesmo conteúdo. O histórico atual será preservado.",
                    style = LivroHubTheme.typography.bodyMedium
                )
            },
            containerColor = LivroHubTheme.colors.surface,
            titleContentColor = LivroHubTheme.colors.onSurface,
            textContentColor = LivroHubTheme.colors.onSurfaceVariant,
            confirmButton = {
                TextButton(
                    onClick = {
                        onRestoreVersion(version)
                        versionToRestore = null
                    }
                ) {
                    Text("Restaurar", color = LivroHubTheme.colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { versionToRestore = null }) {
                    Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun VersionRow(
    version: ChapterVersion,
    isSelectingCompareTarget: Boolean,
    isBaseVersion: Boolean,
    isFirstItem: Boolean = false,
    onBoundsPositioned: (Rect, Rect) -> Unit = { _, _ -> },
    onRestore: () -> Unit,
    onExport: (String) -> Unit,
    onInitiateCompare: () -> Unit,
    onSelectForCompare: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formattedDate = remember(version.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(version.createdAt))
    }

    val cardModifier = if (isSelectingCompareTarget && !isBaseVersion) {
        Modifier.fillMaxWidth().clickable { onSelectForCompare() }
    } else {
        Modifier.fillMaxWidth()
    }
    
    var cardRect by remember { mutableStateOf(Rect.Zero) }
    var menuRect by remember { mutableStateOf(Rect.Zero) }

    val finalCardModifier = if (isFirstItem) {
        cardModifier.onGloballyPositioned {
            cardRect = it.boundsInRoot()
            onBoundsPositioned(cardRect, menuRect)
        }
    } else cardModifier

    LivroHubCard(
        modifier = finalCardModifier,
        onClick = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isBaseVersion) LivroHubTheme.colors.primary.copy(alpha = 0.1f) else LivroHubTheme.colors.surface)
                .padding(LivroHubTheme.spacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.xs)
            ) {
                Text(
                    text = "Versão #${version.sequenceNumber}",
                    style = LivroHubTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LivroHubTheme.colors.onSurface
                )
                Text(
                    text = formattedDate,
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
                Text(
                    text = version.message ?: "Sem mensagem",
                    style = LivroHubTheme.typography.bodyMedium,
                    color = LivroHubTheme.colors.onSurface
                )
            }

            Spacer(modifier = Modifier.width(LivroHubTheme.spacing.m))

            if (!isSelectingCompareTarget) {
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = if (isFirstItem) Modifier.onGloballyPositioned {
                            menuRect = it.boundsInRoot()
                            onBoundsPositioned(cardRect, menuRect)
                        } else Modifier
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opções",
                            tint = LivroHubTheme.colors.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        modifier = Modifier.background(LivroHubTheme.colors.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Restaurar", color = LivroHubTheme.colors.onSurface) },
                            onClick = {
                                menuOpen = false
                                onRestore()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar .txt", color = LivroHubTheme.colors.onSurface) },
                            onClick = {
                                menuOpen = false
                                onExport("txt")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Exportar .md", color = LivroHubTheme.colors.onSurface) },
                            onClick = {
                                menuOpen = false
                                onExport("md")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Comparar com outra versão", color = LivroHubTheme.colors.onSurface) },
                            onClick = {
                                menuOpen = false
                                onInitiateCompare()
                            }
                        )
                    }
                }
            }
        }
    }
}
