package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.livrohub.domain.model.Character

/**
 * Entidade Room representando um personagem de um livro.
 *
 * Cada personagem pertence a um livro (FK com cascade) e armazena
 * nome, sobrenomes, capítulos em que aparece e URI opcional de imagem.
 *
 * Os nomes das colunas seguem o padrão snake_case para consistência
 * com as demais entidades do banco.
 */
@Entity(
    tableName = "characters",
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
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "surnames")
    val surnames: String,
    @ColumnInfo(name = "chapters")
    val chapters: String,
    @ColumnInfo(name = "image_uri")
    val imageUri: String?
) {
    /** Converte a entidade Room para o modelo de domínio [Character]. */
    fun toDomain(): Character = Character(
        id = id,
        bookId = bookId,
        name = name,
        surnames = surnames,
        chapters = chapters,
        imageUri = imageUri
    )
}

/** Converte o modelo de domínio [Character] para a entidade Room [CharacterEntity]. */
fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    bookId = bookId,
    name = name,
    surnames = surnames,
    chapters = chapters,
    imageUri = imageUri
)
