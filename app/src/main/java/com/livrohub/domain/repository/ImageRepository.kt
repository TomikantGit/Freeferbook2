package com.livrohub.domain.repository

import com.livrohub.domain.model.ImageReference
import kotlinx.coroutines.flow.Flow

/**
 * Contrato para operações de imagens de referência de um livro.
 */
interface ImageRepository {
    /** Observa todas as imagens de um livro específico. */
    fun observeImages(bookId: Long): Flow<List<ImageReference>>
    
    /** Adiciona uma nova imagem de referência. */
    suspend fun addImage(image: ImageReference): Long
    
    /** Remove uma imagem de referência. */
    suspend fun deleteImage(image: ImageReference)
}
