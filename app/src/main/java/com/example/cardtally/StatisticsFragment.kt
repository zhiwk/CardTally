package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.FloatingNavLayoutHelper
import com.google.android.material.button.MaterialButtonToggleGroup

class StatisticsFragment : Fragment() {
    private lateinit var toggleLedgerView: MaterialButtonToggleGroup
    private lateinit var textEmpty: TextView
    private lateinit var recyclerStatistics: RecyclerView
    private lateinit var fabAdd: View
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var statisticsAdapter: StatisticsAdapter
    private var recordsAdapter: DateGroupAdapter? = null

    private var currentViewMode = VIEW_MODE_STATISTICS

    companion object {
        private const val VIEW_MODE_STATISTICS = 0
        private const val VIEW_MODE_RECORDS = 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        toggleLedgerView = view.findViewById(R.id.toggle_ledger_view)
        textEmpty = view.findViewById(R.id.text_empty)
        recyclerStatistics = view.findViewById(R.id.recycler_statistics)
        fabAdd = view.findViewById(R.id.fab_add)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerStatistics.layoutManager = LinearLayoutManager(requireContext())
        statisticsAdapter = StatisticsAdapter()

        toggleLedgerView.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            currentViewMode = if (checkedId == R.id.btn_view_records) {
                VIEW_MODE_RECORDS
            } else {
                VIEW_MODE_STATISTICS
            }
            renderCurrentView()
        }

        toggleLedgerView.check(R.id.btn_view_statistics)

        fabAdd.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }

        requireActivity().findViewById<View>(R.id.nav_shell)?.let { navShell ->
            FloatingNavLayoutHelper.applyFabGapAboveBottomNav(fabAdd, navShell)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        renderCurrentView()
    }

    private fun renderCurrentView() {
        if (currentViewMode == VIEW_MODE_RECORDS) {
            loadAllRecords()
        } else {
            loadCategoryStatistics()
        }
    }

    private fun loadCategoryStatistics() {
        val expenseStats = databaseHelper.getCategoryStatistics(0)
        val incomeStats = databaseHelper.getCategoryStatistics(1)

        val allStats = mutableMapOf<String, Double>()
        expenseStats.forEach { (label, amount) ->
            allStats[getString(R.string.statistics_expense_prefix, label)] = -amount
        }
        incomeStats.forEach { (label, amount) ->
            allStats[getString(R.string.statistics_income_prefix, label)] = amount
        }

        if (allStats.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerStatistics.visibility = View.GONE
            textEmpty.text = getString(R.string.ledger_empty_statistics)
        } else {
            textEmpty.visibility = View.GONE
            recyclerStatistics.visibility = View.VISIBLE
            if (recyclerStatistics.adapter !== statisticsAdapter) {
                recyclerStatistics.adapter = statisticsAdapter
            }
            statisticsAdapter.updateData(allStats)
        }
    }

    private fun loadAllRecords() {
        val allRecords = databaseHelper.getAllRecords()

        if (allRecords.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerStatistics.visibility = View.GONE
            textEmpty.text = getString(R.string.ledger_empty_records)
        } else {
            textEmpty.visibility = View.GONE
            recyclerStatistics.visibility = View.VISIBLE

            val dateGroups = groupRecordsByDate(allRecords)
            if (recordsAdapter == null) {
                recordsAdapter = DateGroupAdapter(dateGroups, object : DateGroupAdapter.OnRecordActionListener {
                    override fun onEdit(record: Record) {
                        val editFragment = EditRecordFragment.newInstance(record.id)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, editFragment)
                            .addToBackStack(null)
                            .commit()
                    }

                    override fun onDelete(record: Record) {
                        showDeleteDialog(record)
                    }

                    override fun onMultiSelectChanged(selectedCount: Int) {}

                    override fun onDeleteSelected(records: List<Record>) {}

                    override fun onEnterMultiSelectMode(record: Record) {}

                    override fun onToggleMultiSelect(record: Record) {}
                })
            } else {
                recordsAdapter?.updateDateGroups(dateGroups)
            }

            if (recyclerStatistics.adapter !== recordsAdapter) {
                recyclerStatistics.adapter = recordsAdapter
            }
        }
    }

    private fun groupRecordsByDate(records: List<Record>): List<DateGroup> {
        return records.groupBy { it.date }
            .map { (date, groupedRecords) -> DateGroup(date, groupedRecords) }
            .sortedByDescending { it.date }
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_record_title)
            .setMessage(R.string.delete_record_message)
            .setPositiveButton(R.string.dialog_confirm) { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), R.string.toast_delete_success, Toast.LENGTH_SHORT).show()
                loadAllRecords()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }
}
