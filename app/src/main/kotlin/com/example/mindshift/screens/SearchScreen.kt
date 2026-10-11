package com.example.mindshift.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.Glossary
import com.example.mindshift.data.Worksheets
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun categoryOf(id: String): String = when (id) {
    in Worksheets.cbt.map { it.id } -> "CBT"
    in Worksheets.dbt.map { it.id } -> "DBT"
    else -> "Mindfulness"
}

@Composable
fun SearchScreen(nav: NavController) {
    var query by remember { mutableStateOf("") }
    val needle = query.trim().lowercase()
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    val toolResults = remember(needle) {
        if (needle.isEmpty()) emptyList()
        else Worksheets.all.filter { def ->
            (def.title + " " + def.subtitle + " " +
                def.fields.joinToString(" ") { it.label + " " + it.hint })
                .lowercase().contains(needle)
        }
    }
    val glossaryResults = remember(needle) { Glossary.search(needle).take(6) }
    val journalFlow = remember(needle) {
        if (needle.isEmpty()) flowOf(emptyList())
        else dao.searchEntries(needle)
    }
    val journalResults by journalFlow.collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search tools, terms, and your journal") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        when {
            needle.isEmpty() -> Text(
                "Search ${Worksheets.all.size} worksheets, the glossary, or any word you've written.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            toolResults.isEmpty() && journalResults.isEmpty() && glossaryResults.isEmpty() -> Text(
                "No matches for \"$query\".",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (toolResults.isNotEmpty()) {
                    item {
                        Text("Tools", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    items(toolResults) { def ->
                        Card(
                            Modifier.fillMaxWidth().clickable { nav.navigate("worksheet/${def.id}") }
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(def.emoji, fontSize = 26.sp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(def.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${categoryOf(def.id)} · ${def.subtitle}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                if (glossaryResults.isNotEmpty()) {
                    item {
                        Text("Glossary", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    items(glossaryResults, key = { "g${it.id}" }) { term ->
                        Card(
                            Modifier.fillMaxWidth()
                                .clickable { nav.navigate("glossary?focus=${term.id}") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(term.term, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    term.definition,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                if (journalResults.isNotEmpty()) {
                    item {
                        Text(
                            "In your journal (${journalResults.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(journalResults, key = { "j${it.id}" }) { entry ->
                        Card(
                            Modifier.fillMaxWidth().clickable { nav.navigate("edit/${entry.id}") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    dateFormat.format(Date(entry.createdAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                parseContent(entry).firstOrNull()?.let { (label, value) ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "$label — $value",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
