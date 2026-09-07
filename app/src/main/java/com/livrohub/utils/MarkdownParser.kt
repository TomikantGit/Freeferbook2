package com.livrohub.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

object MarkdownParser {

    private val BOLD_REGEX = "\\*\\*(.*?)\\*\\*".toRegex()
    private val ITALIC_REGEX = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()
    private val STRIKETHROUGH_REGEX = "~~(.*?)~~".toRegex()
    private val UNDERLINE_REGEX = "\\+\\+(.*?)\\+\\+".toRegex()
    private val HIGHLIGHT_REGEX = "==(.*?)==".toRegex()
    private val H1_REGEX = "^# (.*)$".toRegex(RegexOption.MULTILINE)
    private val H2_REGEX = "^## (.*)$".toRegex(RegexOption.MULTILINE)
    private val H3_REGEX = "^### (.*)$".toRegex(RegexOption.MULTILINE)
    private val QUOTE_REGEX = "^> (.*)$".toRegex(RegexOption.MULTILINE)
    private val CHECKLIST_REGEX = "^- \\[([ xX])\\] (.*)$".toRegex()
    private val BULLET_LIST_REGEX = "^- (.*)$".toRegex()
    private val NUMBERED_LIST_REGEX = "^\\d+\\. (.*)$".toRegex()
    private val CUSTOM_MARKER_REGEX = "^([•→★✓◆]) (.*)$".toRegex()

