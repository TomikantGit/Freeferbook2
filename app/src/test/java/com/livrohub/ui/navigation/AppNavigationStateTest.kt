package com.livrohub.ui.navigation

import com.livrohub.ui.diff.StaticDiffRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavigationStateTest {

    @Test
    fun `chapter screens are mutually exclusive`() {
        val state = AppNavigationState()
            .openLibrary()
            .openBook(10)
            .openChapter(20)
            .openChapterScreen(ChapterScreen.REVISION)
            .openChapterScreen(ChapterScreen.HISTORY)

        assertEquals(ChapterScreen.HISTORY, state.chapterScreen)
        assertNull(state.dynamicDiffContent)
        assertNull(state.staticDiffRequest)
    }

    @Test
    fun `static diff returns to history`() {
        val request = StaticDiffRequest(
            title = "Capítulo",
            oldLabel = "v1",
            newLabel = "v2",
            oldContent = "a",
            newContent = "b"
        )
        val state = AppNavigationState()
            .openBook(1)
            .openChapter(2)
            .openChapterScreen(ChapterScreen.HISTORY)
            .showStaticDiff(request)
            .closeChapterScreen()

        assertEquals(ChapterScreen.HISTORY, state.chapterScreen)
        assertNull(state.staticDiffRequest)
    }

    @Test
    fun `settings returns to route that opened it`() {
        val state = AppNavigationState()
            .openLibrary()
            .openSettings()
            .closeSettings()

        assertEquals(AppRoute.LIBRARY, state.route)
    }
}
