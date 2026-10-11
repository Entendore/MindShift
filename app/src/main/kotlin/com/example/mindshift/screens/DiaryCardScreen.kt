package com.example.mindshift.screens

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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.WorksheetEntry
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiaryCardScreen(nav: NavController, entryId: Long? = null) {
    val emotions = listOf(
        "anger" to "😠 Anger", "fear" to "😨 Fear", "sadness" to "😢 Sadness",
        "shame" to "😳 Shame", "joy" to "😊 Joy"
    )
    val skills = listOf(
        "Mindfulness", "Opposite Action", "DEAR MAN", "TIPP",
        "Pros & Cons", "5-4-3-2-1", "Gratitude", "Self-soothing",
        "Radical Acceptance", "Wise Mind", "Cope Ahead", "GIVE", "HALT",
        "Exposure", "Defusion"
    )

    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()

    val ratings = remember {
        mutableStateMapOf<String, Float>().apply { emotions.forEach { put(it.first, 0f) } }
    }
    var distress by remember { mutableFloatStateOf(0f) }
    val usedSkills = remember { mutableStateMapOf<String, Boolean>() }
    var note by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var originalEntry by remember { mutableStateOf<WorksheetEntry?>(null) }

    // Prefill when editing an existing diary card
    LaunchedEffect(entryId) {
        if (entryId != null) {
            dao.byId(entryId)?.let { e ->
                originalEntry = e
                runCatching { JSONObject(e.contentJson) }.getOrNull()?.let { o ->
                    emotions.forEach { (key, _) -> ratings[key] = o.optInt(key, 0).toFloat() }
                    distress = o.optInt("distress", 0).toFloat()
                    o.optJSONArray("skills")?.let { arr ->
                        for (i in 0 until arr.length()) usedSkills[arr.optString(i)] = true
                    }
                    note = o.optString("note", "")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("📔", fontSize = 34.sp)
        Text(
            if (entryId != null) "Edit Diary Card" else "Diary Card",
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold
        )
        Text(
            "A two-minute daily check-in. Rates are for today.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(20.dp))

        Text("Emotions (0–5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        emotions.forEach { (key, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Text("${ratings[key]!!.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = ratings[key]!!,
                onValueChange = { ratings[key] = it; saved = false },
                valueRange = 0f..5f,
                steps = 4
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Distress", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Text("${distress.toInt()} / 10",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = distress,
            onValueChange = { distress = it; saved = false },
            valueRange = 0f..10f,
            steps = 9
        )

        Spacer(Modifier.height(16.dp))
        Text("Skills I used today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            skills.forEach { skill ->
                FilterChip(
                    selected = usedSkills[skill] == true,
                    onClick = { usedSkills[skill] = !(usedSkills[skill] ?: false); saved = false },
                    label = { Text(skill) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it; saved = false },
            label = { Text("Anything worth remembering about today") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                scope.launch {
                    val json = JSONObject().apply {
                        emotions.forEach { (key, _) -> put(key, ratings[key]!!.toInt()) }
                        put("distress", distress.toInt())
                        put("skills", JSONArray().apply {
                            skills.filter { usedSkills[it] == true }.forEach { put(it) }
                        })
                        put("note", note)
                    }.toString()

                    val existing = originalEntry
                    if (entryId != null && existing != null) {
                        dao.update(existing.copy(contentJson = json))
                    } else {
                        dao.insert(WorksheetEntry(type = "diary_card", title = "Diary card", contentJson = json))
                    }
                    saved = true
                }
            },
            enabled = !saved,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                when {
                    saved -> "✓ Saved"
                    entryId != null -> "Save changes"
                    else -> "Save diary card"
                }
            )
        }

        if (saved) {
            Spacer(Modifier.height(12.dp))
            Button(onClick = { nav.popBackStack() }) { Text("Done") }
        }
        Spacer(Modifier.height(32.dp))
    }
}
