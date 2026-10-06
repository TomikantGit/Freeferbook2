package com.livrohub.domain.markdown

enum class MarkdownTextFormat {
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

data class MarkdownFormattingResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
)

object MarkdownTextFormatter {
    val customMarkers = listOf("•", "→", "★", "✓", "◆")

    fun apply(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        format: MarkdownTextFormat
    ): MarkdownFormattingResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, text.length)
        if (start == end || text.isEmpty()) return MarkdownFormattingResult(text, start, end)

        return when (format) {
            MarkdownTextFormat.Bold -> toggleInline(text, start, end, "**", "**")
            MarkdownTextFormat.Italic -> toggleInline(text, start, end, "*", "*")
            MarkdownTextFormat.Strikethrough -> toggleInline(text, start, end, "~~", "~~")
            MarkdownTextFormat.Underline -> toggleInline(text, start, end, "++", "++")
            MarkdownTextFormat.Highlight -> toggleInline(text, start, end, "==", "==")
            MarkdownTextFormat.Heading -> toggleLinePrefix(text, start, end, "### ")
            MarkdownTextFormat.Quote -> toggleLinePrefix(text, start, end, "> ")
            MarkdownTextFormat.BulletedList -> toggleListStyle(text, start, end, ListStyle.Bulleted)
            MarkdownTextFormat.NumberedList -> toggleListStyle(text, start, end, ListStyle.Numbered)
            MarkdownTextFormat.Checklist -> toggleListStyle(text, start, end, ListStyle.Checklist)
        }
    }

    fun applyCustomMarker(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        marker: String
    ): MarkdownFormattingResult {
        val start = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val end = maxOf(selectionStart, selectionEnd).coerceIn(start, text.length)
        if (marker !in customMarkers || start == end || text.isEmpty()) {
            return MarkdownFormattingResult(text, start, end)
        }
        return toggleListStyle(text, start, end, ListStyle.Custom(marker))
    }

    private fun toggleInline(
        text: String,
        start: Int,
        end: Int,
        prefix: String,
        suffix: String
    ): MarkdownFormattingResult {
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
            MarkdownFormattingResult(newText, markerStart, markerStart + (end - start))
        } else {
            val newText = buildString(text.length + prefix.length + suffix.length) {
                append(text, 0, start)
                append(prefix)
                append(text, start, end)
                append(suffix)
                append(text, end, text.length)
            }
            val contentStart = start + prefix.length
            MarkdownFormattingResult(newText, contentStart, contentStart + (end - start))
        }
    }

    private fun toggleLinePrefix(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        prefix: String
    ): MarkdownFormattingResult {
        val lineStart = text.lastIndexOf('\n', (selectionStart - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
        val lineEndBreak = text.indexOf('\n', selectionEnd)
        val lineEnd = if (lineEndBreak == -1) text.length else lineEndBreak
        val lines = text.substring(lineStart, lineEnd).split('\n')
        val removePrefix = lines.all { it.startsWith(prefix) }

        var selectionStartDelta = 0
        var selectionEndDelta = 0
        var runningOffset = lineStart

        val transformedLines = lines.mapIndexed { index, line ->
            val lineOriginalStart = runningOffset
            val lineOriginalEnd = lineOriginalStart + line.length
            val affectsStart = selectionStart in lineOriginalStart..lineOriginalEnd
            val affectsEnd = selectionEnd in lineOriginalStart..lineOriginalEnd

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
        return MarkdownFormattingResult(newText, newStart, newEnd)
    }

    private sealed interface ListStyle {
        data object Bulleted : ListStyle
        data object Numbered : ListStyle
        data object Checklist : ListStyle
        data class Custom(val marker: String) : ListStyle
    }

    private data class ParsedListPrefix(val style: ListStyle, val length: Int)

    private val numberedListRegex = "^(\\d+)\\. ".toRegex()

    private fun parseListPrefix(line: String): ParsedListPrefix? {
        if (line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ")) {
            return ParsedListPrefix(ListStyle.Checklist, 6)
        }
        if (line.startsWith("- ")) return ParsedListPrefix(ListStyle.Bulleted, 2)
        numberedListRegex.find(line)?.let { return ParsedListPrefix(ListStyle.Numbered, it.value.length) }
        customMarkers.firstOrNull { line.startsWith("$it ") }?.let {
            return ParsedListPrefix(ListStyle.Custom(it), it.length + 1)
        }
        return null
    }

    private fun toggleListStyle(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        targetStyle: ListStyle
    ): MarkdownFormattingResult {
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
            if (selectionStart in oldAbsoluteLineStart..oldLineEnd) mappedSelectionStart = mapOffset(selectionStart)
            if (selectionEnd in oldAbsoluteLineStart..oldLineEnd) mappedSelectionEnd = mapOffset(selectionEnd)

            oldAbsoluteLineStart = oldLineEnd + if (index < lines.lastIndex) 1 else 0
            newAbsoluteLineStart += transformed.length + if (index < lines.lastIndex) 1 else 0
            transformed
        }

        val newBlock = transformedLines.joinToString("\n")
        val newText = text.replaceRange(lineStart, lineEnd, newBlock)
        val newStart = mappedSelectionStart.coerceIn(0, newText.length)
        val newEnd = mappedSelectionEnd.coerceIn(newStart, newText.length)
        return MarkdownFormattingResult(newText, newStart, newEnd)
    }
}
