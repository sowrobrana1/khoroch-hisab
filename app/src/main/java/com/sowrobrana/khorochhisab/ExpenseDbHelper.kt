package com.sowrobrana.khorochhisab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Expense(val id: Long, val date: String, val place: String, val amount: Long)

/** On-device SQLite storage only. No account, no internet, no sync. */
class ExpenseDbHelper(context: Context) :
    SQLiteOpenHelper(context, "khoroch.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "place TEXT NOT NULL, " +
                "amount INTEGER NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1: nothing to migrate.
    }

    /** Oldest first (serial order, newest appended at the bottom). */
    fun all(): List<Expense> {
        val out = ArrayList<Expense>()
        readableDatabase.rawQuery(
            "SELECT id, date, place, amount FROM expenses ORDER BY id ASC", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Expense(
                        id = c.getLong(0),
                        date = c.getString(1),
                        place = c.getString(2),
                        amount = c.getLong(3)
                    )
                )
            }
        }
        return out
    }

    fun add(date: String, place: String, amount: Long): Long {
        val values = ContentValues().apply {
            put("date", date)
            put("place", place)
            put("amount", amount)
        }
        return writableDatabase.insert("expenses", null, values)
    }

    fun delete(id: Long) {
        writableDatabase.delete("expenses", "id = ?", arrayOf(id.toString()))
    }
}
