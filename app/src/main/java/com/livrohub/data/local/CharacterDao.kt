package com.livrohub.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO Room para operações CRUD de personagens.
 *
 * Todas as queries de leitura retornam [Flow] reativo para observação automática.
 */
@Dao
interface CharacterDao {

    /** Observa todos os personagens de um livro, ordenados por nome. */
    @Query("SELECT * FROM characters WHERE book_id = :bookId ORDER BY name ASC")
    fun observeCharactersByBook(bookId: Long): Flow<List<CharacterEntity>>

    /** Insere ou substitui um personagem (upsert via REPLACE). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity): Long

    /** Atualiza um personagem existente. */
    @Update
    suspend fun updateCharacter(character: CharacterEntity)

    /** Exclui um personagem. */
    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)
}
