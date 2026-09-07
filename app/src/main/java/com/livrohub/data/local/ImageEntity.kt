package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.livrohub.domain.model.ImageReference

/**
 * Entidade Room representando uma imagem de referência.
 *
 * Associada a um livro, excluída em cascata se o livro for excluído.
 */
@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["book_id"])]
)
data class ImageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    
    @ColumnInfo(name = "url")
    val url: String,
    
    @ColumnInfo(name = "description")
    val description: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    fun toDomain(): ImageReference = ImageReference(
        id = id,
        bookId = bookId,
        url = url,
        description = description,
        createdAt = createdAt
    )
}
