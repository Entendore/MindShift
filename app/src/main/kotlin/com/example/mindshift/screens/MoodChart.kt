package com.example.mindshift.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.mindshift.data.MoodEntry
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

// ---------- Day stamps (yyyyMMdd ints, computed in AppClock's zone) ----------

/** Calendar day of [millis] as a yyyyMMdd int, in [zone]. */
fun dayStamp(millis: Long = AppClock.nowMillis(), zone: java.time.ZoneId = AppClock.zone()): Int {
    val d = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    return d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth
}

fun todayStamp(): Int = dayStamp()

/** Pure date arithmetic — DST-proof by construction (dates, not 24-hour blocks). */
fun addDays(stamp: Int, delta: Int): Int {
    val d = LocalDate.of(stamp / 10_000, (stamp / 100) % 100, stamp % 100)
        .plusDays(delta.toLong())
    return d.year * 10_000 + d.monthValue * 100 + d.dayOfMonth
}

fun stampLabel(stamp: Int): String = "${(stamp / 100) % 100}/${stamp % 100}"

// ---------- Range helpers ----------

/** Local midnight, [days] calendar days ago — never `now - days * 24h`. */
fun startOfDaysAgoMillis(days: Int): Long =
    LocalDate.now(AppClock.clock)
        .minusDays((days - 1).toLong())
        .atStartOfDay(AppClock.zone())
        .toInstant().toEpochMilli()

fun startOf7DaysAgoMillis(): Long = startOfDaysAgoMillis(7)

fun lastNDayStamps(n: Int): List<Int> =
    (n - 1 downTo 0).map { addDays(todayStamp(), -it) }

// ---------- Streaks ----------

fun streakFromStamps(stamps: Set<Int>): Int {
    if (stamps.isEmpty()) return 0
    var stamp = todayStamp()
    if (stamp !in stamps) {
        stamp = addDays(stamp, -1)
        if (stamp !in stamps) return 0
    }
    var streak = 0
    while (stamp in stamps) { streak++; stamp = addDays(stamp, -1) }
    return streak
}

fun bestStreakFromStamps(stamps: Set<Int>): Int {
    if (stamps.isEmpty()) return 0
    val sorted = stamps.sorted()
    var best = 1; var run = 1
    for (i in 1 until sorted.size) {
        run = if (addDays(sorted[i - 1], 1) == sorted[i]) run + 1 else 1
        if (run > best) best = run
    }
    return best
}

// ---------- Mood line chart ----------

@Composable
fun MoodChart(moods: List<MoodEntry>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val stamps = remember { (13 downTo 0).map { addDays(todayStamp(), -it) } }
    val byStamp = remember(moods) { moods.groupBy { dayStamp(it.createdAt) } }
    val points = stamps.map { s -> s to byStamp[s]?.map { it.mood }?.average()?.toFloat() }
    val avg = points.mapNotNull { it.second }
        .let { if (it.isNotEmpty()) String.format(Locale.US, "%.1f", it.average()) else null }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mood · last 14 days", style = MaterialTheme.typography.titleMedium)
            avg?.let { Text("avg $it", style = MaterialTheme.typography.titleSmall, color = primary) }
        }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.fillMaxWidth().height(110.dp)) {
            val w = size.width
            val h = size.height
            val stepX = w / 13f
            fun y(mood: Float) = h * (1f - (mood - 1f) / 4f)

            for (m in 1..5) {
                drawLine(grid, Offset(0f, y(m.toFloat())), Offset(w, y(m.toFloat())), strokeWidth = 1.dp.toPx())
            }
            val pts = points.mapIndexed { i, p -> p.second?.let { Offset(i * stepX, y(it)) } }
            for (i in 0 until pts.size - 1) {
                val a = pts[i]; val b = pts[i + 1]
                if (a != null && b != null) {
                    drawLine(primary, a, b, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            pts.forEachIndexed { i, p ->
                p?.let { drawCircle(primary, radius = (if (i == 13) 5f else 4f) * density / 2f, center = it) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stampLabel(stamps.first()), style = MaterialTheme.typography.labelSmall, color = labelColor)
            Text("Today", style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
    }
}

// ---------- Usage bars ----------

@Composable
fun UsageBars(counts: List<Int>, modifier: Modifier = Modifier) {
    val bar = MaterialTheme.colorScheme.primary
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier.height(90.dp)) {
        val stepX = size.width / counts.size.coerceAtLeast(1)
        counts.forEachIndexed { i, c ->
            val h = if (c == 0) 3.dp.toPx() else (size.height - 4.dp.toPx()) * c / max
            drawRoundRect(
                color = bar,
                topLeft = Offset(i * stepX + stepX * 0.2f, size.height - h),
                size = Size(stepX * 0.6f, h),
                cornerRadius = CornerRadius(5f, 5f)
            )
        }
    }
}

// ---------- Activity heatmap (20 weeks) ----------

@Composable
fun UsageHeatmap(counts: List<Int>, modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.surfaceVariant
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)
    val cols = (counts.size / 7).coerceAtLeast(1)

    Canvas(modifier.height(112.dp)) {
        val gap = 2.dp.toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * 6) / 7
        val side = minOf(cellW, cellH)
        counts.forEachIndexed { i, c ->
            val col = i / 7
            val row = i % 7
            val color = if (c == 0) empty
                else base.copy(alpha = 0.25f + 0.75f * (c.toFloat() / max))
            drawRoundRect(
                color = color,
                topLeft = Offset(col * (cellW + gap), row * (cellH + gap)),
                size = Size(side, side),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
    }
}
