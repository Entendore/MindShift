package com.example.mindshift.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.ReminderItem
import com.example.mindshift.data.WorksheetDefinition
import com.example.mindshift.data.Worksheets
import com.example.mindshift.reminders.ItemReminderScheduler
import com.example.mindshift.widgets.NextUpWidget
import kotlinx.coroutines.launch

@Composable
fun RemindersScreen(nav: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()
    val reminders by dao.observeReminders().collectAsState(initial = emptyList())

    var showPicker by remember { mutableStateOf(false) }
    var editorFor by remember { mutableStateOf<WorksheetDefinition?>(null) }
    var editingItem by remember { mutableStateOf<ReminderItem?>(null) }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("Practice reminders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Nudges for specific tools — tap one to edit it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        if (reminders.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "No reminders yet.\n\nOpen any worksheet and tap the alarm icon, or add one below. Repeating reminders fire a quiet notification and open the tool when tapped.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(reminders, key = { it.id }) { r ->
                    val ws = Worksheets.byId(r.worksheetId)
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ws?.emoji ?: "⏰", fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).clickable { editingItem = r }) {
                                Text(r.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    listOfNotNull(
                                        ws?.title,
                                        ItemReminderScheduler.daysLabel(r.days),
                                        ItemReminderScheduler.timeLabel(r.hour, r.minute)
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                if (r.notifText.isNotBlank()) {
                                    Text(
                                        "“${r.notifText}”",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Text(
                                    if (r.enabled) "Next: ${ItemReminderScheduler.nextFireLabel(r)}" else "Paused",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (r.enabled) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            Switch(
                                checked = r.enabled,
                                onCheckedChange = { on ->
                                    scope.launch {
                                        if (on) {
                                            dao.updateReminder(r.copy(enabled = true))
                                            ItemReminderScheduler.schedule(context, r.copy(enabled = true))
                                        } else {
                                            dao.updateReminder(r.copy(enabled = false, snoozedUntil = 0))
                                            ItemReminderScheduler.cancel(context, r.id)
                                        }
                                        NextUpWidget().updateAll(context)
                                    }
                                }
                            )
                            IconButton(onClick = {
                                scope.launch {
                                    ItemReminderScheduler.cancel(context, r.id)
                                    dao.deleteReminder(r)
                                    NextUpWidget().updateAll(context)
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.Delete, contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showPicker = true }) { Text("Add for a worksheet") }
        Spacer(Modifier.height(24.dp))
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Pick a worksheet") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Worksheets.all.forEach { def ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { showPicker = false; editorFor = def }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(def.emoji, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(def.title, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } }
        )
    }

    editorFor?.let { def ->
        ReminderEditorDialog(
            worksheetId = def.id,
            defaultLabel = def.title,
            onDismiss = { editorFor = null }
        )
    }

    editingItem?.let { r ->
        ReminderEditorDialog(
            worksheetId = r.worksheetId,
            defaultLabel = r.label,
            existing = r,
            onDismiss = { editingItem = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderEditorDialog(
    worksheetId: String,
    defaultLabel: String,
    onDismiss: () -> Unit,
    existing: ReminderItem? = null
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()
    val forWorksheet by dao.observeForWorksheet(worksheetId).collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf(existing) }
    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S") // index 0 → Calendar.SUNDAY

    // Re-derives all form state whenever the item being edited changes
    key(editing?.id ?: -1L) {
        var label by remember { mutableStateOf(editing?.label ?: defaultLabel) }
        var notifText by remember { mutableStateOf(editing?.notifText ?: "") }
        var days by remember {
            mutableStateOf(
                editing?.days?.split(',')
                    ?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()
            )
        }
        val timeState = rememberTimePickerState(
            initialHour = editing?.hour ?: 18,
            initialMinute = editing?.minute ?: 0,
            is24Hour = false
        )
        val isEdit = editing != null

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (isEdit) "Edit reminder" else "New reminder") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notifText,
                        onValueChange = { notifText = it },
                        label = { Text("Notification note (optional)") },
                        placeholder = { Text("Exactly what you want to remember in the moment") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Time", style = MaterialTheme.typography.titleSmall)
                    TimePicker(state = timeState)
                    Text("Repeat on", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLetters.forEachIndexed { index, letter ->
                            val dayValue = index + 1
                            FilterChip(
                                selected = dayValue in days,
                                onClick = {
                                    days = if (dayValue in days) days - dayValue else days + dayValue
                                },
                                label = { Text(letter) }
                            )
                        }
                    }
                    Text(
                        "No days selected = one-time reminder.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (!isEdit && forWorksheet.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Already set for this worksheet — tap to edit",
                            style = MaterialTheme.typography.titleSmall
                        )
                        forWorksheet.forEach { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { editing = r }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.label, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${ItemReminderScheduler.timeLabel(r.hour, r.minute)} · ${ItemReminderScheduler.daysLabel(r.days)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        ItemReminderScheduler.cancel(context, r.id)
                                        dao.deleteReminder(r)
                                        NextUpWidget().updateAll(context)
                                    }
                                }) {
                                    Icon(
                                        Icons.Rounded.Delete, contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val cleanLabel = label.ifBlank { defaultLabel }
                        val daysStr = days.sorted().joinToString(",")
                        val current = editing
                        if (current != null) {
                            val updated = current.copy(
                                label = cleanLabel,
                                notifText = notifText.trim(),
                                hour = timeState.hour,
                                minute = timeState.minute,
                                days = daysStr
                            )
                            dao.updateReminder(updated)
                            ItemReminderScheduler.schedule(context, updated)
                        } else {
                            val newId = dao.insertReminder(
                                ReminderItem(
                                    label = cleanLabel,
                                    worksheetId = worksheetId,
                                    notifText = notifText.trim(),
                                    hour = timeState.hour,
                                    minute = timeState.minute,
                                    days = daysStr
                                )
                            )
                            dao.reminderById(newId)?.let { ItemReminderScheduler.schedule(context, it) }
                        }
                        NextUpWidget().updateAll(context)
                    }
                    onDismiss()
                }) { Text(if (isEdit) "Save changes" else "Set reminder") }
            },
            dismissButton = {
                Row {
                    if (isEdit) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    editing?.let {
                                        ItemReminderScheduler.cancel(context, it.id)
                                        dao.deleteReminder(it)
                                        NextUpWidget().updateAll(context)
                                    }
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Delete") }
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            }
        )
    }
}
