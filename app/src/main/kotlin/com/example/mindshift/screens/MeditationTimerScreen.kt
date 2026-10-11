package com.example.mindshift.screens

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MeditationTimerScreen(onBack: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val options = remember { listOf(1, 3, 5, 10, 15, 20) }
    var total by remember { mutableIntStateOf(5 * 60) }
    var remaining by remember { mutableIntStateOf(total) }
    var running by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // null-safe so rare ToneGenerator init failures can't crash the app
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    LaunchedEffect(running, total) {
        while (running && remaining > 0) {
            delay(1000)
            remaining -= 1
            if (remaining == 0) {
                finished = true
                running = false
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            }
        }
    }

    fun selectMinutes(min: Int) {
        total = min * 60
        remaining = total
        running = false
        finished = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← Back") }
        }
        Text("Meditation", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Sit comfortably. Follow your breath. When the mind wanders, gently return.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { min ->
                FilterChip(
                    selected = total == min * 60,
                    onClick = { selectMinutes(min) },
                    label = { Text("$min") }
                )
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(230.dp)) {
                    val stroke = 12.dp.toPx()
                    drawCircle(
                        color = Color(0x1A5F8D75),
                        style = Stroke(width = stroke)
                    )
                    val fraction = if (total > 0) remaining.toFloat() / total else 0f
                    drawArc(
                        color = Color(0xFF5F8D75),
                        startAngle = -90f,
                        sweepAngle = 360f * fraction,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format(Locale.US, "%d:%02d", remaining / 60, remaining % 60),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        when {
                            finished -> "Nice work 🌿"
                            running -> "Breathe…"
                            else -> "Ready when you are"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { if (!finished) running = !running },
                enabled = !finished,
                modifier = Modifier.height(48.dp)
            ) { Text(if (running) "Pause" else if (remaining < total) "Resume" else "Start") }
            OutlinedButton(
                onClick = { selectMinutes(total / 60) },
                modifier = Modifier.height(48.dp)
            ) { Text("Reset") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
