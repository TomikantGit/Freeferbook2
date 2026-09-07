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
        var html = markdown
        
        // Escape HTML
        html = html.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        // Block elements
        html = html.replace(H1_REGEX, "<h1>$1</h1>")
        html = html.replace(H2_REGEX, "<h2>$1</h2>")
        html = html.replace(H3_REGEX, "<h3>$1</h3>")
        html = html.replace(QUOTE_REGEX, "<blockquote>$1</blockquote>")

        // Inline elements
        html = html.replace(BOLD_REGEX, "<strong>$1</strong>")
        html = html.replace(ITALIC_REGEX, "<em>$1</em>")
        html = html.replace(STRIKETHROUGH_REGEX, "<del>$1</del>")

        // Convert newlines to paragraphs or line breaks
        val paragraphs = html.split("\n\n").map { p ->
            val pTrim = p.trim()
            if (pTrim.startsWith("<h") || pTrim.startsWith("<blockquote")) {
                pTrim
            } else {
                "<p>${pTrim.replace("\n", "<br/>")}</p>"
            }
        }

        return paragraphs.joinToString("\n")
    }

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
