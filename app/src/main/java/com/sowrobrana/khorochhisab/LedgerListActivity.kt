package com.sowrobrana.khorochhisab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LedgerListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ledger_list)

        val db = ExpenseDbHelper(this)
        val listView: RecyclerView = findViewById(R.id.ledger_list)
        val emptyView: TextView = findViewById(R.id.empty_view)
        listView.layoutManager = LinearLayoutManager(this)

        val archived = db.archivedLedgers()
        emptyView.visibility = if (archived.isEmpty()) View.VISIBLE else View.GONE
        listView.adapter = LedgerAdapter(archived) { stats ->
            val intent = Intent(this, LedgerViewActivity::class.java)
            intent.putExtra(LedgerViewActivity.EXTRA_LEDGER_ID, stats.ledger.id)
            startActivity(intent)
        }
    }
}
