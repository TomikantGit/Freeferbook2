package com.livrohub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DTO para estatísticas agregadas de um livro.
 *
 * Calculado via query SQL que cruza capítulos com suas últimas versões
 * para obter totais de linhas, palavras e caracteres.
 */
data class BookStatsDto(
    val bookId: Long,
    val totalChapters: Int,
    val totalLines: Int,
    val totalWords: Int,
    val totalChars: Int
)

/**
 * DAO Room para operações CRUD de livros.
 *
 * Todas as queries de leitura retornam [Flow] reativo.
 * A query de estatísticas ([observeBooksStats]) usa LEFT JOIN com subquery
 * para obter a última versão de cada capítulo sem carregar conteúdo.
 */
@Dao
interface BookDao {

    /** Observa todos os livros ordenados por data de criação (mais recente primeiro). */
    @Query("SELECT * FROM books ORDER BY created_at DESC")
    fun observeBooks(): Flow<List<BookEntity>>

    /** Observa um livro específico por ID. Retorna null se não encontrado. */
    @Query("SELECT * FROM books WHERE id = :bookId")
    fun observeBook(bookId: Long): Flow<BookEntity?>

    /**
     * Observa estatísticas agregadas de todos os livros.
     *
     * Para cada livro, conta capítulos e soma linhas/palavras/caracteres
     * da última versão de cada capítulo (por MAX(sequence_number)).
     */
    @Query("""
        SELECT 
            c.book_id AS bookId, 
            COUNT(DISTINCT c.id) AS totalChapters, 
            COALESCE(SUM(cv.line_count), 0) AS totalLines, 
            COALESCE(SUM(cv.word_count), 0) AS totalWords, 
            COALESCE(SUM(cv.char_count), 0) AS totalChars
        FROM chapters c
        LEFT JOIN chapter_versions cv ON c.id = cv.chapter_id 
            AND cv.sequence_number = (SELECT MAX(sequence_number) FROM chapter_versions WHERE chapter_id = c.id)
        GROUP BY c.book_id
    """)
    fun observeBooksStats(): Flow<List<BookStatsDto>>

    /** Insere um novo livro. Aborta em caso de conflito de PK. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(book: BookEntity): Long

    /** Renomeia um livro existente. */
    @Query("UPDATE books SET title = :newTitle WHERE id = :bookId")
    suspend fun rename(bookId: Long, newTitle: String)

    /** Exclui um livro. Capítulos e versões são removidos em cascata. */
    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun delete(bookId: Long)
}
