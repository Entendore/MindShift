package com.example.mindshift

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindshift.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: WorksheetEntryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.entryDao()
    }

    @Test fun insertOrdersNewestFirst() = runBlocking {
        dao.insert(WorksheetEntry(type = "a", title = "A", contentJson = "{}", createdAt = 1000))
        dao.insert(WorksheetEntry(type = "b", title = "B", contentJson = "{}", createdAt = 2000))
        assertEquals(listOf("B", "A"), dao.observeAll().first().map { it.title })
    }

    @Test fun updateChangesContent() = runBlocking {
        val id = dao.insert(WorksheetEntry(type = "t", title = "T", contentJson = "{}"))
        dao.update(dao.byId(id)!!.copy(contentJson = """{"k":"v"}"""))
        assertEquals("v", JSONObject(dao.byId(id)!!.contentJson).getString("k"))
    }

    @Test fun latestOfTypeReturnsNewest() = runBlocking {
        dao.insert(WorksheetEntry(type = "safety_plan", title = "old", contentJson = "{}", createdAt = 1000))
        dao.insert(WorksheetEntry(type = "safety_plan", title = "new", contentJson = "{}", createdAt = 2000))
        dao.insert(WorksheetEntry(type = "gratitude", title = "g", contentJson = "{}", createdAt = 3000))
        assertEquals("new", dao.latestOfType("safety_plan")!!.title)
    }

    @Test fun observeRecentLimitsAndOrders() = runBlocking {
        for (i in 1..5) {
            dao.insert(WorksheetEntry(type = "t", title = "E$i", contentJson = "{}", createdAt = i * 1000L))
        }
        assertEquals(listOf("E5", "E4", "E3"), dao.observeRecent(3).first().map { it.title })
    }

    @Test fun searchMatchesTitleAndContentCaseInsensitively() = runBlocking {
        dao.insert(WorksheetEntry(type = "t", title = "Call Mom", contentJson = "{}"))
        dao.insert(WorksheetEntry(type = "t", title = "Note", contentJson = """{"text":"deadline panic"}"""))
        assertEquals(1, dao.searchEntries("call").first().size)
        assertEquals(1, dao.searchEntries("DEADLINE").first().size)
        assertEquals(0, dao.searchEntries("zzz").first().size)
    }

    @Test fun countEntriesMatchesReality() = runBlocking {
        assertEquals(0, dao.countEntries())
        repeat(3) { dao.insert(WorksheetEntry(type = "t", title = "x", contentJson = "{}")) }
        assertEquals(3, dao.countEntries())
    }

    @Test fun moodTagsRoundTrip() = runBlocking {
        val id = dao.insertMood(MoodEntry(mood = 2))
        dao.updateMoodTags(id, "sleep,work")
        val m = dao.moodsOnce().single()
        assertEquals(2, m.mood)
        assertEquals("sleep,work", m.tags)
    }

    @Test fun reminderCrudAndFilters() = runBlocking {
        val a = dao.insertReminder(ReminderItem(label = "Mon", worksheetId = "dear_man", hour = 9, minute = 0, days = "2"))
        dao.insertReminder(ReminderItem(label = "Off", worksheetId = "dear_man", hour = 20, minute = 30, enabled = false))
        dao.insertReminder(ReminderItem(label = "One", hour = 7, minute = 15))

        assertEquals("Mon", dao.reminderById(a)!!.label)
        assertEquals(listOf("Mon"), dao.enabledReminders().map { it.label })
        assertEquals(2, dao.observeForWorksheet("dear_man").first().size)

        dao.updateReminder(dao.reminderById(a)!!.copy(enabled = false))
        assertTrue(dao.enabledReminders().none { it.label == "Mon" })

        dao.deleteReminder(dao.reminderById(a)!!)
        assertEquals(2, dao.allReminders().size)
    }

    @Test fun deleteAllClearsBothTables() = runBlocking {
        dao.insert(WorksheetEntry(type = "t", title = "x", contentJson = "{}"))
        dao.insertMood(MoodEntry(mood = 3))
        dao.deleteAll()
        dao.deleteAllMoods()
        assertEquals(0, dao.countEntries())
        assertTrue(dao.moodsOnce().isEmpty())
    }
}
