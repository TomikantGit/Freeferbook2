package com.livrohub.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.livrohub.domain.model.Book
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import com.livrohub.domain.revision.TextRevisionEngine

class DesktopAppState(
    private val store: DesktopStore = DesktopStore()
) {
    var books by mutableStateOf(runCatching(store::load).getOrDefault(emptyList()))
        private set

    var selectedBookId by mutableStateOf(books.firstOrNull()?.book?.id)
        private set

    var selectedChapterId by mutableStateOf(
        books.firstOrNull()?.chapters?.minByOrNull { it.chapter.orderIndex }?.chapter?.id
    )
        private set

    val currentBook: DesktopBookDocument?
        get() = books.firstOrNull { it.book.id == selectedBookId }

    val currentChapter: DesktopChapterDocument?
        get() = currentBook?.chapters?.firstOrNull { it.chapter.id == selectedChapterId }

    fun selectBook(bookId: Long) {
        selectedBookId = bookId
        selectedChapterId = currentBook?.chapters?.minByOrNull { it.chapter.orderIndex }?.chapter?.id
    }

    fun selectChapter(chapterId: Long) {
        selectedChapterId = chapterId
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

    fun applySafeRevisionFixes() {
        val chapter = currentChapter ?: return
        updateDraft(TextRevisionEngine.applyAllSafe(chapter.draft))
    }

    fun persist() {
        store.save(books)
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
}
