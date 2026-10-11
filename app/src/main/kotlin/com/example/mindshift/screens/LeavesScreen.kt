package com.example.mindshift.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private class Leaf(
    var x: Float,
    val yFrac: Float,
    val speed: Float,
    val sizeFrac: Float,
    val green: Color,
    var rot: Float = x * 360f
)

@Composable
fun LeavesScreen(onBack: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val phases = remember {
        listOf(
            "Settle in. Picture a slow stream, grass on the banks." to 12,
            "Notice a thought — worry, memory, plan. Don't push it away." to 15,
            "Place it gently on a leaf. Whatever it looks like is fine." to 12,
            "Watch the leaf drift. The stream carries it. You stay on the bank." to 15,
            "Another thought? Good. It goes on the next leaf." to 12
        )
    }

    var running by remember { mutableStateOf(false) }
    var phaseIndex by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(phases[0].second) }
    var cycles by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                phaseIndex = (phaseIndex + 1) % phases.size
                if (phaseIndex == 0) cycles += 1
                secondsLeft = phases[phaseIndex].second
            }
        }
    }

    val leaves = remember {
        val r = Random(7)
        mutableStateListOf<Leaf>().apply {
            repeat(9) {
                val x0 = r.nextFloat() * 1.3f - 0.15f
                add(
                    Leaf(
                        x = x0,
                        yFrac = 0.25f + r.nextFloat() * 0.5f,
                        speed = 0.05f + r.nextFloat() * 0.06f,
                        sizeFrac = 0.035f + r.nextFloat() * 0.03f,
                        green = when (it % 3) {
                            0 -> Color(0xFF8FBC9F)
                            1 -> Color(0xFF5F8D75)
                            else -> Color(0xFFB9D8C4)
                        }
                    )
                )
            }
        }
    }
    // Frame counter: Leaf vars aren't state, so we bump a snapshot-int
    // every animation frame to subscribe the draw pass.
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(running) {
        var last = 0L
        while (running) {
            withFrameNanos { t ->
                if (last != 0L) {
                    val dt = (t - last) / 1_000_000_000f
                    leaves.forEach { l ->
                        l.x += l.speed * dt
                        l.rot += 25f * dt
                        if (l.x > 1.2f) l.x = -0.2f
                    }
                }
                last = t
            }
            frame++
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) { TextButton(onClick = onBack) { Text("← Back") } }
        Text("Leaves on a Stream", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Notice a thought, place it on a leaf, let the water carry it.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val f = frame // state read keeps the canvas subscribed to frames
                drawRoundRect(
                    color = Color(0x1A5F8D75),
                    topLeft = Offset(0f, size.height * 0.22f),
                    size = Size(size.width, size.height * 0.56f),
                    cornerRadius = CornerRadius(size.height * 0.28f)
                )
                leaves.forEach { l ->
                    val side = minOf(size.width, size.height)
                    val s = l.sizeFrac * side * 2f
                    val cx = l.x * size.width
                    val cy = l.yFrac * size.height + sin(l.rot * 0.08f) * side * 0.02f
                    rotate(degrees = l.rot, pivot = Offset(cx, cy)) {
                        drawOval(
                            color = l.green.copy(alpha = 0.85f),
                            topLeft = Offset(cx - s / 2f, cy - s * 0.3f),
                            size = Size(s, s * 0.6f)
                        )
                    }
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Card {
                    Text(
                        phases[phaseIndex].first,
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "$secondsLeft",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            "Cycles: $cycles — one full pass is plenty.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { running = !running }, Modifier.height(48.dp)) {
                Text(if (running) "Pause" else if (cycles > 0 || phaseIndex > 0) "Resume" else "Start")
            }
            OutlinedButton(
                onClick = {
                    running = false
                    phaseIndex = 0
                    secondsLeft = phases[0].second
                    cycles = 0
                },
                Modifier.height(48.dp)
            ) { Text("Reset") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
