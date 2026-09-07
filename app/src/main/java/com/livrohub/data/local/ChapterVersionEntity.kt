package com.livrohub.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.livrohub.domain.model.ChapterVersion

/**
 * Entidade Room representando uma versão salva de um capítulo.
 *
 * Cada versão armazena o conteúdo completo do capítulo no momento do salvamento,
 * junto com métricas pré-calculadas (palavras, caracteres, linhas).
 *
 * O histórico é **imutável**: nunca se edita ou deleta uma versão existente.
 * Restaurar uma versão cria uma nova entrada com o mesmo conteúdo.
 *
 * O índice composto `(chapter_id, sequence_number)` garante unicidade
 * e ordenação estável das versões dentro de cada capítulo.
 */
@Entity(
    tableName = "chapter_versions",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapter_id"]),
        Index(value = ["chapter_id", "sequence_number"], unique = true)
    ]
)
data class ChapterVersionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "chapter_id")
    val chapterId: Long,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "message")
    val message: String?,
    @ColumnInfo(name = "sequence_number")
    val sequenceNumber: Int,
    @ColumnInfo(name = "word_count")
    val wordCount: Int,
    @ColumnInfo(name = "char_count")
    val charCount: Int,
    @ColumnInfo(name = "line_count")
    val lineCount: Int
) {
    /** Converte a entidade Room para o modelo de domínio [ChapterVersion]. */
    fun toDomain(): ChapterVersion = ChapterVersion(
        id = id,
        chapterId = chapterId,
        content = content,
        createdAt = createdAt,
        message = message,
        sequenceNumber = sequenceNumber,
        wordCount = wordCount,
        charCount = charCount,
        lineCount = lineCount
    )
}
