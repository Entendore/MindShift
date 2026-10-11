package com.example.mindshift.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.QuickSkills
import com.example.mindshift.data.WorksheetEntry
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun CopingCardsScreen(nav: NavController) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).entryDao() }
    val scope = rememberCoroutineScope()
    val cards = QuickSkills.cards
    val pagerState = rememberPagerState(pageCount = { cards.size })

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
            Spacer(Modifier.weight(1f))
            Text(
                "${pagerState.currentPage + 1} / ${cards.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            val card = cards[page]
            var logged by remember(card.id) { mutableStateOf(false) }

            Card(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    Text(card.emoji, fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(card.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        card.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(16.dp))
                    card.steps.forEachIndexed { i, step ->
                        Text(
                            "${i + 1}.  $step",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                dao.insert(
                                    WorksheetEntry(
                                        type = "coping_card",
                                        title = "Coping card — ${card.title}",
                                        contentJson = JSONObject()
                                            .put("card", card.id)
                                            .put("name", card.title)
                                            .toString()
                                    )
                                )
                                logged = true
                            }
                        },
                        enabled = !logged,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text(if (logged) "✓ Logged — nice work" else "I tried this") }

                    if (card.relatedWorksheetId != null) {
                        TextButton(onClick = { nav.navigate("worksheet/${card.relatedWorksheetId}") }) {
                            Text("Open the full worksheet →")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(cards.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    Modifier
                        .size(if (active) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                )
            }
        }
    }
}
