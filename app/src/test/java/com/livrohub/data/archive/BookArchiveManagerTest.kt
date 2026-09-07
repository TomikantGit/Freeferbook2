package com.livrohub.data.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class BookArchiveManagerTest {

    @Test
    fun `suggested file name is safe and keeps zip extension`() {
        assertEquals(
            "Meu_Livro_01-freeferbook.zip",
            BookArchiveManager.suggestedFileName("  Meu Livro: 01  ")
        )
    }

    @Test
    fun `blank title falls back to generic book name`() {
        assertEquals(
            "livro-freeferbook.zip",
            BookArchiveManager.suggestedFileName("***")
        )
    }
}
