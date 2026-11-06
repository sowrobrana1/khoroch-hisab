package com.sowrobrana.khorochhisab

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LedgerViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LEDGER_ID = "ledger_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ledger_view)

        val db = ExpenseDbHelper(this)
        val ledgerId = intent.getLongExtra(EXTRA_LEDGER_ID, -1)
        val ledger = db.ledgerById(ledgerId)
        if (ledger == null) {
            finish()
            return
        }
        val expenses = db.expensesForLedger(ledger.id)

        val titleView: TextView = findViewById(R.id.ledger_title)
        val totalView: TextView = findViewById(R.id.ledger_total)
        val listView: RecyclerView = findViewById(R.id.expense_list)
        val shareButton: Button = findViewById(R.id.share_button)
        listView.layoutManager = LinearLayoutManager(this)

        titleView.text = ledger.name
        val totals = runningTotals(expenses.map { it.amount })
        val rows = expenses.mapIndexed { i, e -> ExpenseRow(e, totals[i]) }
        listView.adapter = ExpenseAdapter(rows, null) // read-only: no delete
        totalView.text = "সব মিলিয়ে মোট: ${Bangla.num(totals.lastOrNull() ?: 0L)} টাকা"

        shareButton.setOnClickListener {
            showShareChoices(ledger.name, expenses)
        }
    }

    private fun showShareChoices(ledgerName: String, expenses: List<Expense>) {
        AlertDialog.Builder(this)
            .setTitle("কীভাবে পাঠাবে?")
            .setItems(arrayOf("ছবি হিসেবে", "লেখা হিসেবে")) { _, which ->
                if (which == 0) ShareHelper.shareAsImage(this, ledgerName, expenses)
                else ShareHelper.shareAsText(this, ledgerName, expenses)
            }
            .show()
    }
}
