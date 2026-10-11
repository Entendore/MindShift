package com.example.mindshift.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun BreathingScreen(onBack: () -> Unit) {
    // keep screen on while practicing
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    data class BreathPhase(val label: String, val seconds: Int)

    val patterns = remember {
        listOf(
            "Box 4-4-4-4" to listOf(
                BreathPhase("Breathe in", 4), BreathPhase("Hold", 4),
                BreathPhase("Breathe out", 4), BreathPhase("Hold", 4)
            ),
            "Relax 4-7-8" to listOf(
                BreathPhase("Breathe in", 4), BreathPhase("Hold", 7), BreathPhase("Breathe out", 8)
            ),
            "Focus 5-5" to listOf(
                BreathPhase("Breathe in", 5), BreathPhase("Breathe out", 5)
            )
        )
    }

    var patternIndex by remember { mutableIntStateOf(0) }
    val phases = patterns[patternIndex].second

    var running by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(phases[0].seconds) }
    var cycles by remember { mutableIntStateOf(0) }
    var targetScale by remember { mutableFloatStateOf(0.55f) }

    val haptics = LocalHapticFeedback.current

    // Reset whenever the pattern changes
    LaunchedEffect(patternIndex) {
        running = false
        phaseIndex = 0
        cycles = 0
        secondsLeft = patterns[patternIndex].second.first().seconds
        targetScale = 0.55f
    }

    // Timing loop (keeps progress across pause/resume)
    LaunchedEffect(running, patternIndex) {
        while (running) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                phaseIndex = (phaseIndex + 1) % phases.size
                if (phaseIndex == 0) cycles += 1
                secondsLeft = phases[phaseIndex].seconds
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    // Expand on inhale, shrink on exhale
    LaunchedEffect(phaseIndex, running, patternIndex) {
        if (!running) return@LaunchedEffect
        when (phases[phaseIndex].label) {
            "Breathe in" -> targetScale = 1f
            "Breathe out" -> targetScale = 0.55f
        }
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = phases[phaseIndex].seconds * 1000),
        label = "breathScale"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) { Text("← Back") }
        }
        Text("Breathing", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            patterns.forEachIndexed { i, (name, _) ->
                FilterChip(
                    selected = i == patternIndex,
                    onClick = { patternIndex = i },
                    label = { Text(name) }
                )
            }
        }

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        phases[phaseIndex].label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("$secondsLeft", style = MaterialTheme.typography.displaySmall)
                }
            }
        }

        Text(
            "Cycles completed: $cycles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }, modifier = Modifier.height(48.dp)) {
                Text(if (running) "Pause" else if (cycles > 0 || phaseIndex > 0) "Resume" else "Start")
            }
            OutlinedButton(
                onClick = {
                    running = false
                    phaseIndex = 0
                    cycles = 0
                    secondsLeft = phases[0].seconds
                    targetScale = 0.55f
                },
                modifier = Modifier.height(48.dp)
            ) { Text("Reset") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
