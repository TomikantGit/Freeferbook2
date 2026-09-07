package com.livrohub.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livrohub.domain.model.AppSettings
import com.livrohub.domain.model.AppTheme
import com.livrohub.domain.repository.BookRepository
import com.livrohub.domain.repository.ChapterRepository
import com.livrohub.domain.repository.CharacterRepository
import com.livrohub.domain.repository.ImageRepository
import com.livrohub.domain.repository.LocationRepository
import com.livrohub.domain.repository.SettingsRepository
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.livrohub.ui.chapters.ChaptersViewModel
import com.livrohub.ui.chapters.ChaptersViewModelFactory
import com.livrohub.ui.characters.CharacterViewModel
import com.livrohub.ui.characters.CharacterViewModelFactory
import com.livrohub.ui.diff.DiffScreen
import com.livrohub.ui.diff.DiffViewModel
import com.livrohub.ui.diff.DiffViewModelFactory
import com.livrohub.ui.diff.StaticDiffRequest
import com.livrohub.ui.diff.StaticDiffScreen
import com.livrohub.ui.editor.EditorScreen
import com.livrohub.ui.editor.EditorViewModel
import com.livrohub.ui.editor.EditorViewModelFactory
import com.livrohub.ui.history.HistoryScreen
import com.livrohub.ui.history.HistoryViewModel
import com.livrohub.ui.history.HistoryViewModelFactory
import com.livrohub.ui.home.HomeScreen
import com.livrohub.ui.library.LibraryScreen
import com.livrohub.ui.library.LibraryViewModel
import com.livrohub.ui.library.LibraryViewModelFactory
import com.livrohub.ui.locations.LocationViewModel
import com.livrohub.ui.locations.LocationViewModelFactory
import com.livrohub.ui.images.ImagesViewModel
import com.livrohub.ui.images.ImagesViewModelFactory
import com.livrohub.ui.revision.RevisionScreen
import com.livrohub.ui.preview.ChapterPreviewScreen
import com.livrohub.ui.settings.SettingsScreen
import com.livrohub.ui.settings.SettingsViewModel
import com.livrohub.ui.settings.SettingsViewModelFactory
import com.livrohub.ui.workspace.BookWorkspaceScreen

/**
 * Rotas de navegação de primeiro nível do app.
 *
 * A navegação é gerenciada por estado Compose simples (sem Navigation Component),
 * adequada para o número atual de telas do app.
 */
enum class AppRoute {
    /** Tela inicial com botões de acesso à biblioteca e configurações. */
    HOME,
    /** Biblioteca de livros. */
    LIBRARY,
    /** Tela de configurações do app. */
    SETTINGS,
    /** Workspace de um livro: capítulos, personagens, editor, diff, histórico. */
    BOOK_WORKSPACE
}

/**
 * Composable raiz do LivroHub.
 *
 * Responsável por:
 * - Aplicar o tema (light/dark) baseado nas preferências do usuário
 * - Gerenciar a navegação entre telas via estado Compose
 * - Instanciar ViewModels com suas factories apropriadas
 *
 * @param bookRepository Repositório de livros.
 * @param chapterRepository Repositório de capítulos e versões.
 * @param settingsRepository Repositório de preferências do app.
 * @param characterRepository Repositório de personagens.
 */
