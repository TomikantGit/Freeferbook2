package com.livrohub.ui.chapters

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.domain.model.Chapter
import com.livrohub.ui.components.LivroHubCard
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme
import com.livrohub.utils.EpubExporter
import com.livrohub.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun ChaptersScreen(
    viewModel: ChaptersViewModel,
    onOpenChapter: (Long) -> Unit,
    onFabBoundsPositioned: (androidx.compose.ui.geometry.Rect) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var createDialogOpen by remember { mutableStateOf(false) }
    var chapterToRename by remember { mutableStateOf<Chapter?>(null) }
    var chapterToDelete by remember { mutableStateOf<Chapter?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var chapterToExportPdf by remember { mutableStateOf<Chapter?>(null) }
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val chapter = chapterToExportPdf
        chapterToExportPdf = null
        if (uri != null && chapter != null) {
            coroutineScope.launch {
                val content = viewModel.getChapterContent(chapter.id)
                PdfExporter.exportToPdf(context, uri, chapter.title, content)
            }
        }
    }

    var chapterToExportEpub by remember { mutableStateOf<Chapter?>(null) }
    val epubExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/epub+zip")
    ) { uri ->
        val chapter = chapterToExportEpub
        chapterToExportEpub = null
        if (uri != null && chapter != null) {
            coroutineScope.launch {
                val content = viewModel.getChapterContent(chapter.id)
                EpubExporter.exportToEpub(context, uri, chapter.title, content)
            }
        }
    }

    var chapterToPreview by remember { mutableStateOf<Chapter?>(null) }
    var previewContent by remember { mutableStateOf<androidx.compose.ui.text.AnnotatedString?>(null) }

    LaunchedEffect(chapterToPreview) {
        val chapter = chapterToPreview
        if (chapter != null) {
            val content = viewModel.getChapterContent(chapter.id)
            previewContent = com.livrohub.utils.MarkdownParser.parseToAnnotatedString(content)
        } else {
            previewContent = null
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { createDialogOpen = true },
                containerColor = LivroHubTheme.colors.accent,
                contentColor = LivroHubTheme.colors.onAccent,
                modifier = Modifier.onGloballyPositioned { onFabBoundsPositioned(it.boundsInRoot()) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Capítulo")
            }
        },
        containerColor = LivroHubTheme.colors.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
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
                        text = uiState.errorMessage!!,
                        modifier = Modifier.align(Alignment.Center),
                        color = LivroHubTheme.colors.error,
                        style = LivroHubTheme.typography.bodyLarge
                    )
                }
                uiState.chapters.isEmpty() -> {
                    Text(
                        text = "Nenhum capítulo criado.",
                        modifier = Modifier.align(Alignment.Center),
                        color = LivroHubTheme.colors.onBackground,
                        style = LivroHubTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(LivroHubTheme.spacing.m),
                        verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.s)
                    ) {
                        items(uiState.chapters, key = { it.id }) { chapter ->
                            ChapterRow(
                                chapter = chapter,
                                onOpen = { onOpenChapter(chapter.id) },
                                onPreview = { chapterToPreview = chapter },
                                onRename = { chapterToRename = chapter },
                                onDelete = { chapterToDelete = chapter },
                                onExportPdf = {
                                    chapterToExportPdf = chapter
                                    val safeTitle = chapter.title.replace(Regex("[^A-Za-z0-9_-]+"), "_")
                                    pdfExportLauncher.launch("${safeTitle}.pdf")
                                },
                                onExportEpub = {
                                    chapterToExportEpub = chapter
                                    val safeTitle = chapter.title.replace(Regex("[^A-Za-z0-9_-]+"), "_")
                                    epubExportLauncher.launch("${safeTitle}.epub")
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (createDialogOpen) {
        ChapterTitleDialog(
            title = "Novo Capítulo",
            initialTitle = "",
            confirmText = "Criar",
            onDismiss = { createDialogOpen = false },
            onConfirm = { 
                viewModel.createChapter(it)
                createDialogOpen = false
            }
        )
    }

    chapterToRename?.let { chapter ->
        ChapterTitleDialog(
            title = "Renomear",
            initialTitle = chapter.title,
            confirmText = "Salvar",
            onDismiss = { chapterToRename = null },
            onConfirm = { 
                viewModel.renameChapter(chapter.id, it)
                chapterToRename = null
            }
        )
    }

    chapterToDelete?.let { chapter ->
        AlertDialog(
            onDismissRequest = { chapterToDelete = null },
            title = { Text("Excluir Capítulo", style = LivroHubTheme.typography.titleLarge) },
            text = { Text("Deseja mesmo excluir '${chapter.title}'?", style = LivroHubTheme.typography.bodyMedium) },
            containerColor = LivroHubTheme.colors.surface,
            titleContentColor = LivroHubTheme.colors.onSurface,
            textContentColor = LivroHubTheme.colors.onSurfaceVariant,
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteChapter(chapter.id)
                    chapterToDelete = null
                }) { Text("Excluir", color = LivroHubTheme.colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { chapterToDelete = null }) { 
                    Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant) 
                }
            }
        )
    }

    if (chapterToPreview != null && previewContent != null) {
        AlertDialog(
            onDismissRequest = { chapterToPreview = null },
            title = { Text(chapterToPreview!!.title, style = LivroHubTheme.typography.titleLarge) },
            text = {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = previewContent!!,
                        style = LivroHubTheme.typography.bodyMedium,
                        color = LivroHubTheme.colors.onSurface
                    )
                }
            },
            containerColor = LivroHubTheme.colors.surface,
            titleContentColor = LivroHubTheme.colors.onSurface,
            confirmButton = {
                TextButton(onClick = { chapterToPreview = null }) {
                    Text("Fechar", color = LivroHubTheme.colors.primary)
                }
            }
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit,
    onExportEpub: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    LivroHubCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    style = LivroHubTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LivroHubTheme.colors.onSurface
                )
            }
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "Visualizar", tint = LivroHubTheme.colors.onSurfaceVariant)
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções", tint = LivroHubTheme.colors.onSurfaceVariant)
                    }
                }
                DropdownMenu(
                    expanded = menuOpen, 
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.background(LivroHubTheme.colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Exportar PDF", color = LivroHubTheme.colors.onSurface) },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, tint = LivroHubTheme.colors.onSurfaceVariant) },
                        onClick = { menuOpen = false; onExportPdf() }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar EPUB", color = LivroHubTheme.colors.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Book, null, tint = LivroHubTheme.colors.onSurfaceVariant) },
                        onClick = { menuOpen = false; onExportEpub() }
                    )
                    DropdownMenuItem(
                        text = { Text("Renomear", color = LivroHubTheme.colors.onSurface) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = LivroHubTheme.colors.onSurfaceVariant) },
                        onClick = { menuOpen = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = LivroHubTheme.colors.error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = LivroHubTheme.colors.error) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterTitleDialog(
    title: String,
    initialTitle: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialTitle) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = LivroHubTheme.typography.titleLarge) },
        text = {
            LivroHubTextField(
                value = text,
                onValueChange = { text = it },
                label = "Título",
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        containerColor = LivroHubTheme.colors.surface,
        titleContentColor = LivroHubTheme.colors.onSurface,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) }, 
                enabled = text.isNotBlank()
            ) {
                Text(confirmText, color = if (text.isNotBlank()) LivroHubTheme.colors.primary else LivroHubTheme.colors.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { 
                Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant) 
            }
        }
    )
}
