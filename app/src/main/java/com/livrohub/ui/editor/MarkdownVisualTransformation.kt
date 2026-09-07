package com.livrohub.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

/**
 * [VisualTransformation] que aplica destaque visual de Markdown no editor.
 *
 * Suporta:
 * - **Negrito**: `**texto**` → renderizado em bold
 * - *Itálico*: `*texto*` → renderizado em itálico
 * - Heading 1: `# texto` → bold + 24sp
 * - Heading 2: `## texto` → bold + 20sp
 *
 * Os Regex são compilados uma única vez no [companion object]
 * para evitar recompilação a cada recomposição do Compose.
 *
 * O offset mapping é identidade (1:1) pois apenas estilos visuais
 * são aplicados, sem alterar o texto em si.
 */
class MarkdownVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val inputText = text.text
        val builder = AnnotatedString.Builder(inputText)

        // Bold: **text**
        for (match in BOLD_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Italic: *text* (sem conflitar com **)
        for (match in ITALIC_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        for (match in STRIKETHROUGH_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(textDecoration = TextDecoration.LineThrough),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        for (match in UNDERLINE_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(textDecoration = TextDecoration.Underline),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        for (match in HIGHLIGHT_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(background = Color(0x55FFD54F)),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Heading 1: # text
        for (match in H1_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        // Heading 2: ## text
        for (match in H2_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        for (match in H3_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        for (match in QUOTE_REGEX.findAll(inputText)) {
            builder.addStyle(
                style = SpanStyle(fontStyle = FontStyle.Italic),
                start = match.range.first,
                end = match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    companion object {
        private val BOLD_REGEX = "\\*\\*(.*?)\\*\\*".toRegex()
        private val ITALIC_REGEX = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()
        private val STRIKETHROUGH_REGEX = "~~(.*?)~~".toRegex()
        private val UNDERLINE_REGEX = "\\+\\+(.*?)\\+\\+".toRegex()
        private val HIGHLIGHT_REGEX = "==(.*?)==".toRegex()
        private val H1_REGEX = "^# (.*)$".toRegex(RegexOption.MULTILINE)
        private val H2_REGEX = "^## (.*)$".toRegex(RegexOption.MULTILINE)
        private val H3_REGEX = "^### (.*)$".toRegex(RegexOption.MULTILINE)
        private val QUOTE_REGEX = "^> (.*)$".toRegex(RegexOption.MULTILINE)
    }
}
