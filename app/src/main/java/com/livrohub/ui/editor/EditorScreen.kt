package com.livrohub.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.ui.components.LivroHubButton
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenRevision: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenImages: () -> Unit,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    settings: com.livrohub.domain.model.AppSettings,
    onUpdateSettings: ((com.livrohub.domain.model.AppSettings) -> com.livrohub.domain.model.AppSettings) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.lastSavedNotice) {
        val notice = uiState.lastSavedNotice
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            viewModel.clearSavedNotice()
        }
    }

    EditorContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenPreview = onOpenPreview,
        onOpenRevision = onOpenRevision,
        onOpenHistory = onOpenHistory,
        onOpenImages = onOpenImages,
        onTextChange = viewModel::updateText,
        onSaveVersion = viewModel::saveVersion,
        fontSizeSp = fontSizeSp,
        showLineNumbers = showLineNumbers,
        settings = settings,
        onUpdateSettings = onUpdateSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorContent(
    uiState: EditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenPreview: () -> Unit,
    onOpenRevision: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenImages: () -> Unit,
    onTextChange: (TextFieldValue) -> Unit,
    onSaveVersion: (String?) -> Unit,
    fontSizeSp: Int,
    showLineNumbers: Boolean,
    settings: com.livrohub.domain.model.AppSettings,
    onUpdateSettings: ((com.livrohub.domain.model.AppSettings) -> com.livrohub.domain.model.AppSettings) -> Unit
) {
    var saveDialogOpen by remember { mutableStateOf(false) }
    var topMenuOpen by remember { mutableStateOf(false) }

    var showTutorial by remember(settings.showEditorTutorial) { mutableStateOf(settings.showEditorTutorial) }
    var topActionsBounds by remember { mutableStateOf(Rect.Zero) }
    var editorBounds by remember { mutableStateOf(Rect.Zero) }
    var bottomBarBounds by remember { mutableStateOf(Rect.Zero) }

    val tutorialSteps = remember(topActionsBounds, editorBounds, bottomBarBounds) {
        listOf(
            com.livrohub.ui.components.TutorialStep(
                targetRect = topActionsBounds,
                title = "Recursos Extras",
                description = "Acesse suas imagens e o histórico de versões do capítulo atual."
            ),
            com.livrohub.ui.components.TutorialStep(
                targetRect = editorBounds,
                title = "Editor",
                description = "Escreva o texto do seu livro aqui. Pressione e segure uma palavra para abrir negrito, itálico e outras opções de formatação."
            ),
            com.livrohub.ui.components.TutorialStep(
                targetRect = bottomBarBounds,
                title = "Salvar Progresso",
                description = "Você pode salvar versões específicas do seu trabalho acompanhadas de um comentário para não perder seu progresso."
            )
        )
    }

    // Cache do TextStyle para evitar recriação a cada recomposição
    val editorTextStyle = remember(fontSizeSp) {
        TextStyle(fontSize = fontSizeSp.sp)
    }

    val currentEditorText = rememberUpdatedState(uiState.editorText)
    val currentOnTextChange = rememberUpdatedState(onTextChange)
    val selectionToolbar = remember {
        SelectionFormattingTextToolbar(
            onFormatRequested = { format ->
                currentOnTextChange.value(
                    applyMarkdownFormat(currentEditorText.value, format)
                )
            },
            onCustomMarkerRequested = { marker ->
                currentOnTextChange.value(
                    applyCustomMarkdownMarker(currentEditorText.value, marker)
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.chapterTitle.ifBlank { "Capítulo" },
                            style = LivroHubTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.latestSequenceNumber?.let { sequence ->
                            Text(
                                text = "Última versão: #$sequence",
                                style = LivroHubTheme.typography.bodySmall,
                                color = LivroHubTheme.colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { topMenuOpen = true },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                topActionsBounds = coordinates.boundsInRoot()
                            }
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                        }

                        DropdownMenu(
                            expanded = topMenuOpen,
                            onDismissRequest = { topMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (uiState.isSaving) "Salvando..." else "Salvar versão") },
                                enabled = !uiState.isSaving && !uiState.isLoading,
                                leadingIcon = {
                                    if (uiState.isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(Icons.Default.Save, contentDescription = null)
                                    }
                                },
                                onClick = {
                                    topMenuOpen = false
                                    saveDialogOpen = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Visão do capítulo") },
                                enabled = !uiState.isLoading,
                                leadingIcon = {
                                    Icon(Icons.Default.Visibility, contentDescription = null)
                                },
                                onClick = {
                                    topMenuOpen = false
                                    onOpenPreview()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Revisão") },
                                enabled = !uiState.isLoading,
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null)
                                },
                                onClick = {
                                    topMenuOpen = false
                                    onOpenRevision()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Imagens") },
                                enabled = !uiState.isLoading,
                                leadingIcon = {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                },
                                onClick = {
                                    topMenuOpen = false
                                    onOpenImages()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Histórico") },
                                enabled = !uiState.isLoading,
                                leadingIcon = {
                                    Icon(Icons.Default.History, contentDescription = null)
                                },
                                onClick = {
                                    topMenuOpen = false
                                    onOpenHistory()
                                }
                            )
                        }
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
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(LivroHubTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = LivroHubTheme.colors.primary
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage,
                        color = LivroHubTheme.colors.error,
                        style = LivroHubTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showLineNumbers) {
                    val lineCount = uiState.editorText.text.lines().size.coerceAtLeast(1)
                    Column(
                        modifier = Modifier
                            .padding(top = LivroHubTheme.spacing.m, end = LivroHubTheme.spacing.s)
                            .padding(vertical = LivroHubTheme.spacing.s)
                    ) {
                        for (i in 1..lineCount) {
                            Text(
                                text = i.toString(),
                                style = editorTextStyle,
                                color = LivroHubTheme.colors.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                CompositionLocalProvider(LocalTextToolbar provides selectionToolbar) {
                    OutlinedTextField(
                        value = uiState.editorText,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                            editorBounds = coordinates.boundsInRoot()
                        },
                        enabled = !uiState.isLoading,
                        label = { Text("Texto do livro") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        visualTransformation = remember { MarkdownVisualTransformation() },
                        textStyle = editorTextStyle,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LivroHubTheme.colors.primary,
                            unfocusedBorderColor = LivroHubTheme.colors.border,
                            focusedLabelColor = LivroHubTheme.colors.primary,
                            unfocusedLabelColor = LivroHubTheme.colors.onSurfaceVariant,
                            focusedTextColor = LivroHubTheme.colors.onBackground
                        )
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
                    bottomBarBounds = coordinates.boundsInRoot()
                },
                horizontalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.hasUnsavedChanges) "Alterações não salvas" else "Tudo salvo",
                    modifier = Modifier.weight(1f),
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onBackground
                )

                Text(
                    text = "${if (uiState.editorText.text.isEmpty()) 0 else uiState.editorText.text.lines().size} linhas",
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onBackground
                )
            }
            }
        }

        SelectionFormattingPopup(toolbar = selectionToolbar)
        
        if (showTutorial && topActionsBounds != Rect.Zero && editorBounds != Rect.Zero && bottomBarBounds != Rect.Zero) {
            com.livrohub.ui.components.TutorialOverlay(
                steps = tutorialSteps,
                isVisible = showTutorial,
                onFinish = { dontShowAgain ->
                    showTutorial = false
                    if (dontShowAgain) {
                        onUpdateSettings { it.copy(showEditorTutorial = false) }
                    }
                }
            )
        }
    }

    if (saveDialogOpen) {
        SaveVersionDialog(
            onDismiss = { saveDialogOpen = false },
            onConfirm = { message ->
                onSaveVersion(message)
                saveDialogOpen = false
            }
        )
    }
}

@Composable
private fun SaveVersionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salvar versão", style = LivroHubTheme.typography.titleLarge) },
        text = {
            LivroHubTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Mensagem opcional",
                singleLine = true
            )
        },
        containerColor = LivroHubTheme.colors.surface,
        titleContentColor = LivroHubTheme.colors.onSurface,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(message.trim().ifBlank { null }) }
            ) {
                Text("Salvar", color = LivroHubTheme.colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
            }
        }
    )
}

