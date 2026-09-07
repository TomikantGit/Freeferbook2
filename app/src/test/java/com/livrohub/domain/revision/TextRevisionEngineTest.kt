package com.livrohub.domain.revision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextRevisionEngineTest {

    @Test
    fun `detecta e corrige espacos duplicados`() {
        val text = "Ela  abriu a porta."
        val result = TextRevisionEngine.review(text)

        assertTrue(result.issues.any { it.type == RevisionIssueType.DUPLICATE_SPACE })
        assertEquals("Ela abriu a porta.", TextRevisionEngine.applyAllSafe(text, result))
    }

    @Test
    fun `detecta palavra repetida e preserva apenas uma`() {
        val text = "Ele entrou entrou na sala."
        val result = TextRevisionEngine.review(text)

        assertTrue(result.issues.any { it.type == RevisionIssueType.REPEATED_WORD })
        assertEquals("Ele entrou na sala.", TextRevisionEngine.applyAllSafe(text, result))
    }

    @Test
    fun `remove espaco antes de pontuacao`() {
        val text = "Tudo certo , agora."
        val result = TextRevisionEngine.review(text)

        assertTrue(result.issues.any { it.type == RevisionIssueType.SPACE_BEFORE_PUNCTUATION })
        assertEquals("Tudo certo, agora.", TextRevisionEngine.applyAllSafe(text, result))
    }

    @Test
    fun `frase longa e apenas sugestao`() {
        val words = (1..36).joinToString(" ") { "palavra$it" }
        val text = "$words."
        val result = TextRevisionEngine.review(text)
        val issue = result.issues.first { it.type == RevisionIssueType.LONG_SENTENCE }

        assertTrue(!issue.isAutoFixable)
        assertEquals(text, TextRevisionEngine.applyAllSafe(text, result))
    }
}
