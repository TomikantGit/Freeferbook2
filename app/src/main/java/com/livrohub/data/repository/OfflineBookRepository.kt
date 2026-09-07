package com.livrohub.data.repository

import com.livrohub.data.local.BookEntity
import com.livrohub.data.local.LivroHubDatabase
import com.livrohub.domain.model.Book
import com.livrohub.domain.model.BookWithStats
import com.livrohub.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Implementação offline do [BookRepository] usando Room.
 *
 * Todas as operações de leitura retornam [Flow] reativo via DAOs do Room.
 * Operações de escrita são `suspend` e executam diretamente no DAO.
 *
 * @param database Instância do banco de dados Room.
 * @param clock Função de relógio injetável para facilitar testes. Padrão: [System.currentTimeMillis].
 */
class OfflineBookRepository(
    private val database: LivroHubDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : BookRepository {

    private val bookDao = database.bookDao()

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeBooks().map { books -> books.map { it.toDomain() } }

    /**
     * Observa livros combinados com estatísticas agregadas (capítulos, linhas, palavras, caracteres).
     *
     * Usa [combine] para juntar a lista de livros com as estatísticas calculadas
     * a partir da última versão de cada capítulo.
     */
    override fun observeBooksWithStats(): Flow<List<BookWithStats>> {
        return combine(
            bookDao.observeBooks(),
            bookDao.observeBooksStats()
        ) { books, stats ->
            val statsMap = stats.associateBy { it.bookId }
            books.map { book ->
                val stat = statsMap[book.id]
                BookWithStats(
                    book = book.toDomain(),
                    totalChapters = stat?.totalChapters ?: 0,
                    totalLines = stat?.totalLines ?: 0,
                    totalWords = stat?.totalWords ?: 0,
                    totalChars = stat?.totalChars ?: 0
                )
            }
        }
    }

    override fun observeBook(bookId: Long): Flow<Book?> =
        bookDao.observeBook(bookId).map { it?.toDomain() }

    override suspend fun createBook(title: String): Long =
        bookDao.insert(
            BookEntity(
                title = title.trim(),
                createdAt = clock()
            )
        )

    override suspend fun renameBook(bookId: Long, newTitle: String) {
        bookDao.rename(bookId, newTitle.trim())
    }

    override suspend fun deleteBook(bookId: Long) {
        bookDao.delete(bookId)
    }
}
