package com.livrohub.domain.diff

import com.github.difflib.text.DiffRowGenerator
import com.github.difflib.text.DiffRow

/**
 * Tipo de alteração de uma linha no diff.
 */
enum class DiffLineType {
    /** Linha sem alteração. */
    Unchanged,
    /** Linha adicionada no texto novo. */
    Added,
    /** Linha removida do texto antigo. */
    Removed
}

/**
 * Tipo de alteração de um span (trecho) dentro de uma linha de diff.
 */
enum class DiffSpanType {
    /** Trecho sem alteração. */
    Unchanged,
    /** Trecho adicionado. */
    Added,
    /** Trecho removido. */
    Removed
}

/**
 * Representa um trecho de texto dentro de uma linha de diff,
 * com seu tipo de alteração.
 *
 * @param type Tipo de alteração do trecho.
 * @param text Conteúdo textual do trecho.
 */
data class DiffSpan(
    val type: DiffSpanType,
    val text: String
)

/**
 * Representa uma linha completa no resultado do diff.
 *
 * Cada linha pode ser composta de múltiplos [DiffSpan] quando há
 * diferenças por palavra dentro da mesma linha (inline diff).
 *
 * @param type Tipo geral da linha (adicionada, removida ou inalterada).
 * @param spans Lista de trechos com marcação individual de alteração.
 */
data class DiffLine(
    val type: DiffLineType,
    val spans: List<DiffSpan>
) {
    /** Prefixo visual da linha: `+` para adição, `-` para remoção, espaço para inalterada. */
    val prefix: String
        get() = when (type) {
            DiffLineType.Unchanged -> " "
            DiffLineType.Added -> "+"
            DiffLineType.Removed -> "-"
        }

    /** Texto completo da linha sem marcação de spans (para acesso simplificado). */
    val text: String
        get() = spans.joinToString("") { it.text }
}

/**
 * Motor de comparação de texto usando `java-diff-utils`.
 *
 * Gera diffs linha a linha com marcação inline por palavra.
 * As tags de marcação e o [DiffRowGenerator] são armazenados no
 * [companion object] como constantes thread-safe para evitar
 * reinstanciação a cada uso.
 *
 * Uso:
 * ```kotlin
 * val engine = TextDiffEngine()
 * val diffLines = engine.compare(oldText, newText)
 * ```
 */
class TextDiffEngine {

    /**
     * Compara dois textos e retorna a lista de [DiffLine] resultante.
     *
     * @param oldText Texto base (versão anterior).
     * @param newText Texto alvo (versão atual).
     * @return Lista de linhas de diff com marcação por palavra.
     */
    fun compare(oldText: String, newText: String): List<DiffLine> {
        val oldLines = oldText.toDiffLines()
        val newLines = newText.toDiffLines()

        val rows = generator.generateDiffRows(oldLines, newLines)
        val result = mutableListOf<DiffLine>()

        for (row in rows) {
            when (row.tag) {
                DiffRow.Tag.EQUAL -> {
                    result += DiffLine(
                        type = DiffLineType.Unchanged,
                        spans = listOf(DiffSpan(DiffSpanType.Unchanged, row.oldLine))
                    )
                }
                DiffRow.Tag.INSERT -> {
                    result += DiffLine(
                        type = DiffLineType.Added,
                        spans = parseSpans(row.newLine, INS_START, INS_END, DiffSpanType.Added)
                    )
                }
                DiffRow.Tag.DELETE -> {
                    result += DiffLine(
                        type = DiffLineType.Removed,
                        spans = parseSpans(row.oldLine, DEL_START, DEL_END, DiffSpanType.Removed)
                    )
                }
                DiffRow.Tag.CHANGE -> {
                    result += DiffLine(
                        type = DiffLineType.Removed,
                        spans = parseSpans(row.oldLine, DEL_START, DEL_END, DiffSpanType.Removed)
                    )
                    result += DiffLine(
                        type = DiffLineType.Added,
                        spans = parseSpans(row.newLine, INS_START, INS_END, DiffSpanType.Added)
                    )
                }
                else -> Unit
            }
        }

        return result
    }

    companion object {
        private const val DEL_START = "[[[DEL]]]"
        private const val DEL_END = "[[[/DEL]]]"
        private const val INS_START = "[[[INS]]]"
        private const val INS_END = "[[[/INS]]]"

        /**
         * Gerador de diff rows pré-configurado.
         * Thread-safe e imutável após construção.
         */
        private val generator: DiffRowGenerator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .inlineDiffByWord(true)
            .oldTag { f -> if (f) DEL_START else DEL_END }
            .newTag { f -> if (f) INS_START else INS_END }
            .build()

        /**
         * Faz parsing das tags de marcação inline dentro de uma linha,
         * retornando uma lista de [DiffSpan] com trechos marcados e não marcados.
         */
        internal fun parseSpans(
            textWithTags: String,
            startTag: String,
            endTag: String,
            activeType: DiffSpanType
        ): List<DiffSpan> {
            val spans = mutableListOf<DiffSpan>()
            var currentIdx = 0

            while (currentIdx < textWithTags.length) {
                val startIdx = textWithTags.indexOf(startTag, currentIdx)
                if (startIdx == -1) {
                    val remaining = textWithTags.substring(currentIdx)
                    if (remaining.isNotEmpty()) {
                        spans.add(DiffSpan(DiffSpanType.Unchanged, remaining))
                    }
                    break
                }

                if (startIdx > currentIdx) {
                    val unchangedText = textWithTags.substring(currentIdx, startIdx)
                    spans.add(DiffSpan(DiffSpanType.Unchanged, unchangedText))
                }

                val endIdx = textWithTags.indexOf(endTag, startIdx + startTag.length)
                if (endIdx == -1) {
                    val modifiedText = textWithTags.substring(startIdx + startTag.length)
                    spans.add(DiffSpan(activeType, modifiedText))
                    break
                }

                val modifiedText = textWithTags.substring(startIdx + startTag.length, endIdx)
                spans.add(DiffSpan(activeType, modifiedText))

                currentIdx = endIdx + endTag.length
            }

            if (spans.isEmpty() && textWithTags.isEmpty()) {
                spans.add(DiffSpan(DiffSpanType.Unchanged, ""))
            }

            return spans
        }
    }

    private fun String.toDiffLines(): List<String> =
        if (isEmpty()) emptyList() else lineSequence().toList()
}
