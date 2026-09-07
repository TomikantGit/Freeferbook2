package com.livrohub.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PdfExporter {

    suspend fun exportToPdf(
        context: Context,
        uri: Uri,
        title: String,
        contentMarkdown: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val plainText = MarkdownParser.parseToPlainText(contentMarkdown)
            if (plainText.isBlank()) return@withContext false

            val document = PdfDocument()
            try {
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

                // Criar layout do texto
                val usableWidth = pageWidth - (margin * 2)
                val textLayout = StaticLayout.Builder.obtain(plainText, 0, plainText.length, textPaint, usableWidth)
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
                    if (endLine == startLine && endLine < textLayout.lineCount) {
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

                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: return@withContext false
                outputStream.use {
                    document.writeTo(outputStream)
                }
                true
            } finally {
                document.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

}
