package com.livrohub.data.repository

import com.livrohub.data.local.CharacterDao
import com.livrohub.data.local.toEntity
import com.livrohub.domain.model.Character
import com.livrohub.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementação offline do [CharacterRepository] usando Room.
 *
 * Utiliza as extensões [toDomain] e [toEntity] definidas nas entidades
 * para mapeamento entre camadas, seguindo o padrão do restante do projeto.
 *
 * @param characterDao DAO Room para operações de personagem.
 */
class OfflineCharacterRepository(
    private val characterDao: CharacterDao
) : CharacterRepository {

    override fun observeCharacters(bookId: Long): Flow<List<Character>> {
        return characterDao.observeCharactersByBook(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    /**
     * Salva um personagem (insere se novo, atualiza se existente).
     *
     * A decisão insert vs update é baseada em `character.id == 0L`:
     * - `id == 0`: novo personagem, Room gera o ID via autoGenerate.
     * - `id != 0`: personagem existente, atualiza todos os campos.
     *
     * @return ID do personagem salvo.
     */
    override suspend fun saveCharacter(character: Character): Long {
        val entity = character.toEntity()
        return if (character.id == 0L) {
            characterDao.insertCharacter(entity)
        } else {
            characterDao.updateCharacter(entity)
            character.id
        }
    }

    override suspend fun deleteCharacter(character: Character) {
        characterDao.deleteCharacter(character.toEntity())
    }
}
