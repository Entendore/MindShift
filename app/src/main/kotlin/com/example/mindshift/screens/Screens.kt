package com.example.mindshift.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.Backup
import com.example.mindshift.data.DraftStore
import com.example.mindshift.data.MoodEntry
import com.example.mindshift.data.MoodTags
import com.example.mindshift.data.QuickSkills
import com.example.mindshift.data.SettingsRepository
import com.example.mindshift.data.Suggestions
import com.example.mindshift.data.WorksheetDefinition
import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.data.WorksheetEntryDao
import com.example.mindshift.data.Worksheets
import com.example.mindshift.reminders.ItemReminderScheduler
import com.example.mindshift.util.PdfExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =====================================================================
//  HOME
// =====================================================================

// Safety plan should be edited, not duplicated: open the latest one if it exists
private fun openLatestOrNew(
    nav: NavController, dao: WorksheetEntryDao, type: String, scope: CoroutineScope
) {
    scope.launch {
        val latest = dao.latestOfType(type)
        if (latest != null) nav.navigate("edit/${latest.id}")
        else nav.navigate("worksheet/$type")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(nav: NavController) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()

    val entries by dao.observeAll().collectAsState(initial = emptyList())
    val moods by dao.observeMoods().collectAsState(initial = emptyList())
    val favorites by repo.favorites.collectAsState(initial = emptySet())
    val reminders by dao.observeReminders().collectAsState(initial = emptyList())
    var justCheckedIn by remember { mutableStateOf<Int?>(null) }
    var justInsertedId by remember { mutableStateOf<Long?>(null) }
    var pickedTags by remember { mutableStateOf(setOf<String>()) }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
    val skill = remember { QuickSkills.skillOfTheDay() }
    val moodEmojis = listOf("😞", "😟", "😐", "🙂", "😄")

    val stamps = remember(entries, moods) {
        (entries.map { dayStamp(it.createdAt) } + moods.map { dayStamp(it.createdAt) }).toSet()
    }
    val streak = remember(stamps) { streakFromStamps(stamps) }
    val weekStart = remember { startOf7DaysAgoMillis() }
    val weekActivity = remember(entries, moods, weekStart) {
        entries.count { it.createdAt >= weekStart } + moods.count { it.createdAt >= weekStart }
    }
    val pinned = remember(favorites) { Worksheets.all.filter { it.id in favorites } }

    val nextReminder = remember(reminders) {
        reminders.filter { it.enabled }
            .minByOrNull { ItemReminderScheduler.effectiveNextMillis(it) }
    }

    // ---- Backup nudge state ----
    val lastExport by repo.lastExportAt.collectAsState(initial = 0L)
    val backupDismissedAt by repo.backupDismissedAt.collectAsState(initial = 0L)
    var backupBusy by remember { mutableStateOf(false) }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            backupBusy = true
            scope.launch {
                val ok = runCatching { Backup.export(context, dao, uri) }.isSuccess
                if (ok) repo.setLastExportAt(System.currentTimeMillis())
                backupBusy = false
            }
        }
    }
    val nowMs = remember { System.currentTimeMillis() }
    val showBackupNudge = entries.size >= 20 &&
        (lastExport == 0L || nowMs - lastExport > 30L * 24 * 60 * 60 * 1000) &&
        nowMs - backupDismissedAt > 14L * 24 * 60 * 60 * 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text("MindShift", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { nav.navigate("search") }) { Icon(Icons.Rounded.Search, contentDescription = "Search") }
            IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
        }

        Spacer(Modifier.height(16.dp))

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🔥 $streak", style = MaterialTheme.typography.titleLarge)
                    Text("day streak", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🌿 $weekActivity", style = MaterialTheme.typography.titleLarge)
                    Text("this week", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }

        // Compassion banner after a streak pause
        if (streak == 0 && stamps.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    "🌱 Your streak paused — that's okay. Progress isn't a performance, and a " +
                        "quiet stretch doesn't erase anything. Coming back is the skill.",
                    Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Backup nudge
        if (showBackupNudge) {
            Spacer(Modifier.height(10.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("💾 A gentle heads-up", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your journal — now ${entries.size} entries — lives only on this device. " +
                            "Saving a backup file means a lost phone won't mean lost work.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { backupLauncher.launch("mindshift-backup-${System.currentTimeMillis()}.json") },
                            enabled = !backupBusy
                        ) { Text(if (backupBusy) "Saving…" else "Back up now") }
                        TextButton(onClick = { scope.launch { repo.dismissBackupNudge(System.currentTimeMillis()) } }) {
                            Text("Maybe later")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Skill of the day
        Card(
            Modifier.fillMaxWidth().clickable { nav.navigate("coping") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(skill.emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Today's skill — ${skill.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        skill.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // In-the-moment row
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                Modifier.weight(1f).clickable { nav.navigate("coping") },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("🆘", fontSize = 20.sp)
                    Text("Coping cards", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("For right-now moments", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
            Card(
                Modifier.weight(1f).clickable { openLatestOrNew(nav, dao, "safety_plan", scope) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("🛟", fontSize = 20.sp)
                    Text("Safety plan", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("For the hardest days", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }
        }

        // Next-up reminder
        nextReminder?.let { r ->
            Spacer(Modifier.height(10.dp))
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("reminders") }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 20.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Next: ${r.label}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            ItemReminderScheduler.nextFireLabel(r),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Mood check-in
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("How are you right now?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    moodEmojis.forEachIndexed { index, emoji ->
                        TextButton(onClick = {
                            justCheckedIn = index + 1
                            scope.launch {
                                justInsertedId = dao.insertMood(MoodEntry(mood = index + 1))
                                pickedTags = emptySet()
                            }
                        }) { Text(emoji, fontSize = 26.sp) }
                    }
                }
                justCheckedIn?.let {
                    Text("Noted ${moodEmojis[it - 1]} — thanks for checking in.",
                        color = MaterialTheme.colorScheme.primary)
                }

                // Context tags
                if (justInsertedId != null) {
                    Text(
                        "What's behind it? (optional)",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MoodTags.all.forEach { (key, label) ->
                            FilterChip(
                                selected = key in pickedTags,
                                onClick = {
                                    pickedTags =
                                        if (key in pickedTags) pickedTags - key else pickedTags + key
                                    scope.launch {
                                        justInsertedId?.let { id ->
                                            dao.updateMoodTags(id, pickedTags.joinToString(","))
                                        }
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                // Mood-responsive suggestions
                justCheckedIn?.let { mood ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        when {
                            mood <= 2 -> "Might help right now"
                            mood == 3 -> "Worth a few minutes?"
                            else -> "Savor it"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Suggestions.forMood(mood, moods).forEach { s ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .clickable { nav.navigate(s.route) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(s.emoji, fontSize = 18.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        s.why,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        if (moods.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) { MoodChart(moods, Modifier.padding(16.dp)) }
        } else {
            Spacer(Modifier.height(8.dp))
            Text(
                "Check in for a few days to see your mood trend here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Pinned
        if (pinned.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Pinned", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            pinned.forEach { def ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        .clickable { nav.navigate("worksheet/${def.id}") }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(def.emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(def.title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Icon(
                            Icons.Rounded.Favorite, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Quick tools
        Spacer(Modifier.height(20.dp))
        Text("Quick tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        QuickCard("🌬️", "Breathing exercises", "Box, 4-7-8, and focus patterns") { nav.navigate("breathing") }
        QuickCard("⏱️", "Meditation timer", "Sit quietly for 1–20 minutes") { nav.navigate("timer") }
        QuickCard("⏰", "Practice reminders", "Nudges for specific tools") { nav.navigate("reminders") }
        QuickCard("🪞", "Weekly reflection", "Ten honest minutes on your week") { nav.navigate("weekly") }
        QuickCard("📔", "DBT diary card", "Rate your day in two minutes") { nav.navigate("diary") }
        QuickCard("📝", "Thought record", "Catch and reframe a difficult thought") { nav.navigate("worksheet/thought_record") }
        QuickCard("🌱", "5-4-3-2-1 grounding", "Anchor yourself through your senses") { nav.navigate("worksheet/grounding_54321") }
        QuickCard("🪜", "Exposure ladder", "Face a fear one chosen rung at a time") { nav.navigate("ladders") }
        QuickCard("🍃", "Leaves on a stream", "Unhook from sticky thoughts") { nav.navigate("leaves") }
        QuickCard("🙏", "Gratitude journal", "End the day on purpose") { nav.navigate("worksheet/gratitude") }
        QuickCard("📚", "Learn the basics", "CBT & DBT in plain language") { nav.navigate("learn") }
        QuickCard("📖", "Glossary", "CBT & DBT terms, defined plainly") { nav.navigate("glossary") }

        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth().clickable { nav.navigate("help") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🚨", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Need help now? Crisis resources",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuickCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// =====================================================================
//  CATEGORY LIST
// =====================================================================

@Composable
fun CategoryScreen(
    nav: NavController,
    title: String,
    blurb: String,
    worksheets: List<WorksheetDefinition>,
    header: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val favorites by repo.favorites.collectAsState(initial = emptySet())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    blurb,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        header?.let { item { it() } }
        items(worksheets) { def ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate("worksheet/${def.id}") }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(def.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(def.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            def.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (def.id in favorites) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// =====================================================================
//  CBT REFERENCE CARDS
// =====================================================================

private val distortions = listOf(
    "All-or-Nothing Thinking" to "You see things in black and white. If it's not perfect, it's a total failure.",
    "Catastrophizing" to "You assume the worst-case outcome is the most likely one.",
    "Overgeneralization" to "One bad event becomes \"always\" and \"never\".",
    "Mental Filter" to "You dwell on a single negative and filter out everything positive.",
    "Mind Reading" to "You assume you know what others think — usually something bad about you.",
    "Fortune Telling" to "You treat a grim prediction of the future as fact.",
    "Emotional Reasoning" to "\"I feel it, so it must be true.\" Feelings are real — but they aren't facts.",
    "Should Statements" to "You press on yourself (or others) with rigid rules: should, must, ought.",
    "Labeling" to "Instead of \"I made a mistake\", it becomes \"I'm a failure\".",
    "Personalization" to "You take responsibility — or blame — for things outside your control."
)

@Composable
fun DistortionsReference() {
    var expanded by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📚", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("The 10 thinking traps", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            AnimatedVisibility(expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    distortions.forEach { (name, desc) ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MindfulnessTips() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Practice tips", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            listOf(
                "A few minutes counts — start with 3 and build.",
                "Your mind will wander. Noticing and returning IS the practice.",
                "Try a breathing session first, then a worksheet.",
                "Same time, same place helps a habit stick."
            ).forEach { tip ->
                Text(
                    "•  $tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

// =====================================================================
//  WORKSHEET FILL / EDIT
// =====================================================================

@Composable
private fun MoodChipRow(label: String, selected: Int?, onSelect: (Int) -> Unit) {
    val emojis = listOf("😞", "😟", "😐", "🙂", "😄")
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            emojis.forEachIndexed { i, e ->
                val sel = selected == i + 1
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (sel) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(i + 1) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(e, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
fun WorksheetScreen(
    nav: NavController,
    id: String,
    entryId: Long? = null,
    header: (@Composable () -> Unit)? = null
) {
    val def = remember(id) { Worksheets.byId(id) }
    if (def == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Worksheet not found.") }
        return
    }

    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val repo = remember { SettingsRepository.get(context) }
    val scope = rememberCoroutineScope()

    val answers = remember(def.id, entryId) {
        mutableStateMapOf<String, String>().also { m ->
            if (entryId == null) DraftStore.get(def.id)?.forEach { (k, v) -> m[k] = v }
        }
    }
    val favorites by repo.favorites.collectAsState(initial = emptySet())
    val isFav = def.id in favorites
    var saved by remember { mutableStateOf(false) }
    var savedId by remember { mutableStateOf<Long?>(null) }
    var preMood by remember { mutableStateOf<Int?>(null) }
    var postMood by remember { mutableStateOf<Int?>(null) }
    var originalEntry by remember { mutableStateOf<WorksheetEntry?>(null) }
    var showReminderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        if (entryId != null) {
            dao.byId(entryId)?.let { e ->
                originalEntry = e
                runCatching { JSONObject(e.contentJson) }.getOrNull()?.let { o ->
                    def.fields.forEach { f ->
                        val v = o.optString(f.id, "")
                        if (v.isNotEmpty()) answers[f.id] = v
                    }
                }
            }
        }
    }

    // ---- Voice dictation ----
    var dictatingField by remember { mutableStateOf<String?>(null) }
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull().orEmpty().trim()
        val fieldId = dictatingField
        if (text.isNotEmpty() && fieldId != null) {
            val current = answers[fieldId].orEmpty()
            answers[fieldId] = if (current.isBlank()) text else "$current $text"
            saved = false
            if (entryId == null) DraftStore.put(def.id, fieldId, answers[fieldId]!!)
        }
        dictatingField = null
    }
    fun dictate(fieldId: String) {
        dictatingField = fieldId
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak — it'll be added to this field")
        }
        runCatching { speechLauncher.launch(intent) }.onFailure { dictatingField = null }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
            Row {
                IconButton(onClick = { nav.navigate("glossary") }) {
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = "Glossary",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
                IconButton(onClick = { showReminderDialog = true }) {
                    Icon(
                        Icons.Rounded.Alarm,
                        contentDescription = "Set reminder",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
                IconButton(onClick = { scope.launch { repo.toggleFavorite(def.id) } }) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFav) "Unpin" else "Pin",
                        tint = if (isFav) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
        Text(def.emoji, fontSize = 34.sp)
        Spacer(Modifier.height(6.dp))
        Text(def.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            def.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        header?.invoke()

        if (entryId == null) {
            MoodChipRow("First — how are you feeling right now?", preMood) {
                preMood = it; saved = false
            }
            Spacer(Modifier.height(16.dp))
        }

        def.fields.forEach { field ->
            OutlinedTextField(
                value = answers[field.id].orEmpty(),
                onValueChange = {
                    answers[field.id] = it
                    saved = false
                    if (entryId == null) DraftStore.put(def.id, field.id, it)
                },
                label = { Text(field.label) },
                placeholder = {
                    if (field.hint.isNotEmpty()) Text(field.hint, style = MaterialTheme.typography.bodySmall)
                },
                minLines = field.minLines,
                trailingIcon = {
                    IconButton(onClick = { dictate(field.id) }) {
                        Icon(
                            Icons.Rounded.Mic,
                            contentDescription = "Dictate",
                            tint = if (dictatingField == field.id) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp).testTag("field_${field.id}")
            )
        }

        val canSave = def.fields.any { (answers[it.id] ?: "").isNotBlank() }

        Button(
            onClick = {
                scope.launch {
                    val json = if (entryId != null) {
                        // start from the stored JSON so preMood/postMood survive edits
                        originalEntry?.let { runCatching { JSONObject(it.contentJson) }.getOrNull() }
                            ?: JSONObject()
                    } else JSONObject()

                    def.fields.forEach { f -> json.put(f.id, answers[f.id].orEmpty()) }
                    if (entryId == null && preMood != null) json.put("preMood", preMood)

                    if (entryId != null) {
                        originalEntry?.let { dao.update(it.copy(contentJson = json.toString())) }
                    } else {
                        savedId = dao.insert(
                            WorksheetEntry(type = def.id, title = def.title, contentJson = json.toString())
                        )
                        DraftStore.clear(def.id)
                    }
                    saved = true
                }
            },
            enabled = canSave && !saved,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_entry")
        ) { Text(if (entryId != null) "Save changes" else "Save entry") }

        if (saved) {
            Spacer(Modifier.height(12.dp))
            Text(
                "✓ " + if (entryId != null) "Changes saved" else "Saved to your journal",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            if (entryId == null) {
                Spacer(Modifier.height(12.dp))
                MoodChipRow("And now — how do you feel?", postMood) { m ->
                    postMood = m
                    scope.launch {
                        savedId?.let { sid ->
                            dao.byId(sid)?.let { e ->
                                val o = runCatching { JSONObject(e.contentJson) }.getOrDefault(JSONObject())
                                dao.update(e.copy(contentJson = o.put("postMood", m).toString()))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        answers.clear(); saved = false
                        preMood = null; postMood = null; savedId = null
                    }) { Text("Start another") }
                    Button(onClick = { nav.popBackStack() }) { Text("Done") }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.popBackStack() }) { Text("Done") }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    if (showReminderDialog) {
        ReminderEditorDialog(
            worksheetId = def.id,
            defaultLabel = def.title,
            onDismiss = { showReminderDialog = false }
        )
    }
}

// Routes a saved journal entry into the right editor
@Composable
fun EditScreen(nav: NavController, entryId: Long?) {
    if (entryId == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Entry not found.") }
        return
    }
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    var entry by remember { mutableStateOf<WorksheetEntry?>(null) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(entryId) {
        entry = dao.byId(entryId)
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val e = entry
    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Entry not found.") }
        return
    }
    when (e.type) {
        "diary_card" -> DiaryCardScreen(nav, entryId = e.id)
        "exposure_ladder" -> LadderScreen(nav, entryId = e.id)
        else -> WorksheetScreen(nav, id = e.type, entryId = e.id)
    }
}

// =====================================================================
//  SAVED JOURNAL
// =====================================================================

private val diaryEmotionLabels = linkedMapOf(
    "anger" to "😠 Anger", "fear" to "😨 Fear", "sadness" to "😢 Sadness",
    "shame" to "😳 Shame", "joy" to "😊 Joy"
)

private fun diaryRows(o: JSONObject): List<Pair<String, String>> {
    val rows = mutableListOf<Pair<String, String>>()
    diaryEmotionLabels.forEach { (key, label) ->
        if (o.has(key)) rows += label to "${o.optInt(key, 0)} / 5"
    }
    if (o.has("distress")) rows += "😰 Distress" to "${o.optInt("distress", 0)} / 10"
    o.optJSONArray("skills")?.let { arr ->
        if (arr.length() > 0) {
            rows += "🛠 Skills used" to (0 until arr.length()).joinToString(", ") { arr.optString(it) }
        }
    }
    o.optString("note", "").takeIf { it.isNotBlank() }?.let { rows += "📝 Note" to it }
    return rows
}

internal fun parseContent(entry: WorksheetEntry): List<Pair<String, String>> {
    return try {
        val obj = JSONObject(entry.contentJson)
        when {
            entry.type == "diary_card" -> diaryRows(obj)
            entry.type == "coping_card" -> {
                val name = obj.optString("name")
                listOf("🆘 Coping card" to if (name.isNotBlank()) name else "Practiced")
            }
            entry.type == "exposure_ladder" -> {
                val rows = mutableListOf<Pair<String, String>>()
                obj.optString("fear").takeIf { it.isNotBlank() }?.let { rows += "🎯 Fear" to it }
                val steps = obj.optJSONArray("steps")
                val total = steps?.length() ?: 0
                if (total > 0) {
                    val done = (0 until total).count {
                        steps?.optJSONObject(it)?.optBoolean("done") == true
                    }
                    rows += "🪜 Progress" to "$done of $total steps taken"
                }
                rows
            }
            entry.type == "exposure_step" -> buildList {
                obj.optString("step").takeIf { it.isNotBlank() }?.let { add("🪜 Step" to it) }
                obj.optString("fear").takeIf { it.isNotBlank() }?.let { add("🎯 Fear" to it) }
                if (obj.optInt("suds", 0) > 0) add("😰 Fear felt" to "${obj.optInt("suds")} / 100")
            }
            else -> {
                val def = Worksheets.byId(entry.type)
                if (def != null) {
                    def.fields.mapNotNull { field ->
                        val value = obj.optString(field.id, "")
                        if (value.isNotBlank()) field.label to value else null
                    }
                } else {
                    obj.keys().asSequence().mapNotNull { key ->
                        val value = obj.optString(key, "")
                        if (value.isNotBlank()) key.replaceFirstChar { it.uppercase() } to value else null
                    }.toList()
                }
            }
        }
    } catch (e: JSONException) {
        emptyList()
    }
}

@Composable
fun SavedScreen(nav: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val entries by dao.observeRecent(300).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
    val snackbar = remember { SnackbarHostState() }
    var totalEntries by remember { mutableStateOf(0) }
    LaunchedEffect(entries.size) { totalEntries = dao.countEntries() }

    // PDF export
    var pdfEntry by remember { mutableStateOf<WorksheetEntry?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val target = pdfEntry
        if (uri != null && target != null) {
            scope.launch {
                val ok = runCatching {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        PdfExporter.export(context, target, uri)
                    }
                }.isSuccess
                when {
                    !ok -> snackbar.showSnackbar("Couldn't create the PDF")
                    PdfExporter.share(context, uri) -> Unit
                    else -> snackbar.showSnackbar("PDF saved ✓")
                }
                pdfEntry = null
            }
        }
    }

    if (entries.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "Nothing here yet.\n\nFinish a worksheet, diary card, or mood check-in and it will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Your journal",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { nav.navigate("insights") }) {
                        Icon(Icons.Rounded.Insights, contentDescription = "Insights")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            items(entries, key = { it.id }) { entry ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("edit/${entry.id}") }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    dateFormat.format(Date(entry.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            IconButton(onClick = {
                                val text = buildString {
                                    appendLine(entry.title)
                                    appendLine(dateFormat.format(Date(entry.createdAt)))
                                    appendLine()
                                    parseContent(entry).forEach { (label, value) ->
                                        appendLine("$label: $value")
                                        appendLine()
                                    }
                                    appendLine("— shared from MindShift")
                                }
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share entry"))
                            }) {
                                Icon(
                                    Icons.Rounded.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(onClick = {
                                pdfEntry = entry
                                val safeName = entry.title
                                    .filter { it.isLetterOrDigit() || it == ' ' }
                                    .trim()
                                    .ifBlank { "entry" }
                                pdfLauncher.launch("MindShift-$safeName.pdf")
                            }) {
                                Icon(
                                    Icons.Rounded.PictureAsPdf,
                                    contentDescription = "Export PDF",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    dao.delete(entry)
                                    val result = snackbar.showSnackbar(
                                        "Entry deleted", actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) dao.insert(entry)
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                        parseContent(entry).forEach { (label, value) ->
                            Spacer(Modifier.height(8.dp))
                            Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "Tap to edit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            if (totalEntries > entries.size) {
                item {
                    Text(
                        "Showing your latest ${entries.size} entries — use search (the magnifying glass on Home) to find older ones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
