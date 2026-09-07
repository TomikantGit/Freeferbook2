package com.livrohub.domain.repository

import com.livrohub.domain.model.Book
import com.livrohub.domain.model.BookWithStats
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para operações de livros.
 *
 * Métodos de leitura retornam [Flow] reativo; métodos de escrita são `suspend`.
 */
interface BookRepository {
    /** Observa todos os livros do banco de dados. */
    fun observeBooks(): Flow<List<Book>>
    /** Observa todos os livros agregando estatísticas (como quantidade de capítulos). */
    fun observeBooksWithStats(): Flow<List<BookWithStats>>
    /** Observa um livro específico pelo seu ID. */
    fun observeBook(bookId: Long): Flow<Book?>

    /** Cria um novo livro e retorna o ID gerado. */
    suspend fun createBook(title: String): Long
    /** Renomeia um livro existente. */
    suspend fun renameBook(bookId: Long, newTitle: String)
    /** Exclui um livro pelo seu ID. */
    suspend fun deleteBook(bookId: Long)
}
