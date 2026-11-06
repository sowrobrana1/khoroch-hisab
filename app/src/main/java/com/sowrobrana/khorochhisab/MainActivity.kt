package com.sowrobrana.khorochhisab

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var db: ExpenseDbHelper
    private lateinit var adapter: ExpenseAdapter
    private lateinit var grandTotalView: TextView
    private lateinit var ledgerLabelView: TextView
    private lateinit var emptyView: TextView
    private lateinit var listView: RecyclerView

    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = ExpenseDbHelper(this)

        grandTotalView = findViewById(R.id.grand_total)
        ledgerLabelView = findViewById(R.id.current_ledger_label)
        emptyView = findViewById(R.id.empty_view)
        listView = findViewById(R.id.expense_list)
        listView.layoutManager = LinearLayoutManager(this)

        adapter = ExpenseAdapter(emptyList()) { expense -> confirmDelete(expense) }
        listView.adapter = adapter

        val newHisabButton: Button = findViewById(R.id.new_hisab_button)
        val oldHisabButton: Button = findViewById(R.id.old_hisab_button)
        val shareButton: Button = findViewById(R.id.share_button)
        val dateButton: Button = findViewById(R.id.date_button)
        val placeInput: EditText = findViewById(R.id.place_input)
        val amountInput: EditText = findViewById(R.id.amount_input)
        val addButton: Button = findViewById(R.id.add_button)

        newHisabButton.setOnClickListener { confirmNewHisab() }
        oldHisabButton.setOnClickListener {
            startActivity(Intent(this, LedgerListActivity::class.java))
        }
        shareButton.setOnClickListener {
            val expenses = db.all()
            if (expenses.isEmpty()) {
                Toast.makeText(this, "এখনো কোনো খরচ লেখা হয়নি", Toast.LENGTH_SHORT).show()
            } else {
                showShareChoices(currentLedgerDisplayName(), expenses)
            }
        }

        updateDateButton(dateButton)
        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    updateDateButton(dateButton)
                },
                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
            ).show()
        }

        addButton.setOnClickListener {
            val place = placeInput.text.toString().trim()
            val amountText = amountInput.text.toString().trim()
            val amount = amountText.toLongOrNull()
            if (place.isEmpty()) {
                Toast.makeText(this, "কোথায় খরচ হলো লেখো", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "টাকার অঙ্ক ঠিক করে লেখো", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.add(selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE), place, amount)
            placeInput.text.clear()
            amountInput.text.clear()
            refresh()
            listView.scrollToPosition(adapter.itemCount - 1)
        }

        refresh()
    }

    private fun currentLedgerDisplayName(): String {
        val cur = db.currentLedger()
        return if (cur?.startDate != null) {
            "বর্তমান হিসাব — ${Bangla.formatDate(cur.startDate)} থেকে"
        } else {
            "বর্তমান হিসাব"
        }
    }

    private fun confirmNewHisab() {
        if (db.all().isEmpty()) {
            Toast.makeText(this, "এখনো কোনো খরচ লেখা হয়নি", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("নতুন হিসাব শুরু করবে?")
            .setMessage(
                "এখনকার সব খরচ আলাদা ফাইলে জমা থাকবে — কিছু মুছবে না। " +
                    "তারপর নতুন হিসাব খালি থেকে শুরু হবে।"
            )
            .setPositiveButton("হ্যাঁ, শুরু করো") { _, _ ->
                db.startNewLedger()
                refresh()
                Toast.makeText(
                    this,
                    "নতুন হিসাব শুরু হলো — পুরোনোটা 'পুরোনো হিসাব'-এ জমা আছে",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("না", null)
            .show()
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

    private fun updateDateButton(dateButton: Button) {
        dateButton.text = Bangla.formatDate(
            selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    private fun refresh() {
        val expenses = db.all()
        val totals = runningTotals(expenses.map { it.amount })
        val rows = expenses.mapIndexed { i, e -> ExpenseRow(e, totals[i]) }
        adapter.update(rows)
        val grand = totals.lastOrNull() ?: 0L
        grandTotalView.text = "সব মিলিয়ে মোট: ${Bangla.num(grand)} টাকা"
        ledgerLabelView.text = currentLedgerDisplayName()
        emptyView.visibility =
            if (rows.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(this)
            .setTitle("মুছবে?")
            .setMessage(
                "${Bangla.formatDate(expense.date)} — ${expense.place}, " +
                    "${Bangla.num(expense.amount)} টাকা"
            )
            .setPositiveButton("মুছে দাও") { _, _ ->
                db.delete(expense.id)
                refresh()
            }
            .setNegativeButton("না", null)
            .show()
    }
}
