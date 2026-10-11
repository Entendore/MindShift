package com.example.mindshift.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.mindshift.MainActivity
import com.example.mindshift.R
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.ReminderItem
import com.example.mindshift.data.SettingsRepository
import com.example.mindshift.data.WorksheetEntry
import com.example.mindshift.data.Worksheets
import com.example.mindshift.screens.AppClock
import com.example.mindshift.screens.addDays
import com.example.mindshift.screens.dayStamp
import com.example.mindshift.screens.todayStamp
import com.example.mindshift.widgets.NextUpWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "practice_reminders"
private const val NOTIF_BASE = 2000

internal fun itemSnoozeWorkName(id: Long) = "item_reminder_snooze_$id"
internal const val DAILY_SNOOZE_WORK = "daily_reminder_snooze"

fun ensureItemChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Practice reminders", NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

object ItemReminderScheduler {

    private fun workName(id: Long) = "item_reminder_$id"

    /** Next *scheduled* occurrence of this reminder at/after `now` (ignores snoozes). */
    fun nextFire(
        reminder: ReminderItem,
        now: Calendar = Calendar.getInstance(TimeZone.getTimeZone(AppClock.zone()))
    ): Calendar {
        val days = reminder.days.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        var candidate = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!candidate.after(now)) candidate.add(Calendar.DAY_OF_YEAR, 1)
        if (days.isNotEmpty()) {
            while (candidate.get(Calendar.DAY_OF_WEEK) !in days) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return candidate
    }

    /** What actually fires next — a pending snooze if there is one, else the schedule. */
    fun effectiveNextMillis(reminder: ReminderItem): Long =
        if (reminder.snoozedUntil > System.currentTimeMillis()) reminder.snoozedUntil
        else nextFire(reminder).timeInMillis

    fun schedule(context: Context, reminder: ReminderItem) {
        val delayMs = nextFire(reminder).timeInMillis - System.currentTimeMillis()
        val request = OneTimeWorkRequestBuilder<ItemReminderWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to reminder.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            workName(reminder.id), ExistingWorkPolicy.REPLACE, request
        )
    }

    fun cancel(context: Context, id: Long) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(workName(id))
        wm.cancelUniqueWork(itemSnoozeWorkName(id))
    }

    fun scheduleAll(context: Context, reminders: List<ReminderItem>) {
        reminders.filter { it.enabled }.forEach { schedule(context, it) }
    }

    // ---- Display helpers ----

    fun timeLabel(hour: Int, minute: Int): String {
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        return String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, if (hour < 12) "AM" else "PM")
    }

    fun daysLabel(days: String): String {
        val set = days.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
        if (set.isEmpty()) return "One time"
        if (set.size == 7) return "Every day"
        if (set == setOf(2, 3, 4, 5, 6)) return "Weekdays"
        if (set == setOf(1, 7)) return "Weekends"
        val names = mapOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat")
        return set.sorted().mapNotNull { names[it] }.joinToString(", ")
    }

    fun nextFireLabel(reminder: ReminderItem): String {
        if (reminder.snoozedUntil > System.currentTimeMillis()) {
            val t = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(reminder.snoozedUntil))
            return "Snoozed · $t"
        }
        val next = nextFire(reminder)
        val time = timeLabel(next.get(Calendar.HOUR_OF_DAY), next.get(Calendar.MINUTE))
        val day = dayStamp(next.timeInMillis)
        val today = todayStamp()
        val prefix = when (day) {
            today -> "Today"
            addDays(today, 1) -> "Tomorrow"
            else -> SimpleDateFormat("EEE", Locale.getDefault()).format(next.time)
        }
        return "$prefix · $time"
    }
}

class ItemReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val id = inputData.getLong("id", -1L)
        if (id == -1L) return Result.success()
        val isSnooze = inputData.getBoolean("isSnooze", false)

        val dao = AppDatabase.get(context).entryDao()
        var reminder = dao.reminderById(id) ?: return Result.success()

        // A regular fire for a reminder the user has since disabled: stand down.
        // (Snoozed fires skip this — a retired one-time reminder may legitimately snooze.)
        if (!isSnooze && !reminder.enabled) return Result.success()

        // Clear snooze state now that we're firing.
        if (reminder.snoozedUntil != 0L) {
            reminder = reminder.copy(snoozedUntil = 0)
            dao.updateReminder(reminder)
            NextUpWidget().updateAll(context)
        }

        val canNotify = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (canNotify) {
            ensureItemChannel(context)
            val open = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                if (reminder.worksheetId.isNotBlank()) {
                    putExtra("navigate_to", "worksheet/${reminder.worksheetId}")
                }
            }
            val contentIntent = PendingIntent.getActivity(
                context, (NOTIF_BASE + id).toInt(), open,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val body = reminder.notifText.ifBlank {
                Worksheets.byId(reminder.worksheetId)?.let { "${it.emoji} ${it.title}" }
                    ?: "Time for your practice"
            }

            val snoozePref = SettingsRepository.get(context).snoozeMinutes.first()
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(reminder.label)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .addAction(R.drawable.ic_notification, "Done today", doneIntent(context, id))
            if (snoozePref < 60) {
                builder.addAction(
                    R.drawable.ic_notification, "Snooze $snoozePref min",
                    snoozeIntent(context, id, snoozePref)
                )
            }
            builder.addAction(R.drawable.ic_notification, "Snooze 1 hr", snoozeIntent(context, id, 60))
            NotificationManagerCompat.from(context).notify((NOTIF_BASE + id).toInt(), builder.build())
        }

        // Regular fires keep the cadence going (or retire one-time reminders).
        // Snoozed fires do neither — the schedule is already pending.
        if (!isSnooze) {
            if (reminder.days.isNotBlank()) {
                ItemReminderScheduler.schedule(context, reminder)
            } else {
                dao.updateReminder(reminder.copy(enabled = false))
            }
        }
        return Result.success()
    }

    private fun snoozeIntent(context: Context, id: Long, minutes: Int): PendingIntent {
        val intent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(SnoozeReceiver.EXTRA_KIND, SnoozeReceiver.KIND_ITEM)
            putExtra(SnoozeReceiver.EXTRA_ID, id)
            putExtra(SnoozeReceiver.EXTRA_MINUTES, minutes)
        }
        val code = ((NOTIF_BASE + id) * 10L + minutes).toInt()
        return PendingIntent.getBroadcast(
            context, code, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun doneIntent(context: Context, id: Long): PendingIntent {
        val intent = Intent(context, DoneReceiver::class.java).apply {
            putExtra(SnoozeReceiver.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, ((NOTIF_BASE + id) * 10L + 9).toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

/** Handles snooze actions for both notification flavors. */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 10)
        val kind = intent.getStringExtra(EXTRA_KIND) ?: KIND_ITEM
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (kind) {
                    KIND_ITEM -> snoozeItem(context, intent.getLongExtra(EXTRA_ID, -1L), minutes)
                    else -> snoozeDaily(context, minutes)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun snoozeItem(context: Context, id: Long, minutes: Int) {
        if (id == -1L) return
        val dao = AppDatabase.get(context).entryDao()
        NotificationManagerCompat.from(context).cancel((NOTIF_BASE + id).toInt())
        dao.reminderById(id)?.let {
            dao.updateReminder(
                it.copy(snoozedUntil = System.currentTimeMillis() + minutes * 60_000L)
            )
        }
        val request = OneTimeWorkRequestBuilder<ItemReminderWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf("id" to id, "isSnooze" to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            itemSnoozeWorkName(id), ExistingWorkPolicy.REPLACE, request
        )
        NextUpWidget().updateAll(context)
    }

    private suspend fun snoozeDaily(context: Context, minutes: Int) {
        NotificationManagerCompat.from(context).cancel(NOTIF_ID)
        val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_SNOOZE_WORK, ExistingWorkPolicy.REPLACE, request
        )
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
        const val EXTRA_MINUTES = "minutes"
        const val KIND_ITEM = "item"
        const val KIND_DAILY = "daily"
    }
}

/** "Done today" action: logs the practice and dismisses — no app open required. */
class DoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(SnoozeReceiver.EXTRA_ID, -1L)
        if (id == -1L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).entryDao()
                val reminder = dao.reminderById(id) ?: return@launch
                val tool = Worksheets.byId(reminder.worksheetId)?.title
                dao.insert(
                    WorksheetEntry(
                        type = "reminder_done",
                        title = "Done — ${tool ?: reminder.label}",
                        contentJson = JSONObject().apply {
                            put("tool", tool ?: reminder.label)
                            put("note", "Marked done from the reminder notification")
                        }.toString()
                    )
                )
                NotificationManagerCompat.from(context).cancel((NOTIF_BASE + id).toInt())
                NextUpWidget().updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
