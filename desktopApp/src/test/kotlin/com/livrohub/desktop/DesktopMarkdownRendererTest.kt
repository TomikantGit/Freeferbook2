package com.livrohub.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopMarkdownRendererTest {
    @Test
    fun `preview removes supported markers while preserving visible text`() {
        val rendered = DesktopMarkdownRenderer.toAnnotatedString(
            "# Título\n- item\n++sublinhado++ e **negrito**\n- [x] pronto"
        )

        assertTrue(rendered.text.contains("Título"))
        assertTrue(rendered.text.contains("• item"))
        assertTrue(rendered.text.contains("sublinhado"))
        assertTrue(rendered.text.contains("negrito"))
        assertTrue(rendered.text.contains("☑ pronto"))
        assertFalse(rendered.text.contains("++"))
        assertFalse(rendered.text.contains("**"))
        assertEquals(4, rendered.text.lines().size)
    }
}
