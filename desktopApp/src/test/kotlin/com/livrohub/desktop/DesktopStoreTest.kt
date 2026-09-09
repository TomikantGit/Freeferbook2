package com.livrohub.desktop

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopStoreTest {
    @Test
    fun `save and load preserve draft and versions`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-test")
        val file = directory.resolve("library.bin")
        val store = DesktopStore(file)
        val books = listOf(
            DesktopBookDocument(
                book = Book(1, "Livro", 100),
                chapters = listOf(
                    DesktopChapterDocument(
                        chapter = Chapter(2, 1, "Capítulo", 0, 200),
                        draft = "Texto atual",
                        versions = listOf(
                            ChapterVersion(
                                id = 3,
                                chapterId = 2,
                                content = "Texto salvo",
                                createdAt = 300,
                                message = "Primeira",
                                sequenceNumber = 1,
                                wordCount = 2,
                                charCount = 11,
                                lineCount = 1
                            )
                        )
                    )
                )
            )
        )

        store.save(books)
        val loaded = store.load()

        assertEquals("Livro", loaded.single().book.title)
        assertEquals("Texto atual", loaded.single().chapters.single().draft)
        assertEquals("Texto salvo", loaded.single().chapters.single().versions.single().content)
        assertEquals("Primeira", loaded.single().chapters.single().versions.single().message)
    }
}
