package com.example.mindshift.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.WorksheetEntry
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private data class StepLog(
    val ladderId: String?,
    val fear: String,
    val step: String,
    val suds: Int,
    val createdAt: Long
)

private fun parseStepLog(e: WorksheetEntry): StepLog? = runCatching {
    val o = JSONObject(e.contentJson)
    StepLog(
        ladderId = o.optString("ladderId").takeIf { it.isNotBlank() },
        fear = o.optString("fear"),
        step = o.optString("step"),
        suds = o.optInt("suds", 0),
        createdAt = e.createdAt
    )
}.getOrNull()

// ---------------- HUB ----------------

@Composable
fun ExposureLadderScreen(nav: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()
    val ladders by dao.observeByType("exposure_ladder").collectAsState(initial = emptyList())
    val stepLogs by dao.observeByType("exposure_step").collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("Exposure Ladder", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Face a fear one chosen rung at a time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(14.dp))

        Card(Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(14.dp)) {
                Text("How ladders work", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                listOf(
                    "Name the avoided thing, then break it into steps from easiest to hardest.",
                    "Start on a low rung — around 30/100 fear, not 90.",
                    "Stay in it until the fear drops on its own. That drop is your nervous system learning.",
                    "Repeat a rung until it bores you before moving up.",
                    "Never force a step. If the fear traces back to trauma, climb alongside a therapist."
                ).forEach {
                    Text("•  $it", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Card(
            Modifier.fillMaxWidth().clickable { nav.navigate("worksheet/imaginal_exposure") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🌫️", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Can't practice it in real life?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Imaginal exposure works with the image instead — gently, in bounded sessions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (ladders.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🪜", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No ladders yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pick something you've been avoiding — phone calls, speaking up, a place you dodge — and build it a ladder.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            ladders.forEach { entry ->
                val parsed = remember(entry.id, entry.contentJson) { LadderJson.parse(entry) }
                val fear = parsed?.first ?: "Ladder"
                val steps = parsed?.second ?: emptyList()
                val done = steps.count { it.done }
                val frac = if (steps.isEmpty()) 0f else done.toFloat() / steps.size
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .clickable { nav.navigate("ladder/${entry.id}") }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪜", fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(fear, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        steps.isEmpty() -> "No steps yet — open to finish building"
                                        done == steps.size -> "Complete 🎉"
                                        else -> "$done of ${steps.size} steps taken"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { scope.launch { dao.delete(entry) } }) {
                                Icon(
                                    Icons.Rounded.Delete, contentDescription = "Delete ladder",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                        if (steps.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth())
                        }
                        val climbs = stepLogs.mapNotNull { parseStepLog(it) }.count {
                            it.ladderId == entry.id.toString() ||
                                (it.ladderId == null && it.fear == fear)
                        }
                        if (climbs > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "$climbs climb${if (climbs == 1) "" else "s"} logged",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = { nav.navigate("ladder") }) {
            Text(if (ladders.isEmpty()) "Build a ladder" else "Build another ladder")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------------- FORM ----------------

@Composable
private fun StepRows(steps: MutableList<String>) {
    steps.forEachIndexed { i, step ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${i + 1}.", Modifier.width(26.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = step,
                onValueChange = { steps[i] = it },
                placeholder = { Text("Step ${i + 1}") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { if (i > 0) steps[i] = steps[i - 1].also { steps[i - 1] = steps[i] } },
                enabled = i > 0
            ) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move up") }
            IconButton(
                onClick = {
                    if (i < steps.lastIndex) steps[i] = steps[i + 1].also { steps[i + 1] = steps[i] }
                },
                enabled = i < steps.lastIndex
            ) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move down") }
            IconButton(onClick = { steps.removeAt(i) }) {
                Icon(
                    Icons.Rounded.Close, contentDescription = "Remove step",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun LadderForm(
    initialFear: String,
    initialSteps: List<String>,
    saveLabel: String,
    onSave: (String, List<String>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var fear by remember { mutableStateOf(initialFear) }
    val steps = remember { mutableStateListOf<String>().apply { addAll(initialSteps) } }
    var error by remember { mutableStateOf<String?>(null) }

    OutlinedTextField(
        value = fear,
        onValueChange = { fear = it },
        label = { Text("The fear or avoided situation") },
        placeholder = { Text("e.g. Making phone calls") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))
    Text("Steps — easiest at the top", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))

    StepRows(steps)

    OutlinedButton(onClick = { steps.add("") }, modifier = Modifier.padding(top = 4.dp)) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Add step")
    }

    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    }

    Spacer(Modifier.height(20.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = {
                val clean = steps.filter { it.isNotBlank() }.map { it.trim() }
                if (fear.isBlank() || clean.isEmpty()) {
                    error = "Give the fear a name and at least one step."
                } else onSave(fear.trim(), clean)
            },
            modifier = Modifier.weight(1f).height(52.dp)
        ) { Text(saveLabel) }
        if (onDelete != null) {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(52.dp)
            ) { Text("Delete") }
        }
    }
}

@Composable
private fun LadderCreateScreen(nav: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("🪜", fontSize = 34.sp)
        Text("Build a ladder", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Small counts — \"type the email without sending it\" is a real rung.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        LadderForm(
            initialFear = "",
            initialSteps = listOf(""),
            saveLabel = "Start climbing"
        ) { fear, steps ->
            scope.launch {
                val id = dao.insert(
                    WorksheetEntry(
                        type = "exposure_ladder",
                        title = "Exposure ladder — ${fear.take(40)}",
                        contentJson = LadderJson.toJson(fear, steps.map { LadderStep(it) })
                    )
                )
                nav.popBackStack()
                nav.navigate("ladder/$id")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

// ---------------- WORKING SCREEN ----------------

@Composable
private fun LadderWorkScreen(nav: NavController, entryId: Long) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()

    var loaded by remember { mutableStateOf(false) }
    var entry by remember { mutableStateOf<WorksheetEntry?>(null) }
    var fear by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(listOf<LadderStep>()) }
    var editing by remember { mutableStateOf(false) }
    var sudsTarget by remember { mutableStateOf<Int?>(null) }

    val stepLogs by dao.observeByType("exposure_step").collectAsState(initial = emptyList())

    fun historyFor(index: Int): List<StepLog> {
        if (index >= steps.size) return emptyList()
        val text = steps[index].text
        return stepLogs.mapNotNull { parseStepLog(it) }
            .filter {
                (it.ladderId == entryId.toString() || (it.ladderId == null && it.fear == fear)) &&
                    it.step == text
            }
            .sortedBy { it.createdAt }
    }

    LaunchedEffect(entryId) {
        dao.byId(entryId)?.let { e ->
            entry = e
            LadderJson.parse(e)?.let { (f, s) -> fear = f; steps = s }
        }
        loaded = true
    }

    if (!loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val e = entry
    if (e == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Ladder not found.") }
        return
    }

    suspend fun persist() {
        entry?.let { dao.update(it.copy(contentJson = LadderJson.toJson(fear, steps))) }
    }

    val done = steps.count { it.done }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
            TextButton(onClick = { editing = !editing }) { Text(if (editing) "Done editing" else "Edit steps") }
        }
        Text("🪜", fontSize = 34.sp)
        Text(fear, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (steps.isNotEmpty()) {
            Text(
                if (done == steps.size) "Complete — every rung climbed 🎉"
                else "$done of ${steps.size} steps taken",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { done.toFloat() / steps.size },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(16.dp))

        if (editing) {
            LadderForm(
                initialFear = fear,
                initialSteps = steps.map { it.text },
                saveLabel = "Save changes",
                onDelete = {
                    scope.launch {
                        dao.delete(e)
                        nav.popBackStack()
                    }
                }
            ) { newFear, newSteps ->
                fear = newFear
                steps = newSteps.map { LadderStep(it) }
                scope.launch { persist() }
                editing = false
            }
        } else {
            steps.forEachIndexed { i, step ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = step.done,
                                onCheckedChange = { check ->
                                    if (check) {
                                        sudsTarget = i
                                    } else {
                                        steps = steps.toMutableList().also {
                                            it[i] = step.copy(done = false, suds = 0)
                                        }
                                        scope.launch { persist() }
                                    }
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    step.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textDecoration = if (step.done) TextDecoration.LineThrough else null,
                                    color = if (step.done) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                                            else MaterialTheme.colorScheme.onSurface
                                )
                                if (step.done && step.suds > 0) {
                                    Text(
                                        "Fear felt: ${step.suds}/100",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Text(
                                "Rung ${i + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                        val history = historyFor(i).map { it.suds }
                        if (history.size >= 2) {
                            SudsSparkline(history, Modifier.fillMaxWidth().padding(top = 4.dp))
                            Text(
                                "${history.size} climbs · ${history.first()} → ${history.last()}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (steps.isNotEmpty() && done == steps.size) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        "🎉 Ladder complete. Notice what changed since rung one — then, if you like, pick the next avoided thing and build it a ladder.",
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Climbing tips", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    listOf(
                        "Start low — a rung around 30/100 fear, not the top.",
                        "Stay in the situation until the fear drops on its own; that drop is the learning.",
                        "Repeat a rung until it's boring before moving up.",
                        "Never force. Dropping down a level is skill, not failure."
                    ).forEach {
                        Text("•  $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    sudsTarget?.let { index ->
        var rating by remember(index) { mutableFloatStateOf(40f) }
        val prior = historyFor(index).map { it.suds }
        AlertDialog(
            onDismissRequest = { sudsTarget = null },
            title = { Text("Rung ${index + 1} — done?") },
            text = {
                Column {
                    Text("How much fear did you feel while doing it?", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${rating.toInt()} / 100",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(value = rating, onValueChange = { rating = it }, valueRange = 0f..100f)
                    if (prior.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Previous climbs: " + prior.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = steps[index]
                    steps = steps.toMutableList().also {
                        it[index] = s.copy(done = true, suds = rating.toInt())
                    }
                    scope.launch {
                        persist()
                        dao.insert(
                            WorksheetEntry(
                                type = "exposure_step",
                                title = "Exposure — ${s.text.take(40)}",
                                contentJson = JSONObject()
                                    .put("ladderId", entryId.toString())
                                    .put("fear", fear)
                                    .put("step", s.text)
                                    .put("suds", rating.toInt())
                                    .toString()
                            )
                        )
                    }
                    sudsTarget = null
                }) { Text("Done — save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        val s = steps[index]
                        steps = steps.toMutableList().also { it[index] = s.copy(done = true, suds = 0) }
                        scope.launch { persist() }
                        sudsTarget = null
                    }) { Text("Skip rating") }
                    TextButton(onClick = { sudsTarget = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@Composable
fun LadderScreen(nav: NavController, entryId: Long?) {
    if (entryId == null) LadderCreateScreen(nav) else LadderWorkScreen(nav, entryId)
}

/** Tiny 0–100 SUDS trend across repeated climbs of one rung. */
@Composable
private fun SudsSparkline(ratings: List<Int>, modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val midline = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Canvas(modifier.height(30.dp)) {
        if (ratings.isEmpty()) return@Canvas
        val n = ratings.size
        val pad = 2.dp.toPx()
        val w = size.width
        val h = size.height
        fun y(v: Int) = pad + (h - 2 * pad) * (1f - v / 100f)
        // 50 reference line — below it is the habituated zone
        drawLine(midline, Offset(0f, y(50)), Offset(w, y(50)), strokeWidth = 1f)
        fun x(i: Int) = if (n == 1) w / 2 else i * (w / (n - 1))
        var prev: Offset? = null
        ratings.forEachIndexed { i, v ->
            val p = Offset(x(i), y(v))
            prev?.let { drawLine(line, it, p, strokeWidth = 2f) }
            drawCircle(line, radius = 2.5f, center = p)
            prev = p
        }
    }
}
