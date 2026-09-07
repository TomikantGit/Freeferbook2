package com.livrohub.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object MarkdownParser {

    private val BOLD_REGEX = "\\*\\*(.*?)\\*\\*".toRegex()
    private val ITALIC_REGEX = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()
    private val STRIKETHROUGH_REGEX = "~~(.*?)~~".toRegex()
    private val H1_REGEX = "^# (.*)$".toRegex(RegexOption.MULTILINE)
    private val H2_REGEX = "^## (.*)$".toRegex(RegexOption.MULTILINE)
    private val H3_REGEX = "^### (.*)$".toRegex(RegexOption.MULTILINE)
    private val QUOTE_REGEX = "^> (.*)$".toRegex(RegexOption.MULTILINE)

    /**
     * Gera HTML simples a partir do Markdown (útil para EPUB).
     */
    fun parseToHtml(markdown: String): String {
        if (markdown.isBlank()) return ""

        val output = mutableListOf<String>()
        val paragraphLines = mutableListOf<String>()

        fun flushParagraph() {
            if (paragraphLines.isNotEmpty()) {
                output += "<p>${paragraphLines.joinToString("<br/>")}</p>"
                paragraphLines.clear()
            }
        }

        markdown.lines().forEach { line ->
            when {
                line.isBlank() -> flushParagraph()
                line.startsWith("### ") -> {
                    flushParagraph()
                    output += "<h3>${inlineToHtml(line.substring(4))}</h3>"
                }
                line.startsWith("## ") -> {
                    flushParagraph()
                    output += "<h2>${inlineToHtml(line.substring(3))}</h2>"
                }
                line.startsWith("# ") -> {
                    flushParagraph()
                    output += "<h1>${inlineToHtml(line.substring(2))}</h1>"
                }
                line.startsWith("> ") -> {
                    flushParagraph()
                    output += "<blockquote>${inlineToHtml(line.substring(2))}</blockquote>"
                }
                else -> paragraphLines += inlineToHtml(line)
            }
        }
        flushParagraph()

        return output.joinToString("\n")
    }

    /**
     * Remove apenas a sintaxe Markdown suportada, preservando todo o conteúdo textual.
     * Usado por formatos que precisam de texto visível mesmo sem renderizador HTML.
     */
    fun parseToPlainText(markdown: String): String = markdown
        .lineSequence()
        .map { line ->
            val withoutBlockMarker = when {
                line.startsWith("### ") -> line.substring(4)
                line.startsWith("## ") -> line.substring(3)
                line.startsWith("# ") -> line.substring(2)
                line.startsWith("> ") -> line.substring(2)
                else -> line
            }
            withoutBlockMarker
                .replace(BOLD_REGEX, "$1")
                .replace(ITALIC_REGEX, "$1")
                .replace(STRIKETHROUGH_REGEX, "$1")
        }
        .joinToString("\n")

    private fun inlineToHtml(text: String): String {
        var html = escapeHtml(text)
        html = html.replace(BOLD_REGEX, "<strong>$1</strong>")
        html = html.replace(ITALIC_REGEX, "<em>$1</em>")
        html = html.replace(STRIKETHROUGH_REGEX, "<del>$1</del>")
        return html
    }

    private fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /**
     * Converte o Markdown em uma `AnnotatedString` do Compose.
     */
    fun parseToAnnotatedString(markdown: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = markdown.lines()
        
        for ((index, line) in lines.withIndex()) {
            var currentLine = line
            var isH1 = false
            var isH2 = false
            var isH3 = false
            var isQuote = false
            
            if (currentLine.startsWith("### ")) {
                isH3 = true
                currentLine = currentLine.substring(4)
            } else if (currentLine.startsWith("## ")) {
                isH2 = true
                currentLine = currentLine.substring(3)
            } else if (currentLine.startsWith("# ")) {
                isH1 = true
                currentLine = currentLine.substring(2)
            } else if (currentLine.startsWith("> ")) {
                isQuote = true
                currentLine = currentLine.substring(2)
            }
            
            val lineStartOffset = builder.length
            
            val inlineSegments = processInlineStyles(currentLine)
            for (segment in inlineSegments) {
                val start = builder.length
                builder.append(segment.text)
                for (style in segment.styles) {
                    builder.addStyle(style, start, builder.length)
                }
            }
            
            if (isH1) {
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp), lineStartOffset, builder.length)
            } else if (isH2) {
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp), lineStartOffset, builder.length)
            } else if (isH3) {
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp), lineStartOffset, builder.length)
            } else if (isQuote) {
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = androidx.compose.ui.graphics.Color.Gray), lineStartOffset, builder.length)
            }
            
            if (index < lines.size - 1) {
                builder.append("\n")
            }
        }

        return builder.toAnnotatedString()
    }
    
    private data class InlineSegment(val text: String, val styles: List<SpanStyle>)
    
    private fun processInlineStyles(text: String): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldMatch = BOLD_REGEX.find(remaining)
            val italicMatch = ITALIC_REGEX.find(remaining)
            val strikeMatch = STRIKETHROUGH_REGEX.find(remaining)
            
            val matches = listOfNotNull(boldMatch, italicMatch, strikeMatch).sortedBy { it.range.first }
            
            if (matches.isEmpty()) {
                segments.add(InlineSegment(remaining, emptyList()))
                break
            }
            
            val firstMatch = matches.first()
            if (firstMatch.range.first > 0) {
                segments.add(InlineSegment(remaining.substring(0, firstMatch.range.first), emptyList()))
            }
            
            val style = when (firstMatch) {
                boldMatch -> SpanStyle(fontWeight = FontWeight.Bold)
                italicMatch -> SpanStyle(fontStyle = FontStyle.Italic)
                strikeMatch -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                else -> SpanStyle()
            }
            
            val content = firstMatch.groupValues[1]
            segments.add(InlineSegment(content, listOf(style)))
            
            remaining = remaining.substring(firstMatch.range.last + 1)
        }
        
        return segments
    }
}
