package com.example.mindshift.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "worksheet_entries")
data class WorksheetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val contentJson: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mood: Int,          // 1..5
    val tags: String = "",  // comma-separated MoodTags keys
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val worksheetId: String = "",   // blank = generic reminder
    val notifText: String = "",     // custom notification line (optional)
    val hour: Int,
    val minute: Int,
    val days: String = "",          // Calendar.DAY_OF_WEEK ints, comma-separated; blank = one-time
    val enabled: Boolean = true,
    val snoozedUntil: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface WorksheetEntryDao {
    @Insert suspend fun insert(entry: WorksheetEntry): Long
    @Update suspend fun update(entry: WorksheetEntry)
    @Delete suspend fun delete(entry: WorksheetEntry)
    @Query("SELECT * FROM worksheet_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<WorksheetEntry>>
    @Query("SELECT * FROM worksheet_entries WHERE id = :id")
    suspend fun byId(id: Long): WorksheetEntry?
    @Query("SELECT * FROM worksheet_entries")
    suspend fun entriesOnce(): List<WorksheetEntry>
    @Query("DELETE FROM worksheet_entries") suspend fun deleteAll()

    @Insert suspend fun insertMood(mood: MoodEntry): Long
    @Query("UPDATE mood_entries SET tags = :tags WHERE id = :id")
    suspend fun updateMoodTags(id: Long, tags: String)
    @Query("SELECT * FROM mood_entries ORDER BY createdAt DESC LIMIT 60")
    fun observeMoods(): Flow<List<MoodEntry>>
    @Query("SELECT * FROM mood_entries") suspend fun moodsOnce(): List<MoodEntry>
    @Query("DELETE FROM mood_entries") suspend fun deleteAllMoods()

    @Query("SELECT * FROM worksheet_entries WHERE type = :type ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestOfType(type: String): WorksheetEntry?
    @Query("SELECT * FROM worksheet_entries WHERE type = :type ORDER BY createdAt DESC")
    fun observeByType(type: String): Flow<List<WorksheetEntry>>

    // Pagination guard: the journal UI lists a bounded recent slice
    @Query("SELECT * FROM worksheet_entries ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<WorksheetEntry>>
    @Query("SELECT COUNT(*) FROM worksheet_entries")
    suspend fun countEntries(): Int
    // DB-backed search keeps large journals off the heap
    @Query("SELECT * FROM worksheet_entries WHERE title LIKE '%' || :q || '%' " +
        "OR contentJson LIKE '%' || :q || '%' ORDER BY createdAt DESC LIMIT 50")
    fun searchEntries(q: String): Flow<List<WorksheetEntry>>

    // Reminders
    @Insert suspend fun insertReminder(r: ReminderItem): Long
    @Update suspend fun updateReminder(r: ReminderItem)
    @Delete suspend fun deleteReminder(r: ReminderItem)
    @Query("SELECT * FROM reminders ORDER BY hour, minute")
    fun observeReminders(): Flow<List<ReminderItem>>
    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun reminderById(id: Long): ReminderItem?
    @Query("SELECT * FROM reminders WHERE enabled = 1")
    suspend fun enabledReminders(): List<ReminderItem>
    @Query("SELECT * FROM reminders WHERE worksheetId = :worksheetId ORDER BY hour, minute")
    fun observeForWorksheet(worksheetId: String): Flow<List<ReminderItem>>
    @Query("SELECT * FROM reminders")
    suspend fun allReminders(): List<ReminderItem>
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `mood_entries` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`mood` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE mood_entries ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `reminders` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`label` TEXT NOT NULL, `worksheetId` TEXT NOT NULL, " +
                "`hour` INTEGER NOT NULL, `minute` INTEGER NOT NULL, " +
                "`days` TEXT NOT NULL, `enabled` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE reminders ADD COLUMN notifText TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE reminders ADD COLUMN snoozedUntil INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [WorksheetEntry::class, MoodEntry::class, ReminderItem::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): WorksheetEntryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mindshift.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build().also { INSTANCE = it }
            }
    }
}

object MoodTags {
    val all = listOf(
        "sleep" to "😴 Sleep",
        "work" to "🧰 Work",
        "people" to "👥 People",
        "health" to "🩺 Health",
        "money" to "💸 Money",
        "good" to "✨ Good day"
    )
}
