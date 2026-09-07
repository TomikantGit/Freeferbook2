package com.livrohub.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExporter {

    private val BOLD_REGEX = "\\*\\*(.*?)\\*\\*".toRegex()
    private val ITALIC_REGEX = "(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)".toRegex()
    private val H1_REGEX = "^# (.*)$".toRegex(RegexOption.MULTILINE)
    private val H2_REGEX = "^## (.*)$".toRegex(RegexOption.MULTILINE)

    suspend fun exportToPdf(
        context: Context,
        uri: Uri,
        title: String,
        contentMarkdown: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val document = PdfDocument()

            // A4 size: 595 x 842 points
            val pageWidth = 595
            val pageHeight = 842
            val margin = 50

            val textPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            }
            
            val titlePaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 24f
                color = Color.BLACK
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            // Converter markdown para Spannable
            val spannable = buildSpannable(contentMarkdown)

            // Criar layout do texto
            val usableWidth = pageWidth - (margin * 2)
            val textLayout = StaticLayout.Builder.obtain(spannable, 0, spannable.length, textPaint, usableWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setIncludePad(false)
                .build()

            var currentLine = 0
            var pageNumber = 1

            while (currentLine < textLayout.lineCount) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                var yOffset = margin

                // Draw title on first page
                if (pageNumber == 1) {
                    canvas.drawText(title, pageWidth / 2f, yOffset.toFloat() + titlePaint.textSize, titlePaint)
                    yOffset += (titlePaint.textSize * 3).toInt()
                }

                // Check how many lines fit on this page
                val usableHeight = pageHeight - margin - yOffset
                
                canvas.save()
                canvas.translate(margin.toFloat(), yOffset.toFloat())
                
                val startLine = currentLine
                var endLine = currentLine
                
                while (endLine < textLayout.lineCount && textLayout.getLineBottom(endLine) - textLayout.getLineTop(startLine) < usableHeight) {
                    endLine++
                }
                
                // Draw only the lines for this page
                canvas.clipRect(0, 0, usableWidth, textLayout.getLineBottom(endLine - 1) - textLayout.getLineTop(startLine) + 10)
                canvas.translate(0f, -textLayout.getLineTop(startLine).toFloat())
                
                textLayout.draw(canvas)
                
                canvas.restore()
                document.finishPage(page)
                
                currentLine = endLine
                pageNumber++
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            }
            document.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun buildSpannable(markdown: String): SpannableStringBuilder {
        var text = markdown
        
        // Block formatting first
        text = text.replace(H1_REGEX, "$1")
        text = text.replace(H2_REGEX, "$1")
        
        val builder = SpannableStringBuilder(text)
        
        // H1 spans
        for (match in H1_REGEX.findAll(markdown)) {
            val start = builder.indexOf(match.groupValues[1])
            if (start != -1) {
                builder.setSpan(StyleSpan(Typeface.BOLD), start, start + match.groupValues[1].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(1.5f), start, start + match.groupValues[1].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        // H2 spans
        for (match in H2_REGEX.findAll(markdown)) {
            val start = builder.indexOf(match.groupValues[1])
            if (start != -1) {
                builder.setSpan(StyleSpan(Typeface.BOLD), start, start + match.groupValues[1].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(RelativeSizeSpan(1.3f), start, start + match.groupValues[1].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        
        // Inline (very simplified, actual regex replacement in builder is tricky)
        // For PDF, we can just strip them if complex, but let's just strip them for now
        var cleanText = builder.toString()
        cleanText = cleanText.replace(BOLD_REGEX, "$1")
        cleanText = cleanText.replace(ITALIC_REGEX, "$1")
        cleanText = cleanText.replace(Regex("~~(.*?)~~"), "$1")
        cleanText = cleanText.replace(Regex("^> (.*)$", RegexOption.MULTILINE), "  $1")
        
        return SpannableStringBuilder(cleanText)
    }
}
