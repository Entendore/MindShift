package com.example.mindshift.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.Backup
import com.example.mindshift.data.MoodEntry
import com.example.mindshift.data.ReminderItem
import com.example.mindshift.data.SettingsRepository
import com.example.mindshift.data.TextSize
import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.reminders.ItemReminderScheduler
import com.example.mindshift.reminders.ReminderScheduler
import com.example.mindshift.ui.ThemeMode
import com.example.mindshift.ui.canUseBiometric
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { SettingsRepository.get(context) }
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()

    val theme by repo.theme.collectAsState(initial = ThemeMode.SYSTEM)
    val textSize by repo.textSize.collectAsState(initial = TextSize.NORMAL)
    val reminderEnabled by repo.reminderEnabled.collectAsState(initial = false)
    val reminderHour by repo.reminderHour.collectAsState(initial = 20)
    val lockEnabled by repo.lockEnabled.collectAsState(initial = false)
    val snoozeMinutes by repo.snoozeMinutes.collectAsState(initial = 10)

    val snackbar = remember { SnackbarHostState() }
    var pendingHour by remember { mutableStateOf<Int?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val applyReminder: (Boolean, Int) -> Unit = { enabled, hour ->
        scope.launch {
            repo.setReminder(enabled, hour)
            if (enabled) ReminderScheduler.schedule(context, hour)
            else ReminderScheduler.cancel(context)
        }
    }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingHour?.let { h -> if (granted) applyReminder(true, h) }
        pendingHour = null
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val count = runCatching { Backup.export(context, dao, uri) }.getOrDefault(-1)
                if (count >= 0) {
                    repo.setLastExportAt(System.currentTimeMillis())
                    snackbar.showSnackbar("Journal exported ($count entries) ✓")
                } else {
                    snackbar.showSnackbar("Couldn't create the backup")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val text = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: ""
                    val root = JSONObject(text)
                    var count = 0
                    root.optJSONArray("worksheetEntries")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            dao.insert(
                                WorksheetEntry(
                                    type = o.optString("type"),
                                    title = o.optString("title"),
                                    contentJson = o.optJSONObject("content")?.toString() ?: "{}",
                                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                            count++
                        }
                    }
                    root.optJSONArray("moods")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            dao.insertMood(
                                MoodEntry(
                                    mood = o.optInt("mood", 3),
                                    tags = o.optString("tags", ""),
                                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    root.optJSONArray("reminders")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            dao.insertReminder(
                                ReminderItem(
                                    label = o.optString("label", "Practice"),
                                    worksheetId = o.optString("worksheetId"),
                                    notifText = o.optString("notifText", ""),
                                    hour = o.optInt("hour", 9),
                                    minute = o.optInt("minute", 0),
                                    days = o.optString("days", ""),
                                    enabled = o.optBoolean("enabled", true)
                                )
                            )
                        }
                        dao.enabledReminders().forEach { ItemReminderScheduler.schedule(context, it) }
                    }
                    snackbar.showSnackbar("Imported $count entries ✓")
                } catch (e: Exception) {
                    snackbar.showSnackbar("Couldn't read that file")
                }
            }
        }
    }

    val hourLabel = String.format(
        Locale.US, "%d:00 %s",
        if (reminderHour % 12 == 0) 12 else reminderHour % 12,
        if (reminderHour < 12) "AM" else "PM"
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // Appearance
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ThemeMode.SYSTEM to "System",
                            ThemeMode.LIGHT to "Light",
                            ThemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = theme == mode,
                                onClick = { scope.launch { repo.setTheme(mode) } },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            // Text size
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Text size", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextSize.entries.forEach { size ->
                            FilterChip(
                                selected = textSize == size,
                                onClick = { scope.launch { repo.setTextSize(size) } },
                                label = { Text(size.label) }
                            )
                        }
                    }
                }
            }

            // App lock
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("App lock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Require fingerprint, face, or screen lock. Re-locks whenever you leave the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = lockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (canUseBiometric(context)) {
                                    scope.launch { repo.setLock(true) }
                                } else {
                                    scope.launch {
                                        snackbar.showSnackbar("Set up a fingerprint, face, or screen lock first")
                                    }
                                }
                            } else {
                                scope.launch { repo.setLock(false) }
                            }
                        }
                    )
                }
            }

            // Daily reminder
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Daily reminder", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "A gentle nudge at $hourLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= 33 &&
                                    ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    pendingHour = reminderHour
                                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    applyReminder(enabled, reminderHour)
                                }
                            }
                        )
                    }
                    TextButton(onClick = { showTimePicker = true }) { Text("Change time") }
                }
            }

            // Snooze length
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Snooze length", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "How long notification snoozes last by default.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 30, 60).forEach { m ->
                            FilterChip(
                                selected = snoozeMinutes == m,
                                onClick = { scope.launch { repo.setSnoozeMinutes(m) } },
                                label = { Text(if (m == 60) "1 hr" else "$m min") }
                            )
                        }
                    }
                }
            }

            // Data
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your data", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Everything stays on this device. Nothing is uploaded anywhere.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            exportLauncher.launch("mindshift-backup-${System.currentTimeMillis()}.json")
                        }) { Text("Export journal") }
                        OutlinedButton(onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        }) { Text("Import backup") }
                        TextButton(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Delete all") }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "MindShift is a self-help tool, not a substitute for professional care.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Delete everything?") },
            text = { Text("All worksheets, diary cards, and mood check-ins will be permanently removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    scope.launch {
                        dao.deleteAll()
                        dao.deleteAllMoods()
                        snackbar.showSnackbar("All data deleted")
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }

    if (showTimePicker) {
        val state = remember(reminderHour) {
            rememberTimePickerState(initialHour = reminderHour, initialMinute = 0, is24Hour = false)
        }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    applyReminder(true, state.hour)
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }
}
