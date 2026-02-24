package com.example.cardtally

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.google.android.material.bottomnavigation.BottomNavigationView

class AssetRecordsFragment : Fragment() {
    private lateinit var textTitle: TextView
    private lateinit var textAssetInfo: TextView
    private lateinit var textEmpty: TextView
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var databaseHelper: DatabaseHelper
    private var adapter: DateGroupAdapter? = null

    private var assetName: String = ""
    private var assetAmount: Double = 0.0
    private var assetType: Int = 0

    companion object {
        fun newInstance(assetName: String, assetAmount: Double, assetType: Int): AssetRecordsFragment {
            val fragment = AssetRecordsFragment()
            val args = Bundle()
            args.putString("asset_name", assetName)
            args.putDouble("asset_amount", assetAmount)
            args.putInt("asset_type", assetType)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            assetName = it.getString("asset_name", "")
            assetAmount = it.getDouble("asset_amount", 0.0)
            assetType = it.getInt("asset_type", 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asset_records, container, false)

        textTitle = view.findViewById(R.id.text_title)
        textAssetInfo = view.findViewById(R.id.text_asset_info)
        textEmpty = view.findViewById(R.id.text_empty)
        recyclerRecords = view.findViewById(R.id.recycler_records)

        databaseHelper = DatabaseHelper(requireContext())

        textTitle.text = assetName
        
        val typeText = when (assetType) {
            0 -> "现金"
            1 -> "银行卡"
            2 -> "支付宝"
            3 -> "微信"
            else -> "其他"
        }
        textAssetInfo.text = String.format("%s · ¥%.2f", typeText, assetAmount)

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        loadRecords()

        return view
    }

    override fun onResume() {
        super.onResume()
        hideBottomNav()
        loadRecords()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun loadRecords() {
        val records = databaseHelper.getRecordsByAssetSource(assetName)

        if (records.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            recyclerRecords.visibility = View.GONE
            adapter = null
            recyclerRecords.adapter = null
        } else {
            textEmpty.visibility = View.GONE
            recyclerRecords.visibility = View.VISIBLE

            val dateGroups = groupRecordsByDate(records)

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
                        databaseHelper.deleteRecord(record.id)
                        loadRecords()
                    }

                    override fun onMultiSelectChanged(selectedCount: Int) {}

                    override fun onDeleteSelected(records: List<Record>) {}

                    override fun onEnterMultiSelectMode(record: Record) {}

                    override fun onToggleMultiSelect(record: Record) {}
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

    private fun hideBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.GONE
    }

    private fun showBottomNav() {
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.visibility = View.VISIBLE
    }
}
