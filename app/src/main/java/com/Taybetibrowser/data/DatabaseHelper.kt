package com.Taybetibrowser.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "taybeti_browser.db"
        private const val DATABASE_VERSION = 1

        private const val BOOKMARKS_TABLE = "bookmarks"
        private const val PASSWORDS_TABLE = "passwords"
        private const val HISTORY_TABLE = "history"

        private const val COL_ID = "id"
        private const val COL_TITLE = "title"
        private const val COL_URL = "url"
        private const val COL_USERNAME = "username"
        private const val COL_PASSWORD = "password"
        private const val COL_CREATED = "created_at"
        private const val COL_VISITED = "visited_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $BOOKMARKS_TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_URL TEXT NOT NULL,
                $COL_CREATED INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """)

        db.execSQL("""
            CREATE TABLE $PASSWORDS_TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_URL TEXT NOT NULL,
                $COL_USERNAME TEXT NOT NULL,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_CREATED INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """)

        db.execSQL("""
            CREATE TABLE $HISTORY_TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_URL TEXT NOT NULL,
                $COL_TITLE TEXT,
                $COL_VISITED INTEGER DEFAULT (strftime('%s', 'now'))
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $BOOKMARKS_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $PASSWORDS_TABLE")
        onCreate(db)
    }

    fun addBookmark(title: String, url: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_URL, url)
        }
        return db.insert(BOOKMARKS_TABLE, null, values)
    }

    fun getAllBookmarks(): List<Bookmark> {
        val bookmarks = mutableListOf<Bookmark>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $BOOKMARKS_TABLE ORDER BY $COL_CREATED DESC", null)

        cursor.use {
            while (it.moveToNext()) {
                bookmarks.add(
                    Bookmark(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        url = it.getString(it.getColumnIndexOrThrow(COL_URL))
                    )
                )
            }
        }
        return bookmarks
    }

    fun deleteBookmark(id: Int): Int {
        val db = writableDatabase
        return db.delete(BOOKMARKS_TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun addPassword(title: String, url: String, username: String, password: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_URL, url)
            put(COL_USERNAME, username)
            put(COL_PASSWORD, password)
        }
        return db.insert(PASSWORDS_TABLE, null, values)
    }

    fun getAllPasswords(): List<PasswordEntry> {
        val passwords = mutableListOf<PasswordEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $PASSWORDS_TABLE ORDER BY $COL_CREATED DESC", null)

        cursor.use {
            while (it.moveToNext()) {
                passwords.add(
                    PasswordEntry(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                        url = it.getString(it.getColumnIndexOrThrow(COL_URL)),
                        username = it.getString(it.getColumnIndexOrThrow(COL_USERNAME)),
                        password = it.getString(it.getColumnIndexOrThrow(COL_PASSWORD))
                    )
                )
            }
        }
        return passwords
    }

    fun getPasswordForUrl(url: String): PasswordEntry? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM $PASSWORDS_TABLE WHERE ? LIKE '%' || $COL_URL || '%' LIMIT 1",
            arrayOf(url)
        )

        cursor.use {
            if (it.moveToFirst()) {
                return PasswordEntry(
                    id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                    title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)),
                    url = it.getString(it.getColumnIndexOrThrow(COL_URL)),
                    username = it.getString(it.getColumnIndexOrThrow(COL_USERNAME)),
                    password = it.getString(it.getColumnIndexOrThrow(COL_PASSWORD))
                )
            }
        }
        return null
    }

    fun deletePassword(id: Int): Int {
        val db = writableDatabase
        return db.delete(PASSWORDS_TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun addHistoryEntry(url: String, title: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_URL, url)
            put(COL_TITLE, title)
        }
        return db.insert(HISTORY_TABLE, null, values)
    }

    fun getAllHistory(): List<HistoryEntry> {
        val history = mutableListOf<HistoryEntry>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $HISTORY_TABLE ORDER BY $COL_VISITED DESC", null)

        cursor.use {
            while (it.moveToNext()) {
                history.add(
                    HistoryEntry(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        url = it.getString(it.getColumnIndexOrThrow(COL_URL)),
                        title = it.getString(it.getColumnIndexOrThrow(COL_TITLE)) ?: "",
                        visitedAt = it.getLong(it.getColumnIndexOrThrow(COL_VISITED))
                    )
                )
            }
        }
        return history
    }

    fun clearHistory(): Int {
        val db = writableDatabase
        return db.delete(HISTORY_TABLE, null, null)
    }

    fun deleteHistoryEntry(id: Int): Int {
        val db = writableDatabase
        return db.delete(HISTORY_TABLE, "$COL_ID = ?", arrayOf(id.toString()))
    }

    data class Bookmark(val id: Int, val title: String, val url: String)
    data class PasswordEntry(val id: Int, val title: String, val url: String, val username: String, val password: String)
    data class HistoryEntry(val id: Int, val url: String, val title: String, val visitedAt: Long)
}