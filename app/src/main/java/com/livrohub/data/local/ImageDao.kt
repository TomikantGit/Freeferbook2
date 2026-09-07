package com.livrohub.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO para gerenciar imagens.
 */
@Dao
interface ImageDao {
    /** Observa todas as imagens de um livro, ordenadas por data de criação. */
    @Query("SELECT * FROM images WHERE book_id = :bookId ORDER BY created_at DESC")
    fun observeImages(bookId: Long): Flow<List<ImageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageEntity): Long

    @Delete
    suspend fun delete(image: ImageEntity)
}
