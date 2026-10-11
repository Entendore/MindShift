package com.example.mindshift

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindshift.data.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val V1_ENTRIES = "CREATE TABLE IF NOT EXISTS `worksheet_entries` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, " +
        "`title` TEXT NOT NULL, `contentJson` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
    private val V2_MOODS = "CREATE TABLE IF NOT EXISTS `mood_entries` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `mood` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
    private val V4_REMINDERS = "CREATE TABLE IF NOT EXISTS `reminders` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `label` TEXT NOT NULL, " +
        "`worksheetId` TEXT NOT NULL, `hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, " +
        "`days` TEXT NOT NULL, `enabled` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"

    private fun rawDb(block: (SupportSQLiteDatabase) -> Unit) {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {}
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        val db = helper.writableDatabase
        block(db)
        db.close()
    }

    @After fun cleanUp() { context.deleteDatabase(dbName) }

    @Test fun `v2 moods gain an empty tags column`() {
        context.deleteDatabase(dbName)
        rawDb {
            it.execSQL(V2_MOODS)
            it.execSQL("INSERT INTO mood_entries (mood, createdAt) VALUES (3, 1000)")
        }
        rawDb {
            MIGRATION_2_3.migrate(it)
            it.query("SELECT mood, tags FROM mood_entries").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals(3, c.getInt(0))
                assertEquals("", c.getString(1))
            }
        }
    }

    @Test fun `v4 reminders keep their data through v5`() {
        context.deleteDatabase(dbName)
        rawDb {
            it.execSQL(V4_REMINDERS)
            it.execSQL(
                "INSERT INTO reminders (label, worksheetId, hour, minute, days, enabled, createdAt) " +
                    "VALUES ('DEAR MAN', 'dear_man', 18, 0, '2', 1, 1000)"
            )
        }
        rawDb {
            MIGRATION_4_5.migrate(it)
            it.query("SELECT label, notifText, snoozedUntil FROM reminders").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("DEAR MAN", c.getString(0))
                assertEquals("", c.getString(1))
                assertEquals(0L, c.getLong(2))
            }
        }
    }

    @Test fun `full chain migrates v1 data and Room accepts the final schema`() = runBlocking {
        context.deleteDatabase(dbName)
        rawDb {
            it.execSQL(V1_ENTRIES)
            it.execSQL(
                "INSERT INTO worksheet_entries (type, title, contentJson, createdAt) " +
                    "VALUES ('thought_record','Thought Record','{\"situation\":\"pre-migration\"}', 1000)"
            )
        }
        rawDb {
            MIGRATION_1_2.migrate(it)
            MIGRATION_2_3.migrate(it)
            MIGRATION_3_4.migrate(it)
            MIGRATION_4_5.migrate(it)
            it.version = 5
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = room.entryDao()
            val old = dao.entriesOnce().single()
            assertEquals("pre-migration", JSONObject(old.contentJson).optString("situation"))

            dao.insertMood(MoodEntry(mood = 5, tags = "good"))
            assertEquals("good", dao.moodsOnce().single().tags)

            dao.insertReminder(ReminderItem(label = "kept", hour = 8, minute = 0, days = "1"))
            assertEquals("kept", dao.enabledReminders().single().label)
        } finally {
            room.close()
        }
    }
}
