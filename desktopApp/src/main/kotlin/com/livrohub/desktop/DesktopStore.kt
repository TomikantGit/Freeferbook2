package com.livrohub.desktop

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

data class DesktopChapterDocument(
    val chapter: Chapter,
    val draft: String,
    val versions: List<ChapterVersion>
)

data class DesktopCharacterDocument(
    val name: String,
    val surnames: String,
    val chapters: String,
    val imageUri: String?,
    val mediaId: String?
)

data class DesktopLocationDocument(
    val name: String,
    val description: String,
    val chapters: String,
    val imageUri: String?,
    val mediaId: String?
)

data class DesktopImageDocument(
    val url: String,
    val description: String,
    val createdAt: Long,
    val mediaId: String?
)

data class DesktopMediaDocument(
    val id: String,
    val entryName: String,
    val localPath: String
)

data class DesktopBookDocument(
    val book: Book,
    val chapters: List<DesktopChapterDocument>,
    val characters: List<DesktopCharacterDocument> = emptyList(),
    val locations: List<DesktopLocationDocument> = emptyList(),
    val images: List<DesktopImageDocument> = emptyList(),
    val media: List<DesktopMediaDocument> = emptyList()
)

class DesktopStore(
    private val file: Path = defaultPath()
) {
    fun load(): List<DesktopBookDocument> {
        if (!Files.exists(file)) return emptyList()
        DataInputStream(BufferedInputStream(Files.newInputStream(file))).use { input ->
            require(input.readInt() == MAGIC) { "Arquivo local do Freeferbook inválido." }
            val storageVersion = input.readInt()
            require(storageVersion in 1..VERSION) { "Versão do armazenamento Desktop não suportada." }
            val bookCount = input.readBoundedCount(MAX_BOOKS)
            return List(bookCount) {
                val book = Book(
                    id = input.readLong(),
                    title = input.readStringValue(),
                    createdAt = input.readLong()
                )
                val chapterCount = input.readBoundedCount(MAX_CHAPTERS_PER_BOOK)
                val chapters = List(chapterCount) {
                    val chapter = Chapter(
                        id = input.readLong(),
                        bookId = input.readLong(),
                        title = input.readStringValue(),
                        orderIndex = input.readInt(),
                        createdAt = input.readLong()
                    )
                    val draft = input.readStringValue()
                    val versionCount = input.readBoundedCount(MAX_VERSIONS_PER_CHAPTER)
                    val versions = List(versionCount) {
                        ChapterVersion(
                            id = input.readLong(),
                            chapterId = input.readLong(),
                            content = input.readStringValue(),
                            createdAt = input.readLong(),
                            message = input.readNullableString(),
                            sequenceNumber = input.readInt(),
                            wordCount = input.readInt(),
                            charCount = input.readInt(),
                            lineCount = input.readInt()
                        )
                    }
                    DesktopChapterDocument(chapter, draft, versions)
                }
                if (storageVersion == 1) {
                    DesktopBookDocument(book, chapters)
                } else {
                    val characters = List(input.readBoundedCount(MAX_WORLD_ITEMS_PER_BOOK)) {
                        DesktopCharacterDocument(
                            name = input.readStringValue(),
                            surnames = input.readStringValue(),
                            chapters = input.readStringValue(),
                            imageUri = input.readNullableString(),
                            mediaId = input.readNullableString()
                        )
                    }
                    val locations = List(input.readBoundedCount(MAX_WORLD_ITEMS_PER_BOOK)) {
                        DesktopLocationDocument(
                            name = input.readStringValue(),
                            description = input.readStringValue(),
                            chapters = input.readStringValue(),
                            imageUri = input.readNullableString(),
                            mediaId = input.readNullableString()
                        )
                    }
                    val images = List(input.readBoundedCount(MAX_WORLD_ITEMS_PER_BOOK)) {
                        DesktopImageDocument(
                            url = input.readStringValue(),
                            description = input.readStringValue(),
                            createdAt = input.readLong(),
                            mediaId = input.readNullableString()
                        )
                    }
                    val media = List(input.readBoundedCount(MAX_MEDIA_ITEMS_PER_BOOK)) {
                        DesktopMediaDocument(
                            id = input.readStringValue(),
                            entryName = input.readStringValue(),
                            localPath = input.readStringValue()
                        )
                    }
                    DesktopBookDocument(book, chapters, characters, locations, images, media)
                }
            }
        }
    }

    fun save(books: List<DesktopBookDocument>) {
        Files.createDirectories(file.parent)
        val temporary = file.resolveSibling("${file.fileName}.tmp")
        DataOutputStream(BufferedOutputStream(Files.newOutputStream(temporary))).use { output ->
            output.writeInt(MAGIC)
            output.writeInt(VERSION)
            output.writeInt(books.size)
            books.forEach { document ->
                output.writeLong(document.book.id)
                output.writeStringValue(document.book.title)
                output.writeLong(document.book.createdAt)
                output.writeInt(document.chapters.size)
                document.chapters.forEach { chapterDocument ->
                    val chapter = chapterDocument.chapter
                    output.writeLong(chapter.id)
                    output.writeLong(chapter.bookId)
                    output.writeStringValue(chapter.title)
                    output.writeInt(chapter.orderIndex)
                    output.writeLong(chapter.createdAt)
                    output.writeStringValue(chapterDocument.draft)
                    output.writeInt(chapterDocument.versions.size)
                    chapterDocument.versions.forEach { version ->
                        output.writeLong(version.id)
                        output.writeLong(version.chapterId)
                        output.writeStringValue(version.content)
                        output.writeLong(version.createdAt)
                        output.writeNullableString(version.message)
                        output.writeInt(version.sequenceNumber)
                        output.writeInt(version.wordCount)
                        output.writeInt(version.charCount)
                        output.writeInt(version.lineCount)
                    }
                }
                output.writeInt(document.characters.size)
                document.characters.forEach { character ->
                    output.writeStringValue(character.name)
                    output.writeStringValue(character.surnames)
                    output.writeStringValue(character.chapters)
                    output.writeNullableString(character.imageUri)
                    output.writeNullableString(character.mediaId)
                }
                output.writeInt(document.locations.size)
                document.locations.forEach { location ->
                    output.writeStringValue(location.name)
                    output.writeStringValue(location.description)
                    output.writeStringValue(location.chapters)
                    output.writeNullableString(location.imageUri)
                    output.writeNullableString(location.mediaId)
                }
                output.writeInt(document.images.size)
                document.images.forEach { image ->
                    output.writeStringValue(image.url)
                    output.writeStringValue(image.description)
                    output.writeLong(image.createdAt)
                    output.writeNullableString(image.mediaId)
                }
                output.writeInt(document.media.size)
                document.media.forEach { media ->
                    output.writeStringValue(media.id)
                    output.writeStringValue(media.entryName)
                    output.writeStringValue(media.localPath)
                }
            }
        }

        runCatching {
            Files.move(
                temporary,
                file,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }.getOrElse {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun DataInputStream.readBoundedCount(max: Int): Int =
        readInt().also { require(it in 0..max) { "Contagem inválida no armazenamento Desktop." } }

    private fun DataInputStream.readStringValue(): String {
        val size = readInt()
        require(size in 0..MAX_STRING_BYTES) { "Texto local excede o limite suportado." }
        val bytes = ByteArray(size)
        readFully(bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun DataOutputStream.writeStringValue(value: String) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Texto local excede o limite suportado." }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readNullableString(): String? =
        if (readBoolean()) readStringValue() else null

    private fun DataOutputStream.writeNullableString(value: String?) {
        writeBoolean(value != null)
        if (value != null) writeStringValue(value)
    }

    companion object {
        private const val MAGIC = 0x46464231 // FFB1
        private const val VERSION = 2
        private const val MAX_BOOKS = 10_000
        private const val MAX_CHAPTERS_PER_BOOK = 100_000
        private const val MAX_VERSIONS_PER_CHAPTER = 100_000
        private const val MAX_WORLD_ITEMS_PER_BOOK = 100_000
        private const val MAX_MEDIA_ITEMS_PER_BOOK = 100_000
        private const val MAX_STRING_BYTES = 100 * 1024 * 1024

        fun defaultPath(): Path = Path.of(
            System.getProperty("user.home"),
            ".freeferbook",
            "desktop-library-v1.bin"
        )
    }
}
