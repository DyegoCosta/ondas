package com.dyegocosta.ondas

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** intensity: 1 = leve, 2 = moderada, 3 = forte; null = não informada. */
data class Entry(val id: Long, val at: Long, val note: String, val intensity: Int? = null)

private class Db(context: Context) : SQLiteOpenHelper(context, "ondas.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, at INTEGER NOT NULL, note TEXT NOT NULL DEFAULT '', intensity INTEGER)")
        db.execSQL("CREATE INDEX entries_at ON entries(at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE entries ADD COLUMN intensity INTEGER")
    }
}

/** Todos os registros ficam em um banco local; o app não tem permissão de internet. */
class OndasRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val db = Db(appContext)
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<Entry>> = _entries

    private fun load(): List<Entry> =
        db.readableDatabase.query("entries", arrayOf("id", "at", "note", "intensity"), null, null, null, null, "at DESC, id DESC").use { c ->
            buildList {
                while (c.moveToNext()) add(Entry(c.getLong(0), c.getLong(1), c.getString(2), if (c.isNull(3)) null else c.getInt(3)))
            }
        }

    private fun changed(refreshWidgets: Boolean = true) {
        _entries.value = load()
        if (refreshWidgets) Widgets.refresh(appContext)
    }

    fun find(id: Long): Entry? = _entries.value.firstOrNull { it.id == id }

    fun add(at: Long = System.currentTimeMillis()): Long {
        val id = db.writableDatabase.insertOrThrow("entries", null, ContentValues().apply {
            put("at", at)
            put("note", "")
        })
        changed()
        return id
    }

    fun updateNote(id: Long, note: String) {
        db.writableDatabase.update("entries", ContentValues().apply { put("note", note) }, "id = ?", arrayOf(id.toString()))
        changed(refreshWidgets = false)
    }

    fun updateIntensity(id: Long, intensity: Int?) {
        db.writableDatabase.update("entries", ContentValues().apply { put("intensity", intensity) }, "id = ?", arrayOf(id.toString()))
        changed(refreshWidgets = false)
    }

    fun updateAt(id: Long, at: Long) {
        db.writableDatabase.update("entries", ContentValues().apply { put("at", at) }, "id = ?", arrayOf(id.toString()))
        changed()
    }

    fun delete(id: Long) {
        db.writableDatabase.delete("entries", "id = ?", arrayOf(id.toString()))
        changed()
    }

    /** Devolve um registro excluído, com o mesmo id, data, anotação e intensidade. */
    fun restore(entry: Entry) {
        db.writableDatabase.insertWithOnConflict("entries", null, ContentValues().apply {
            put("id", entry.id)
            put("at", entry.at)
            put("note", entry.note)
            put("intensity", entry.intensity)
        }, SQLiteDatabase.CONFLICT_REPLACE)
        changed()
    }

    fun todayCount(today: LocalDate = LocalDate.now()): Int =
        _entries.value.count { Format.date(it.at) == today }

    companion object {
        @Volatile private var instance: OndasRepository? = null

        fun get(context: Context): OndasRepository =
            instance ?: synchronized(this) {
                instance ?: OndasRepository(context).also { instance = it }
            }
    }
}
