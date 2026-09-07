package com.livrohub.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.livrohub.di.AppDependencies
import com.livrohub.domain.model.AppSettings
import com.livrohub.ui.chapters.ChaptersViewModel
import com.livrohub.ui.characters.CharacterViewModel
import com.livrohub.ui.common.viewModelFactory
import com.livrohub.ui.diff.DiffScreen
import com.livrohub.ui.diff.DiffViewModel
import com.livrohub.ui.diff.StaticDiffScreen
import com.livrohub.ui.editor.EditorScreen
import com.livrohub.ui.editor.EditorViewModel
import com.livrohub.ui.history.HistoryScreen
import com.livrohub.ui.history.HistoryViewModel
import com.livrohub.ui.home.HomeScreen
import com.livrohub.ui.images.ImagesScreen
import com.livrohub.ui.images.ImagesViewModel
import com.livrohub.ui.library.LibraryScreen
import com.livrohub.ui.library.LibraryViewModel
import com.livrohub.ui.locations.LocationViewModel
import com.livrohub.ui.navigation.AppNavigationState
import com.livrohub.ui.navigation.AppRoute
import com.livrohub.ui.navigation.ChapterScreen
import com.livrohub.ui.preview.ChapterPreviewScreen
import com.livrohub.ui.revision.RevisionScreen
import com.livrohub.ui.settings.SettingsScreen
import com.livrohub.ui.settings.SettingsViewModel
import com.livrohub.ui.theme.LivroHubTheme
import com.livrohub.ui.workspace.BookWorkspaceScreen
import kotlinx.coroutines.launch

/**
 * Composable raiz do aplicativo.
 *
 * Mantém somente responsabilidades de composição global: tema, snapshot de navegação e
 * dispatch das rotas de primeiro nível. Cada escopo de tela cria seus próprios ViewModels.
 */
