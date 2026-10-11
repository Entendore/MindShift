package com.example.mindshift.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object Backup {

    /** Writes the full journal (entries, moods, reminders) to [uri]. Returns entry count. */
    suspend fun export(context: Context, dao: WorksheetEntryDao, uri: Uri): Int {
        val entries = dao.entriesOnce()
        val moods = dao.moodsOnce()
        val reminders = dao.allReminders()
        val root = JSONObject().apply {
            put("app", "MindShift")
            put("exportedAt", System.currentTimeMillis())
            put("worksheetEntries", JSONArray().apply {
                entries.forEach { e ->
                    put(JSONObject().apply {
                        put("type", e.type)
                        put("title", e.title)
                        put("createdAt", e.createdAt)
                        put("content", JSONObject(e.contentJson))
                    })
                }
            })
            put("moods", JSONArray().apply {
                moods.forEach { m ->
                    put(JSONObject()
                        .put("mood", m.mood)
                        .put("tags", m.tags)
                        .put("createdAt", m.createdAt))
                }
            })
            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject()
                        .put("label", r.label)
                        .put("worksheetId", r.worksheetId)
                        .put("notifText", r.notifText)
                        .put("hour", r.hour)
                        .put("minute", r.minute)
                        .put("days", r.days)
                        .put("enabled", r.enabled))
                }
            })
        }
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(root.toString(2).toByteArray())
            } ?: throw IllegalStateException("Couldn't open output stream")
        }
        return entries.size
    }
}
