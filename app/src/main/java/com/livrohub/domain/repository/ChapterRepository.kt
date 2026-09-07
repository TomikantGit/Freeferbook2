package com.livrohub.domain.repository

import com.livrohub.domain.model.Chapter
import com.livrohub.domain.model.ChapterVersion
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para operações de capítulos e suas versões.
 *
 * Cada capítulo pertence a um livro. Versões formam um histórico imutável:
 * cada salvamento cria uma nova entrada sem alterar as anteriores.
 */
interface ChapterRepository {
    /** Observa todos os capítulos de um livro específico. */
    fun observeChapters(bookId: Long): Flow<List<Chapter>>
    /** Observa um capítulo específico pelo seu ID. */
    fun observeChapter(chapterId: Long): Flow<Chapter?>
    /** Observa todas as versões (histórico) de um capítulo. */
    fun observeVersions(chapterId: Long): Flow<List<ChapterVersion>>
    /** Observa apenas a versão mais recente de um capítulo. */
    fun observeLatestVersion(chapterId: Long): Flow<ChapterVersion?>
    /** Busca diretamente a versão mais recente de um capítulo. */
    suspend fun getLatestVersion(chapterId: Long): ChapterVersion?

    /** Cria um novo capítulo associado a um livro. */
    suspend fun createChapter(bookId: Long, title: String, initialContent: String = ""): Long
    /** Renomeia o título de um capítulo existente. */
    suspend fun renameChapter(chapterId: Long, newTitle: String)
    /** Exclui um capítulo e todo o seu histórico de versões. */
    suspend fun deleteChapter(chapterId: Long)
    /** Salva uma nova versão com o conteúdo atual (implements Immutability pattern). */
    suspend fun saveVersion(chapterId: Long, content: String, message: String?): Long
    /** Obtém uma versão específica pelo ID da versão. */
    suspend fun getVersion(versionId: Long): ChapterVersion?
    /** Obtém uma versão específica baseada no seu número de sequência. */
    suspend fun getVersionBySequence(chapterId: Long, sequenceNumber: Int): ChapterVersion?
}
