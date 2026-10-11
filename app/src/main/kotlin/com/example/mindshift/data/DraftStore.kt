package com.example.mindshift.data

/** In-memory drafts — they survive navigation, not process death. */
object DraftStore {
    private val drafts = HashMap<String, HashMap<String, String>>()

    fun get(worksheetId: String): Map<String, String>? = drafts[worksheetId]
    fun put(worksheetId: String, fieldId: String, value: String) {
        drafts.getOrPut(worksheetId) { HashMap() }[fieldId] = value
    }
    fun clear(worksheetId: String) { drafts.remove(worksheetId) }
}
