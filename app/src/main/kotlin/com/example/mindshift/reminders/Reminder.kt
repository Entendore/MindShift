package com.example.mindshift.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.example.mindshift.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal const val CHANNEL_ID = "daily_reminder"
internal const val NOTIF_ID = 1001
private const val WORK_NAME = "daily_reminder"

fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Daily practice reminder", NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

class DailyReminderWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun snoozeAction(minutes: Int, code: Int): NotificationCompat.Action {
            val i = Intent(context, SnoozeReceiver::class.java).apply {
                putExtra(SnoozeReceiver.EXTRA_KIND, SnoozeReceiver.KIND_DAILY)
                putExtra(SnoozeReceiver.EXTRA_MINUTES, minutes)
            }
            val pi = PendingIntent.getBroadcast(
                context, code, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return NotificationCompat.Action(
                R.drawable.ic_notification,
                if (minutes == 60) "Snooze 1 hr" else "Snooze $minutes min",
                pi
            )
        }

        val snoozePref = SettingsRepository.get(applicationContext).snoozeMinutes.first()
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time for a mindful minute 🌿")
            .setContentText("A breathing exercise or worksheet is waiting for you.")
            .setAutoCancel(true)
            .setContentIntent(pending)
        if (snoozePref < 60) builder.addAction(snoozeAction(snoozePref, 9001))
        builder.addAction(snoozeAction(60, 9002))
        NotificationManagerCompat.from(context).notify(NOTIF_ID, builder.build())
        return Result.success()
    }
}

object ReminderScheduler {
    fun schedule(context: Context, hour: Int) {
        ensureChannel(context)
        val now = Calendar.getInstance()
        val trigger = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(trigger.timeInMillis - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_NAME)
        wm.cancelUniqueWork(DAILY_SNOOZE_WORK)
    }
}
