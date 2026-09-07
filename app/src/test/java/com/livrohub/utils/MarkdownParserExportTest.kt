package com.livrohub.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserExportTest {

    @Test
    fun `plain text export preserves chapter text and removes markdown markers`() {
        val markdown = """
            # Capítulo 1

            Este é um **trecho importante** com *ênfase*.
            > Uma citação relevante.
            ~~Texto revisado~~
        """.trimIndent()

        val plainText = MarkdownParser.parseToPlainText(markdown)

        assertTrue(plainText.contains("Capítulo 1"))
        assertTrue(plainText.contains("trecho importante"))
        assertTrue(plainText.contains("ênfase"))
        assertTrue(plainText.contains("Uma citação relevante."))
        assertTrue(plainText.contains("Texto revisado"))
        assertFalse(plainText.contains("**"))
        assertFalse(plainText.contains("~~"))
    }

    @Test
    fun `html export contains visible text and valid block elements`() {
        val markdown = """
            # Título

            Parágrafo com **negrito** e <caractere reservado>.
            > Citação
        """.trimIndent()

        val html = MarkdownParser.parseToHtml(markdown)

        assertTrue(html.contains("<h1>Título</h1>"))
        assertTrue(html.contains("<strong>negrito</strong>"))
        assertTrue(html.contains("&lt;caractere reservado&gt;"))
        assertTrue(html.contains("<blockquote>Citação</blockquote>"))
        assertTrue(html.contains("Parágrafo"))
    }
}
