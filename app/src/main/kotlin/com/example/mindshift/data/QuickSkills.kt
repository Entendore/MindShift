package com.example.mindshift.data

import java.util.Calendar

data class QuickSkill(
    val id: String,
    val emoji: String,
    val title: String,
    val tagline: String,
    val steps: List<String>,
    val relatedWorksheetId: String? = null
)

object QuickSkills {
    val cards = listOf(
        QuickSkill("ice", "🧊", "Cold water reset", "Fast-acting body trick (TIPP: T)",
            listOf(
                "Hold a cold pack, ice, or a cold can against your face or eyes.",
                "Or splash cold water on your face.",
                "Stay with it 15–30 seconds. The dive reflex slows your heart rate down."
            )),
        QuickSkill("move", "🏃", "Burn it off", "Match the adrenaline (TIPP: I)",
            listOf(
                "60–90 seconds of intense movement: stairs, jumping jacks, fast walk, wall push-ups.",
                "Let your body spend the stress chemistry.",
                "Then slow down and take three long exhales."
            )),
        QuickSkill("exhale", "🌬️", "Long exhales", "In 4 · out 8 (TIPP: P)",
            listOf(
                "Breathe in through your nose for a count of 4.",
                "Out through your mouth for a count of 8 — like fogging a mirror.",
                "Repeat 10 times. The long exhale tells your body: safe enough."
            )),
        QuickSkill("tense", "💪", "Tense & release", "Pair muscles with breath (TIPP: P)",
            listOf(
                "Tense your hands, arms, or whole body for 5–10 seconds — not to pain.",
                "Release all at once, with a long exhale.",
                "Notice the difference. Repeat with another muscle group."
            )),
        QuickSkill("senses", "🌱", "5-4-3-2-1 senses", "Come back to the room",
            listOf(
                "Name 5 things you can see, slowly.",
                "4 things you can feel. 3 you can hear.",
                "2 you can smell. 1 you can taste.",
                "You're here. Right now, you're safe enough to keep going."
            ), relatedWorksheetId = "grounding_54321"),
        QuickSkill("wave", "🌊", "Ride the wave", "Urges crest, then fall",
            listOf(
                "Don't fight the urge, don't obey it. Watch it like weather.",
                "Where do you feel it in your body? Breathe into that place.",
                "Set a timer for 10 minutes. Waves usually crest and drop within it."
            ), relatedWorksheetId = "ride_the_wave"),
        QuickSkill("opposite", "🔄", "Opposite action check", "Act against the unhelpful urge",
            listOf(
                "Name the emotion and what it wants you to do (hide? snap? avoid?).",
                "Ask: does the emotion fit the facts, and is the intensity right?",
                "If not: pick the opposite action — and do it fully, body and mind."
            ), relatedWorksheetId = "opposite_action"),
        QuickSkill("soothe", "🕯️", "Soothe one sense", "Comfort, on purpose",
            listOf(
                "Pick one sense and give it something kind: warm tea, a soft blanket, music, a scent you love.",
                "Do it slowly, with full attention.",
                "Comfort isn't giving up — it's refueling."
            ), relatedWorksheetId = "self_soothe"),
        QuickSkill("reach", "💬", "Reach out", "Connection lowers the temperature",
            listOf(
                "Pick one safe person. Send a low-stakes message: \"hey, rough day, got a minute?\"",
                "If no one's available, a helpline counts: call or text 988 (US).",
                "Asking for help is a skill, not a weakness."
            )),
        QuickSkill("feet", "🦶", "Ground through the floor", "30-second anchor",
            listOf(
                "Press your feet into the floor. Feel the ground hold you.",
                "Name 3 sounds you can hear, near and far.",
                "Unclench your jaw. Drop your shoulders. One long breath out."
            ))
    )

    fun byId(id: String): QuickSkill? = cards.firstOrNull { it.id == id }

    fun skillOfTheDay(): QuickSkill {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return cards[day % cards.size]
    }
}
