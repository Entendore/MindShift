package com.example.mindshift.screens

import com.example.mindshift.data.WorksheetEntry
import org.json.JSONArray
import org.json.JSONObject

data class LadderStep(val text: String, val done: Boolean = false, val suds: Int = 0)

internal object LadderJson {
    fun parse(entry: WorksheetEntry): Pair<String, List<LadderStep>>? = runCatching {
        val o = JSONObject(entry.contentJson)
        val steps = mutableListOf<LadderStep>()
        o.optJSONArray("steps")?.let { arr ->
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                steps += LadderStep(s.optString("text"), s.optBoolean("done"), s.optInt("suds"))
            }
        }
        o.optString("fear") to steps
    }.getOrNull()

    fun toJson(fear: String, steps: List<LadderStep>): String {
        val arr = JSONArray()
        steps.forEach { s ->
            arr.put(JSONObject().put("text", s.text).put("done", s.done).put("suds", s.suds))
        }
        return JSONObject().put("fear", fear).put("steps", arr).toString()
    }
}
