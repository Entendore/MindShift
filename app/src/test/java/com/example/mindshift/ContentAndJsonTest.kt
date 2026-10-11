package com.example.mindshift

import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.screens.LadderJson
import com.example.mindshift.screens.LadderStep
import com.example.mindshift.screens.parseContent
import com.example.mindshift.util.PdfExporter
import org.junit.Assert.*
import org.junit.Test

class ContentAndJsonTest {

    private fun entry(type: String, json: String) =
        WorksheetEntry(type = type, title = "t", contentJson = json)

    @Test fun `worksheet fields map to labels and skip blanks`() {
        val rows = parseContent(
            entry("thought_record", """{"situation":"Met Ana","emotions":"","balanced":"ok"}""")
        )
        assertEquals(
            listOf("1. The situation" to "Met Ana", "6. Balanced thought" to "ok"),
            rows
        )
    }

    @Test fun `diary card renders ratings, skills, and note`() {
        val rows = parseContent(
            entry("diary_card", """{"anger":3,"distress":7,"skills":["TIPP","GIVE"],"note":"rough pm"}""")
        )
        assertTrue("😠 Anger" to "3 / 5" in rows)
        assertTrue("😰 Distress" to "7 / 10" in rows)
        assertTrue("🛠 Skills used" to "TIPP, GIVE" in rows)
        assertTrue("📝 Note" to "rough pm" in rows)
    }

    @Test fun `coping card shows its name`() {
        val rows = parseContent(entry("coping_card", """{"card":"ice","name":"Cold water reset"}"""))
        assertEquals(listOf("🆘 Coping card" to "Cold water reset"), rows)
    }

    @Test fun `exposure ladder shows fear and progress`() {
        val rows = parseContent(
            entry("exposure_ladder", """{"fear":"Calls","steps":[{"text":"a","done":true},{"text":"b","done":false}]}""")
        )
        assertTrue("🎯 Fear" to "Calls" in rows)
        assertTrue("🪜 Progress" to "1 of 2 steps taken" in rows)
    }

    @Test fun `exposure step shows the suds rating`() {
        val rows = parseContent(
            entry("exposure_step", """{"fear":"Calls","step":"a","suds":30}""")
        )
        assertTrue("😰 Fear felt" to "30 / 100" in rows)
    }

    @Test fun `unknown types fall back to uppercased keys`() {
        val rows = parseContent(entry("check_in", """{"mood":"5"}"""))
        assertEquals(listOf("Mood" to "5"), rows)
    }

    @Test fun `malformed json renders as nothing instead of crashing`() {
        assertTrue(parseContent(entry("x", "{not json")).isEmpty())
    }

    @Test fun `ladder round-trips through json`() {
        val steps = listOf(LadderStep("Say hi", true, 40), LadderStep("Make the call"))
        val json = LadderJson.toJson("Phone calls", steps)
        val parsed = LadderJson.parse(WorksheetEntry(contentJson = json))!!
        assertEquals("Phone calls", parsed.first)
        assertEquals(steps, parsed.second)
    }

    @Test fun `ladder parse survives garbage`() {
        assertNull(LadderJson.parse(WorksheetEntry(contentJson = "{oops")))
    }

    @Test fun `strip removes emoji but keeps words`() {
        assertEquals("Coping card", PdfExporter.strip("🆘 Coping card"))
        assertEquals("Ride the Wave", PdfExporter.strip("🌊 Ride the Wave"))
        assertEquals("ok", PdfExporter.strip("  ok  "))
    }
}
