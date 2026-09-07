package com.livrohub.data

import com.livrohub.data.local.BookDao
import com.livrohub.data.local.BookEntity
import com.livrohub.data.local.BookStatsDto
import com.livrohub.data.repository.OfflineBookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfflineBookRepositoryTest {

    @Test
    fun `createBook trims title and uses injected clock`() = runTest {
        val dao = FakeBookDao()
        val repository = OfflineBookRepository(dao, clock = { 123_456L })

        val id = repository.createBook("  Meu Livro  ")

        assertEquals(1L, id)
        assertEquals("Meu Livro", dao.books.single().title)
        assertEquals(123_456L, dao.books.single().createdAt)
    }

    @Test
    fun `rename and delete delegate normalized values to dao`() = runTest {
        val dao = FakeBookDao(
            initialBooks = listOf(BookEntity(id = 7L, title = "Antigo", createdAt = 10L))
        )
        val repository = OfflineBookRepository(dao)

        repository.renameBook(7L, "  Novo Título  ")
        assertEquals("Novo Título", dao.books.single().title)

        repository.deleteBook(7L)
        assertNull(dao.books.firstOrNull())
    }

    @Test
    fun `observeBooksWithStats maps missing and existing aggregates`() = runTest {
        val dao = FakeBookDao(
            initialBooks = listOf(
                BookEntity(id = 1L, title = "A", createdAt = 20L),
                BookEntity(id = 2L, title = "B", createdAt = 10L)
            ),
            stats = listOf(
                BookStatsDto(
                    bookId = 1L,
                    totalChapters = 3,
                    totalLines = 40,
                    totalWords = 500,
                    totalChars = 3_000
                )
            )
        )
        val repository = OfflineBookRepository(dao)

        val result = repository.observeBooksWithStats().first()

        assertEquals(2, result.size)
        assertEquals(3, result[0].totalChapters)
        assertEquals(500, result[0].totalWords)
        assertEquals(0, result[1].totalChapters)
        assertEquals(0, result[1].totalWords)
    }

    private class FakeBookDao(
        initialBooks: List<BookEntity> = emptyList(),
        private val stats: List<BookStatsDto> = emptyList()
    ) : BookDao {
        val books = initialBooks.toMutableList()
        private var nextId = (books.maxOfOrNull { it.id } ?: 0L) + 1L

        override fun observeBooks(): Flow<List<BookEntity>> = flowOf(books.toList())

        override fun observeBook(bookId: Long): Flow<BookEntity?> =
            flowOf(books.firstOrNull { it.id == bookId })

        override suspend fun getBook(bookId: Long): BookEntity? =
            books.firstOrNull { it.id == bookId }

        override fun observeBooksStats(): Flow<List<BookStatsDto>> = flowOf(stats)

        override suspend fun insert(book: BookEntity): Long {
            val assignedId = if (book.id == 0L) nextId++ else book.id
            books += book.copy(id = assignedId)
            return assignedId
        }

        override suspend fun rename(bookId: Long, newTitle: String) {
            val index = books.indexOfFirst { it.id == bookId }
            if (index >= 0) books[index] = books[index].copy(title = newTitle)
        }

        override suspend fun delete(bookId: Long) {
            books.removeAll { it.id == bookId }
        }
    }
}
