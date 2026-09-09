package com.livrohub.desktop

import java.nio.file.Files
import java.util.zip.ZipFile
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopAppStateArchiveTest {
    @Test
    fun `export creates version for unsaved draft before writing backup`() {
        val directory = Files.createTempDirectory("freeferbook-desktop-state-archive")
        val state = DesktopAppState(
            store = DesktopStore(directory.resolve("library.bin")),
            archiveManager = DesktopArchiveManager(directory.resolve("media"))
        )
        state.createBook("Livro")
        state.createChapter("Capítulo")
        state.updateDraft("Rascunho ainda não versionado")
        val archive = directory.resolve("backup.freeferbook")

        state.exportCurrentBook(archive)

        assertTrue(Files.isRegularFile(archive))
        assertEquals(1, state.currentChapter?.versions?.size)
        assertEquals("Rascunho ainda não versionado", state.currentChapter?.versions?.single()?.content)
        ZipFile(archive.toFile()).use { zip ->
            val manifest = JSONObject(zip.getInputStream(zip.getEntry("manifest.json")).reader().readText())
            val versions = manifest.getJSONArray("chapters").getJSONObject(0).getJSONArray("versions")
            assertEquals(1, versions.length())
            assertEquals("Rascunho ainda não versionado", versions.getJSONObject(0).getString("content"))
        }
    }
}
