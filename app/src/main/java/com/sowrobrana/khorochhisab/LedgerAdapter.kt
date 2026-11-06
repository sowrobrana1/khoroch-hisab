package com.sowrobrana.khorochhisab

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LedgerAdapter(
    private val items: List<LedgerStats>,
    private val onTap: (LedgerStats) -> Unit
) : RecyclerView.Adapter<LedgerAdapter.Holder>() {

    class Holder(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView = v.findViewById(R.id.ledger_name)
        val info: TextView = v.findViewById(R.id.ledger_info)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ledger, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(h: Holder, position: Int) {
        val item = items[position]
        h.name.text = item.ledger.name
        h.info.text = "${Bangla.num(item.entryCount)} টি খরচ • মোট " +
            "${Bangla.num(item.total)} টাকা"
        h.itemView.setOnClickListener { onTap(item) }
    }

    override fun getItemCount(): Int = items.size
}
