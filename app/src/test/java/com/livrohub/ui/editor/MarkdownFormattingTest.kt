package com.livrohub.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownFormattingTest {

    @Test
    fun bold_wraps_selected_word_and_keeps_only_word_selected() {
        val result = applyMarkdownFormat(
            TextFieldValue("uma palavra aqui", TextRange(4, 11)),
            MarkdownFormat.Bold
        )

        assertEquals("uma **palavra** aqui", result.text)
        assertEquals(TextRange(6, 13), result.selection)
    }

    @Test
    fun bold_is_toggleable_without_losing_selection() {
        val formatted = TextFieldValue("uma **palavra** aqui", TextRange(6, 13))

        val result = applyMarkdownFormat(formatted, MarkdownFormat.Bold)

        assertEquals("uma palavra aqui", result.text)
        assertEquals(TextRange(4, 11), result.selection)
    }

    @Test
    fun heading_formats_current_line_without_adding_newline() {
        val result = applyMarkdownFormat(
            TextFieldValue("primeira\nsegunda linha", TextRange(9, 16)),
            MarkdownFormat.Heading
        )

        assertEquals("primeira\n### segunda linha", result.text)
        assertEquals(TextRange(13, 20), result.selection)
    }

    @Test
    fun quote_is_toggleable_for_selected_line() {
        val formatted = TextFieldValue("> uma frase", TextRange(2, 5))

        val result = applyMarkdownFormat(formatted, MarkdownFormat.Quote)

        assertEquals("uma frase", result.text)
        assertEquals(TextRange(0, 3), result.selection)
    }
}
