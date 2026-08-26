package io.github.langstudy.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.withTranslation
import io.github.langstudy.data.local.entity.JournalEntryEntity
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JournalExportUtils {

    fun shareEntryAsPdf(context: Context, entry: JournalEntryEntity) {
        val fileName = "Journal_${entry.title.filter { it.isLetterOrDigit() }}_${entry.id.take(4)}.pdf"
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { it.write(generatePdfBytes(entry)) }
        shareFile(context, file, "application/pdf")
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Journal Entry"))
    }

    fun generatePdfBytes(entry: JournalEntryEntity): ByteArray {
        return generateBatchPdfBytes(listOf(entry))
    }

    fun generateBatchPdfBytes(entries: List<JournalEntryEntity>): ByteArray {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 50f
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val titlePaint = TextPaint().apply {
            textSize = 20f
            isFakeBoldText = true
        }

        val metaPaint = TextPaint().apply {
            textSize = 10f
            color = android.graphics.Color.GRAY
        }

        val contentPaint = TextPaint().apply {
            textSize = 12f
        }

        var currentPageNumber = 1
        var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
        var currentCanvas = currentPage.canvas
        var y = margin

        entries.forEach { entry ->
            val dateStr = dateFormat.format(Date(entry.timestamp))
            val metaStr = "$dateStr | ${entry.language}"

            val titleLayout = StaticLayout.Builder.obtain(entry.title, 0, entry.title.length, titlePaint, pageWidth - (margin * 2).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            val contentLayout = StaticLayout.Builder.obtain(entry.content, 0, entry.content.length, contentPaint, pageWidth - (margin * 2).toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()

            val entryHeight = titleLayout.height + 10f + 15f + 15f + contentLayout.height + 30f // title + space + meta + space + content + padding

            // Check if entry fits on current page
            if ((y + entryHeight) > (pageHeight - margin)) {
                pdfDocument.finishPage(currentPage)
                currentPageNumber++
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
                currentCanvas = currentPage.canvas
                y = margin
            }

            // Draw Title
            currentCanvas.withTranslation(margin, y) {
                titleLayout.draw(currentCanvas)
            }
            y += titleLayout.height + 5f

            // Draw Meta
            currentCanvas.drawText(metaStr, margin, y + 10f, metaPaint)
            y += 25f

            // Draw Content
            currentCanvas.withTranslation(margin, y) {
                contentLayout.draw(currentCanvas)
            }
            y += contentLayout.height + 40f // Extra space between entries
            
            // Draw separator line if not at the bottom
            if (y < pageHeight - margin - 20f) {
                val linePaint = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    strokeWidth = 1f
                }
                currentCanvas.drawLine(margin, y - 20f, pageWidth - margin, y - 20f, linePaint)
            }
        }

        pdfDocument.finishPage(currentPage)

        val outputStream = ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        return outputStream.toByteArray()
    }

    fun generateWordBytes(entry: JournalEntryEntity): ByteArray {
        return generateBatchWordBytes(listOf(entry))
    }

    fun generateBatchWordBytes(entries: List<JournalEntryEntity>): ByteArray {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        val entriesHtml = entries.joinToString("<hr style='border: 0; border-top: 1px solid #eee; margin: 20px 0;'>") { entry ->
            val dateStr = dateFormat.format(Date(entry.timestamp))
            """
                <div style='margin-bottom: 30px;'>
                    <h2 style='margin-bottom: 5px;'>${entry.title}</h2>
                    <p style='color: #666; font-size: 11pt; margin-top: 0;'><i>$dateStr | ${entry.language}</i></p>
                    <div style='white-space: pre-wrap; font-size: 12pt;'>
                        ${entry.content.replace("\n", "<br>")}
                    </div>
                </div>
            """.trimIndent()
        }

        val htmlContent = """
            <html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word' xmlns='http://www.w3.org/TR/REC-html40'>
            <head><meta charset='utf-8'><title>Journal Export</title></head>
            <body style='font-family: Arial, sans-serif; line-height: 1.6; padding: 20px;'>
                $entriesHtml
            </body>
            </html>
        """.trimIndent()

        return htmlContent.toByteArray()
    }
}
