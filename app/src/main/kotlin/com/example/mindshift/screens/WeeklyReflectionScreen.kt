package com.example.mindshift.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mindshift.data.AppDatabase
import java.util.Locale

@Composable
fun WeeklyReflectionScreen(nav: NavController) {
    WorksheetScreen(nav = nav, id = "weekly_reflection", header = { ReflectionStats() })
}

@Composable
private fun ReflectionStats() {
    val dao = remember { AppDatabase.get(LocalContext.current).entryDao() }
    val entries by dao.observeAll().collectAsState(initial = emptyList())
    val moods by dao.observeMoods().collectAsState(initial = emptyList())

    val weekStart = remember { startOfDaysAgoMillis(7) }
    val twoWeekStart = remember { startOfDaysAgoMillis(14) }
    val thisWeek = moods.filter { it.createdAt >= weekStart }
    val lastWeek = moods.filter { it.createdAt >= twoWeekStart && it.createdAt < weekStart }
    val avgThis = thisWeek.map { it.mood }.average().takeIf { thisWeek.isNotEmpty() }
    val avgLast = lastWeek.map { it.mood }.average().takeIf { lastWeek.isNotEmpty() }
    val entryCount = entries.count { it.createdAt >= weekStart }
    val streak = remember(entries, moods) {
        streakFromStamps(
            (entries.map { dayStamp(it.createdAt) } + moods.map { dayStamp(it.createdAt) }).toSet()
        )
    }

    Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Your week so far", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiniStat("Check-ins", "${thisWeek.size}")
                MiniStat("Entries", "$entryCount")
                MiniStat("Streak", "🔥 $streak")
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    avgThis == null -> "No mood check-ins this week yet — the comparison below fills in as you go."
                    avgLast == null ->
                        "Average mood ${String.format(Locale.US, "%.1f", avgThis)}. No data from last week to compare yet."
                    else -> {
                        val d = avgThis - avgLast
                        "Average mood ${String.format(Locale.US, "%.1f", avgThis)} — " + when {
                            d >= 0.25 -> "up ${String.format(Locale.US, "%.1f", d)} vs last week 📈"
                            d <= -0.25 -> "down ${String.format(Locale.US, "%.1f", -d)} vs last week 📉"
                            else -> "about even with last week ➖"
                        }
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
