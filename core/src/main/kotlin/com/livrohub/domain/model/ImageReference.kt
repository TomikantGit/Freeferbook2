package com.livrohub.domain.model

/**
 * Modelo de domínio representando uma imagem de referência anexada a um livro.
 *
 * @param id Identificador único (0 para novas imagens).
 * @param bookId ID do livro ao qual esta imagem pertence.
 * @param url URL da imagem (web ou URI local convertida em string).
 * @param description Descrição ou título opcional para a imagem.
 * @param createdAt Timestamp de criação.
 */
data class ImageReference(
    val id: Long = 0,
    val bookId: Long,
    val url: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
