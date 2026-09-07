package com.livrohub.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livrohub.R
import com.livrohub.data.archive.BookArchiveManager
import com.livrohub.domain.model.BookWithStats
import com.livrohub.ui.components.LivroHubButton
import com.livrohub.ui.components.LivroHubCard
import com.livrohub.ui.components.LivroHubHeader
import com.livrohub.ui.components.LivroHubTextField
import com.livrohub.ui.theme.LivroHubTheme
import java.text.DateFormat
import java.util.Date

/**
 * Tela da Biblioteca (Library).
 *
 * Exibe a lista de todos os livros (manuscritos) criados pelo usuário, junto
 * com estatísticas agregadas (número de capítulos, linhas, caracteres).
 * Permite criar, renomear, excluir e abrir livros.
 *
 * @param viewModel ViewModel responsável pelo gerenciamento da biblioteca.
 * @param onOpenBook Callback acionado ao selecionar um livro para leitura/edição.
 * @param onOpenSettings Callback acionado para abrir as configurações.
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenBook: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingExportBookId by rememberSaveable { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BookArchiveManager.MIME_TYPE)
    ) { uri ->
        val bookId = pendingExportBookId
        pendingExportBookId = null
        if (uri != null && bookId != null) {
            viewModel.exportBook(bookId, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBook(uri)
        }
    }

    LaunchedEffect(uiState.archiveMessage) {
        uiState.archiveMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearArchiveMessage()
        }
    }

    LibraryContent(
        uiState = uiState,
        onOpenBook = onOpenBook,
        onCreateBook = viewModel::createBook,
        onRenameBook = viewModel::renameBook,
        onDeleteBook = viewModel::deleteBook,
        onOpenSettings = onOpenSettings,
        onImportBook = { importLauncher.launch(arrayOf("*/*")) },
        onExportBook = { book ->
            pendingExportBookId = book.book.id
            exportLauncher.launch(BookArchiveManager.suggestedFileName(book.book.title))
        },
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onOpenBook: (Long) -> Unit,
    onCreateBook: (String) -> Unit,
    onRenameBook: (Long, String) -> Unit,
    onDeleteBook: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onImportBook: () -> Unit,
    onExportBook: (BookWithStats) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    var createDialogOpen by remember { mutableStateOf(false) }
    var bookToRename by remember { mutableStateOf<BookWithStats?>(null) }
    var bookToDelete by remember { mutableStateOf<BookWithStats?>(null) }
    var appMenuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.app_name), style = LivroHubTheme.typography.titleLarge) },
                actions = {
                    Box {
                        IconButton(onClick = { appMenuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opções da biblioteca")
                        }
                        DropdownMenu(
                            expanded = appMenuOpen,
                            onDismissRequest = { appMenuOpen = false },
                            modifier = Modifier.background(LivroHubTheme.colors.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Importar livro", color = LivroHubTheme.colors.onSurface) },
                                enabled = !uiState.isArchiveBusy,
                                onClick = {
                                    appMenuOpen = false
                                    onImportBook()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Configurações", color = LivroHubTheme.colors.onSurface) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = LivroHubTheme.colors.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    appMenuOpen = false
                                    onOpenSettings()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LivroHubTheme.colors.background,
                    titleContentColor = LivroHubTheme.colors.onBackground,
                    actionIconContentColor = LivroHubTheme.colors.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!uiState.isArchiveBusy) createDialogOpen = true
                },
                containerColor = LivroHubTheme.colors.accent,
                contentColor = LivroHubTheme.colors.onAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "Criar livro")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        style = LivroHubTheme.typography.bodyLarge,
                        color = LivroHubTheme.colors.error
                    )
                }

                uiState.books.isEmpty() -> {
                    EmptyLibrary(
                        onCreateClick = { createDialogOpen = true },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m),
                        contentPadding = PaddingValues(LivroHubTheme.spacing.m)
                    ) {
                        items(
                            items = uiState.books,
                            key = { statBook -> statBook.book.id }
                        ) { statBook ->
                            BookRow(
                                bookWithStats = statBook,
                                onOpen = { onOpenBook(statBook.book.id) },
                                onExport = { onExportBook(statBook) },
                                onRename = { bookToRename = statBook },
                                onDelete = { bookToDelete = statBook },
                                archiveBusy = uiState.isArchiveBusy
                            )
                        }
                    }
                }
            }

            if (uiState.isArchiveBusy) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = LivroHubTheme.colors.primary
                )
            }
        }
    }

    if (createDialogOpen) {
        BookTitleDialog(
            title = "Novo livro",
            confirmText = "Criar",
            initialTitle = "",
            onDismiss = { createDialogOpen = false },
            onConfirm = { title ->
                onCreateBook(title)
                createDialogOpen = false
            }
        )
    }

    bookToRename?.let { statBook ->
        BookTitleDialog(
            title = "Renomear livro",
            confirmText = "Salvar",
            initialTitle = statBook.book.title,
            onDismiss = { bookToRename = null },
            onConfirm = { title ->
                onRenameBook(statBook.book.id, title)
                bookToRename = null
            }
        )
    }

    bookToDelete?.let { statBook ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Excluir livro", style = LivroHubTheme.typography.titleLarge) },
            text = {
                Text(
                    text = "Excluir \"${statBook.book.title}\" também remove todas as versões salvas deste livro.",
                    style = LivroHubTheme.typography.bodyMedium
                )
            },
            containerColor = LivroHubTheme.colors.surface,
            titleContentColor = LivroHubTheme.colors.onSurface,
            textContentColor = LivroHubTheme.colors.onSurfaceVariant,
            confirmButton = {
                TextButton(onClick = {
                    onDeleteBook(statBook.book.id)
                    bookToDelete = null
                }) {
                    Text("Excluir", color = LivroHubTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
private fun EmptyLibrary(
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(LivroHubTheme.spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.m)
    ) {
        LivroHubHeader(
            title = "Nenhum livro ainda",
            subtitle = "Crie seu primeiro manuscrito para começar a versionar o texto.",
            modifier = Modifier.padding(bottom = LivroHubTheme.spacing.m)
        )
        LivroHubButton(
            text = "Criar livro",
            onClick = onCreateClick,
            isPrimary = true
        )
    }
}

@Composable
private fun BookRow(
    bookWithStats: BookWithStats,
    onOpen: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    archiveBusy: Boolean
) {
    var menuOpen by remember { mutableStateOf(false) }
    val formattedDate = remember(bookWithStats.book.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(bookWithStats.book.createdAt))
    }

    LivroHubCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LivroHubTheme.spacing.xs)
            ) {
                Text(
                    text = bookWithStats.book.title,
                    style = LivroHubTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = LivroHubTheme.colors.onSurface
                )
                Text(
                    text = "Criado em $formattedDate",
                    style = LivroHubTheme.typography.bodySmall,
                    color = LivroHubTheme.colors.onSurfaceVariant
                )
                Text(
                    text = "${bookWithStats.totalChapters} capítulos • ${bookWithStats.totalLines} linhas • ${bookWithStats.totalChars} caracteres",
                    style = LivroHubTheme.typography.labelMedium,
                    color = LivroHubTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.width(LivroHubTheme.spacing.s))

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Default.MoreVert,
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
                        text = { Text("Exportar livro (.zip)", color = LivroHubTheme.colors.onSurface) },
                        enabled = !archiveBusy,
                        onClick = {
                            menuOpen = false
                            onExport()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Renomear", color = LivroHubTheme.colors.onSurface) },
                        enabled = !archiveBusy,
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = LivroHubTheme.colors.onSurfaceVariant) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = LivroHubTheme.colors.error) },
                        enabled = !archiveBusy,
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LivroHubTheme.colors.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookTitleDialog(
    title: String,
    confirmText: String,
    initialTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(initialTitle) { mutableStateOf(initialTitle) }
    val canConfirm = text.trim().isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = LivroHubTheme.typography.titleLarge) },
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
                enabled = canConfirm
            ) {
                Text(confirmText, color = if (canConfirm) LivroHubTheme.colors.primary else LivroHubTheme.colors.onSurfaceVariant)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LivroHubTheme.colors.onSurfaceVariant)
            }
        }
    )
}
