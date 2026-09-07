package com.livrohub.data.repository

import com.livrohub.data.local.ImageEntity
import com.livrohub.data.local.LivroHubDatabase
import com.livrohub.domain.model.ImageReference
import com.livrohub.domain.repository.ImageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineImageRepository(
    private val database: LivroHubDatabase
) : ImageRepository {

    private val imageDao = database.imageDao()

    override fun observeImages(bookId: Long): Flow<List<ImageReference>> {
        return imageDao.observeImages(bookId).map { entities -> 
            entities.map { it.toDomain() } 
        }
    }

    override suspend fun addImage(image: ImageReference): Long {
        return imageDao.insert(
            ImageEntity(
                id = image.id,
                bookId = image.bookId,
                url = image.url,
                description = image.description,
                createdAt = image.createdAt
            )
        )
    }

    override suspend fun deleteImage(image: ImageReference) {
        imageDao.delete(
            ImageEntity(
                id = image.id,
                bookId = image.bookId,
                url = image.url,
                description = image.description,
                createdAt = image.createdAt
            )
        )
    }
}
