package com.livrohub.desktop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

object DesktopMarkdownRenderer {
    private val boldRegex = "\\*\\*(.*?)\\*\\*".toRegex()
    private val italicRegex = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()
    private val strikeRegex = "~~(.*?)~~".toRegex()
    private val underlineRegex = "\\+\\+(.*?)\\+\\+".toRegex()
    private val highlightRegex = "==(.*?)==".toRegex()
    private val checklistRegex = "^- \\[([ xX])\\] (.*)$".toRegex()
    private val bulletRegex = "^- (.*)$".toRegex()

    fun toAnnotatedString(markdown: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val lines = markdown.lines()

        lines.forEachIndexed { index, rawLine ->
            var line = rawLine
            var blockStyle: SpanStyle? = null
            when {
                line.startsWith("### ") -> {
                    line = line.substring(4)
                    blockStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                line.startsWith("## ") -> {
                    line = line.substring(3)
                    blockStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                line.startsWith("# ") -> {
                    line = line.substring(2)
                    blockStyle = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
                }
                line.startsWith("> ") -> {
                    line = line.substring(2)
                    blockStyle = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)
                }
                checklistRegex.matches(line) -> {
                    val match = requireNotNull(checklistRegex.matchEntire(line))
                    val checked = match.groupValues[1].equals("x", ignoreCase = true)
                    line = "${if (checked) "☑" else "☐"} ${match.groupValues[2]}"
                }
                bulletRegex.matches(line) -> {
                    val match = requireNotNull(bulletRegex.matchEntire(line))
                    line = "• ${match.groupValues[1]}"
                }
            }

            val lineStart = builder.length
            processInline(line).forEach { segment ->
                val start = builder.length
                builder.append(segment.text)
                segment.styles.forEach { style -> builder.addStyle(style, start, builder.length) }
            }
            blockStyle?.let { builder.addStyle(it, lineStart, builder.length) }
            if (index < lines.lastIndex) builder.append("\n")
        }
        return builder.toAnnotatedString()
    }

    private data class Segment(val text: String, val styles: List<SpanStyle>)

    private fun processInline(text: String, inherited: List<SpanStyle> = emptyList()): List<Segment> {
        val segments = mutableListOf<Segment>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val bold = boldRegex.find(remaining)
            val italic = italicRegex.find(remaining)
            val strike = strikeRegex.find(remaining)
            val underline = underlineRegex.find(remaining)
            val highlight = highlightRegex.find(remaining)
            val first = listOfNotNull(bold, italic, strike, underline, highlight)
                .minByOrNull { it.range.first }

            if (first == null) {
                segments += Segment(remaining, inherited)
                break
            }
            if (first.range.first > 0) {
                segments += Segment(remaining.substring(0, first.range.first), inherited)
            }
            val style = when (first) {
                bold -> SpanStyle(fontWeight = FontWeight.Bold)
                italic -> SpanStyle(fontStyle = FontStyle.Italic)
                strike -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                underline -> SpanStyle(textDecoration = TextDecoration.Underline)
                highlight -> SpanStyle(background = Color(0x55FFD54F))
                else -> SpanStyle()
            }
            segments += processInline(first.groupValues[1], inherited + style)
            remaining = remaining.substring(first.range.last + 1)
        }
        return segments
    }
}
