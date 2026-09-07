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

    @Test
    fun underline_wraps_selection_and_is_toggleable() {
        val formatted = applyMarkdownFormat(
            TextFieldValue("texto importante", TextRange(6, 16)),
            MarkdownFormat.Underline
        )

        assertEquals("texto ++importante++", formatted.text)
        assertEquals(TextRange(8, 18), formatted.selection)

        val restored = applyMarkdownFormat(formatted, MarkdownFormat.Underline)
        assertEquals("texto importante", restored.text)
        assertEquals(TextRange(6, 16), restored.selection)
    }

    @Test
    fun bulleted_list_formats_multiple_selected_lines() {
        val result = applyMarkdownFormat(
            TextFieldValue("um\ndois\ntres", TextRange(0, 12)),
            MarkdownFormat.BulletedList
        )

        assertEquals("- um\n- dois\n- tres", result.text)
    }

    @Test
    fun numbered_list_replaces_existing_bullets_and_numbers_lines() {
        val result = applyMarkdownFormat(
            TextFieldValue("- um\n- dois\n- tres", TextRange(2, 18)),
            MarkdownFormat.NumberedList
        )

        assertEquals("1. um\n2. dois\n3. tres", result.text)
    }

    @Test
    fun checklist_is_toggleable() {
        val formatted = applyMarkdownFormat(
            TextFieldValue("comprar pão\nrevisar capítulo", TextRange(0, 27)),
            MarkdownFormat.Checklist
        )
        assertEquals("- [ ] comprar pão\n- [ ] revisar capítulo", formatted.text)

        val restored = applyMarkdownFormat(formatted, MarkdownFormat.Checklist)
        assertEquals("comprar pão\nrevisar capítulo", restored.text)
    }

    @Test
    fun custom_marker_replaces_list_prefix() {
        val result = applyCustomMarkdownMarker(
            TextFieldValue("- cena um\n- cena dois", TextRange(2, 20)),
            "★"
        )

        assertEquals("★ cena um\n★ cena dois", result.text)
    }
}
