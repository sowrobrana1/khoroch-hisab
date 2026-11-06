package com.sowrobrana.khorochhisab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ExpenseRow(val expense: Expense, val runningTotal: Long)

class ExpenseAdapter(
    private var rows: List<ExpenseRow>,
    private val onLongPress: ((Expense) -> Unit)? = null
) : RecyclerView.Adapter<ExpenseAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val date: TextView = v.findViewById(R.id.row_date)
        val place: TextView = v.findViewById(R.id.row_place)
        val amount: TextView = v.findViewById(R.id.row_amount)
        val total: TextView = v.findViewById(R.id.row_total)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val row = rows[position]
        h.date.text = Bangla.formatDate(row.expense.date)
        h.place.text = row.expense.place
        h.amount.text = "${Bangla.num(row.expense.amount)} টাকা"
        h.total.text = "মোট: ${Bangla.num(row.runningTotal)} টাকা"
        if (onLongPress != null) {
            h.itemView.setOnLongClickListener {
                onLongPress.invoke(row.expense)
                true
            }
        }
    }

    override fun getItemCount(): Int = rows.size

    fun update(newRows: List<ExpenseRow>) {
        rows = newRows
        notifyDataSetChanged()
    }
}
