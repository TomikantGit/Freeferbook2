package com.livrohub.desktop

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class DesktopArchiveManager(
    private val mediaRoot: Path = defaultMediaRoot()
) {
    fun exportBook(document: DesktopBookDocument, destination: Path) {
        destination.parent?.let(Files::createDirectories)
        val entryByMediaId = linkedMapOf<String, String>()
        val usedEntries = linkedSetOf<String>()
        val validMedia = document.media.mapNotNull { media ->
            val source = runCatching { Path.of(media.localPath) }.getOrNull()
                ?.takeIf { Files.isRegularFile(it) }
                ?: return@mapNotNull null
            val size = Files.size(source)
            if (size <= 0L || size > MAX_MEDIA_ENTRY_BYTES) return@mapNotNull null
            val entryName = uniqueMediaEntry(media.entryName, usedEntries, entryByMediaId.size)
            entryByMediaId[media.id] = entryName
            Triple(media.id, entryName, source)
        }

        val totalMedia = validMedia.sumOf { Files.size(it.third) }
        require(totalMedia <= MAX_TOTAL_MEDIA_BYTES) { "As mídias do livro excedem o limite de 900 MB." }

        ZipOutputStream(BufferedOutputStream(Files.newOutputStream(destination))).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(buildManifest(document, entryByMediaId).toString(2).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            validMedia.forEach { (_, entryName, source) ->
                zip.putNextEntry(ZipEntry(entryName))
                Files.newInputStream(source).buffered().use { input ->
                    copyWithLimit(input, zip, MAX_MEDIA_ENTRY_BYTES)
                }
                zip.closeEntry()
            }
        }

    }

    fun importBook(source: Path, existingBooks: List<DesktopBookDocument>): DesktopBookDocument {
        require(Files.isRegularFile(source)) { "Arquivo de backup não encontrado." }
        require(Files.size(source) <= MAX_ARCHIVE_BYTES) { "O backup excede o limite de 1 GB." }
        detectUnsupportedArchive(source)?.let { error(it) }

        val createdMedia = mutableListOf<Path>()
        try {
            ZipFile(source.toFile()).use { archive ->
                val manifestEntry = archive.getEntry(MANIFEST_ENTRY)
                    ?: error("Este ZIP não contém um backup Freeferbook reconhecível.")
                if (manifestEntry.size > MAX_MANIFEST_BYTES) error("O manifesto do backup é grande demais.")

                val manifestText = archive.getInputStream(manifestEntry).use { input ->
                    readTextWithLimit(input, MAX_MANIFEST_BYTES)
                }
                val root = runCatching { JSONObject(manifestText) }
                    .getOrElse { error("O manifest.json do backup está corrompido.") }
                validateManifest(root)

                val nextBookId = (existingBooks.maxOfOrNull { it.book.id } ?: 0L) + 1L
                var nextChapterId = (existingBooks.flatMap { it.chapters }.maxOfOrNull { it.chapter.id } ?: 0L) + 1L
                var nextVersionId = (
                    existingBooks.flatMap { it.chapters }.flatMap { it.versions }.maxOfOrNull { it.id } ?: 0L
                    ) + 1L

                val mediaNames = linkedSetOf<String>()
                val charactersArray = root.optJSONArray("characters") ?: JSONArray()
                val locationsArray = root.optJSONArray("locations") ?: JSONArray()
                val imagesArray = root.optJSONArray("images") ?: JSONArray()
                for (index in 0 until charactersArray.length()) {
                    charactersArray.optJSONObject(index)?.nullableString("imageEntry")
                        ?.takeIf(::isSafeMediaEntry)
                        ?.let(mediaNames::add)
                }
                for (index in 0 until locationsArray.length()) {
                    locationsArray.optJSONObject(index)?.nullableString("imageEntry")
                        ?.takeIf(::isSafeMediaEntry)
                        ?.let(mediaNames::add)
                }
                for (index in 0 until imagesArray.length()) {
                    imagesArray.optJSONObject(index)?.nullableString("mediaEntry")
                        ?.takeIf(::isSafeMediaEntry)
                        ?.let(mediaNames::add)
                }

                Files.createDirectories(mediaRoot)
                var totalMedia = 0L
                val mediaIdByEntry = linkedMapOf<String, String>()
                val mediaDocuments = mutableListOf<DesktopMediaDocument>()
                mediaNames.forEachIndexed { index, entryName ->
                    val entry = archive.getEntry(entryName) ?: return@forEachIndexed
                    if (entry.isDirectory || entry.size > MAX_MEDIA_ENTRY_BYTES) return@forEachIndexed
                    val extension = safeExtension(entryName)
                    val mediaId = UUID.randomUUID().toString()
                    val destination = mediaRoot.resolve("$mediaId.$extension")
                    val written = archive.getInputStream(entry).use { input ->
                        Files.newOutputStream(destination).use { output ->
                            copyWithLimit(input, output, MAX_MEDIA_ENTRY_BYTES)
                        }
                    }
                    if (written <= 0L) {
                        Files.deleteIfExists(destination)
                        return@forEachIndexed
                    }
                    totalMedia += written
                    if (totalMedia > MAX_TOTAL_MEDIA_BYTES) {
                        Files.deleteIfExists(destination)
                        error("As mídias do backup excedem o limite de 900 MB.")
                    }
                    createdMedia.add(destination)
                    mediaIdByEntry[entryName] = mediaId
                    mediaDocuments += DesktopMediaDocument(
                        id = mediaId,
                        entryName = sanitizeMediaEntry(entryName, index),
                        localPath = destination.toAbsolutePath().toString()
                    )
                }

                val bookObject = root.getJSONObject("book")
                val chaptersArray = root.optJSONArray("chapters") ?: JSONArray()
                val chapters = buildList {
                    for (index in 0 until chaptersArray.length()) {
                        val chapterObject = chaptersArray.optJSONObject(index) ?: continue
                        val chapterId = nextChapterId++
                        val versionsArray = chapterObject.optJSONArray("versions") ?: JSONArray()
                        val versions = buildList {
                            for (versionIndex in 0 until versionsArray.length()) {
                                val version = versionsArray.optJSONObject(versionIndex) ?: continue
                                val content = version.optString("content", "")
                                add(
                                    ChapterVersion(
                                        id = nextVersionId++,
                                        chapterId = chapterId,
                                        content = content,
                                        createdAt = version.optLong("createdAt", System.currentTimeMillis()),
                                        message = version.nullableString("message"),
                                        sequenceNumber = version.optInt("sequenceNumber", versionIndex + 1)
                                            .takeIf { it > 0 } ?: versionIndex + 1,
                                        wordCount = version.optInt("wordCount", countWords(content)),
                                        charCount = version.optInt("charCount", content.length),
                                        lineCount = version.optInt("lineCount", countLines(content))
                                    )
                                )
                            }
                        }.sortedBy { it.sequenceNumber }
                        add(
                            DesktopChapterDocument(
                                chapter = Chapter(
                                    id = chapterId,
                                    bookId = nextBookId,
                                    title = chapterObject.optString("title", "Capítulo ${index + 1}")
                                        .trim().ifBlank { "Capítulo ${index + 1}" },
                                    orderIndex = chapterObject.optInt("orderIndex", index),
                                    createdAt = chapterObject.optLong("createdAt", System.currentTimeMillis())
                                ),
                                draft = versions.lastOrNull()?.content.orEmpty(),
                                versions = versions
                            )
                        )
                    }
                }

                val characters = buildList {
                    for (index in 0 until charactersArray.length()) {
                        val item = charactersArray.optJSONObject(index) ?: continue
                        add(
                            DesktopCharacterDocument(
                                name = item.optString("name", "Personagem"),
                                surnames = item.optString("surnames", ""),
                                chapters = item.optString("chapters", ""),
                                imageUri = item.nullableString("imageUri"),
                                mediaId = item.nullableString("imageEntry")?.let(mediaIdByEntry::get)
                            )
                        )
                    }
                }
                val locations = buildList {
                    for (index in 0 until locationsArray.length()) {
                        val item = locationsArray.optJSONObject(index) ?: continue
                        add(
                            DesktopLocationDocument(
                                name = item.optString("name", "Local"),
                                description = item.optString("description", ""),
                                chapters = item.optString("chapters", ""),
                                imageUri = item.nullableString("imageUri"),
                                mediaId = item.nullableString("imageEntry")?.let(mediaIdByEntry::get)
                            )
                        )
                    }
                }
                val images = buildList {
                    for (index in 0 until imagesArray.length()) {
                        val item = imagesArray.optJSONObject(index) ?: continue
                        add(
                            DesktopImageDocument(
                                url = item.optString("url", ""),
                                description = item.optString("description", ""),
                                createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                                mediaId = item.nullableString("mediaEntry")?.let(mediaIdByEntry::get)
                            )
                        )
                    }
                }

                return DesktopBookDocument(
                    book = Book(
                        id = nextBookId,
                        title = bookObject.optString("title", "Livro importado").trim().ifBlank { "Livro importado" },
                        createdAt = bookObject.optLong("createdAt", System.currentTimeMillis())
                    ),
                    chapters = chapters,
                    characters = characters,
                    locations = locations,
                    images = images,
                    media = mediaDocuments
                )
            }
        } catch (error: Exception) {
            createdMedia.forEach { runCatching { Files.deleteIfExists(it) } }
            throw error
        }
    }

    private fun buildManifest(
        document: DesktopBookDocument,
        entryByMediaId: Map<String, String>
    ): JSONObject = JSONObject().apply {
        put("format", FORMAT_ID)
        put("schemaVersion", SCHEMA_VERSION)
        put("exportedAt", System.currentTimeMillis())
        put("book", JSONObject().apply {
            put("title", document.book.title)
            put("createdAt", document.book.createdAt)
        })
        put("chapters", JSONArray().apply {
            document.chapters.sortedBy { it.chapter.orderIndex }.forEach { chapter ->
                put(JSONObject().apply {
                    put("title", chapter.chapter.title)
                    put("orderIndex", chapter.chapter.orderIndex)
                    put("createdAt", chapter.chapter.createdAt)
                    put("versions", JSONArray().apply {
                        chapter.versions.sortedBy { it.sequenceNumber }.forEach { version ->
                            put(JSONObject().apply {
                                put("content", version.content)
                                put("createdAt", version.createdAt)
                                put("message", version.message ?: JSONObject.NULL)
                                put("sequenceNumber", version.sequenceNumber)
                                put("wordCount", version.wordCount)
                                put("charCount", version.charCount)
                                put("lineCount", version.lineCount)
                            })
                        }
                    })
                })
            }
        })
        put("characters", JSONArray().apply {
            document.characters.forEach { item ->
                put(JSONObject().apply {
                    put("name", item.name)
                    put("surnames", item.surnames)
                    put("chapters", item.chapters)
                    putNullable("imageUri", item.imageUri)
                    putNullable("imageEntry", item.mediaId?.let(entryByMediaId::get))
                })
            }
        })
        put("locations", JSONArray().apply {
            document.locations.forEach { item ->
                put(JSONObject().apply {
                    put("name", item.name)
                    put("description", item.description)
                    put("chapters", item.chapters)
                    putNullable("imageUri", item.imageUri)
                    putNullable("imageEntry", item.mediaId?.let(entryByMediaId::get))
                })
            }
        })
        put("images", JSONArray().apply {
            document.images.forEach { item ->
                put(JSONObject().apply {
                    put("url", item.url)
                    put("description", item.description)
                    put("createdAt", item.createdAt)
                    putNullable("mediaEntry", item.mediaId?.let(entryByMediaId::get))
                })
            }
        })
    }

    private fun validateManifest(root: JSONObject) {
        require(root.optString("format") == FORMAT_ID) { "Este arquivo não é um backup de livro do Freeferbook." }
        val schema = root.optInt("schemaVersion", 0)
        require(schema > 0) { "O backup não informa uma versão de formato válida." }
        require(schema <= SCHEMA_VERSION) { "Este backup foi criado por uma versão mais nova do Freeferbook." }
        require(root.has("book")) { "O backup não contém os dados do livro." }
    }

    private fun detectUnsupportedArchive(file: Path): String? {
        val header = ByteArray(8)
        val count = FileInputStream(file.toFile()).use { it.read(header) }
        if (count >= 7 && header.copyOfRange(0, 7).contentEquals(RAR4_SIGNATURE)) {
            return "Arquivos RAR ainda não são suportados diretamente. Use o backup ZIP do Freeferbook."
        }
        if (count >= 8 && header.contentEquals(RAR5_SIGNATURE)) {
            return "Arquivos RAR ainda não são suportados diretamente. Use o backup ZIP do Freeferbook."
        }
        if (count >= 6 && header.copyOfRange(0, 6).contentEquals(SEVEN_Z_SIGNATURE)) {
            return "Arquivos 7z ainda não são suportados diretamente. Use o backup ZIP do Freeferbook."
        }
        return null
    }

    private fun readTextWithLimit(input: InputStream, maxBytes: Long): String {
        val output = java.io.ByteArrayOutputStream()
        copyWithLimit(input, output, maxBytes)
        return output.toString(StandardCharsets.UTF_8)
    }

    private fun copyWithLimit(input: InputStream, output: OutputStream, maxBytes: Long): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            total += read
            require(total <= maxBytes) { "Uma entrada do backup excede o limite permitido." }
            output.write(buffer, 0, read)
        }
        return total
    }

    private fun sanitizeMediaEntry(value: String, fallbackIndex: Int): String {
        val safe = value.takeIf(::isSafeMediaEntry)
        if (safe != null) return safe
        return "media/${fallbackIndex.toString().padStart(4, '0')}.bin"
    }

    private fun uniqueMediaEntry(value: String, used: MutableSet<String>, fallbackIndex: Int): String {
        val preferred = sanitizeMediaEntry(value, fallbackIndex)
        if (used.add(preferred)) return preferred

        val extension = safeExtension(preferred)
        var index = fallbackIndex
        while (true) {
            val candidate = "media/${index.toString().padStart(4, '0')}.$extension"
            index++
            if (used.add(candidate)) return candidate
        }
    }

    private fun isSafeMediaEntry(value: String): Boolean =
        value.startsWith("media/") && !value.contains("..") && !value.contains('\\')

    private fun safeExtension(entryName: String): String = entryName.substringAfterLast('.', "bin")
        .lowercase()
        .filter { it.isLetterOrDigit() }
        .take(8)
        .ifBlank { "bin" }

    private fun countWords(content: String): Int =
        if (content.isBlank()) 0 else content.trim().split(WHITESPACE_REGEX).size

    private fun countLines(content: String): Int = if (content.isEmpty()) 0 else content.lineSequence().count()

    companion object {
        const val FORMAT_ID = "freeferbook-book-backup"
        const val SCHEMA_VERSION = 1
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val MAX_ARCHIVE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_MANIFEST_BYTES = 20L * 1024L * 1024L
        private const val MAX_MEDIA_ENTRY_BYTES = 100L * 1024L * 1024L
        private const val MAX_TOTAL_MEDIA_BYTES = 900L * 1024L * 1024L
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val RAR4_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00)
        private val RAR5_SIGNATURE = byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00)
        private val SEVEN_Z_SIGNATURE = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)

        fun suggestedFileName(title: String): String {
            val safe = title.trim()
                .replace(Regex("[^A-Za-z0-9À-ÿ_-]+"), "_")
                .trim('_')
                .ifBlank { "livro" }
                .take(80)
            return "$safe-freeferbook.zip"
        }

        fun defaultMediaRoot(): Path = Path.of(
            System.getProperty("user.home"),
            ".freeferbook",
            "media"
        )
    }
}

private fun JSONObject.putNullable(key: String, value: String?) {
    put(key, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