private data class SelectionMenuState(
    val rect: Rect,
    val onCopyRequested: (() -> Unit)?,
    val onPasteRequested: (() -> Unit)?,
    val onCutRequested: (() -> Unit)?,
    val onSelectAllRequested: (() -> Unit)?
)

/**
 * Substitui a toolbar padrão de seleção do Compose apenas dentro do editor.
 * O próprio TextField chama [showMenu] quando a seleção textual é aberta —
 * normalmente após o gesto de pressionar e segurar uma palavra.
 */
private class SelectionFormattingTextToolbar(
    private val onFormatRequested: (MarkdownFormat) -> Unit,
    private val onCustomMarkerRequested: (String) -> Unit
) : TextToolbar {
    var menuState by mutableStateOf<SelectionMenuState?>(null)
        private set
    var customMarkersVisible by mutableStateOf(false)
        private set

    override val status: TextToolbarStatus
        get() = if (menuState == null) TextToolbarStatus.Hidden else TextToolbarStatus.Shown

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        menuState = SelectionMenuState(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested,
            onSelectAllRequested = onSelectAllRequested
        )
    }

    override fun hide() {
        menuState = null
        customMarkersVisible = false
    }

    fun applyFormat(format: MarkdownFormat) {
        onFormatRequested(format)
    }

    fun toggleCustomMarkers() {
        customMarkersVisible = !customMarkersVisible
    }

    fun applyCustomMarker(marker: String) {
        onCustomMarkerRequested(marker)
        customMarkersVisible = false
    }

    fun copy() {
        menuState?.onCopyRequested?.invoke()
        hide()
    }

    fun paste() {
        menuState?.onPasteRequested?.invoke()
        hide()
    }

    fun cut() {
        menuState?.onCutRequested?.invoke()
        hide()
    }

    fun selectAll() {
        menuState?.onSelectAllRequested?.invoke()
    }
}

