package com.livrohub.ui.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import com.livrohub.ui.diff.StaticDiffRequest

/** Rotas de primeiro nível do app. */
enum class AppRoute {
    HOME,
    LIBRARY,
    SETTINGS,
    BOOK_WORKSPACE
}

/**
 * Subtelas mutuamente exclusivas dentro de um capítulo.
 *
 * Um enum único substitui vários booleanos independentes e impede estados como
 * "histórico e revisão abertos ao mesmo tempo".
 */
enum class ChapterScreen {
    EDITOR,
    PREVIEW,
    REVISION,
    HISTORY,
    IMAGES,
    DYNAMIC_DIFF,
    STATIC_DIFF
}

/**
 * Snapshot imutável de navegação.
 *
 * Conteúdos grandes de diff são transitórios e deliberadamente não entram no
 * Bundle de `rememberSaveable`, evitando `TransactionTooLargeException` com
 * capítulos extensos. Após recriação de processo, diffs retornam ao editor.
 */
@Immutable
data class AppNavigationState(
    val route: AppRoute = AppRoute.HOME,
    val settingsReturnRoute: AppRoute = AppRoute.HOME,
    val selectedBookId: Long? = null,
    val selectedChapterId: Long? = null,
    val chapterScreen: ChapterScreen = ChapterScreen.EDITOR,
    val dynamicDiffContent: String? = null,
    val staticDiffRequest: StaticDiffRequest? = null
) {
    fun openHome(): AppNavigationState = copy(
        route = AppRoute.HOME,
        selectedBookId = null,
        selectedChapterId = null,
        chapterScreen = ChapterScreen.EDITOR,
        dynamicDiffContent = null,
        staticDiffRequest = null
    )

    fun openLibrary(): AppNavigationState = copy(
        route = AppRoute.LIBRARY,
        selectedBookId = null,
        selectedChapterId = null,
        chapterScreen = ChapterScreen.EDITOR,
        dynamicDiffContent = null,
        staticDiffRequest = null
    )

    fun openSettings(): AppNavigationState = copy(
        route = AppRoute.SETTINGS,
        settingsReturnRoute = when (route) {
            AppRoute.HOME, AppRoute.LIBRARY -> route
            else -> AppRoute.HOME
        }
    )

    fun closeSettings(): AppNavigationState = copy(route = settingsReturnRoute)

    fun openBook(bookId: Long): AppNavigationState = copy(
        route = AppRoute.BOOK_WORKSPACE,
        selectedBookId = bookId,
        selectedChapterId = null,
        chapterScreen = ChapterScreen.EDITOR,
        dynamicDiffContent = null,
        staticDiffRequest = null
    )

    fun closeBook(): AppNavigationState = openLibrary()

    fun openChapter(chapterId: Long): AppNavigationState = copy(
        selectedChapterId = chapterId,
        chapterScreen = ChapterScreen.EDITOR,
        dynamicDiffContent = null,
        staticDiffRequest = null
    )

    fun closeChapter(): AppNavigationState = copy(
        selectedChapterId = null,
        chapterScreen = ChapterScreen.EDITOR,
        dynamicDiffContent = null,
        staticDiffRequest = null
    )

    fun openChapterScreen(screen: ChapterScreen): AppNavigationState {
        require(screen != ChapterScreen.DYNAMIC_DIFF && screen != ChapterScreen.STATIC_DIFF) {
            "Use showDynamicDiff/showStaticDiff para telas de diff."
        }
        return copy(
            chapterScreen = screen,
            dynamicDiffContent = null,
            staticDiffRequest = null
        )
    }

    fun showDynamicDiff(content: String): AppNavigationState = copy(
        chapterScreen = ChapterScreen.DYNAMIC_DIFF,
        dynamicDiffContent = content,
        staticDiffRequest = null
    )

    fun showStaticDiff(request: StaticDiffRequest): AppNavigationState = copy(
        chapterScreen = ChapterScreen.STATIC_DIFF,
        dynamicDiffContent = null,
        staticDiffRequest = request
    )

    fun closeChapterScreen(): AppNavigationState = when (chapterScreen) {
        ChapterScreen.STATIC_DIFF -> copy(
            chapterScreen = ChapterScreen.HISTORY,
            staticDiffRequest = null
        )

        else -> copy(
            chapterScreen = ChapterScreen.EDITOR,
            dynamicDiffContent = null,
            staticDiffRequest = null
        )
    }

    companion object {
        /**
         * Salva somente estado pequeno e seguro para Bundle.
         * Diffs carregam texto potencialmente grande e voltam para EDITOR na restauração.
         */
        val Saver: Saver<AppNavigationState, Any> = mapSaver(
            save = { state ->
                buildMap {
                    put("route", state.route.name)
                    put("settingsReturnRoute", state.settingsReturnRoute.name)
                    state.selectedBookId?.let { put("bookId", it) }
                    state.selectedChapterId?.let { put("chapterId", it) }
                    val restorableScreen = when (state.chapterScreen) {
                        ChapterScreen.DYNAMIC_DIFF,
                        ChapterScreen.STATIC_DIFF -> ChapterScreen.EDITOR

                        else -> state.chapterScreen
                    }
                    put("chapterScreen", restorableScreen.name)
                }
            },
            restore = { saved ->
                AppNavigationState(
                    route = saved["route"]
                        ?.toString()
                        ?.let(AppRoute::valueOf)
                        ?: AppRoute.HOME,
                    settingsReturnRoute = saved["settingsReturnRoute"]
                        ?.toString()
                        ?.let(AppRoute::valueOf)
                        ?: AppRoute.HOME,
                    selectedBookId = saved["bookId"] as? Long,
                    selectedChapterId = saved["chapterId"] as? Long,
                    chapterScreen = saved["chapterScreen"]
                        ?.toString()
                        ?.let(ChapterScreen::valueOf)
                        ?: ChapterScreen.EDITOR
                )
            }
        )
    }
}
