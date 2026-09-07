package com.livrohub.domain.model

/**
 * Modelo de domínio representando um capítulo de um livro.
 *
 * @param id Identificador único (0 para novos capítulos).
 * @param bookId ID do livro ao qual este capítulo pertence.
 * @param title Título do capítulo.
 * @param orderIndex Índice de ordenação dentro do livro (0-based).
 * @param createdAt Timestamp de criação (millis desde epoch).
 */
data class Chapter(
    val id: Long = 0,
    val bookId: Long,
    val title: String,
    val orderIndex: Int,
    val createdAt: Long
)
