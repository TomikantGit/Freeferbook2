package com.livrohub.domain.diff

import org.junit.Assert.assertEquals
import org.junit.Test

class TextDiffEngineTest {

    private val engine = TextDiffEngine()

    @Test
    fun `compare identic texts should return only unchanged lines`() {
        val text = "Linha 1\nLinha 2\nLinha 3"
        val diff = engine.compare(text, text)

        assertEquals(3, diff.size)
        assertEquals(DiffLineType.Unchanged, diff[0].type)
        assertEquals(DiffLineType.Unchanged, diff[1].type)
        assertEquals(DiffLineType.Unchanged, diff[2].type)
    }

    @Test
    fun `compare with added lines should return added type`() {
        val oldText = "Linha 1\nLinha 3"
        val newText = "Linha 1\nLinha 2\nLinha 3"
        val diff = engine.compare(oldText, newText)

        assertEquals(3, diff.size)
        assertEquals(DiffLineType.Unchanged, diff[0].type)
        assertEquals("Linha 1", diff[0].text)

        assertEquals(DiffLineType.Added, diff[1].type)
        assertEquals("Linha 2", diff[1].text)
        assertEquals("+", diff[1].prefix)

        assertEquals(DiffLineType.Unchanged, diff[2].type)
        assertEquals("Linha 3", diff[2].text)
    }

    @Test
    fun `compare with removed lines should return removed type`() {
        val oldText = "Linha 1\nLinha 2\nLinha 3"
        val newText = "Linha 1\nLinha 3"
        val diff = engine.compare(oldText, newText)

        assertEquals(3, diff.size)
        assertEquals(DiffLineType.Unchanged, diff[0].type)
        assertEquals("Linha 1", diff[0].text)

        assertEquals(DiffLineType.Removed, diff[1].type)
        assertEquals("Linha 2", diff[1].text)
        assertEquals("-", diff[1].prefix)

        assertEquals(DiffLineType.Unchanged, diff[2].type)
        assertEquals("Linha 3", diff[2].text)
    }

    @Test
    fun `compare empty texts should return empty list`() {
        val diff = engine.compare("", "")
        assertEquals(0, diff.size)
    }

    @Test
    fun `compare complete change should return all removed and added lines`() {
        val oldText = "A\nB"
        val newText = "C\nD"
        val diff = engine.compare(oldText, newText)

        assertEquals(4, diff.size)
        assertEquals(DiffLineType.Removed, diff[0].type)
        assertEquals("A", diff[0].text)
        assertEquals(DiffLineType.Removed, diff[1].type)
        assertEquals("B", diff[1].text)
        assertEquals(DiffLineType.Added, diff[2].type)
        assertEquals("C", diff[2].text)
        assertEquals(DiffLineType.Added, diff[3].type)
        assertEquals("D", diff[3].text)
    }

    @Test
    fun `compare line with inline word changes should return spans correctly`() {
        val oldText = "O gato preto pulou"
        val newText = "O cachorro preto pulou alto"
        val diff = engine.compare(oldText, newText)

        // Uma linha alterada gera uma REMOVED e uma ADDED
        assertEquals(2, diff.size)
        
        val oldLine = diff[0]
        assertEquals(DiffLineType.Removed, oldLine.type)
        assertEquals("O gato preto pulou", oldLine.text)
        // Spans esperados: [Unchanged("O "), Removed("gato"), Unchanged(" preto pulou")]
        assertEquals(3, oldLine.spans.size)
        assertEquals(DiffSpanType.Unchanged, oldLine.spans[0].type)
        assertEquals("O ", oldLine.spans[0].text)
        assertEquals(DiffSpanType.Removed, oldLine.spans[1].type)
        assertEquals("gato", oldLine.spans[1].text)
        assertEquals(DiffSpanType.Unchanged, oldLine.spans[2].type)
        assertEquals(" preto pulou", oldLine.spans[2].text)

        val newLine = diff[1]
        assertEquals(DiffLineType.Added, newLine.type)
        assertEquals("O cachorro preto pulou alto", newLine.text)
        // Spans esperados: [Unchanged("O "), Added("cachorro"), Unchanged(" preto pulou"), Added(" alto")]
        assertEquals(4, newLine.spans.size)
        assertEquals(DiffSpanType.Unchanged, newLine.spans[0].type)
        assertEquals("O ", newLine.spans[0].text)
        assertEquals(DiffSpanType.Added, newLine.spans[1].type)
        assertEquals("cachorro", newLine.spans[1].text)
        assertEquals(DiffSpanType.Unchanged, newLine.spans[2].type)
        assertEquals(" preto pulou", newLine.spans[2].text)
        assertEquals(DiffSpanType.Added, newLine.spans[3].type)
        assertEquals(" alto", newLine.spans[3].text)
    }
}
