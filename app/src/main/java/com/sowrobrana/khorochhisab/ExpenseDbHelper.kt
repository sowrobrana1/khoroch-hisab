package com.sowrobrana.khorochhisab

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Expense(val id: Long, val date: String, val place: String, val amount: Long)

data class Ledger(
    val id: Long,
    val name: String,
    val startDate: String?,
    val endDate: String?,
    val isCurrent: Boolean,
    val createdAt: Long
)

data class LedgerStats(
    val ledger: Ledger,
    val entryCount: Int,
    val total: Long
)

/**
 * On-device SQLite storage only. No account, no internet, no sync.
 *
 * v1: single expenses table (no ledgers).
 * v2: ledgers table added. Migration (v1 -> v2) wraps ALL existing expenses
 * into one "current" ledger — nothing is ever deleted or lost.
 */
class ExpenseDbHelper(context: Context) :
    SQLiteOpenHelper(context, "khoroch.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE ledgers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "start_date TEXT, " +
                "end_date TEXT, " +
                "is_current INTEGER NOT NULL DEFAULT 0, " +
                "created_at INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT NOT NULL, " +
                "place TEXT NOT NULL, " +
                "amount INTEGER NOT NULL, " +
                "ledger_id INTEGER NOT NULL DEFAULT 1)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "CREATE TABLE ledgers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, " +
                    "start_date TEXT, " +
                    "end_date TEXT, " +
                    "is_current INTEGER NOT NULL DEFAULT 0, " +
                    "created_at INTEGER NOT NULL)"
            )
            // Every expense written in v1 moves into one current ledger, untouched.
            db.execSQL(
                "INSERT INTO ledgers (name, start_date, end_date, is_current, created_at) " +
                    "SELECT 'চলমান হিসাব', MIN(date), NULL, 1, strftime('%s','now')*1000 FROM expenses"
            )
            db.execSQL("ALTER TABLE expenses ADD COLUMN ledger_id INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                "UPDATE expenses SET ledger_id = " +
                    "(SELECT id FROM ledgers WHERE is_current = 1 LIMIT 1)"
            )
        }
    }

    private fun rowToLedger(c: android.database.Cursor): Ledger {
        return Ledger(
            id = c.getLong(0),
            name = c.getString(1),
            startDate = c.getString(2),
            endDate = c.getString(3),
            isCurrent = c.getInt(4) == 1,
            createdAt = c.getLong(5)
        )
    }

    fun currentLedger(): Ledger? {
        readableDatabase.rawQuery(
            "SELECT id, name, start_date, end_date, is_current, created_at " +
                "FROM ledgers WHERE is_current = 1 LIMIT 1",
            null
        ).use { c ->
            if (c.moveToFirst()) return rowToLedger(c)
        }
        return null
    }

    fun getOrCreateCurrentLedger(): Ledger {
        currentLedger()?.let { return it }
        val values = ContentValues().apply {
            put("name", "চলমান হিসাব")
            put("is_current", 1)
            put("created_at", System.currentTimeMillis())
        }
        val id = writableDatabase.insert("ledgers", null, values)
        return Ledger(id, "চলমান হিসাব", null, null, true, System.currentTimeMillis())
    }

    /** Archived (closed) ledgers, newest first, with entry count + grand total each. */
    fun archivedLedgers(): List<LedgerStats> {
        val out = ArrayList<LedgerStats>()
        readableDatabase.rawQuery(
            "SELECT l.id, l.name, l.start_date, l.end_date, l.is_current, l.created_at, " +
                "COUNT(e.id), COALESCE(SUM(e.amount), 0) " +
                "FROM ledgers l LEFT JOIN expenses e ON e.ledger_id = l.id " +
                "WHERE l.is_current = 0 GROUP BY l.id ORDER BY l.id DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                val ledger = Ledger(
                    id = c.getLong(0),
                    name = c.getString(1),
                    startDate = c.getString(2),
                    endDate = c.getString(3),
                    isCurrent = c.getInt(4) == 1,
                    createdAt = c.getLong(5)
                )
                out.add(LedgerStats(ledger, c.getInt(6), c.getLong(7)))
            }
        }
        return out
    }

    fun ledgerById(id: Long): Ledger? {
        readableDatabase.rawQuery(
            "SELECT id, name, start_date, end_date, is_current, created_at " +
                "FROM ledgers WHERE id = ? LIMIT 1",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) return rowToLedger(c)
        }
        return null
    }

    /**
     * Close the current ledger (it becomes an archived file, named by its date
     * range) and start a fresh empty one. The old expenses are only moved,
     * never deleted.
     */
    fun startNewLedger(): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cur = currentLedger() ?: getOrCreateCurrentLedger()
            val exps = expensesForLedger(cur.id)
            val end = exps.lastOrNull()?.date
                ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val start = cur.startDate ?: exps.firstOrNull()?.date
            val closeValues = ContentValues().apply {
                put("is_current", 0)
                put("end_date", end)
                put("name", ledgerName(start, end))
            }
            db.update("ledgers", closeValues, "id = ?", arrayOf(cur.id.toString()))
            val newValues = ContentValues().apply {
                put("name", "চলমান হিসাব")
                put("is_current", 1)
                put("created_at", System.currentTimeMillis())
            }
            val id = db.insert("ledgers", null, newValues)
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    /** Current ledger's expenses, oldest first (serial order, newest at the bottom). */
    fun all(): List<Expense> {
        return expensesForLedger(getOrCreateCurrentLedger().id)
    }

    fun expensesForLedger(ledgerId: Long): List<Expense> {
        val out = ArrayList<Expense>()
        readableDatabase.rawQuery(
            "SELECT id, date, place, amount FROM expenses " +
                "WHERE ledger_id = ? ORDER BY id ASC",
            arrayOf(ledgerId.toString())
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
        val cur = getOrCreateCurrentLedger()
        val values = ContentValues().apply {
            put("date", date)
            put("place", place)
            put("amount", amount)
            put("ledger_id", cur.id)
        }
        val id = writableDatabase.insert("expenses", null, values)
        if (cur.startDate == null) {
            val fix = ContentValues().apply { put("start_date", date) }
            writableDatabase.update("ledgers", fix, "id = ?", arrayOf(cur.id.toString()))
        }
        return id
    }

    fun delete(id: Long) {
        writableDatabase.delete("expenses", "id = ?", arrayOf(id.toString()))
    }
}
