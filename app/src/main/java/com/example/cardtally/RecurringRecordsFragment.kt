package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.RecurringRecordAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.RecurringRecordScheduler

class RecurringRecordsFragment : Fragment() {
    private lateinit var database: DatabaseHelper
    private lateinit var adapter: RecurringRecordAdapter
    private lateinit var empty: TextView
    private var ledgerNames: Map<Long, String> = emptyMap()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_recurring_records, container, false)
        database = DatabaseHelper(requireContext())
        empty = view.findViewById(R.id.text_recurring_empty)
        adapter = RecurringRecordAdapter(emptyList(), { item -> openEditor(item.id) }, { item, enabled ->
            database.setRecurringEnabled(item.id, enabled)
            RecurringRecordScheduler.schedule(requireContext())
            loadItems()
        }, ledgerNameFor = { ledgerNames[it].orEmpty() })
        view.findViewById<RecyclerView>(R.id.recycler_recurring_records).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@RecurringRecordsFragment.adapter
        }
        view.findViewById<ImageButton>(R.id.btn_recurring_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<ImageButton>(R.id.btn_recurring_add).setOnClickListener { openEditor(null) }
        loadItems()
        return view
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            database.processDueRecurringRecordsForAllLedgers()
            RecurringRecordScheduler.schedule(requireContext())
            loadItems()
        }
    }

    override fun onDestroyView() {
        if (::database.isInitialized) database.close()
        super.onDestroyView()
    }

    private fun loadItems() {
        database.repairInvalidRecurringNextDueDates()
        ledgerNames = database.getLedgers().associate { it.id to it.name }
        val items = database.getAllRecurringRecords()
        adapter.updateItems(items)
        empty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openEditor(id: Long?) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecurringRecordEditFragment.newInstance(id))
            .addToBackStack(null)
            .commit()
    }
}
