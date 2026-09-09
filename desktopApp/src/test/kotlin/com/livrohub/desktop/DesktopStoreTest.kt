package com.livrohub.desktop

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `storage v2 preserves worldbuilding and media metadata`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-v2")
        val store = DesktopStore(directory.resolve("library.bin"))
        val book = DesktopBookDocument(
            book = Book(1, "Livro", 100),
            chapters = emptyList(),
            characters = listOf(DesktopCharacterDocument("Ana", "Silva", "1", null, "media-1")),
            locations = listOf(DesktopLocationDocument("Biblioteca", "Antiga", "1", null, null)),
            images = listOf(DesktopImageDocument("capa.png", "Capa", 200, "media-1")),
            media = listOf(DesktopMediaDocument("media-1", "media/capa.png", "C:/tmp/capa.png"))
        )

        store.save(listOf(book))
        val loaded = store.load().single()

        assertEquals("Ana", loaded.characters.single().name)
        assertEquals("Biblioteca", loaded.locations.single().name)
        assertEquals("Capa", loaded.images.single().description)
        assertEquals("media/capa.png", loaded.media.single().entryName)
    }

    @Test
    fun `storage v1 remains readable after v2 migration`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-v1")
        val file = directory.resolve("library.bin")
        DataOutputStream(BufferedOutputStream(Files.newOutputStream(file))).use { output ->
            output.writeInt(0x46464231)
            output.writeInt(1)
            output.writeInt(1)
            output.writeLong(7)
            output.writeLegacyString("Livro legado")
            output.writeLong(123)
            output.writeInt(0)
        }

        val loaded = DesktopStore(file).load().single()

        assertEquals("Livro legado", loaded.book.title)
        assertTrue(loaded.characters.isEmpty())
        assertTrue(loaded.locations.isEmpty())
        assertTrue(loaded.images.isEmpty())
        assertTrue(loaded.media.isEmpty())
    }

    private fun DataOutputStream.writeLegacyString(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        writeInt(bytes.size)
        write(bytes)
    }
}
