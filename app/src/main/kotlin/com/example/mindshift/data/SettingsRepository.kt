package com.example.mindshift.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mindshift.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class TextSize(val multiplier: Float, val label: String) {
    SMALL(0.9f, "Small"),
    NORMAL(1f, "Default"),
    LARGE(1.15f, "Large"),
    XLARGE(1.3f, "Extra large")
}

class SettingsRepository private constructor(private val context: Context) {

    private val THEME = stringPreferencesKey("theme")
    private val REM_ENABLED = booleanPreferencesKey("reminder_enabled")
    private val REM_HOUR = intPreferencesKey("reminder_hour")
    private val FAVORITES = stringSetPreferencesKey("favorites")
    private val LOCK = booleanPreferencesKey("app_lock")
    private val ONBOARDED = booleanPreferencesKey("onboarded")
    private val TEXT_SIZE = intPreferencesKey("text_size")
    private val LAST_EXPORT = longPreferencesKey("last_export_at")
    private val BACKUP_DISMISS = longPreferencesKey("backup_dismissed_at")
    private val SNOOZE = intPreferencesKey("snooze_minutes")

    val theme: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[REM_ENABLED] ?: false }
    val reminderHour: Flow<Int> = context.dataStore.data.map { it[REM_HOUR] ?: 20 }
    val favorites: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITES] ?: emptySet() }
    val lockEnabled: Flow<Boolean> = context.dataStore.data.map { it[LOCK] ?: false }
    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDED] ?: false }
    val textSize: Flow<TextSize> = context.dataStore.data.map { prefs ->
        TextSize.entries.getOrNull(prefs[TEXT_SIZE] ?: 1) ?: TextSize.NORMAL
    }
    val lastExportAt: Flow<Long> = context.dataStore.data.map { it[LAST_EXPORT] ?: 0L }
    val backupDismissedAt: Flow<Long> = context.dataStore.data.map { it[BACKUP_DISMISS] ?: 0L }
    val snoozeMinutes: Flow<Int> = context.dataStore.data.map { it[SNOOZE] ?: 10 }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[THEME] = mode.name }
    suspend fun setReminder(enabled: Boolean, hour: Int) = context.dataStore.edit {
        it[REM_ENABLED] = enabled
        it[REM_HOUR] = hour
    }
    suspend fun toggleFavorite(id: String) = context.dataStore.edit { prefs ->
        val current = prefs[FAVORITES] ?: emptySet()
        prefs[FAVORITES] = if (id in current) current - id else current + id
    }
    suspend fun setLock(enabled: Boolean) = context.dataStore.edit { it[LOCK] = enabled }
    suspend fun setOnboarded(done: Boolean) = context.dataStore.edit { it[ONBOARDED] = done }
    suspend fun setTextSize(size: TextSize) = context.dataStore.edit { it[TEXT_SIZE] = size.ordinal }
    suspend fun setLastExportAt(millis: Long) = context.dataStore.edit { it[LAST_EXPORT] = millis }
    suspend fun dismissBackupNudge(millis: Long) = context.dataStore.edit { it[BACKUP_DISMISS] = millis }
    suspend fun setSnoozeMinutes(min: Int) = context.dataStore.edit { it[SNOOZE] = min }

    companion object {
        @Volatile private var instance: SettingsRepository? = null
        fun get(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
