package com.livrohub.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/** Formatos Markdown disponíveis no menu contextual do editor. */
enum class MarkdownFormat {
    Bold,
    Italic,
    Strikethrough,
    Underline,
    Highlight,
    Heading,
    Quote,
    BulletedList,
    NumberedList,
    Checklist
}

val CustomMarkdownMarkers = listOf("•", "→", "★", "✓", "◆")

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
        MarkdownFormat.Underline -> toggleInline(value, "++", "++")
        MarkdownFormat.Highlight -> toggleInline(value, "==", "==")
        MarkdownFormat.Heading -> toggleLinePrefix(value, "### ")
        MarkdownFormat.Quote -> toggleLinePrefix(value, "> ")
        MarkdownFormat.BulletedList -> toggleListStyle(value, ListStyle.Bulleted)
        MarkdownFormat.NumberedList -> toggleListStyle(value, ListStyle.Numbered)
        MarkdownFormat.Checklist -> toggleListStyle(value, ListStyle.Checklist)
    }
}

fun applyCustomMarkdownMarker(
    value: TextFieldValue,
    marker: String
): TextFieldValue {
    if (marker !in CustomMarkdownMarkers || value.selection.collapsed || value.text.isEmpty()) {
        return value
    }
    return toggleListStyle(value, ListStyle.Custom(marker))
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

private sealed interface ListStyle {
    data object Bulleted : ListStyle
    data object Numbered : ListStyle
    data object Checklist : ListStyle
    data class Custom(val marker: String) : ListStyle
}

private data class ParsedListPrefix(
    val style: ListStyle,
    val length: Int
)

private val NUMBERED_LIST_REGEX = "^(\\d+)\\. ".toRegex()

private fun parseListPrefix(line: String): ParsedListPrefix? {
    if (line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ")) {
        return ParsedListPrefix(ListStyle.Checklist, 6)
    }
    if (line.startsWith("- ")) {
        return ParsedListPrefix(ListStyle.Bulleted, 2)
    }
    NUMBERED_LIST_REGEX.find(line)?.let { match ->
        return ParsedListPrefix(ListStyle.Numbered, match.value.length)
    }
    CustomMarkdownMarkers.firstOrNull { line.startsWith("$it ") }?.let { marker ->
        return ParsedListPrefix(ListStyle.Custom(marker), marker.length + 1)
    }
    return null
}

private fun toggleListStyle(
    value: TextFieldValue,
    targetStyle: ListStyle
): TextFieldValue {
    val text = value.text
    val selectionStart = value.selection.min
    val selectionEnd = value.selection.max
    val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0))
        .let { if (it == -1) 0 else it + 1 }
    val lineEndBreak = text.indexOf('\n', selectionEnd)
    val lineEnd = if (lineEndBreak == -1) text.length else lineEndBreak
    val lines = text.substring(lineStart, lineEnd).split('\n')

    val parsedPrefixes = lines.map(::parseListPrefix)
    val allAlreadyTarget = parsedPrefixes.all { prefix ->
        when {
            prefix == null -> false
            targetStyle is ListStyle.Custom && prefix.style is ListStyle.Custom ->
                prefix.style.marker == targetStyle.marker
            else -> prefix.style::class == targetStyle::class
        }
    }

    var oldAbsoluteLineStart = lineStart
    var newAbsoluteLineStart = lineStart
    var mappedSelectionStart = selectionStart
    var mappedSelectionEnd = selectionEnd

    val transformedLines = lines.mapIndexed { index, line ->
        val oldPrefix = parsedPrefixes[index]
        val oldPrefixLength = oldPrefix?.length ?: 0
        val content = line.drop(oldPrefixLength)
        val newPrefix = if (allAlreadyTarget) {
            ""
        } else {
            when (targetStyle) {
                ListStyle.Bulleted -> "- "
                ListStyle.Numbered -> "${index + 1}. "
                ListStyle.Checklist -> "- [ ] "
                is ListStyle.Custom -> "${targetStyle.marker} "
            }
        }
        val transformed = newPrefix + content

        fun mapOffset(offset: Int): Int {
            val positionInOldLine = (offset - oldAbsoluteLineStart).coerceIn(0, line.length)
            val contentOffset = (positionInOldLine - oldPrefixLength).coerceAtLeast(0)
            return newAbsoluteLineStart + newPrefix.length + contentOffset
        }

        val oldLineEnd = oldAbsoluteLineStart + line.length
        if (selectionStart in oldAbsoluteLineStart..oldLineEnd) {
            mappedSelectionStart = mapOffset(selectionStart)
        }
        if (selectionEnd in oldAbsoluteLineStart..oldLineEnd) {
            mappedSelectionEnd = mapOffset(selectionEnd)
        }

        oldAbsoluteLineStart = oldLineEnd + if (index < lines.lastIndex) 1 else 0
        newAbsoluteLineStart += transformed.length + if (index < lines.lastIndex) 1 else 0
        transformed
    }

    val newBlock = transformedLines.joinToString("\n")
    val newText = text.replaceRange(lineStart, lineEnd, newBlock)
    val newStart = mappedSelectionStart.coerceIn(0, newText.length)
    val newEnd = mappedSelectionEnd.coerceIn(newStart, newText.length)
    return TextFieldValue(newText, TextRange(newStart, newEnd))
}
