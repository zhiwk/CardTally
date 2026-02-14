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
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView

class SearchFragment : Fragment() {
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var textEmpty: TextView
    private lateinit var textKeyword: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: DateGroupAdapter? = null

    private var searchKeyword: String = ""

    companion object {
        fun newInstance(keyword: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString("keyword", keyword)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            searchKeyword = it.getString("keyword", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerRecords = view.findViewById(R.id.recycler_records)
        textEmpty = view.findViewById(R.id.text_empty)
        textKeyword = view.findViewById(R.id.text_keyword)

        databaseHelper = DatabaseHelper(requireContext())

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        textKeyword.text = String.format("关键词：%s", searchKeyword)

        searchRecords()

        return view
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun searchRecords() {
        val allRecords = databaseHelper.getAllRecords()

        val filteredRecords = allRecords.filter { record ->
            record.category.contains(searchKeyword, ignoreCase = true) ||
            record.description?.contains(searchKeyword, ignoreCase = true) == true ||
            record.amount.toString().contains(searchKeyword) ||
            String.format("%.2f", record.amount).contains(searchKeyword) ||
            record.assetSource?.contains(searchKeyword, ignoreCase = true) == true ||
            record.date.contains(searchKeyword)
        }

        if (filteredRecords.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE

            val dateGroups = groupRecordsByDate(filteredRecords)

            if (adapter == null) {
                adapter = DateGroupAdapter(dateGroups, object : DateGroupAdapter.OnRecordActionListener {
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

                    override fun onMultiSelectChanged(selectedCount: Int) {
                    }

                    override fun onDeleteSelected(records: List<Record>) {
                    }

                    override fun onEnterMultiSelectMode(record: Record) {
                    }

                    override fun onToggleMultiSelect(record: Record) {
                    }
                })
                recyclerRecords.adapter = adapter
            } else {
                adapter?.updateDateGroups(dateGroups)
                if (recyclerRecords.adapter == null) {
                    recyclerRecords.adapter = adapter
                }
            }
        }
    }

    private fun groupRecordsByDate(records: List<Record>): List<DateGroup> {
        val grouped = records.groupBy { it.date }
        return grouped.map { (date, records) ->
            DateGroup(date, records)
        }.sortedByDescending { it.date }
    }

    private fun showDeleteDialog(record: Record) {
        AlertDialog.Builder(requireContext())
            .setTitle("删除记录")
            .setMessage("确定要删除这条记录吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.deleteRecord(record.id)
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                searchRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
}
