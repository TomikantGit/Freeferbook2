package com.livrohub.domain.model

/**
 * Modelo de domínio representando um personagem de um livro.
 *
 * @param id Identificador único (0 para novos personagens).
 * @param bookId ID do livro ao qual este personagem pertence.
 * @param name Nome do personagem.
 * @param surnames Sobrenomes do personagem.
 * @param chapters Capítulos em que o personagem aparece (formato livre, ex: "1, 3-5").
 * @param imageUri URI opcional da foto/avatar do personagem.
 */
data class Character(
    val id: Long = 0,
    val bookId: Long,
    val name: String,
    val surnames: String,
    val chapters: String,
    val imageUri: String?
)
