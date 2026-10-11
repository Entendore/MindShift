package com.example.mindshift.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.mindshift.reminders.ReminderScheduler
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    var remindEnabled by remember { mutableStateOf(true) }
    var morning by remember { mutableStateOf(false) }
    var pendingOnboard by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) ReminderScheduler.schedule(context, if (morning) 9 else 20)
        if (pendingOnboard) { pendingOnboard = false; onDone() }
    }

    fun finish() {
        when {
            !remindEnabled -> onDone()
            Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED -> {
                pendingOnboard = true
                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> {
                ReminderScheduler.schedule(context, if (morning) 9 else 20)
                onDone()
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Skip") }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            when (page) {
                0 -> OnboardingPage("🌿", "Welcome to MindShift", listOf(
                    "Small, steady tools for your mind.",
                    "CBT & DBT worksheets · breathing · meditation timer",
                    "Mood tracking that shows you real patterns",
                    "A private journal you can search, edit, and share"
                ))
                1 -> OnboardingPage("🔒", "Private by design", listOf(
                    "Everything stays on this device — nothing is uploaded.",
                    "Export or back up your journal anytime, as a file.",
                    "An optional fingerprint lock lives in Settings.",
                    "Crisis resources are always one tap away."
                ))
                else -> OnboardingPage("🔔", "A gentle nudge", listOf(
                    "One quiet reminder a day can carry the habit."
                )) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily reminder", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Switch(checked = remindEnabled, onCheckedChange = { remindEnabled = it })
                    }
                    if (remindEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !morning, onClick = { morning = false },
                                label = { Text("☕ Morning · 9 AM") }
                            )
                            FilterChip(
                                selected = morning, onClick = { morning = true },
                                label = { Text("🌙 Evening · 8 PM") }
                            )
                        }
                    }
                    Text(
                        "You can change or remove this anytime in Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Row(
            Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(3) { i ->
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(
                onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                enabled = pagerState.currentPage > 0
            ) { Text("Back") }
            Button(onClick = {
                if (pagerState.currentPage == 2) finish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }) { Text(if (pagerState.currentPage == 2) "Get started" else "Next") }
        }
    }
}

@Composable
private fun OnboardingPage(
    emoji: String,
    title: String,
    lines: List<String>,
    extra: (@Composable ColumnScope.() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxSize().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        lines.forEach {
            Text(
                "•  $it",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        extra?.invoke()
    }
}
