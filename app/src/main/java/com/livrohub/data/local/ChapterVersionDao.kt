package com.livrohub.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room para operações de versões de capítulos.
 *
 * Versões são ordenadas por `sequence_number` decrescente (mais recente primeiro)
 * para facilitar a exibição no histórico e a recuperação da última versão.
 */
@Dao
interface ChapterVersionDao {

    /** Observa todas as versões de um capítulo (mais recente primeiro). */
    @Query("SELECT * FROM chapter_versions WHERE chapter_id = :chapterId ORDER BY sequence_number DESC")
    fun observeVersions(chapterId: Long): Flow<List<ChapterVersionEntity>>

    /** Observa apenas a última versão (mais recente) de um capítulo. */
    @Query("SELECT * FROM chapter_versions WHERE chapter_id = :chapterId ORDER BY sequence_number DESC LIMIT 1")
    fun observeLatestVersion(chapterId: Long): Flow<ChapterVersionEntity?>

    /** Busca uma versão por ID (não reativo). */
    @Query("SELECT * FROM chapter_versions WHERE id = :versionId")
    suspend fun getById(versionId: Long): ChapterVersionEntity?

    /** Busca uma versão por capítulo e número sequencial (não reativo). */
    @Query("SELECT * FROM chapter_versions WHERE chapter_id = :chapterId AND sequence_number = :sequenceNumber")
    suspend fun getBySequence(chapterId: Long, sequenceNumber: Int): ChapterVersionEntity?

    /** Retorna o maior sequence_number do capítulo (0 se sem versões). */
    @Query("SELECT COALESCE(MAX(sequence_number), 0) FROM chapter_versions WHERE chapter_id = :chapterId")
    suspend fun getLastSequenceNumber(chapterId: Long): Int

    /** Insere uma nova versão. Aborta em caso de conflito de índice único. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(version: ChapterVersionEntity): Long
}
