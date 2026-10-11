package com.example.mindshift.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Need help now?", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "If you're struggling, reaching out is a strength — and there are people ready 24/7.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )
        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("📞 988 Suicide & Crisis Lifeline", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Free, confidential support 24/7 in the US. Call or text 988.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = { uri.openUri("tel:988") }) { Text("Call 988") }
            }
        }

        Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("💬 Crisis Text Line", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Text HOME to 741741 (US/Canada/UK) to chat with a trained counselor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { uri.openUri("smsto:741741") }) { Text("Open messaging") }
            }
        }

        Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("🌍 Find a helpline worldwide", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Free, confidential helplines in over 130 countries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { uri.openUri("https://findahelpline.com") }) { Text("Visit site") }
            }
        }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("About this app", style = MaterialTheme.typography.titleMedium)
                Text(
                    "MindShift is a self-help tool, not therapy, and not a substitute for " +
                        "professional care. If you're in immediate danger, call your local " +
                        "emergency number. A therapist, doctor, or personal care. " +
                        "and if you're in crisis, use the resources above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
