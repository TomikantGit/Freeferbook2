package com.livrohub.domain.model

/**
 * Modelo de domínio representando um livro/manuscrito.
 *
 * @param id Identificador único (gerado pelo Room).
 * @param title Título do livro.
 * @param createdAt Timestamp de criação (millis desde epoch).
 */
data class Book(
    val id: Long,
    val title: String,
    val createdAt: Long
)
