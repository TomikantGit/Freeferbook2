package com.livrohub.domain.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTextFormatterTest {
    @Test
    fun `inline format toggles and preserves content selection`() {
        val formatted = MarkdownTextFormatter.apply("texto importante", 6, 16, MarkdownTextFormat.Underline)
        assertEquals("texto ++importante++", formatted.text)
        assertEquals(8, formatted.selectionStart)
        assertEquals(18, formatted.selectionEnd)

        val restored = MarkdownTextFormatter.apply(
            formatted.text,
            formatted.selectionStart,
            formatted.selectionEnd,
            MarkdownTextFormat.Underline
        )
        assertEquals("texto importante", restored.text)
        assertEquals(6, restored.selectionStart)
        assertEquals(16, restored.selectionEnd)
    }

    @Test
    fun `list styles replace previous prefixes`() {
        val numbered = MarkdownTextFormatter.apply("- um\n- dois", 2, 11, MarkdownTextFormat.NumberedList)
        assertEquals("1. um\n2. dois", numbered.text)

        val starred = MarkdownTextFormatter.applyCustomMarker(
            numbered.text,
            numbered.selectionStart,
            numbered.selectionEnd,
            "★"
        )
        assertEquals("★ um\n★ dois", starred.text)
    }
}
