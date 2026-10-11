package com.example.mindshift.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.MoodEntry
import com.example.mindshift.data.MoodTags
import com.example.mindshift.data.Worksheets
import com.example.mindshift.util.ReportPdf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()
    val entries by dao.observeAll().collectAsState(initial = emptyList())
    val moods by dao.observeMoods().collectAsState(initial = emptyList())

    val stamps = remember(entries, moods) {
        (entries.map { dayStamp(it.createdAt) } + moods.map { dayStamp(it.createdAt) }).toSet()
    }
    val currentStreak = remember(stamps) { streakFromStamps(stamps) }
    val bestStreak = remember(stamps) { bestStreakFromStamps(stamps) }

    val weekStart = remember { startOfDaysAgoMillis(7) }
    val twoWeekStart = remember { startOfDaysAgoMillis(14) }
    val moodThisWeek = moods.filter { it.createdAt >= weekStart }
    val moodLastWeek = moods.filter { it.createdAt >= twoWeekStart && it.createdAt < weekStart }
    val avgThis = moodThisWeek.map { it.mood }.average().takeIf { moodThisWeek.isNotEmpty() }
    val avgLast = moodLastWeek.map { it.mood }.average().takeIf { moodLastWeek.isNotEmpty() }

    val deltas = remember(entries) {
        entries.mapNotNull { e ->
            val o = runCatching { JSONObject(e.contentJson) }.getOrNull()
            val pre = o?.optInt("preMood", 0) ?: 0
            val post = o?.optInt("postMood", 0) ?: 0
            if (pre in 1..5 && post in 1..5) post - pre else null
        }
    }
    val avgDelta = deltas.takeIf { it.isNotEmpty() }?.average()

    val toolCounts = remember(entries) {
        entries.groupingBy { it.type }.eachCount().toList().sortedByDescending { it.second }
    }
    val maxTool = toolCounts.firstOrNull()?.second ?: 0

    val dailyCounts = remember(entries) {
        val byDay = entries.groupBy { dayStamp(it.createdAt) }
        lastNDayStamps(14).map { s -> s to (byDay[s]?.size ?: 0) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Insights", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Patterns from your own data — everything stays on this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(Modifier.weight(1f), "🔥", "$currentStreak", "current streak")
            StatCard(Modifier.weight(1f), "🏆", "$bestStreak", "best streak")
            StatCard(Modifier.weight(1f), "📔", "${entries.size}", "entries")
        }

        // ---- Progress report with range options ----
        Spacer(Modifier.height(12.dp))
        var reportRange by remember { mutableStateOf(7) }
        var includeFull by remember { mutableStateOf(true) }
        var reportMsg by remember { mutableStateOf<String?>(null) }
        var reportBusy by remember { mutableStateOf(false) }
        val reportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                reportBusy = true
                scope.launch {
                    val ok = withContext(Dispatchers.IO) {
                        runCatching {
                            ReportPdf.generate(context, uri, entries, moods, reportRange, includeFull)
                        }.isSuccess
                    }
                    reportBusy = false
                    reportMsg = if (ok) "✓ Report saved" else "Couldn't create the PDF"
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Progress report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "A print-ready PDF of your last $reportRange days — mood chart, practice stats, and " +
                        if (includeFull) "your full entry text." else "an entry summary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7 to "7 days", 30 to "30 days", 90 to "90 days").forEach { (d, label) ->
                        FilterChip(
                            selected = reportRange == d,
                            onClick = { reportRange = d; reportMsg = null },
                            label = { Text(label) }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = includeFull, onCheckedChange = { includeFull = it })
                    Text("Include full entry text", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { reportLauncher.launch("MindShift-Report-${reportRange}d.pdf") },
                    enabled = !reportBusy
                ) { Text(if (reportBusy) "Creating…" else "Create PDF") }
                reportMsg?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ---- Mood week over week ----
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Mood", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("This week", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            avgThis?.let { String.format(Locale.US, "%.1f", it) } ?: "–",
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Last week", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(
                            avgLast?.let { String.format(Locale.US, "%.1f", it) } ?: "–",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                if (avgThis != null && avgLast != null) {
                    Spacer(Modifier.height(8.dp))
                    val d = avgThis - avgLast
                    Text(
                        when {
                            d >= 0.25 -> "📈 Up ${String.format(Locale.US, "%.1f", d)} vs last week — nice."
                            d <= -0.25 -> "📉 Down ${String.format(Locale.US, "%.1f", -d)} vs last week — be gentle with yourself."
                            else -> "➖ About the same as last week."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Check in with your mood on Home and this comparison will fill in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        MoodByContext(moods)

        // ---- Heatmap ----
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Activity · last 20 weeks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val heat = remember(entries, moods) {
                    val byDay = (entries.map { dayStamp(it.createdAt) } +
                        moods.map { dayStamp(it.createdAt) }).groupingBy { it }.eachCount()
                    lastNDayStamps(20 * 7).map { byDay[it] ?: 0 }
                }
                UsageHeatmap(heat, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    "Darker squares = more practice that day.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // ---- Do your tools work ----
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Do your tools work?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (avgDelta != null)
                        "Across ${deltas.size} worksheet${if (deltas.size == 1) "" else "s"} where you rated your mood before/after, " +
                            "it moved ${String.format(Locale.US, "%+.1f", avgDelta)} on average."
                    else
                        "Pick a mood at the top of a worksheet and again right after saving — " +
                            "your average change will show up here.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ---- Top tools ----
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Your most-used tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (toolCounts.isEmpty()) {
                    Text(
                        "Save a few worksheets to see which ones you reach for most.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                toolCounts.take(3).forEach { (type, count) ->
                    val name = when {
                        type == "diary_card" -> "📔  Diary card"
                        type == "coping_card" -> "🆘  Coping card"
                        type == "reminder_done" -> "✅  Done from notifications"
                        type == "exposure_ladder" -> "🪜  Exposure ladder"
                        type == "exposure_step" -> "🪜  Exposure step"
                        else -> Worksheets.byId(type)?.let { "${it.emoji}  ${it.title}" } ?: type
                    }
                    val frac = if (maxTool > 0) count.toFloat() / maxTool else 0f
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(name, style = MaterialTheme.typography.bodyMedium)
                            Text("$count×", style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(frac)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // ---- Usage bars ----
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Entries · last 14 days", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                UsageBars(dailyCounts.map { it.second }, Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stampLabel(dailyCounts.first().first),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Today", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatCard(modifier: Modifier, emoji: String, value: String, label: String) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MoodByContext(moods: List<MoodEntry>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Mood by context", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            val tagged = moods.filter { it.tags.isNotBlank() }
            val rows = MoodTags.all.mapNotNull { (key, label) ->
                val vals = tagged.filter { key in it.tags.split(',').map(String::trim) }.map { it.mood }
                if (vals.size >= 2) Triple(label, vals.average(), vals.size) else null
            }.sortedByDescending { it.second }

            if (rows.isEmpty()) {
                Text(
                    "After a check-in, tag what's behind it (sleep, work, people…). " +
                        "After a few tagged days, patterns will show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                rows.forEach { (label, avg, n) ->
                    val frac = (((avg - 1.0) / 4.0).coerceIn(0.0, 1.0)).toFloat()
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                String.format(Locale.US, "%.1f", avg) + "  ·  ${n}×",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(frac)
                                    .fillMaxHeight()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Averages on days you tagged each context (2+ check-ins each). " +
                        "Patterns are clues, not verdicts.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