@Composable
fun LivroHubAppRoot(dependencies: AppDependencies) {
    val settings by dependencies.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )
    val scope = rememberCoroutineScope()
    var navigation by rememberSaveable(stateSaver = AppNavigationState.Saver) {
        mutableStateOf(AppNavigationState())
    }

    val updateSettings: (((AppSettings) -> AppSettings) -> Unit) = { transform ->
        scope.launch {
            dependencies.settingsRepository.updateSettings(transform)
        }
    }

    LivroHubTheme(
        settings = settings,
        isSystemDark = isSystemInDarkTheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LivroHubTheme.colors.background
        ) {
            AnimatedContent(
                targetState = navigation.route,
                label = "AppRouteTransition"
            ) { route ->
                when (route) {
                    AppRoute.HOME -> HomeScreen(
                        onNavigateToLibrary = { navigation = navigation.openLibrary() },
                        onNavigateToSettings = { navigation = navigation.openSettings() }
                    )

                    AppRoute.LIBRARY -> LibraryRoute(
                        dependencies = dependencies,
                        onOpenBook = { bookId -> navigation = navigation.openBook(bookId) },
                        onOpenSettings = { navigation = navigation.openSettings() }
                    )

                    AppRoute.SETTINGS -> SettingsRoute(
                        dependencies = dependencies,
                        onBack = { navigation = navigation.closeSettings() }
                    )

                    AppRoute.BOOK_WORKSPACE -> BookWorkspaceRoute(
                        navigation = navigation,
                        dependencies = dependencies,
                        settings = settings,
                        onNavigate = { navigation = it },
                        onUpdateSettings = updateSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryRoute(
    dependencies: AppDependencies,
    onOpenBook: (Long) -> Unit,
    onOpenSettings: () -> Unit
) {
    val viewModel: LibraryViewModel = viewModel(
        factory = viewModelFactory {
            LibraryViewModel(dependencies.bookRepository)
        }
    )
    LibraryScreen(
        viewModel = viewModel,
        onOpenBook = onOpenBook,
        onOpenSettings = onOpenSettings
    )
}

@Composable
private fun SettingsRoute(
    dependencies: AppDependencies,
    onBack: () -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            SettingsViewModel(dependencies.settingsRepository)
        }
    )
    SettingsScreen(
        viewModel = viewModel,
        onBack = onBack
    )
}

@Composable
private fun BookWorkspaceRoute(
    navigation: AppNavigationState,
    dependencies: AppDependencies,
    settings: AppSettings,
    onNavigate: (AppNavigationState) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    val bookId = navigation.selectedBookId
    if (bookId == null) {
        LaunchedEffect(Unit) { onNavigate(navigation.openLibrary()) }
        return
    }

    val chapterId = navigation.selectedChapterId
    if (chapterId == null) {
        BookOverviewRoute(
            bookId = bookId,
            dependencies = dependencies,
            settings = settings,
            onUpdateSettings = onUpdateSettings,
            onBack = { onNavigate(navigation.closeBook()) },
            onOpenChapter = { id -> onNavigate(navigation.openChapter(id)) }
        )
        return
    }

    ChapterRoute(
        navigation = navigation,
        bookId = bookId,
        chapterId = chapterId,
        dependencies = dependencies,
        settings = settings,
        onNavigate = onNavigate,
        onUpdateSettings = onUpdateSettings
    )
}

@Composable
private fun BookOverviewRoute(
    bookId: Long,
    dependencies: AppDependencies,
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onBack: () -> Unit,
    onOpenChapter: (Long) -> Unit
) {
    val chaptersViewModel: ChaptersViewModel = viewModel(
        key = "chapters-$bookId",
        factory = viewModelFactory {
            ChaptersViewModel(
                bookId = bookId,
                repository = dependencies.chapterRepository
            )
        }
    )
    val characterViewModel: CharacterViewModel = viewModel(
        key = "character-$bookId",
        factory = viewModelFactory {
            CharacterViewModel(
                bookId = bookId,
                repository = dependencies.characterRepository
            )
        }
    )
    val locationViewModel: LocationViewModel = viewModel(
        key = "location-$bookId",
        factory = viewModelFactory {
            LocationViewModel(
                bookId = bookId,
                repository = dependencies.locationRepository
            )
        }
    )

    BookWorkspaceScreen(
        chaptersViewModel = chaptersViewModel,
        characterViewModel = characterViewModel,
        locationViewModel = locationViewModel,
        settings = settings,
        onUpdateSettings = onUpdateSettings,
        onBack = onBack,
        onOpenChapter = onOpenChapter
    )
}

@Composable
private fun ChapterRoute(
    navigation: AppNavigationState,
    bookId: Long,
    chapterId: Long,
    dependencies: AppDependencies,
    settings: AppSettings,
    onNavigate: (AppNavigationState) -> Unit,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit
) {
    // Uma única instância por capítulo é compartilhada entre editor, visão e revisão.
    // Isso preserva texto ainda não salvo ao alternar entre essas telas.
    val editorViewModel: EditorViewModel = viewModel(
        key = "editor-$chapterId",
        factory = viewModelFactory {
            EditorViewModel(
                chapterId = chapterId,
                bookId = bookId,
                repository = dependencies.chapterRepository,
                mentionSynchronizer = dependencies.chapterMentionSynchronizer
            )
        }
    )

    when (navigation.chapterScreen) {
        ChapterScreen.EDITOR -> EditorScreen(
            viewModel = editorViewModel,
            onBack = { onNavigate(navigation.closeChapter()) },
            onOpenPreview = {
                onNavigate(navigation.openChapterScreen(ChapterScreen.PREVIEW))
            },
            onOpenRevision = {
                onNavigate(navigation.openChapterScreen(ChapterScreen.REVISION))
            },
            onOpenHistory = {
                onNavigate(navigation.openChapterScreen(ChapterScreen.HISTORY))
            },
            onOpenImages = {
                onNavigate(navigation.openChapterScreen(ChapterScreen.IMAGES))
            },
            fontSizeSp = settings.fontSizeSp,
            showLineNumbers = settings.showLineNumbers,
            settings = settings,
            onUpdateSettings = onUpdateSettings
        )

        ChapterScreen.PREVIEW -> {
            val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
            ChapterPreviewScreen(
                chapterTitle = editorState.chapterTitle,
                markdown = editorState.editorText.text,
                hasUnsavedChanges = editorState.hasUnsavedChanges,
                fontSizeSp = settings.fontSizeSp,
                onBack = { onNavigate(navigation.closeChapterScreen()) }
            )
        }

        ChapterScreen.REVISION -> {
            val editorState by editorViewModel.uiState.collectAsStateWithLifecycle()
            RevisionScreen(
                chapterTitle = editorState.chapterTitle,
                text = editorState.editorText.text,
                onBack = { onNavigate(navigation.closeChapterScreen()) },
                onApplyText = { revisedText ->
                    editorViewModel.updateText(TextFieldValue(revisedText))
                }
            )
        }

        ChapterScreen.HISTORY -> {
            val historyViewModel: HistoryViewModel = viewModel(
                key = "history-$chapterId",
                factory = viewModelFactory {
                    HistoryViewModel(
                        chapterId = chapterId,
                        repository = dependencies.chapterRepository
                    )
                }
            )
            HistoryScreen(
                viewModel = historyViewModel,
                onBack = { onNavigate(navigation.closeChapterScreen()) },
                onCompareVersions = { request ->
                    onNavigate(navigation.showStaticDiff(request))
                },
                settings = settings,
                onUpdateSettings = onUpdateSettings
            )
        }

        ChapterScreen.IMAGES -> {
            val imagesViewModel: ImagesViewModel = viewModel(
                key = "images-$bookId",
                factory = viewModelFactory {
                    ImagesViewModel(
                        bookId = bookId,
                        repository = dependencies.imageRepository
                    )
                }
            )
            ImagesScreen(
                viewModel = imagesViewModel,
                onBack = { onNavigate(navigation.closeChapterScreen()) }
            )
        }

        ChapterScreen.STATIC_DIFF -> {
            val request = navigation.staticDiffRequest
            if (request == null) {
                LaunchedEffect(Unit) {
                    onNavigate(navigation.openChapterScreen(ChapterScreen.HISTORY))
                }
            } else {
                StaticDiffScreen(
                    request = request,
                    onBack = { onNavigate(navigation.closeChapterScreen()) }
                )
            }
        }

        ChapterScreen.DYNAMIC_DIFF -> {
            val currentContent = navigation.dynamicDiffContent
            if (currentContent == null) {
                LaunchedEffect(Unit) {
                    onNavigate(navigation.openChapterScreen(ChapterScreen.EDITOR))
                }
            } else {
                val diffViewModel: DiffViewModel = viewModel(
                    key = "diff-$chapterId-${currentContent.hashCode()}",
                    factory = viewModelFactory {
                        DiffViewModel(
                            chapterId = chapterId,
                            currentContent = currentContent,
                            repository = dependencies.chapterRepository
                        )
                    }
                )
                DiffScreen(
                    viewModel = diffViewModel,
                    onBack = { onNavigate(navigation.closeChapterScreen()) }
                )
            }
        }
    }
}
