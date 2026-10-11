package com.example.mindshift.util

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.mindshift.data.MoodEntry
import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.data.Worksheets
import com.example.mindshift.screens.bestStreakFromStamps
import com.example.mindshift.screens.dayStamp
import com.example.mindshift.screens.lastNDayStamps
import com.example.mindshift.screens.parseContent
import com.example.mindshift.screens.startOfDaysAgoMillis
import com.example.mindshift.screens.streakFromStamps
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportPdf {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private val DARK = Color.rgb(0x26, 0x33, 0x2C)
    private val ACCENT = Color.rgb(0x5F, 0x8D, 0x75)
    private val GRAY = Color.rgb(0x8A, 0x97, 0x8F)

    fun rangeTitle(days: Int): String =
        if (days == 7) "Weekly Report" else "$days-Day Report"

    fun generate(
        context: Context,
        uri: Uri,
        entries: List<WorksheetEntry>,
        moods: List<MoodEntry>,
        rangeDays: Int = 7,
        includeFullEntries: Boolean = true
    ) {
        val doc = PdfDocument()
        val periodStart = startOfDaysAgoMillis(rangeDays)
        val prevStart = startOfDaysAgoMillis(rangeDays * 2)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 21f; color = DARK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val h2Paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f; color = ACCENT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10.5f; color = GRAY }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 12.5f; color = DARK }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11.5f; color = ACCENT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
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
            p.canvas.drawText("Generated with MindShift", MARGIN, PAGE_H - 26f, metaPaint)
        }
        fun ensure(h: Float) {
            if (page == null) startPage()
            if (y + h > PAGE_H - MARGIN) startPage()
        }
        fun heading(text: String) {
            ensure(34f)
            y += 8f
            page!!.canvas.drawText(text, MARGIN, y + 14f, h2Paint)
            y += 26f
        }
        fun line(text: String) {
            ensure(20f)
            page!!.canvas.drawText(text, MARGIN, y + 12f, bodyPaint)
            y += 18f
        }
        fun wrapped(label: String, value: String) {
            val text = PdfExporter.strip("$label: $value")
            val layout = StaticLayout.Builder
                .obtain(text, 0, text.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1f)
                .build()
            ensure(layout.height + 12f)
            val c = page!!.canvas
            c.save(); c.translate(MARGIN, y); layout.draw(c); c.restore()
            y += layout.height + 10f
        }

        startPage()
        var canvas = page!!.canvas
        val dayFmt = SimpleDateFormat("MMM d", Locale.getDefault())
        val fullFmt = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())

        canvas.drawText("MindShift — ${rangeTitle(rangeDays)}", MARGIN, y + 20f, titlePaint)
        y += 34f
        canvas.drawText(
            "Generated " + SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date()) +
                "  ·  covering " + dayFmt.format(Date(periodStart)) + " – " + dayFmt.format(Date()) +
                "  ($rangeDays days)",
            MARGIN, y + 10f, metaPaint
        )
        y += 22f
        canvas.drawLine(MARGIN, y, PAGE_W - MARGIN, y, linePaint)
        y += 14f

        heading("Mood")
        val inRange = moods.filter { it.createdAt >= periodStart }
        val prevRange = moods.filter { it.createdAt >= prevStart && it.createdAt < periodStart }
        val avgThis = inRange.map { it.mood }.average().takeIf { inRange.isNotEmpty() }
        val avgPrev = prevRange.map { it.mood }.average().takeIf { prevRange.isNotEmpty() }
        line(
            "Check-ins: ${inRange.size}" +
                (avgThis?.let { String.format(Locale.US, "  ·  average %.1f / 5", it) } ?: "")
        )
        if (avgThis != null && avgPrev != null) {
            val d = avgThis - avgPrev
            line(
                "Compared to the previous $rangeDays days (" +
                    String.format(Locale.US, "%.1f", avgPrev) + "): " +
                    if (d >= 0.25) "up " + String.format(Locale.US, "%.1f", d)
                    else if (d <= -0.25) "down " + String.format(Locale.US, "%.1f", -d)
                    else "about the same"
            )
        }

        val chartDays = minOf(rangeDays, 30)
        ensure(200f)
        if (moods.isEmpty()) {
            page!!.canvas.drawText("No mood check-ins recorded yet.", MARGIN, y + 20f, bodyPaint)
            y += 34f
        } else {
            drawMoodChart(page!!.canvas, MARGIN, y, PAGE_W - 2 * MARGIN, 150f, moods, metaPaint, dayFmt, chartDays)
            y += 168f
            ensure(18f)
            page!!.canvas.drawText(
                "Daily average mood, last $chartDays days (1–5)", MARGIN, y + 10f, metaPaint
            )
            y += 24f
        }

        heading("Practice")
        val stamps = (entries.map { dayStamp(it.createdAt) } + moods.map { dayStamp(it.createdAt) }).toSet()
        line("Entries this period: " + entries.count { it.createdAt >= periodStart })
        line("Current streak: ${streakFromStamps(stamps)} day(s)")
        line("Best streak: ${bestStreakFromStamps(stamps)} day(s)")
        val topTools = entries.filter { it.createdAt >= periodStart }
            .groupingBy { it.type }.eachCount().toList()
            .sortedByDescending { it.second }.take(5)
        if (topTools.isNotEmpty()) {
            line("Most-used tools (this period):")
            topTools.forEach { (type, count) ->
                val name = when {
                    type == "diary_card" -> "Diary card"
                    type == "coping_card" -> "Coping card"
                    type == "reminder_done" -> "Done from notifications"
                    type == "exposure_ladder" -> "Exposure ladder"
                    type == "exposure_step" -> "Exposure step"
                    else -> Worksheets.byId(type)?.title ?: type
                }
                line("   •  $name  ($count)")
            }
        }

        if (includeFullEntries) {
            heading("Recent entries")
            val limit = if (rangeDays <= 7) 8 else if (rangeDays <= 30) 12 else 14
            val recent = entries.filter { it.createdAt >= periodStart }
                .sortedByDescending { it.createdAt }.take(limit)
            if (recent.isEmpty()) line("No entries this period.")
            recent.forEach { e ->
                ensure(44f)
                y += 6f
                page!!.canvas.drawText(PdfExporter.strip(e.title), MARGIN, y + 12f, labelPaint)
                y += 20f
                page!!.canvas.drawText(fullFmt.format(Date(e.createdAt)), MARGIN, y + 9f, metaPaint)
                y += 18f
                parseContent(e).forEach { (label, value) -> wrapped(label, value) }
            }
        } else {
            heading("Entries this period")
            val compact = entries.filter { it.createdAt >= periodStart }
                .sortedByDescending { it.createdAt }
            if (compact.isEmpty()) {
                line("No entries this period.")
            } else {
                line("${compact.size} entr${if (compact.size == 1) "y" else "ies"}:")
                compact.take(20).forEach { e ->
                    line("   •  ${PdfExporter.strip(e.title)} — ${fullFmt.format(Date(e.createdAt))}")
                }
                if (compact.size > 20) line("   …and ${compact.size - 20} more not listed")
            }
        }

        page?.let { doc.finishPage(it) }
        context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
        doc.close()
    }

    private fun drawMoodChart(
        canvas: android.graphics.Canvas, left: Float, top: Float, w: Float, h: Float,
        moods: List<MoodEntry>, meta: TextPaint, dayFmt: SimpleDateFormat, days: Int
    ) {
        val stamps = lastNDayStamps(days)
        val byStamp = moods.groupBy { dayStamp(it.createdAt) }
        val avgs = stamps.map { s -> byStamp[s]?.map { it.mood }?.average() }

        val grid = Paint().apply { color = Color.rgb(0xE0, 0xE4, 0xE1); strokeWidth = 1f }
        for (m in 1..5) {
            val yy = top + h * (1f - (m - 1) / 4f)
            canvas.drawLine(left, yy, left + w, yy, grid)
        }

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ACCENT; strokeWidth = 2.5f; strokeCap = Paint.Cap.ROUND
        }
        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
        val stepX = if (days > 1) w / (days - 1) else w
        fun px(i: Int) = left + i * stepX
        fun py(v: Double) = (top + h * (1.0 - (v - 1) / 4.0)).toFloat()

        for (i in avgs.indices) {
            val a = avgs[i] ?: continue
            if (i > 0) avgs[i - 1]?.let { prev -> canvas.drawLine(px(i - 1), py(prev), px(i), py(a), line) }
            canvas.drawCircle(px(i), py(a), 3f, dot)
        }

        val midIdx = days / 2
        listOf(0 to stamps.first(), midIdx to stamps[midIdx], days - 1 to stamps.last())
            .forEach { (i, s) ->
                val text = dayFmt.format(Date(timestampOf(s)))
                val x = when (i) {
                    0 -> left
                    days - 1 -> left + w - meta.measureText(text)
                    else -> left + w / 2 - meta.measureText(text) / 2
                }
                canvas.drawText(text, x, top + h + 16f, meta)
            }
    }

    private fun timestampOf(stamp: Int): Long {
        val c = java.util.Calendar.getInstance().apply {
            clear()
            set(stamp / 10000, (stamp / 100) % 100 - 1, stamp % 100, 12, 0)
        }
        return c.timeInMillis
    }
}