    /**
     * Gera HTML simples a partir do Markdown (útil para EPUB).
     */
    fun parseToHtml(markdown: String): String {
        if (markdown.isBlank()) return ""

        val output = mutableListOf<String>()
        val paragraphLines = mutableListOf<String>()
        val listItems = mutableListOf<String>()
        var listKind: HtmlListKind? = null

        fun flushParagraph() {
            if (paragraphLines.isNotEmpty()) {
                output += "<p>${paragraphLines.joinToString("<br/>")}</p>"
                paragraphLines.clear()
            }
        }

        fun flushList() {
            val kind = listKind ?: return
            if (listItems.isNotEmpty()) {
                val tag = if (kind == HtmlListKind.Numbered) "ol" else "ul"
                val cssClass = if (kind == HtmlListKind.Checklist) " class=\"checklist\"" else ""
                output += "<$tag$cssClass>" +
                    listItems.joinToString("") { "<li>$it</li>" } +
                    "</$tag>"
                listItems.clear()
            }
            listKind = null
        }

        fun startListItem(kind: HtmlListKind, item: String) {
            flushParagraph()
            if (listKind != kind) {
                flushList()
                listKind = kind
            }
            listItems += item
        }

        markdown.lines().forEach { line ->
            when {
                line.isBlank() -> {
                    flushParagraph()
                    flushList()
                }
                line.startsWith("### ") -> {
                    flushParagraph()
                    flushList()
                    output += "<h3>${inlineToHtml(line.substring(4))}</h3>"
                }
                line.startsWith("## ") -> {
                    flushParagraph()
                    flushList()
                    output += "<h2>${inlineToHtml(line.substring(3))}</h2>"
                }
                line.startsWith("# ") -> {
                    flushParagraph()
                    flushList()
                    output += "<h1>${inlineToHtml(line.substring(2))}</h1>"
                }
                line.startsWith("> ") -> {
                    flushParagraph()
                    flushList()
                    output += "<blockquote>${inlineToHtml(line.substring(2))}</blockquote>"
                }
                CHECKLIST_REGEX.matches(line) -> {
                    val match = requireNotNull(CHECKLIST_REGEX.matchEntire(line))
                    val checked = match.groupValues[1].equals("x", ignoreCase = true)
                    val symbol = if (checked) "☑" else "☐"
                    startListItem(HtmlListKind.Checklist, "$symbol ${inlineToHtml(match.groupValues[2])}")
                }
                BULLET_LIST_REGEX.matches(line) -> {
                    val match = requireNotNull(BULLET_LIST_REGEX.matchEntire(line))
                    startListItem(HtmlListKind.Bulleted, inlineToHtml(match.groupValues[1]))
                }
                NUMBERED_LIST_REGEX.matches(line) -> {
                    val match = requireNotNull(NUMBERED_LIST_REGEX.matchEntire(line))
                    startListItem(HtmlListKind.Numbered, inlineToHtml(match.groupValues[1]))
                }
                CUSTOM_MARKER_REGEX.matches(line) -> {
                    flushParagraph()
                    flushList()
                    val match = requireNotNull(CUSTOM_MARKER_REGEX.matchEntire(line))
                    output += "<p>${match.groupValues[1]} ${inlineToHtml(match.groupValues[2])}</p>"
                }
                else -> {
                    flushList()
                    paragraphLines += inlineToHtml(line)
                }
            }
        }
        flushParagraph()
        flushList()

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
                CHECKLIST_REGEX.matches(line) -> {
                    val match = requireNotNull(CHECKLIST_REGEX.matchEntire(line))
                    val symbol = if (match.groupValues[1].equals("x", ignoreCase = true)) "☑" else "☐"
                    "$symbol ${match.groupValues[2]}"
                }
                BULLET_LIST_REGEX.matches(line) -> {
                    val match = requireNotNull(BULLET_LIST_REGEX.matchEntire(line))
                    "• ${match.groupValues[1]}"
                }
                else -> line
            }
            withoutBlockMarker
                .replace(BOLD_REGEX, "$1")
                .replace(ITALIC_REGEX, "$1")
                .replace(STRIKETHROUGH_REGEX, "$1")
                .replace(UNDERLINE_REGEX, "$1")
                .replace(HIGHLIGHT_REGEX, "$1")
        }
        .joinToString("\n")

    private fun inlineToHtml(text: String): String {
        var html = escapeHtml(text)
        html = html.replace(BOLD_REGEX, "<strong>$1</strong>")
        html = html.replace(ITALIC_REGEX, "<em>$1</em>")
        html = html.replace(STRIKETHROUGH_REGEX, "<del>$1</del>")
        html = html.replace(UNDERLINE_REGEX, "<span style=\"text-decoration: underline;\">$1</span>")
        html = html.replace(HIGHLIGHT_REGEX, "<span style=\"background-color: #fff2a8;\">$1</span>")
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
            } else if (CHECKLIST_REGEX.matches(currentLine)) {
                val match = requireNotNull(CHECKLIST_REGEX.matchEntire(currentLine))
                val symbol = if (match.groupValues[1].equals("x", ignoreCase = true)) "☑" else "☐"
                currentLine = "$symbol ${match.groupValues[2]}"
            } else if (BULLET_LIST_REGEX.matches(currentLine)) {
                val match = requireNotNull(BULLET_LIST_REGEX.matchEntire(currentLine))
                currentLine = "• ${match.groupValues[1]}"
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
    
    private fun processInlineStyles(
        text: String,
        inheritedStyles: List<SpanStyle> = emptyList()
    ): List<InlineSegment> {
        val segments = mutableListOf<InlineSegment>()
        
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldMatch = BOLD_REGEX.find(remaining)
            val italicMatch = ITALIC_REGEX.find(remaining)
            val strikeMatch = STRIKETHROUGH_REGEX.find(remaining)
            val underlineMatch = UNDERLINE_REGEX.find(remaining)
            val highlightMatch = HIGHLIGHT_REGEX.find(remaining)
            
            val matches = listOfNotNull(
                boldMatch,
                italicMatch,
                strikeMatch,
                underlineMatch,
                highlightMatch
            ).sortedBy { it.range.first }
            
            if (matches.isEmpty()) {
                segments.add(InlineSegment(remaining, inheritedStyles))
                break
            }
            
            val firstMatch = matches.first()
            if (firstMatch.range.first > 0) {
                segments.add(
                    InlineSegment(
                        remaining.substring(0, firstMatch.range.first),
                        inheritedStyles
                    )
                )
            }
            
            val style = when (firstMatch) {
                boldMatch -> SpanStyle(fontWeight = FontWeight.Bold)
                italicMatch -> SpanStyle(fontStyle = FontStyle.Italic)
                strikeMatch -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                underlineMatch -> SpanStyle(textDecoration = TextDecoration.Underline)
                highlightMatch -> SpanStyle(background = Color(0x55FFD54F))
                else -> SpanStyle()
            }
            
            val content = firstMatch.groupValues[1]
            segments += processInlineStyles(content, inheritedStyles + style)
            
            remaining = remaining.substring(firstMatch.range.last + 1)
        }
        
        return segments
    }

    private enum class HtmlListKind {
        Bulleted,
        Numbered,
        Checklist
    }
}
