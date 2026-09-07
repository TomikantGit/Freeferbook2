package com.livrohub.data

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.BookVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import com.livrohub.data.repository.OfflineBookRepository
import com.livrohub.data.local.BookDao
import com.livrohub.data.local.BookVersionDao
import com.livrohub.data.local.BookEntity
import com.livrohub.data.local.BookVersionEntity

class OfflineBookRepositoryTest {

    // Fakes simples para testar a logica
    private val bookDao = FakeBookDao()
    private val bookVersionDao = FakeBookVersionDao()
    private val repository = OfflineBookRepository(bookDao, bookVersionDao)

    @Test
    fun `saveVersion should create new version with incremented sequence number`() = runTest {
        val bookId = 1L
        
        val version1 = repository.saveVersion(bookId, "Conteudo V1", "Mensagem 1")
        assertEquals(1, version1.sequenceNumber)
        assertEquals("Conteudo V1", version1.content)

        val version2 = repository.saveVersion(bookId, "Conteudo V2", "Mensagem 2")
        assertEquals(2, version2.sequenceNumber)
        assertEquals("Conteudo V2", version2.content)
    }

    class FakeBookDao : BookDao {
        override fun getAllBooks(): Flow<List<BookEntity>> = flowOf(emptyList())
        override fun getBookById(id: Long): Flow<BookEntity?> = flowOf(null)
        override suspend fun insert(book: BookEntity): Long = 1L
        override suspend fun update(book: BookEntity) {}
        override suspend fun delete(book: BookEntity) {}
    }

    class FakeBookVersionDao : BookVersionDao {
        private var maxSequence = 0

        override fun getVersionsByBookId(bookId: Long): Flow<List<BookVersionEntity>> = flowOf(emptyList())
        override fun getLatestVersionByBookId(bookId: Long): Flow<BookVersionEntity?> = flowOf(null)
        
        override suspend fun getMaxSequenceNumber(bookId: Long): Int? {
            return if (maxSequence == 0) null else maxSequence
        }
        
        override suspend fun insert(version: BookVersionEntity): Long {
            maxSequence = version.sequenceNumber
            return 1L
        }
    }
}
