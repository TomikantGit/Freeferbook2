package com.livrohub.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.livrohub.domain.markdown.MarkdownTextFormat
import com.livrohub.domain.markdown.MarkdownTextFormatter

/** Formatos Markdown disponíveis no menu contextual do editor Android. */
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

val CustomMarkdownMarkers = MarkdownTextFormatter.customMarkers

/** Adapta a regra pura compartilhada em `:core` para o `TextFieldValue` do Compose. */
fun applyMarkdownFormat(value: TextFieldValue, format: MarkdownFormat): TextFieldValue {
    val result = MarkdownTextFormatter.apply(
        text = value.text,
        selectionStart = value.selection.start,
        selectionEnd = value.selection.end,
        format = format.toDomainFormat()
    )
    return TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd))
}

fun applyCustomMarkdownMarker(value: TextFieldValue, marker: String): TextFieldValue {
    val result = MarkdownTextFormatter.applyCustomMarker(
        text = value.text,
        selectionStart = value.selection.start,
        selectionEnd = value.selection.end,
        marker = marker
    )
    return TextFieldValue(result.text, TextRange(result.selectionStart, result.selectionEnd))
}

private fun MarkdownFormat.toDomainFormat(): MarkdownTextFormat = when (this) {
    MarkdownFormat.Bold -> MarkdownTextFormat.Bold
    MarkdownFormat.Italic -> MarkdownTextFormat.Italic
    MarkdownFormat.Strikethrough -> MarkdownTextFormat.Strikethrough
    MarkdownFormat.Underline -> MarkdownTextFormat.Underline
    MarkdownFormat.Highlight -> MarkdownTextFormat.Highlight
    MarkdownFormat.Heading -> MarkdownTextFormat.Heading
    MarkdownFormat.Quote -> MarkdownTextFormat.Quote
    MarkdownFormat.BulletedList -> MarkdownTextFormat.BulletedList
    MarkdownFormat.NumberedList -> MarkdownTextFormat.NumberedList
    MarkdownFormat.Checklist -> MarkdownTextFormat.Checklist
}
