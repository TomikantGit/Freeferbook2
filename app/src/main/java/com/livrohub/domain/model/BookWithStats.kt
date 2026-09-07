package com.livrohub.domain.model

/**
 * Modelo de domínio que agrupa um livro com suas estatísticas.
 *
 * As estatísticas são calculadas a partir da última versão de cada capítulo.
 *
 * @param book O livro base.
 * @param totalChapters Número total de capítulos.
 * @param totalLines Soma de linhas da última versão de cada capítulo.
 * @param totalWords Soma de palavras da última versão de cada capítulo.
 * @param totalChars Soma de caracteres da última versão de cada capítulo.
 */
data class BookWithStats(
    val book: Book,
    val totalChapters: Int,
    val totalLines: Int,
    val totalWords: Int,
    val totalChars: Int
)
