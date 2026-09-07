package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.livrohub.domain.model.Book

/**
 * Entidade Room representando um livro/manuscrito.
 *
 * Cada livro possui um título e timestamp de criação.
 * A exclusão de um livro causa exclusão em cascata dos seus
 * capítulos ([ChapterEntity]) e, consequentemente, das versões
 * ([ChapterVersionEntity]) e personagens ([CharacterEntity]).
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    /** Converte a entidade Room para o modelo de domínio [Book]. */
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        createdAt = createdAt
    )
}
