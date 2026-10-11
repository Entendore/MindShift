package com.example.mindshift.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

private val learnSections = listOf(
    Triple("🧠", "What is CBT?",
        "Cognitive behavioral therapy starts from a simple loop:\n\n" +
            "situation → thoughts → feelings → actions\n\n" +
            "Thoughts aren't facts — but they steer feelings and behavior. CBT worksheets " +
            "(thought records, trap checks) slow the loop down so you can examine the thought " +
            "and test it against evidence, instead of just believing it."),
    Triple("⚖️", "What is DBT?",
        "Dialectical behavior therapy balances two truths at once: \"I'm doing my best\" " +
            "and \"I can do better.\"\n\nIts skills come in four modules:\n" +
            "• Mindfulness — attention\n" +
            "• Distress tolerance — crisis survival\n" +
            "• Emotion regulation — changing emotions over time\n" +
            "• Interpersonal effectiveness — asking and saying no"),
    Triple("🧘", "What is mindfulness?",
        "Paying attention, on purpose, without judgment.\n\n" +
            "It's not emptying the mind — it's noticing where the mind went, and returning. " +
            "Every return is one repetition. The wandering isn't failure; it's the workout."),
    Triple("🧭", "Which tool, when?",
        "Crisis or overwhelm → coping cards, TIPP, HALT\n" +
            "A looping worry → the worry tree\n" +
            "A recurring painful thought → thought record\n" +
            "Digging under a thought → downward arrow\n" +
            "Testing a fear for real → behavioral experiment\n" +
            "A big emotion that doesn't fit the facts → check the facts, opposite action\n" +
            "Conflict or a hard ask → DEAR MAN + GIVE\n" +
            "Holding your ground → FAST\n" +
            "A hard decision → wise mind\n" +
            "A hard event coming up → cope ahead\n" +
            "Fighting something that already happened → radical acceptance\n" +
            "A fear you keep avoiding → exposure ladder\n" +
            "A fear that can't be safely rehearsed → imaginal exposure\n" +
            "Sticky thoughts that argue back → leaves on a stream\n" +
            "Low motivation → action before motivation\n" +
            "The daily picture → diary card + mood check-in"),
    Triple("🪜", "What is exposure?",
        "Avoidance is fear's best friend: every time you dodge the scary thing, the fear gets " +
            "proof it was right.\n\n" +
            "Exposure reverses it — a ladder of small steps you choose, easiest to hardest. " +
            "Climb one rung, stay until the fear drops on its own (it always does — that's " +
            "habituation), repeat until it bores you, then climb.\n\n" +
            "Rule one: never force it. If a rung is too much, drop down a level. And if the " +
            "fear traces back to trauma, do this work alongside a therapist.\n\n" +
            "Can't rehearse it in real life? That's imaginal exposure — and for trauma " +
            "memories it's work to do with a therapist, not alone."),
    Triple("🍃", "Defusion — unhooking from thoughts",
        "Some thoughts don't need arguing with — they need distance.\n\n" +
            "Defusion treats a thought as weather passing through, not a command to obey. " +
            "Leaves on a Stream is the classic version: place each thought on a leaf and let " +
            "the water take it.\n\n" +
            "You're not making thoughts go away. You're practicing being the bank of the " +
            "river, not the leaves."),
    Triple("📈", "How skills get stronger",
        "Practice while calm, so the skills are there when you're not.\n\n" +
            "Reps beat intensity: 3 minutes daily beats 30 minutes monthly. And progress is a " +
            "wobble line, not a ramp — a hard day is data, not failure."),
    Triple("🤝", "This app vs. therapy",
        "Worksheets are practice, not treatment. A therapist tailors this work to you.\n\n" +
            "If things feel heavy, bring your journal to a session — concrete entries make " +
            "sessions more useful. And if you're in crisis, use the resources on the Help screen.")
)

@Composable
fun LearnScreen(nav: NavController) {
    var expanded by remember { mutableStateOf(setOf<Int>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = { nav.popBackStack() }) { Text("← Back") }
        Text("Learn", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "The ideas behind the worksheets — short and plain.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))

        Card(
            Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { nav.navigate("glossary") },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("📖", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Glossary", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold)
                    Text(
                        "Every term in this app, defined in plain language",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        learnSections.forEachIndexed { i, (emoji, title, body) ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            expanded = if (i in expanded) expanded - i else expanded + i
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(emoji, fontSize = 22.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (i in expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                    AnimatedVisibility(i in expanded) {
                        Text(
                            body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
