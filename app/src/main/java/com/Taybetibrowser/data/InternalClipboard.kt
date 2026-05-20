package com.Taybetibrowser.data

import android.content.Context
import android.content.SharedPreferences

class InternalClipboard(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("internal_clipboard", Context.MODE_PRIVATE)

    private var clipboardData = mutableListOf<ClipboardEntry>()

    companion object {
        private const val CLIPBOARD_ENTRIES = "clipboard_entries"
        private const val MAX_ENTRIES = 50
    }

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val data = prefs.getString(CLIPBOARD_ENTRIES, null)
        if (data != null) {
            try {
                val parts = data.split("||DELIM||")
                clipboardData = parts.filter { it.isNotEmpty() }.mapNotNull { decodeEntry(it) }.toMutableList()
            } catch (e: Exception) {
                clipboardData = mutableListOf()
            }
        }
    }

    private fun saveToPrefs() {
        val encoded = clipboardData.joinToString("||DELIM||") { encodeEntry(it) }
        prefs.edit().putString(CLIPBOARD_ENTRIES, encoded).apply()
    }

    private fun encodeEntry(entry: ClipboardEntry): String {
        return "${entry.text.hashCode()}|${entry.timestamp}|${android.util.Base64.encodeToString(entry.text.toByteArray(), android.util.Base64.NO_WRAP)}"
    }

    private fun decodeEntry(encoded: String): ClipboardEntry? {
        return try {
            val parts = encoded.split("|")
            if (parts.size >= 3) {
                val hash = parts[0].toIntOrNull() ?: return null
                val timestamp = parts[1].toLongOrNull() ?: return null
                val text = String(android.util.Base64.decode(parts[2], android.util.Base64.NO_WRAP))
                ClipboardEntry(text, timestamp)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun copy(text: String) {
        val entry = ClipboardEntry(text, System.currentTimeMillis() / 1000)
        clipboardData.removeAll { it.text == text }
        clipboardData.add(0, entry)
        if (clipboardData.size > MAX_ENTRIES) {
            clipboardData = clipboardData.take(MAX_ENTRIES).toMutableList()
        }
        saveToPrefs()
    }

    fun paste(): String? {
        return clipboardData.firstOrNull()?.text
    }

    fun getHistory(): List<ClipboardEntry> = clipboardData.toList()

    fun clear() {
        clipboardData.clear()
        saveToPrefs()
    }

    fun deleteEntry(timestamp: Long) {
        clipboardData.removeAll { it.timestamp == timestamp }
        saveToPrefs()
    }

    data class ClipboardEntry(val text: String, val timestamp: Long)
}