package com.sowrobrana.khorochhisab

import android.app.DatePickerDialog
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
    private lateinit var emptyView: TextView
    private lateinit var listView: RecyclerView

    private var selectedDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = ExpenseDbHelper(this)

        grandTotalView = findViewById(R.id.grand_total)
        emptyView = findViewById(R.id.empty_view)
        listView = findViewById(R.id.expense_list)
        listView.layoutManager = LinearLayoutManager(this)

        adapter = ExpenseAdapter(emptyList()) { expense -> confirmDelete(expense) }
        listView.adapter = adapter

        val dateButton: Button = findViewById(R.id.date_button)
        val placeInput: EditText = findViewById(R.id.place_input)
        val amountInput: EditText = findViewById(R.id.amount_input)
        val addButton: Button = findViewById(R.id.add_button)

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
