package com.livrohub.desktop

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import java.nio.file.Files
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopArchiveManagerTest {
    @Test
    fun `zip round trip preserves chapters worldbuilding and embedded media`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-archive")
        val sourceMedia = directory.resolve("source.png")
        val mediaBytes = byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10)
        Files.write(sourceMedia, mediaBytes)
        val exportManager = DesktopArchiveManager(directory.resolve("export-media"))
        val archive = directory.resolve("book.freeferbook")
        val document = DesktopBookDocument(
            book = Book(1, "Livro compatível", 100),
            chapters = listOf(
                DesktopChapterDocument(
                    chapter = Chapter(2, 1, "Capítulo 1", 0, 110),
                    draft = "Texto salvo",
                    versions = listOf(
                        ChapterVersion(3, 2, "Texto salvo", 120, "Fixture", 1, 2, 11, 1)
                    )
                )
            ),
            characters = listOf(DesktopCharacterDocument("Ana", "Silva", "1", null, "m1")),
            locations = listOf(DesktopLocationDocument("Biblioteca", "Cenário", "1", null, null)),
            images = listOf(DesktopImageDocument("source.png", "Imagem", 130, "m1")),
            media = listOf(DesktopMediaDocument("m1", "media/source.png", sourceMedia.toString()))
        )

        exportManager.exportBook(document, archive)

        ZipFile(archive.toFile()).use { zip ->
            val manifest = JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).reader().readText())
            assertEquals(DesktopArchiveManager.FORMAT_ID, manifest.getString("format"))
            assertEquals(DesktopArchiveManager.SCHEMA_VERSION, manifest.getInt("schemaVersion"))
            assertNotNull(zip.getEntry("media/source.png"))
        }

        val importMediaRoot = directory.resolve("import-media")
        val imported = DesktopArchiveManager(importMediaRoot).importBook(archive, emptyList())

        assertEquals("Livro compatível", imported.book.title)
        assertEquals("Texto salvo", imported.chapters.single().versions.single().content)
        assertEquals("Ana", imported.characters.single().name)
        assertEquals("Biblioteca", imported.locations.single().name)
        assertEquals("Imagem", imported.images.single().description)
        val media = imported.media.single()
        assertEquals(media.id, imported.characters.single().mediaId)
        assertEquals(media.id, imported.images.single().mediaId)
        assertTrue(Files.isRegularFile(java.nio.file.Path.of(media.localPath)))
        assertArrayEquals(mediaBytes, Files.readAllBytes(java.nio.file.Path.of(media.localPath)))
    }
}
