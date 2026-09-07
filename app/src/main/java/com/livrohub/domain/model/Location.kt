package com.livrohub.domain.model

/**
 * Modelo de domínio representando um local (cenário, reino, cidade) de um livro.
 *
 * @param id Identificador único (0 para novos locais).
 * @param bookId ID do livro ao qual este local pertence.
 * @param name Nome do local.
 * @param description Descrição ou pseudônimos/apelidos do local. Usado também na busca textual.
 * @param chapters Capítulos em que o local aparece (formato livre, ex: "1, 3-5").
 * @param imageUri URI opcional da foto/mapa do local.
 */
data class Location(
    val id: Long = 0,
    val bookId: Long,
    val name: String,
    val description: String,
    val chapters: String,
    val imageUri: String?
)
