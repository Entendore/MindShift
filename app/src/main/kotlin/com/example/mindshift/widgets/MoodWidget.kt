package com.example.mindshift.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.mindshift.data.AppDatabase
import com.example.mindshift.data.MoodEntry
import com.example.mindshift.screens.dayStamp

class MoodWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = AppDatabase.get(context).entryDao()
        val today = dayStamp()
        val checkedToday = dao.moodsOnce().firstOrNull { dayStamp(it.createdAt) == today }
        provideContent { MoodContent(checkedToday) }
    }

    @Composable
    private fun MoodContent(checkedToday: MoodEntry?) {
        GlanceTheme {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.widgetBackground)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "How are you?",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp)
                )
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    listOf("😞", "😟", "😐", "🙂", "😄").forEachIndexed { i, emoji ->
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .padding(2.dp)
                                .background(
                                    if (checkedToday?.mood == i + 1)
                                        GlanceTheme.colors.secondaryContainer
                                    else GlanceTheme.colors.widgetBackground
                                )
                                .clickable(
                                    actionRunCallback<LogMoodAction>(
                                        actionParametersOf(LogMoodAction.moodKey to (i + 1))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, style = TextStyle(fontSize = 24.sp))
                        }
                    }
                }
                if (checkedToday != null) {
                    Text(
                        "Checked in ✓",
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

class LogMoodAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[moodKey] ?: 3
        AppDatabase.get(context).entryDao().insertMood(MoodEntry(mood = mood))
        MoodWidget().update(context, glanceId)
    }

    companion object {
        val moodKey = ActionParameters.Key<Int>("mood")
    }
}

class MoodWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MoodWidget()
}