@Composable
private fun SelectionFormattingPopup(toolbar: SelectionFormattingTextToolbar) {
    val menuState = toolbar.menuState ?: return

    Popup(
        popupPositionProvider = SelectionToolbarPositionProvider(menuState.rect),
        properties = PopupProperties(focusable = false)
    ) {
        Surface(
            color = LivroHubTheme.colors.surface,
            contentColor = LivroHubTheme.colors.onSurface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Bold) },
                        contentDescription = "Negrito"
                    ) { Icon(Icons.Default.FormatBold, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Italic) },
                        contentDescription = "Itálico"
                    ) { Icon(Icons.Default.FormatItalic, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Strikethrough) },
                        contentDescription = "Riscado"
                    ) { Icon(Icons.Default.FormatStrikethrough, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Heading) },
                        contentDescription = "Título"
                    ) { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Quote) },
                        contentDescription = "Citação"
                    ) { Icon(Icons.Default.FormatQuote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }

                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Underline) },
                        contentDescription = "Sublinhado"
                    ) { Icon(Icons.Default.FormatUnderlined, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Highlight) },
                        contentDescription = "Destaque"
                    ) { Icon(Icons.Default.Highlight, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.BulletedList) },
                        contentDescription = "Lista com marcadores"
                    ) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.NumberedList) },
                        contentDescription = "Lista numerada"
                    ) { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = { toolbar.applyFormat(MarkdownFormat.Checklist) },
                        contentDescription = "Checklist"
                    ) { Icon(Icons.Default.CheckBox, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    CompactFormattingButton(
                        onClick = toolbar::toggleCustomMarkers,
                        contentDescription = "Marcador personalizado"
                    ) { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp)) }
                }

                if (toolbar.customMarkersVisible) {
                    HorizontalDivider(color = LivroHubTheme.colors.border)
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomMarkdownMarkers.forEach { marker ->
                            CompactFormattingButton(
                                onClick = { toolbar.applyCustomMarker(marker) },
                                contentDescription = "Usar marcador $marker"
                            ) {
                                Text(
                                    text = marker,
                                    style = LivroHubTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }

                if (
                    menuState.onCopyRequested != null ||
                    menuState.onPasteRequested != null ||
                    menuState.onCutRequested != null ||
                    menuState.onSelectAllRequested != null
                ) {
                    HorizontalDivider(color = LivroHubTheme.colors.border)
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (menuState.onCopyRequested != null) {
                            TextButton(onClick = toolbar::copy) { Text("Copiar") }
                        }
                        if (menuState.onCutRequested != null) {
                            TextButton(onClick = toolbar::cut) { Text("Recortar") }
                        }
                        if (menuState.onPasteRequested != null) {
                            TextButton(onClick = toolbar::paste) { Text("Colar") }
                        }
                        if (menuState.onSelectAllRequested != null) {
                            TextButton(onClick = toolbar::selectAll) { Text("Tudo") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFormattingButton(
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(
                onClickLabel = contentDescription,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

private class SelectionToolbarPositionProvider(
    private val selectionRect: Rect
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = 8
        val gap = 12
        val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
        val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
        val x = (selectionRect.center.x.toInt() - popupContentSize.width / 2)
            .coerceIn(margin, maxX)
        val aboveSelection = selectionRect.top.toInt() - popupContentSize.height - gap
        val belowSelection = selectionRect.bottom.toInt() + gap
        val y = (if (aboveSelection >= margin) aboveSelection else belowSelection)
            .coerceIn(margin, maxY)

        return IntOffset(x, y)
    }
}
