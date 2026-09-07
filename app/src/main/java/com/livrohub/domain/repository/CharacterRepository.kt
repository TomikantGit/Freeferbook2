package com.livrohub.domain.repository

import com.livrohub.domain.model.Character
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para operações CRUD de personagens de um livro.
 */
interface CharacterRepository {
    /** Observa a lista de personagens de um determinado livro. */
    fun observeCharacters(bookId: Long): Flow<List<Character>>
    /** Salva um personagem (cria se o ID for 0, atualiza caso contrário) e retorna seu ID. */
    suspend fun saveCharacter(character: Character): Long
    /** Exclui um personagem do banco de dados. */
    suspend fun deleteCharacter(character: Character)
}
