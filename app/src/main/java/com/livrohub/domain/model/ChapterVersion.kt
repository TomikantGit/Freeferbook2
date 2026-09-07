package com.livrohub.domain.model

/**
 * Modelo de domínio representando uma versão salva de um capítulo.
 *
 * Cada versão contém o conteúdo completo do capítulo no momento do salvamento,
 * junto com métricas de texto pré-calculadas para consultas eficientes.
 *
 * @param id Identificador único (0 para novas versões).
 * @param chapterId ID do capítulo ao qual esta versão pertence.
 * @param content Conteúdo textual completo da versão.
 * @param createdAt Timestamp de criação (millis desde epoch).
 * @param message Mensagem opcional descrevendo as alterações.
 * @param sequenceNumber Número sequencial da versão (1, 2, 3...).
 * @param wordCount Contagem de palavras (pré-calculada no salvamento).
 * @param charCount Contagem de caracteres (pré-calculada no salvamento).
 * @param lineCount Contagem de linhas (pré-calculada no salvamento).
 */
data class ChapterVersion(
    val id: Long = 0,
    val chapterId: Long,
    val content: String,
    val createdAt: Long,
    val message: String?,
    val sequenceNumber: Int,
    val wordCount: Int,
    val charCount: Int,
    val lineCount: Int
)
