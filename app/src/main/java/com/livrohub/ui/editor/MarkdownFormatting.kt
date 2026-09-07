package com.livrohub.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Formatos Markdown disponíveis no menu contextual do editor. */
enum class MarkdownFormat {
    Bold,
    Italic,
    Strikethrough,
    Heading,
    Quote
}

/**
 * Aplica ou remove um formato Markdown sobre a seleção atual.
 *
 * Formatos inline preservam a seleção somente sobre o conteúdo, deixando os
 * marcadores fora dela. Isso permite tocar novamente no mesmo formato para
 * removê-lo sem alterar a palavra selecionada.
 *
 * Título e citação trabalham por linha e não inserem quebras de linha extras.
 */
fun applyMarkdownFormat(
    value: TextFieldValue,
    format: MarkdownFormat
): TextFieldValue {
    if (value.selection.collapsed || value.text.isEmpty()) return value

    return when (format) {
        MarkdownFormat.Bold -> toggleInline(value, "**", "**")
        MarkdownFormat.Italic -> toggleInline(value, "*", "*")
        MarkdownFormat.Strikethrough -> toggleInline(value, "~~", "~~")
        MarkdownFormat.Heading -> toggleLinePrefix(value, "### ")
        MarkdownFormat.Quote -> toggleLinePrefix(value, "> ")
    }
}

private fun toggleInline(
    value: TextFieldValue,
    prefix: String,
    suffix: String
): TextFieldValue {
    val text = value.text
    val start = value.selection.min
    val end = value.selection.max

    val hasOuterMarkers =
        start >= prefix.length &&
            end + suffix.length <= text.length &&
            text.substring(start - prefix.length, start) == prefix &&
            text.substring(end, end + suffix.length) == suffix

    return if (hasOuterMarkers) {
        val markerStart = start - prefix.length
        val newText = buildString(text.length - prefix.length - suffix.length) {
            append(text, 0, markerStart)
            append(text, start, end)
            append(text, end + suffix.length, text.length)
        }
        TextFieldValue(
            text = newText,
            selection = TextRange(markerStart, markerStart + (end - start))
        )
    } else {
        val newText = buildString(text.length + prefix.length + suffix.length) {
            append(text, 0, start)
            append(prefix)
            append(text, start, end)
            append(suffix)
            append(text, end, text.length)
        }
        val contentStart = start + prefix.length
        TextFieldValue(
            text = newText,
            selection = TextRange(contentStart, contentStart + (end - start))
        )
    }
}

private fun toggleLinePrefix(
    value: TextFieldValue,
    prefix: String
): TextFieldValue {
    val text = value.text
    val selectionStart = value.selection.min
    val selectionEnd = value.selection.max
    val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEndBreak = text.indexOf('\n', selectionEnd)
    val lineEnd = if (lineEndBreak == -1) text.length else lineEndBreak
    val block = text.substring(lineStart, lineEnd)
    val lines = block.split('\n')
    val removePrefix = lines.all { it.startsWith(prefix) }

    var selectionStartDelta = 0
    var selectionEndDelta = 0
    var runningOffset = lineStart

    val transformedLines = lines.mapIndexed { index, line ->
        val lineOriginalStart = runningOffset
        val lineOriginalEnd = lineOriginalStart + line.length
        val affectsStart = selectionStart >= lineOriginalStart && selectionStart <= lineOriginalEnd
        val affectsEnd = selectionEnd >= lineOriginalStart && selectionEnd <= lineOriginalEnd

        if (removePrefix) {
            if (affectsStart) selectionStartDelta -= prefix.length.coerceAtMost(selectionStart - lineOriginalStart)
            if (lineOriginalStart < selectionEnd || affectsEnd) selectionEndDelta -= prefix.length
            line.removePrefix(prefix)
        } else {
            if (affectsStart) selectionStartDelta += prefix.length
            if (lineOriginalStart < selectionEnd || affectsEnd) selectionEndDelta += prefix.length
            prefix + line
        }.also {
            runningOffset = lineOriginalEnd + if (index < lines.lastIndex) 1 else 0
        }
    }

    val transformedBlock = transformedLines.joinToString("\n")
    val newText = text.replaceRange(lineStart, lineEnd, transformedBlock)
    val newStart = (selectionStart + selectionStartDelta).coerceIn(0, newText.length)
    val newEnd = (selectionEnd + selectionEndDelta).coerceIn(newStart, newText.length)

    return TextFieldValue(newText, TextRange(newStart, newEnd))
}
