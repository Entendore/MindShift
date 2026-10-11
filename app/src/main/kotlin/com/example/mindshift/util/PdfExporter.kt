package com.example.mindshift.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.screens.parseContent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    private const val PAGE_W = 595   // A4 at 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private val DARK = Color.rgb(0x26, 0x33, 0x2C)
    private val ACCENT = Color.rgb(0x5F, 0x8D, 0x75)
    private val GRAY = Color.rgb(0x8A, 0x97, 0x8F)

    /** Writes the entry as a paginated PDF to `uri`. Throws on failure. */
    fun export(context: Context, entry: WorksheetEntry, uri: Uri) {
        val doc = PdfDocument()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 22f; color = DARK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; color = GRAY }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f; color = ACCENT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f; color = DARK }
        val linePaint = Paint().apply { color = ACCENT; strokeWidth = 1.5f }
        val contentWidth = (PAGE_W - 2 * MARGIN).toInt()

        var page: PdfDocument.Page? = null
        var pageNo = 0
        var y = 0f

        fun startPage() {
            page?.let { doc.finishPage(it) }
            val p = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, ++pageNo).create())
            page = p
            y = MARGIN
            p.canvas.drawText("MindShift", MARGIN, PAGE_H - 26f, metaPaint)
        }
        fun ensure(height: Float) {
            if (page == null) startPage()
            if (y + height > PAGE_H - MARGIN) startPage()
        }

        startPage()
        var canvas = page!!.canvas

        canvas.drawText(strip(entry.title), MARGIN, y + 20f, titlePaint)
        y += 34f
        val dateStr = SimpleDateFormat("MMMM d, yyyy · h:mm a", Locale.getDefault())
            .format(Date(entry.createdAt))
        canvas.drawText(dateStr, MARGIN, y + 10f, metaPaint)
        y += 22f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 20f

        parseContent(entry).forEach { (label, value) ->
            ensure(24f)
            canvas = page!!.canvas
            canvas.drawText(strip(label), MARGIN, y + 14f, labelPaint)
            y += 24f

            val text = strip(value)
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1f)
                .build()

            ensure(layout.height.toFloat() + 6f)
            canvas = page!!.canvas
            canvas.save()
            canvas.translate(MARGIN, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height + 18f
        }

        page?.let { doc.finishPage(it) }
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
    }

    /** Opens the system share sheet for the PDF. Returns false if no target handled it. */
    fun share(context: Context, uri: Uri): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share PDF"))
    }.isSuccess

    /** PDF text rendering handles emoji unreliably — strip them for clean printouts. */
    internal fun strip(s: String): String = s
        .replace(Regex("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]|[\\u2600-\\u27BF]|\\uFE0F|\\u200D"), "")
        .trim()
}
