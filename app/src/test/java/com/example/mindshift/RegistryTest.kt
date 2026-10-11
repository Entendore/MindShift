package com.example.mindshift

import com.example.mindshift.data.Glossary
import com.example.mindshift.data.QuickSkills
import com.example.mindshift.data.Worksheets
import org.junit.Assert.*
import org.junit.Test

class RegistryTest {

    @Test fun `worksheet ids are unique`() {
        val ids = Worksheets.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `every worksheet has an emoji, subtitle, and at least one field`() {
        Worksheets.all.forEach { def ->
            assertTrue("${def.id} emoji", def.emoji.isNotBlank())
            assertTrue("${def.id} subtitle", def.subtitle.isNotBlank())
            assertTrue("${def.id} fields", def.fields.isNotEmpty())
        }
    }

    @Test fun `field ids are unique within a worksheet`() {
        Worksheets.all.forEach { def ->
            val ids = def.fields.map { it.id }
            assertEquals("${def.id} field ids", ids.size, ids.toSet().size)
        }
    }

    @Test fun `category lists tile the registry exactly`() {
        val fromLists = (Worksheets.cbt + Worksheets.dbt + Worksheets.mindfulness + Worksheets.special)
            .map { it.id }.toSet()
        assertEquals(Worksheets.all.map { it.id }.toSet(), fromLists)
    }

    @Test fun `byId round-trips for every worksheet`() {
        Worksheets.all.forEach { def -> assertEquals(def.id, Worksheets.byId(def.id)!!.id) }
        assertNull(Worksheets.byId("does_not_exist"))
    }

    @Test fun `safety plan lives in special, not the tabs`() {
        val tabs = (Worksheets.cbt + Worksheets.dbt + Worksheets.mindfulness).map { it.id }
        assertTrue("safety_plan" !in tabs)
        assertTrue("safety_plan" in Worksheets.special.map { it.id })
    }

    @Test fun `glossary term ids are unique`() {
        val ids = Glossary.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test fun `every seeAlso reference resolves`() {
        Glossary.all.forEach { term ->
            term.seeAlso.forEach { ref ->
                assertNotNull("${term.id} -> $ref", Glossary.byId(ref))
            }
        }
    }

    @Test fun `every glossary route resolves to a real destination`() {
        val nonWorksheetRoutes = setOf("diary", "breathing", "timer", "coping", "ladder", "leaves")
        Glossary.all.forEach { term ->
            val route = term.route ?: return@forEach
            if (route.startsWith("worksheet/")) {
                assertNotNull("${term.id} route $route", Worksheets.byId(route.removePrefix("worksheet/")))
            } else {
                assertTrue("${term.id} route $route", route in nonWorksheetRoutes)
            }
        }
    }

    @Test fun `search is case-insensitive across term and definition`() {
        assertTrue(Glossary.search("tipp").any { it.id == "tipp" })
        assertTrue(Glossary.search("urge").any { it.id == "ride_the_wave" })
        assertTrue(Glossary.search("").isEmpty())
        assertTrue(Glossary.search("zzzznothing").isEmpty())
    }

    @Test fun `skill of the day is always one of the cards`() {
        assertTrue(QuickSkills.cards.contains(QuickSkills.skillOfTheDay()))
    }

    @Test fun `related worksheets on coping cards exist`() {
        QuickSkills.cards.mapNotNull { it.relatedWorksheetId }.forEach {
            assertNotNull("related worksheet $it", Worksheets.byId(it))
        }
    }

    @Test fun `coping card ids are unique`() {
        val ids = QuickSkills.cards.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }
}