@Composable
fun LivroHubAppRoot(
    bookRepository: BookRepository,
    chapterRepository: ChapterRepository,
    settingsRepository: SettingsRepository,
    characterRepository: CharacterRepository,
    imageRepository: ImageRepository,
    locationRepository: LocationRepository
) {
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )

    val isSystemDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    com.livrohub.ui.theme.LivroHubTheme(
        settings = settings,
        isSystemDark = isSystemDark
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = com.livrohub.ui.theme.LivroHubTheme.colors.background
        ) {
            var currentRoute by rememberSaveable { mutableStateOf(AppRoute.HOME) }
            var selectedBookId by rememberSaveable { mutableStateOf<Long?>(null) }
            var selectedChapterId by rememberSaveable { mutableStateOf<Long?>(null) }
            var diffCurrentContent by rememberSaveable { mutableStateOf<String?>(null) }
            var historyOpen by rememberSaveable { mutableStateOf(false) }
            var imagesOpen by rememberSaveable { mutableStateOf(false) }
            var revisionOpen by rememberSaveable { mutableStateOf(false) }
            var previewOpen by rememberSaveable { mutableStateOf(false) }
            var staticDiffRequest by remember { mutableStateOf<StaticDiffRequest?>(null) }

            val bookId = selectedBookId
            val chapterId = selectedChapterId

            androidx.compose.animation.AnimatedContent(
                targetState = currentRoute,
                label = "AppRouteTransition"
            ) { targetRoute ->
                when (targetRoute) {
                    AppRoute.HOME -> {
                        HomeScreen(
                            onNavigateToLibrary = { currentRoute = AppRoute.LIBRARY },
                            onNavigateToSettings = { currentRoute = AppRoute.SETTINGS }
                        )
                    }

                    AppRoute.SETTINGS -> {
                        val viewModel: SettingsViewModel = viewModel(
                            factory = SettingsViewModelFactory(settingsRepository)
                        )
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { currentRoute = AppRoute.HOME }
                        )
                    }

                    AppRoute.LIBRARY -> {
                        val viewModel: LibraryViewModel = viewModel(
                            factory = LibraryViewModelFactory(bookRepository)
                        )
                        LibraryScreen(
                            viewModel = viewModel,
                            onOpenBook = { id ->
                                selectedBookId = id
                                selectedChapterId = null
                                historyOpen = false
                                imagesOpen = false
                                revisionOpen = false
                                previewOpen = false
                                diffCurrentContent = null
                                staticDiffRequest = null
                                currentRoute = AppRoute.BOOK_WORKSPACE
                            },
                            onOpenSettings = { currentRoute = AppRoute.SETTINGS }
                        )
                    }

                    AppRoute.BOOK_WORKSPACE -> {
                        if (bookId == null) {
                            currentRoute = AppRoute.LIBRARY
                        } else if (chapterId == null) {
                            // Workspace do livro: abas de capítulos e personagens
                            val chaptersViewModel: ChaptersViewModel = viewModel(
                                key = "chapters-$bookId",
                                factory = ChaptersViewModelFactory(
                                    bookId = bookId,
                                    repository = chapterRepository
                                )
                            )
                            val characterViewModel: CharacterViewModel = viewModel(
                                key = "character-$bookId",
                                factory = CharacterViewModelFactory(
                                    bookId = bookId,
                                    repository = characterRepository
                                )
                            )
                            val locationViewModel: LocationViewModel = viewModel(
                                key = "location-$bookId",
                                factory = LocationViewModelFactory(
                                    bookId = bookId,
                                    repository = locationRepository
                                )
                            )
                            BookWorkspaceScreen(
                                chaptersViewModel = chaptersViewModel,
                                characterViewModel = characterViewModel,
                                locationViewModel = locationViewModel,
                                settings = settings,
                                onUpdateSettings = { updateFn ->
                                    scope.launch {
                                        settingsRepository.updateSettings(updateFn)
                                    }
                                },
                                onBack = {
                                    selectedBookId = null
                                    selectedChapterId = null
                                    currentRoute = AppRoute.LIBRARY
                                },
                                onOpenChapter = { id ->
                                    selectedChapterId = id
                                    historyOpen = false
                                    imagesOpen = false
                                    revisionOpen = false
                                    previewOpen = false
                                    diffCurrentContent = null
                                    staticDiffRequest = null
                                }
                            )
                        } else {
                            // Subtelas do capítulo: editor, diff, histórico
                            val contentForDiff = diffCurrentContent
                            val savedDiff = staticDiffRequest

                            if (savedDiff != null) {
                                StaticDiffScreen(
                                    request = savedDiff,
                                    onBack = { staticDiffRequest = null }
                                )
                            } else if (contentForDiff == null && historyOpen) {
                                val viewModel: HistoryViewModel = viewModel(
                                    key = "history-$chapterId",
                                    factory = HistoryViewModelFactory(
                                        chapterId = chapterId,
                                        repository = chapterRepository
                                    )
                                )
                                HistoryScreen(
                                    viewModel = viewModel,
                                    onBack = { historyOpen = false },
                                    onCompareVersions = { request -> staticDiffRequest = request },
                                    settings = settings,
                                    onUpdateSettings = { updateFn ->
                                        scope.launch {
                                            settingsRepository.updateSettings(updateFn)
                                        }
                                    }
                                )
                            } else if (contentForDiff == null && imagesOpen) {
                                val viewModel: ImagesViewModel = viewModel(
                                    key = "images-$bookId",
                                    factory = ImagesViewModelFactory(
                                        bookId = bookId,
                                        repository = imageRepository
                                    )
                                )
                                com.livrohub.ui.images.ImagesScreen(
                                    viewModel = viewModel,
                                    onBack = { imagesOpen = false }
                                )
                            } else if (contentForDiff == null && previewOpen) {
                                val editorViewModel: EditorViewModel = viewModel(
                                    key = "editor-$chapterId",
                                    factory = EditorViewModelFactory(
                                        chapterId = chapterId,
                                        bookId = bookId,
                                        repository = chapterRepository,
                                        characterRepository = characterRepository,
                                        locationRepository = locationRepository
                                    )
                                )
                                val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
                                ChapterPreviewScreen(
                                    chapterTitle = editorState.chapterTitle,
                                    markdown = editorState.editorText.text,
                                    hasUnsavedChanges = editorState.hasUnsavedChanges,
                                    fontSizeSp = settings.fontSizeSp,
                                    onBack = { previewOpen = false }
                                )
                            } else if (contentForDiff == null && revisionOpen) {
                                val editorViewModel: EditorViewModel = viewModel(
                                    key = "editor-$chapterId",
                                    factory = EditorViewModelFactory(
                                        chapterId = chapterId,
                                        bookId = bookId,
                                        repository = chapterRepository,
                                        characterRepository = characterRepository,
                                        locationRepository = locationRepository
                                    )
                                )
                                val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
                                RevisionScreen(
                                    chapterTitle = editorState.chapterTitle,
                                    text = editorState.editorText.text,
                                    onBack = { revisionOpen = false },
                                    onApplyText = { revisedText ->
                                        editorViewModel.updateText(
                                            androidx.compose.ui.text.input.TextFieldValue(revisedText)
                                        )
                                    }
                                )
                            } else if (contentForDiff == null) {
                                val editorViewModel: EditorViewModel = viewModel(
                                    key = "editor-$chapterId",
                                    factory = EditorViewModelFactory(
                                        chapterId = chapterId,
                                        bookId = bookId,
                                        repository = chapterRepository,
                                        characterRepository = characterRepository,
                                        locationRepository = locationRepository
                                    )
                                )
                                EditorScreen(
                                    viewModel = editorViewModel,
                                    onBack = {
                                        selectedChapterId = null
                                        historyOpen = false
                                        imagesOpen = false
                                        revisionOpen = false
                                        previewOpen = false
                                        diffCurrentContent = null
                                        staticDiffRequest = null
                                    },
                                    onOpenPreview = { previewOpen = true },
                                    onOpenRevision = { revisionOpen = true },
                                    onOpenHistory = { historyOpen = true },
                                    onOpenImages = { imagesOpen = true },
                                    fontSizeSp = settings.fontSizeSp,
                                    showLineNumbers = settings.showLineNumbers,
                                    settings = settings,
                                    onUpdateSettings = { updateFn ->
                                        scope.launch {
                                            settingsRepository.updateSettings(updateFn)
                                        }
                                    }
                                )
                            } else {
                                val viewModel: DiffViewModel = viewModel(
                                    key = "diff-$chapterId-${contentForDiff.hashCode()}",
                                    factory = DiffViewModelFactory(
                                        chapterId = chapterId,
                                        currentContent = contentForDiff,
                                        repository = chapterRepository
                                    )
                                )
                                DiffScreen(
                                    viewModel = viewModel,
                                    onBack = { diffCurrentContent = null }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
