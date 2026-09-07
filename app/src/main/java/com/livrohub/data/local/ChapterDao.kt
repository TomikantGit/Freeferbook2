package com.livrohub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room para operações CRUD de capítulos.
 *
 * Capítulos são ordenados por `order_index` dentro de cada livro.
 */
@Dao
interface ChapterDao {

    /** Observa todos os capítulos de um livro, ordenados por índice. */
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY order_index ASC")
    fun observeChapters(bookId: Long): Flow<List<ChapterEntity>>

    /** Busca diretamente todos os capítulos de um livro para exportação/importação. */
    @Query("SELECT * FROM chapters WHERE book_id = :bookId ORDER BY order_index ASC")
    suspend fun getChapters(bookId: Long): List<ChapterEntity>

    /** Observa um capítulo específico. Retorna null se não encontrado. */
    @Query("SELECT * FROM chapters WHERE id = :chapterId")
    fun observeChapter(chapterId: Long): Flow<ChapterEntity?>

    /** Insere um novo capítulo. Aborta em caso de conflito. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(chapter: ChapterEntity): Long

    /** Renomeia um capítulo existente. */
    @Query("UPDATE chapters SET title = :newTitle WHERE id = :chapterId")
    suspend fun rename(chapterId: Long, newTitle: String)

    /** Exclui um capítulo. Versões são removidas em cascata. */
    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun delete(chapterId: Long)

    /** Retorna o maior order_index do livro (-1 se sem capítulos). */
    @Query("SELECT COALESCE(MAX(order_index), -1) FROM chapters WHERE book_id = :bookId")
    suspend fun getLastOrderIndex(bookId: Long): Int
}
