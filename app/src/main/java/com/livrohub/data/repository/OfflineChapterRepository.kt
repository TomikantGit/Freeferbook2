package com.livrohub.data.repository

import androidx.room.withTransaction
import com.livrohub.data.local.ChapterEntity
import com.livrohub.data.local.ChapterVersionEntity
import com.livrohub.data.local.LivroHubDatabase
import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import com.livrohub.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação offline do [ChapterRepository] usando Room.
 *
 * Gerencia capítulos e suas versões. Cada salvamento cria uma nova
 * [ChapterVersionEntity] preservando o histórico completo.
 * A contagem de palavras, caracteres e linhas é calculada no momento
 * do salvamento para evitar reprocessamento em consultas de leitura.
 *
 * @param database Instância do banco de dados Room.
 * @param clock Função de relógio injetável para facilitar testes.
 */
class OfflineChapterRepository(
    private val database: LivroHubDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ChapterRepository {

    private val chapterDao = database.chapterDao()
    private val versionDao = database.chapterVersionDao()

    override fun observeChapters(bookId: Long): Flow<List<Chapter>> =
        chapterDao.observeChapters(bookId).map { chapters -> chapters.map { it.toDomain() } }

    override fun observeChapter(chapterId: Long): Flow<Chapter?> =
        chapterDao.observeChapter(chapterId).map { it?.toDomain() }

    override fun observeVersions(chapterId: Long): Flow<List<ChapterVersion>> =
        versionDao.observeVersions(chapterId).map { versions -> versions.map { it.toDomain() } }

    override fun observeLatestVersion(chapterId: Long): Flow<ChapterVersion?> =
        versionDao.observeLatestVersion(chapterId).map { it?.toDomain() }

    /**
     * Cria um novo capítulo com uma versão inicial dentro de uma transação Room.
     *
     * O `orderIndex` é calculado automaticamente como `MAX(order_index) + 1`
     * dentro do livro, garantindo ordenação estável.
     */
    override suspend fun createChapter(bookId: Long, title: String, initialContent: String): Long =
        database.withTransaction {
            val now = clock()
            val orderIndex = chapterDao.getLastOrderIndex(bookId) + 1
            val chapterId = chapterDao.insert(
                ChapterEntity(
                    bookId = bookId,
                    title = title.trim(),
                    orderIndex = orderIndex,
                    createdAt = now
                )
            )

            insertVersion(chapterId, initialContent, "Versao inicial", 1, now)

            chapterId
        }

    override suspend fun renameChapter(chapterId: Long, newTitle: String) {
        chapterDao.rename(chapterId, newTitle.trim())
    }

    override suspend fun deleteChapter(chapterId: Long) {
        chapterDao.delete(chapterId)
    }

    /**
     * Salva uma nova versão do capítulo dentro de uma transação Room.
     *
     * O `sequenceNumber` é calculado como `MAX(sequence_number) + 1`
     * para o capítulo, garantindo numeração crescente sem lacunas.
     */
    override suspend fun saveVersion(chapterId: Long, content: String, message: String?): Long =
        database.withTransaction {
            val nextSequence = versionDao.getLastSequenceNumber(chapterId) + 1
            insertVersion(chapterId, content, message?.trim()?.ifBlank { null }, nextSequence, clock())
        }

    /**
     * Insere uma versão calculando métricas de texto (palavras, caracteres, linhas).
     *
     * O Regex de whitespace é compilado como constante no [companion object]
     * para evitar recompilação a cada chamada.
     */
    private suspend fun insertVersion(
        chapterId: Long,
        content: String,
        message: String?,
        sequence: Int,
        time: Long
    ): Long {
        val wordCount = if (content.isBlank()) 0 else WHITESPACE_REGEX.split(content).size
        val lineCount = if (content.isEmpty()) 0 else content.lines().size

        return versionDao.insert(
            ChapterVersionEntity(
                chapterId = chapterId,
                content = content,
                createdAt = time,
                message = message,
                sequenceNumber = sequence,
                wordCount = wordCount,
                charCount = content.length,
                lineCount = lineCount
            )
        )
    }

    override suspend fun getVersion(versionId: Long): ChapterVersion? =
        versionDao.getById(versionId)?.toDomain()

    override suspend fun getVersionBySequence(chapterId: Long, sequenceNumber: Int): ChapterVersion? =
        versionDao.getBySequence(chapterId, sequenceNumber)?.toDomain()

    companion object {
        /** Regex pré-compilado para separação de palavras por whitespace. */
        private val WHITESPACE_REGEX = "\\s+".toRegex()
    }
}
