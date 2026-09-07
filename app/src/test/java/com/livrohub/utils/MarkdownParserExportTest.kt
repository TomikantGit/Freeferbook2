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

    @Test
    fun `plain text export preserves lists underline highlight and custom markers`() {
        val markdown = """
            - item simples
            1. primeiro
            - [ ] pendente
            - [x] concluído
            ★ cena importante
            ++sublinhado++ e ==destaque==
        """.trimIndent()

        val plainText = MarkdownParser.parseToPlainText(markdown)

        assertTrue(plainText.contains("• item simples"))
        assertTrue(plainText.contains("1. primeiro"))
        assertTrue(plainText.contains("☐ pendente"))
        assertTrue(plainText.contains("☑ concluído"))
        assertTrue(plainText.contains("★ cena importante"))
        assertTrue(plainText.contains("sublinhado"))
        assertTrue(plainText.contains("destaque"))
        assertFalse(plainText.contains("++"))
        assertFalse(plainText.contains("=="))
    }

    @Test
    fun `html export renders lists underline highlight and checklist`() {
        val markdown = """
            - item um
            - item dois
            1. primeiro
            2. segundo
            - [ ] revisar
            ++sublinhado++ ==destaque==
        """.trimIndent()

        val html = MarkdownParser.parseToHtml(markdown)

        assertTrue(html.contains("<ul><li>item um</li><li>item dois</li></ul>"))
        assertTrue(html.contains("<ol><li>primeiro</li><li>segundo</li></ol>"))
        assertTrue(html.contains("class=\"checklist\""))
        assertTrue(html.contains("☐ revisar"))
        assertTrue(html.contains("text-decoration: underline"))
        assertTrue(html.contains("background-color"))
    }
}
