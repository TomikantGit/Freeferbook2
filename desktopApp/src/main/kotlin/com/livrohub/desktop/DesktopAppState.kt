package com.livrohub.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import com.livrohub.domain.diff.DiffLine
import com.livrohub.domain.diff.TextDiffEngine
import com.livrohub.domain.revision.TextRevisionEngine
import java.nio.file.Path

enum class DesktopWorkspaceMode {
    CHAPTERS,
    CHARACTERS,
    LOCATIONS,
    IMAGES
}

data class DesktopVersionDiff(
    val baseSequence: Int,
    val targetSequence: Int,
    val lines: List<DiffLine>
)

class DesktopAppState(
    private val store: DesktopStore = DesktopStore(),
    private val archiveManager: DesktopArchiveManager = DesktopArchiveManager()
) {
    private val diffEngine = TextDiffEngine()

    var books by mutableStateOf(runCatching(store::load).getOrDefault(emptyList()))
        private set

    var selectedBookId by mutableStateOf(books.firstOrNull()?.book?.id)
        private set

    var selectedChapterId by mutableStateOf(
        books.firstOrNull()?.chapters?.minByOrNull { it.chapter.orderIndex }?.chapter?.id
    )
        private set

    var notice by mutableStateOf<String?>(null)
        private set

    var workspaceMode by mutableStateOf(DesktopWorkspaceMode.CHAPTERS)
        private set

    var selectedCharacterIndex by mutableStateOf<Int?>(null)
        private set

    var selectedLocationIndex by mutableStateOf<Int?>(null)
        private set

    var selectedImageIndex by mutableStateOf<Int?>(null)
        private set

    var compareBaseVersionId by mutableStateOf<Long?>(null)
        private set

    var versionDiff by mutableStateOf<DesktopVersionDiff?>(null)
        private set

    val currentBook: DesktopBookDocument?
        get() = books.firstOrNull { it.book.id == selectedBookId }

    val currentChapter: DesktopChapterDocument?
        get() = currentBook?.chapters?.firstOrNull { it.chapter.id == selectedChapterId }

    val currentCharacter: DesktopCharacterDocument?
        get() = selectedCharacterIndex?.let { currentBook?.characters?.getOrNull(it) }

    val currentLocation: DesktopLocationDocument?
        get() = selectedLocationIndex?.let { currentBook?.locations?.getOrNull(it) }

    val currentImage: DesktopImageDocument?
        get() = selectedImageIndex?.let { currentBook?.images?.getOrNull(it) }

    fun selectBook(bookId: Long) {
        selectedBookId = bookId
        selectedChapterId = currentBook?.chapters?.minByOrNull { it.chapter.orderIndex }?.chapter?.id
        workspaceMode = DesktopWorkspaceMode.CHAPTERS
        selectedCharacterIndex = null
        selectedLocationIndex = null
        selectedImageIndex = null
        compareBaseVersionId = null
        versionDiff = null
    }

    fun selectWorkspaceMode(mode: DesktopWorkspaceMode) {
        workspaceMode = mode
    }

    fun selectChapter(chapterId: Long) {
        selectedChapterId = chapterId
        compareBaseVersionId = null
        versionDiff = null
    }

    fun selectCharacter(index: Int) {
        selectedCharacterIndex = index.takeIf { it in currentBook?.characters.orEmpty().indices }
    }

    fun selectLocation(index: Int) {
        selectedLocationIndex = index.takeIf { it in currentBook?.locations.orEmpty().indices }
    }

    fun selectImage(index: Int) {
        selectedImageIndex = index.takeIf { it in currentBook?.images.orEmpty().indices }
    }

    fun createBook(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        val id = (books.maxOfOrNull { it.book.id } ?: 0L) + 1L
        val document = DesktopBookDocument(
            book = Book(id = id, title = clean, createdAt = System.currentTimeMillis()),
            chapters = emptyList()
        )
        books = books + document
        selectedBookId = id
        selectedChapterId = null
        workspaceMode = DesktopWorkspaceMode.CHAPTERS
        selectedCharacterIndex = null
        selectedLocationIndex = null
        selectedImageIndex = null
        persist()
    }

    fun createChapter(title: String) {
        val book = currentBook ?: return
        val clean = title.trim()
        if (clean.isEmpty()) return
        val id = (books.flatMap { it.chapters }.maxOfOrNull { it.chapter.id } ?: 0L) + 1L
        val order = (book.chapters.maxOfOrNull { it.chapter.orderIndex } ?: -1) + 1
        val chapter = DesktopChapterDocument(
            chapter = Chapter(
                id = id,
                bookId = book.book.id,
                title = clean,
                orderIndex = order,
                createdAt = System.currentTimeMillis()
            ),
            draft = "",
            versions = emptyList()
        )
        replaceCurrentBook(book.copy(chapters = book.chapters + chapter))
        selectedChapterId = id
        persist()
    }

    fun updateDraft(text: String) {
        updateCurrentChapter { it.copy(draft = text) }
    }

    fun saveVersion(message: String? = null) {
        val document = currentChapter ?: return
        val nextSequence = (document.versions.maxOfOrNull { it.sequenceNumber } ?: 0) + 1
        val nextId = (books.flatMap { it.chapters }.flatMap { it.versions }.maxOfOrNull { it.id } ?: 0L) + 1L
        val content = document.draft
        val result = TextRevisionEngine.review(content)
        val version = ChapterVersion(
            id = nextId,
            chapterId = document.chapter.id,
            content = content,
            createdAt = System.currentTimeMillis(),
            message = message?.trim()?.takeIf { it.isNotEmpty() },
            sequenceNumber = nextSequence,
            wordCount = result.wordCount,
            charCount = content.length,
            lineCount = if (content.isEmpty()) 0 else content.lineSequence().count()
        )
        updateCurrentChapter { it.copy(versions = it.versions + version) }
        persist()
    }

    fun restoreVersion(version: ChapterVersion) {
        updateDraft(version.content)
        saveVersion("Restaurado da versão #${version.sequenceNumber}")
    }

    fun selectVersionForComparison(version: ChapterVersion) {
        val chapter = currentChapter ?: return
        val baseId = compareBaseVersionId
        if (baseId == null) {
            compareBaseVersionId = version.id
            notice = "Versão #${version.sequenceNumber} selecionada como base da comparação."
            return
        }
        if (baseId == version.id) {
            compareBaseVersionId = null
            notice = "Comparação cancelada."
            return
        }

        val base = chapter.versions.firstOrNull { it.id == baseId } ?: run {
            compareBaseVersionId = null
            return
        }
        val (oldVersion, newVersion) = if (base.sequenceNumber <= version.sequenceNumber) {
            base to version
        } else {
            version to base
        }
        versionDiff = DesktopVersionDiff(
            baseSequence = oldVersion.sequenceNumber,
            targetSequence = newVersion.sequenceNumber,
            lines = diffEngine.compare(oldVersion.content, newVersion.content)
        )
        compareBaseVersionId = null
    }

    fun clearVersionDiff() {
        versionDiff = null
    }

    fun applySafeRevisionFixes() {
        val chapter = currentChapter ?: return
        updateDraft(TextRevisionEngine.applyAllSafe(chapter.draft))
    }

    fun createCharacter() {
        val book = currentBook ?: return
        val next = book.characters + DesktopCharacterDocument(
            name = "Novo personagem",
            surnames = "",
            chapters = "",
            imageUri = null,
            mediaId = null
        )
        replaceCurrentBook(book.copy(characters = next))
        workspaceMode = DesktopWorkspaceMode.CHARACTERS
        selectedCharacterIndex = next.lastIndex
        persist()
    }

    fun updateCurrentCharacter(value: DesktopCharacterDocument) {
        val book = currentBook ?: return
        val index = selectedCharacterIndex ?: return
        val old = book.characters.getOrNull(index) ?: return
        val adjusted = if (old.imageUri != value.imageUri) value.copy(mediaId = null) else value
        replaceCurrentBookWithMediaCleanup(
            book.copy(characters = book.characters.mapIndexed { itemIndex, item ->
                if (itemIndex == index) adjusted else item
            })
        )
        persist()
    }

    fun deleteCurrentCharacter() {
        val book = currentBook ?: return
        val index = selectedCharacterIndex ?: return
        if (index !in book.characters.indices) return
        replaceCurrentBookWithMediaCleanup(
            book.copy(characters = book.characters.filterIndexed { itemIndex, _ -> itemIndex != index })
        )
        selectedCharacterIndex = null
        persist()
    }

    fun createLocation() {
        val book = currentBook ?: return
        val next = book.locations + DesktopLocationDocument(
            name = "Novo local",
            description = "",
            chapters = "",
            imageUri = null,
            mediaId = null
        )
        replaceCurrentBook(book.copy(locations = next))
        workspaceMode = DesktopWorkspaceMode.LOCATIONS
        selectedLocationIndex = next.lastIndex
        persist()
    }

    fun updateCurrentLocation(value: DesktopLocationDocument) {
        val book = currentBook ?: return
        val index = selectedLocationIndex ?: return
        val old = book.locations.getOrNull(index) ?: return
        val adjusted = if (old.imageUri != value.imageUri) value.copy(mediaId = null) else value
        replaceCurrentBookWithMediaCleanup(
            book.copy(locations = book.locations.mapIndexed { itemIndex, item ->
                if (itemIndex == index) adjusted else item
            })
        )
        persist()
    }

    fun deleteCurrentLocation() {
        val book = currentBook ?: return
        val index = selectedLocationIndex ?: return
        if (index !in book.locations.indices) return
        replaceCurrentBookWithMediaCleanup(
            book.copy(locations = book.locations.filterIndexed { itemIndex, _ -> itemIndex != index })
        )
        selectedLocationIndex = null
        persist()
    }

    fun createImageUrl(url: String) {
        val book = currentBook ?: return
        val clean = url.trim()
        if (clean.isEmpty()) return
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            notice = "Use uma URL http:// ou https:// para imagens remotas."
            return
        }
        val next = book.images + DesktopImageDocument(
            url = clean,
            description = "",
            createdAt = System.currentTimeMillis(),
            mediaId = null
        )
        replaceCurrentBook(book.copy(images = next))
        workspaceMode = DesktopWorkspaceMode.IMAGES
        selectedImageIndex = next.lastIndex
        persist()
    }

    fun createLocalImage(source: Path) {
        val book = currentBook ?: return
        runCatching { archiveManager.attachLocalMedia(source) }
            .onSuccess { media ->
                val nextImages = book.images + DesktopImageDocument(
                    url = source.fileName.toString(),
                    description = source.fileName.toString(),
                    createdAt = System.currentTimeMillis(),
                    mediaId = media.id
                )
                replaceCurrentBook(book.copy(images = nextImages, media = book.media + media))
                workspaceMode = DesktopWorkspaceMode.IMAGES
                selectedImageIndex = nextImages.lastIndex
                persist()
            }
            .onFailure { error -> notice = error.message ?: "Não foi possível adicionar a imagem." }
    }

    fun updateCurrentImage(value: DesktopImageDocument) {
        val book = currentBook ?: return
        val index = selectedImageIndex ?: return
        val old = book.images.getOrNull(index) ?: return
        val adjusted = if (old.url != value.url && old.mediaId != null) value.copy(mediaId = null) else value
        replaceCurrentBookWithMediaCleanup(
            book.copy(images = book.images.mapIndexed { itemIndex, item ->
                if (itemIndex == index) adjusted else item
            })
        )
        persist()
    }

    fun deleteCurrentImage() {
        val book = currentBook ?: return
        val index = selectedImageIndex ?: return
        if (index !in book.images.indices) return
        replaceCurrentBookWithMediaCleanup(
            book.copy(images = book.images.filterIndexed { itemIndex, _ -> itemIndex != index })
        )
        selectedImageIndex = null
        persist()
    }

    fun importBook(source: Path) {
        runCatching { archiveManager.importBook(source, books) }
            .onSuccess { imported ->
                books = books + imported
                selectedBookId = imported.book.id
                selectedChapterId = imported.chapters.minByOrNull { it.chapter.orderIndex }?.chapter?.id
                workspaceMode = DesktopWorkspaceMode.CHAPTERS
                selectedCharacterIndex = null
                selectedLocationIndex = null
                selectedImageIndex = null
                compareBaseVersionId = null
                versionDiff = null
                persist()
                notice = "Livro \"${imported.book.title}\" importado com ${imported.chapters.size} capítulo(s)."
            }
            .onFailure { error ->
                notice = error.message ?: "Não foi possível importar o backup."
            }
    }

    fun exportCurrentBook(destination: Path) {
        val book = currentBook ?: return
        runCatching {
            val prepared = prepareBookForBackup(book)
            archiveManager.exportBook(prepared, destination)
        }.onSuccess {
            notice = "Backup de \"${book.book.title}\" criado em ${destination.fileName}."
        }.onFailure { error ->
            notice = error.message ?: "Não foi possível exportar o backup."
        }
    }

    fun clearNotice() {
        notice = null
    }

    fun persist() {
        store.save(books)
    }

    private fun prepareBookForBackup(book: DesktopBookDocument): DesktopBookDocument {
        var nextVersionId = (
            books.flatMap { it.chapters }.flatMap { it.versions }.maxOfOrNull { it.id } ?: 0L
            ) + 1L
        var changed = false
        val chapters = book.chapters.map { document ->
            val latest = document.versions.maxByOrNull { it.sequenceNumber }
            if (latest?.content == document.draft) {
                document
            } else {
                changed = true
                val content = document.draft
                val review = TextRevisionEngine.review(content)
                val nextSequence = (document.versions.maxOfOrNull { it.sequenceNumber } ?: 0) + 1
                document.copy(
                    versions = document.versions + ChapterVersion(
                        id = nextVersionId++,
                        chapterId = document.chapter.id,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        message = "Versão automática antes do backup Desktop",
                        sequenceNumber = nextSequence,
                        wordCount = review.wordCount,
                        charCount = content.length,
                        lineCount = if (content.isEmpty()) 0 else content.lineSequence().count()
                    )
                )
            }
        }
        val prepared = book.copy(chapters = chapters)
        if (changed) {
            replaceCurrentBook(prepared)
            persist()
        }
        return prepared
    }

    private fun updateCurrentChapter(transform: (DesktopChapterDocument) -> DesktopChapterDocument) {
        val book = currentBook ?: return
        val chapterId = selectedChapterId ?: return
        replaceCurrentBook(
            book.copy(
                chapters = book.chapters.map { chapter ->
                    if (chapter.chapter.id == chapterId) transform(chapter) else chapter
                }
            )
        )
    }

    private fun replaceCurrentBook(next: DesktopBookDocument) {
        books = books.map { if (it.book.id == next.book.id) next else it }
    }

    private fun replaceCurrentBookWithMediaCleanup(next: DesktopBookDocument) {
        val referenced = buildSet {
            next.characters.mapNotNullTo(this) { it.mediaId }
            next.locations.mapNotNullTo(this) { it.mediaId }
            next.images.mapNotNullTo(this) { it.mediaId }
        }
        val (kept, orphaned) = next.media.partition { it.id in referenced }
        orphaned.forEach(archiveManager::deleteLocalMedia)
        replaceCurrentBook(next.copy(media = kept))
    }
}
