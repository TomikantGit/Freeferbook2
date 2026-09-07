package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.livrohub.domain.model.Location

/**
 * Entidade Room representando um local de um livro.
 *
 * Cada local pertence a um livro (FK com cascade) e armazena
 * nome, descrição, capítulos em que aparece e URI opcional de imagem.
 */
@Entity(
    tableName = "locations",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("book_id")
    ]
)
data class LocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String,
    @ColumnInfo(name = "chapters")
    val chapters: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?
) {
    /** Converte a entidade Room para o modelo de domínio [Location]. */
    fun toDomain(): Location = Location(
        id = id,
        bookId = bookId,
        name = name,
        description = description,
        chapters = chapters,
        imageUri = imageUri
    )
}

/** Converte o modelo de domínio [Location] para a entidade Room [LocationEntity]. */
fun Location.toEntity(): LocationEntity = LocationEntity(
    id = id,
    bookId = bookId,
    name = name,
    description = description,
    chapters = chapters,
    imageUri = imageUri
)
