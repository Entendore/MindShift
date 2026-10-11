package com.example.mindshift.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.Glossary
import com.example.mindshift.data.GlossaryCategory
import com.example.mindshift.data.GlossaryTerm
import com.example.mindshift.data.Worksheets
import kotlinx.coroutines.launch

private fun routeLabel(route: String): String = when {
    route.startsWith("worksheet/") ->
        Worksheets.byId(route.removePrefix("worksheet/"))?.title ?: "worksheet"
    route == "diary" -> "Diary card"
    route == "breathing" -> "Breathing exercises"
    route == "timer" -> "Meditation timer"
    route == "coping" -> "Coping cards"
    route == "ladder" -> "Exposure ladder"
    route == "leaves" -> "Leaves on a Stream"
    else -> "the app"
}

@Composable
fun GlossaryScreen(nav: NavController, focus: String = "") {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<GlossaryCategory?>(null) }
    var expandedId by remember { mutableStateOf(focus.takeIf { Glossary.byId(it) != null }) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    fun jumpTo(termId: String) {
        expandedId = termId
        val needle = query.trim().lowercase()
        val list = Glossary.all
            .filter { category == null || it.category == category }
            .filter {
                needle.isEmpty() || it.term.lowercase().contains(needle) ||
                    it.definition.lowercase().contains(needle)
            }
            .sortedBy { it.term.lowercase() }
        val idx = list.indexOfFirst { it.id == termId }
        if (idx >= 0) scope.launch { listState.animateScrollToItem(idx) }
    }

    LaunchedEffect(focus) {
        if (focus.isNotBlank() && Glossary.byId(focus) != null) jumpTo(focus)
    }

    val needle = query.trim().lowercase()
    val displayList = remember(needle, category) {
        Glossary.all
            .filter { category == null || it.category == category }
            .filter {
                needle.isEmpty() || it.term.lowercase().contains(needle) ||
                    it.definition.lowercase().contains(needle)
            }
            .sortedBy { it.term.lowercase() }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("Glossary", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "The vocabulary of CBT and DBT — plain and simple.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search terms — try 'urge' or 'distortion'") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("glossary_search")
        )
        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = category == null,
                onClick = { category = null },
                label = { Text("All") },
                modifier = Modifier.testTag("cat_all")
            )
            GlossaryCategory.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = if (category == c) null else c },
                    label = { Text(c.label) },
                    modifier = Modifier.testTag("cat_${c.name}")
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${displayList.size} term${if (displayList.size == 1) "" else "s"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(6.dp))

        if (displayList.isEmpty()) {
            Text(
                "No terms match.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(displayList, key = { it.id }) { term ->
                GlossaryCard(
                    term = term,
                    expanded = expandedId == term.id,
                    onToggle = { expandedId = if (expandedId == term.id) null else term.id },
                    onOpenTerm = { jumpTo(it) },
                    onOpenWorksheet = { nav.navigate(it) }
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GlossaryCard(
    term: GlossaryTerm,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenTerm: (String) -> Unit,
    onOpenWorksheet: (String) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(term.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        term.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    term.definition,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                term.route?.let { route ->
                    TextButton(onClick = { onOpenWorksheet(route) }) {
                        Text("Try it → ${routeLabel(route)}")
                    }
                }
                if (term.seeAlso.isNotEmpty()) {
                    Text(
                        "Related: " + term.seeAlso.mapNotNull { Glossary.byId(it)?.term }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        term.seeAlso.mapNotNull { Glossary.byId(it) }.forEach { related ->
                            Text(
                                related.term,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onOpenTerm(related.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
