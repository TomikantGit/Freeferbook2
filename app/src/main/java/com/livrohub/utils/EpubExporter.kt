package com.livrohub.utils

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object EpubExporter {

    suspend fun exportToEpub(
        context: Context,
        uri: Uri,
        title: String,
        contentMarkdown: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (contentMarkdown.isBlank()) return@withContext false
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: return@withContext false
            outputStream.use { stream ->
                ZipOutputStream(stream).use { zos ->
                    // 1. mimetype (must be first, uncompressed)
                    val mimetypeEntry = ZipEntry("mimetype").apply {
                        method = ZipEntry.STORED
                        val bytes = "application/epub+zip".toByteArray()
                        size = bytes.size.toLong()
                        compressedSize = bytes.size.toLong()
                        crc = computeCrc(bytes)
                    }
                    zos.putNextEntry(mimetypeEntry)
                    zos.write("application/epub+zip".toByteArray())
                    zos.closeEntry()

                    // 2. META-INF/container.xml
                    zos.putNextEntry(ZipEntry("META-INF/container.xml"))
                    zos.write(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                            <rootfiles>
                                <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                            </rootfiles>
                        </container>
                        """.trimIndent().toByteArray()
                    )
                    zos.closeEntry()

                    // 3. OEBPS/content.opf
                    val htmlContent = MarkdownParser.parseToHtml(contentMarkdown)
                    val safeTitle = title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    
                    zos.putNextEntry(ZipEntry("OEBPS/content.opf"))
                    zos.write(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookId" version="2.0">
                            <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                                <dc:title>$safeTitle</dc:title>
                                <dc:language>pt</dc:language>
                                <dc:identifier id="BookId">urn:uuid:12345678-1234-1234-1234-123456789012</dc:identifier>
                            </metadata>
                            <manifest>
                                <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                                <item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/>
                            </manifest>
                            <spine toc="ncx">
                                <itemref idref="chapter"/>
                            </spine>
                        </package>
                        """.trimIndent().toByteArray()
                    )
                    zos.closeEntry()

                    // 4. OEBPS/toc.ncx
                    zos.putNextEntry(ZipEntry("OEBPS/toc.ncx"))
                    zos.write(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                            <head>
                                <meta name="dtb:uid" content="urn:uuid:12345678-1234-1234-1234-123456789012"/>
                                <meta name="dtb:depth" content="1"/>
                                <meta name="dtb:totalPageCount" content="0"/>
                                <meta name="dtb:maxPageNumber" content="0"/>
                            </head>
                            <docTitle>
                                <text>$safeTitle</text>
                            </docTitle>
                            <navMap>
                                <navPoint id="navPoint-1" playOrder="1">
                                    <navLabel><text>$safeTitle</text></navLabel>
                                    <content src="chapter.xhtml"/>
                                </navPoint>
                            </navMap>
                        </ncx>
                        """.trimIndent().toByteArray()
                    )
                    zos.closeEntry()

                    // 5. OEBPS/chapter.xhtml
                    zos.putNextEntry(ZipEntry("OEBPS/chapter.xhtml"))
                    zos.write(
                        """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
                        <html xmlns="http://www.w3.org/1999/xhtml">
                        <head>
                            <title>$safeTitle</title>
                        </head>
                        <body>
                            <h1>$safeTitle</h1>
                            $htmlContent
                        </body>
                        </html>
                        """.trimIndent().toByteArray()
                    )
                    zos.closeEntry()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun computeCrc(bytes: ByteArray): Long {
        val crc = java.util.zip.CRC32()
        crc.update(bytes)
        return crc.value
    }
}
