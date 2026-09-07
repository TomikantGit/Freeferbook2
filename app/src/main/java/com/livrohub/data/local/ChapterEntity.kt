package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.livrohub.domain.model.Chapter

/**
 * Entidade Room representando um capítulo de um livro.
 *
 * Cada capítulo pertence a um livro (FK com cascade) e possui
 * um `orderIndex` para manter a ordenação dentro do livro.
 *
 * A exclusão do livro pai causa exclusão em cascata deste capítulo
 * e de todas as suas versões ([ChapterVersionEntity]).
 */
@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["book_id"])
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "order_index")
    val orderIndex: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
) {
    /** Converte a entidade Room para o modelo de domínio [Chapter]. */
    fun toDomain(): Chapter = Chapter(
        id = id,
        bookId = bookId,
        title = title,
        orderIndex = orderIndex,
        createdAt = createdAt
    )
}
