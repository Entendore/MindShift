package com.example.mindshift.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.mindshift.MainActivity
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.reminders.ItemReminderScheduler
import kotlinx.coroutines.flow.first

class NextUpWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.get(context).entryDao()
        val next = dao.observeReminders().first()
            .filter { it.enabled }
            .minByOrNull { ItemReminderScheduler.effectiveNextMillis(it) }
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(12.dp)
                        .clickable(
                            actionStartActivity(
                                Intent(context, MainActivity::class.java).apply {
                                    putExtra("navigate_to", "reminders")
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (next == null) {
                        Text(
                            "No practice reminders yet",
                            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp)
                        )
                    } else {
                        Column {
                            Text(
                                "⏰  ${next.label}",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1
                            )
                            Text(
                                ItemReminderScheduler.nextFireLabel(next),
                                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 12.sp)
                            )
                        }
                    }
                }
            }
        }
    }
}

class NextUpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = NextUpWidget()
}
