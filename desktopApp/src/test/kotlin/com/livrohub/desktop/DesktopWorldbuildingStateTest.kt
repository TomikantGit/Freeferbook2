package com.livrohub.desktop

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopWorldbuildingStateTest {
    @Test
    fun `worldbuilding edits persist and orphan local media is deleted`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-world")
        val storeFile = directory.resolve("library.bin")
        val mediaRoot = directory.resolve("media")
        val state = DesktopAppState(
            store = DesktopStore(storeFile),
            archiveManager = DesktopArchiveManager(mediaRoot)
        )
        state.createBook("Livro")

        state.createCharacter()
        state.updateCurrentCharacter(
            requireNotNull(state.currentCharacter).copy(
                name = "Ana",
                surnames = "Silva",
                chapters = "1",
                imageUri = "https://example.com/ana.png"
            )
        )
        state.createLocation()
        state.updateCurrentLocation(
            requireNotNull(state.currentLocation).copy(
                name = "Biblioteca",
                description = "Cenário principal",
                chapters = "1"
            )
        )

        val source = directory.resolve("image.png")
        Files.write(source, byteArrayOf(1, 2, 3, 4))
        state.createLocalImage(source)
        val mediaPath = Path.of(requireNotNull(state.currentBook).media.single().localPath)

        assertTrue(Files.isRegularFile(mediaPath))
        assertEquals("Ana", state.currentBook?.characters?.single()?.name)
        assertEquals("Biblioteca", state.currentBook?.locations?.single()?.name)
        assertEquals(1, state.currentBook?.images?.size)

        state.deleteCurrentImage()

        assertTrue(state.currentBook?.images?.isEmpty() == true)
        assertTrue(state.currentBook?.media?.isEmpty() == true)
        assertFalse(Files.exists(mediaPath))

        val loaded = DesktopStore(storeFile).load().single()
        assertEquals("Ana", loaded.characters.single().name)
        assertEquals("Biblioteca", loaded.locations.single().name)
    }
}
