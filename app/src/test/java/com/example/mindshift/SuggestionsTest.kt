package com.example.mindshift

import com.example.mindshift.data.MoodEntry
import com.example.mindshift.data.Suggestions
import org.junit.Assert.*
import org.junit.Test

class SuggestionsTest {

    @Test fun `distress leads with coping cards and includes the crisis tools`() {
        val routes = Suggestions.forMood(1, emptyList()).map { it.route }
        assertEquals(listOf("coping", "worksheet/tipp_plan", "worksheet/ride_the_wave"), routes)
    }

    @Test fun `distress always keeps coping cards even with two tag matches`() {
        val lows = listOf(
            MoodEntry(mood = 1, tags = "sleep"),
            MoodEntry(mood = 2, tags = "people"),
            MoodEntry(mood = 1, tags = "sleep")
        )
        val routes = Suggestions.forMood(1, lows).map { it.route }
        assertEquals(3, routes.size)
        assertTrue("coping must never be displaced in distress", "coping" in routes)
        assertEquals("worksheet/evening_wind_down", routes.first())
        assertTrue("worksheet/dear_man" in routes)
    }

    @Test fun `repeated low sleep tags lead with evening wind-down`() {
        val lows = List(3) { MoodEntry(mood = 1, tags = "sleep") }
        assertEquals("worksheet/evening_wind_down", Suggestions.forMood(1, lows).first().route)
    }

    @Test fun `people tags surface dear man in distress`() {
        val s = Suggestions.forMood(1, listOf(MoodEntry(mood = 2, tags = "people")))
        assertTrue(s.any { it.route == "worksheet/dear_man" })
    }

    @Test fun `neutral mood suggests activation tools`() {
        val routes = Suggestions.forMood(3, emptyList()).map { it.route }.toSet()
        assertEquals(
            setOf("breathing", "worksheet/thought_record", "worksheet/behavioral_activation"),
            routes
        )
    }

    @Test fun `work-tagged neutral days lead with the worry tree`() {
        val s = Suggestions.forMood(3, listOf(MoodEntry(mood = 2, tags = "work")))
        assertEquals("worksheet/worry_tree", s.first().route)
        assertEquals(3, s.size)
    }

    @Test fun `good moods suggest savoring`() {
        for (mood in listOf(4, 5)) {
            val routes = Suggestions.forMood(mood, emptyList()).map { it.route }.toSet()
            assertEquals(
                setOf("worksheet/gratitude", "worksheet/evening_wind_down", "timer"),
                routes
            )
        }
    }

    @Test fun `positive tags alone do not hijack distress suggestions`() {
        val highs = List(5) { MoodEntry(mood = 5, tags = "sleep") }
        val routes = Suggestions.forMood(1, highs).map { it.route }
        assertEquals(listOf("coping", "worksheet/tipp_plan", "worksheet/ride_the_wave"), routes)
    }
}
