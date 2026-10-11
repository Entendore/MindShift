package com.example.mindshift.data

data class ToolSuggestion(
    val emoji: String,
    val title: String,
    val why: String,
    val route: String
)

object Suggestions {

    fun forMood(mood: Int, recentMoods: List<MoodEntry>): List<ToolSuggestion> = when {
        mood <= 2 -> {
            val tagBased = fromTags(recentMoods)
            val defaults = listOf(
                ToolSuggestion("🆘", "Coping cards", "Right-now skills — no typing needed", "coping"),
                ToolSuggestion("🧊", "TIPP — crisis reset", "Bring the body down first", "worksheet/tipp_plan"),
                ToolSuggestion("🌊", "Ride the Wave", "Don't fight it, don't feed it", "worksheet/ride_the_wave")
            )
            // Tag-based suggestions lead; the fill guarantees coping cards are
            // always among the three during distress.
            (tagBased + defaults).distinctBy { it.route }.take(3)
        }
        mood == 3 -> {
            val defaults = listOf(
                ToolSuggestion("🌬️", "Breathing exercises", "A few slow minutes resets the baseline", "breathing"),
                ToolSuggestion("📝", "Thought record", "What's the story under the 'meh'?", "worksheet/thought_record"),
                ToolSuggestion("🌤️", "Action before motivation", "One tiny step beats waiting to feel like it", "worksheet/behavioral_activation")
            )
            val worried = recentMoods.take(6).any {
                it.mood <= 3 && ("work" in it.tags.split(',') || "money" in it.tags.split(','))
            }
            if (worried) listOf(
                ToolSuggestion("🌳", "The Worry Tree", "Sort it: fix it, schedule it, or set it down", "worksheet/worry_tree")
            ) + defaults.take(2)
            else defaults
        }
        else -> listOf(
            ToolSuggestion("🙏", "Gratitude journal", "Lock in what went right", "worksheet/gratitude"),
            ToolSuggestion("🌙", "Evening wind-down", "Savor it while it's here", "worksheet/evening_wind_down"),
            ToolSuggestion("⏱️", "Meditation timer", "Good days are the easy days to practice", "timer")
        )
    }

    /** If a context tag keeps showing up on low check-ins, lead with the tool for it. */
    private fun fromTags(recentMoods: List<MoodEntry>): List<ToolSuggestion> {
        val low = recentMoods.filter { it.mood <= 2 }.take(6)
        val tags = low.flatMap { it.tags.split(',').map(String::trim) }.toSet()
        val out = mutableListOf<ToolSuggestion>()
        if ("sleep" in tags) {
            out += ToolSuggestion("🌙", "Evening wind-down",
                "Sleep keeps showing up on your hard days", "worksheet/evening_wind_down")
        }
        if ("people" in tags) {
            out += ToolSuggestion("💬", "DEAR MAN",
                "People keep coming up — something to ask or say?", "worksheet/dear_man")
        }
        return out
    }
}
