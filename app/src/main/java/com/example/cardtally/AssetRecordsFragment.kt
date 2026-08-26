package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import android.widget.Toast
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
    private var assetCategoryLabel: String = ""
    private var assetId: Long = 0L

    companion object {
        fun newInstance(assetName: String, assetAmount: Double, assetType: Int, assetId: Long = 0L, assetCategoryLabel: String = ""): AssetRecordsFragment {
            val fragment = AssetRecordsFragment()
            val args = Bundle()
            args.putString("asset_name", assetName)
            args.putDouble("asset_amount", assetAmount)
            args.putInt("asset_type", assetType)
            args.putLong("asset_id", assetId)
            args.putString("asset_category_label", assetCategoryLabel)
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
            assetId = it.getLong("asset_id", 0L)
            assetCategoryLabel = it.getString("asset_category_label", "")
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

        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        view.findViewById<ImageButton>(R.id.btn_edit_asset).setOnClickListener {
            if (assetId != 0L) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddAssetFragment.newEditInstance(assetId))
                    .addToBackStack(null)
                    .commit()
            }
        }
        view.findViewById<ImageButton>(R.id.btn_pin_asset).setOnClickListener {
            if (assetId != 0L) {
                databaseHelper.setAssetPinned(assetId, true)
                Toast.makeText(requireContext(), "已置顶", Toast.LENGTH_SHORT).show()
            }
        }
        view.findViewById<ImageButton>(R.id.btn_archive_asset).setOnClickListener {
            if (assetId != 0L) {
                AlertDialog.Builder(requireContext())
                    .setTitle("归档资产")
                    .setMessage("确定归档“$assetName”吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("归档") { _, _ ->
                        databaseHelper.archiveAsset(assetId)
                        Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                    .show()
            }
        }
        view.findViewById<ImageButton>(R.id.btn_delete_asset).setOnClickListener {
            if (assetId != 0L) {
                AlertDialog.Builder(requireContext())
                    .setTitle("删除资产")
                    .setMessage("确定删除“$assetName”吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        try {
                            if (databaseHelper.deleteAsset(assetId)) {
                                Toast.makeText(requireContext(), "已删除", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.popBackStack()
                            } else {
                                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
                            }
                        } catch (error: DatabaseHelper.AssetOperationException) {
                            Toast.makeText(requireContext(), "该资产仍有关联账单，无法删除", Toast.LENGTH_LONG).show()
                        }
                    }
                    .show()
            }
        }
        view.findViewById<View>(R.id.action_transfer).setOnClickListener {
            Toast.makeText(requireContext(), "转账功能暂未配置", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.action_record).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<TextView>(R.id.text_asset_balance).text = String.format("¥%.2f", assetAmount)

        databaseHelper = DatabaseHelper(requireContext())
        val canManage = assetId == 0L || databaseHelper.canManageAsset(assetId)
        view.findViewById<ImageButton>(R.id.btn_edit_asset).visibility = if (canManage) View.VISIBLE else View.GONE
        view.findViewById<ImageButton>(R.id.btn_pin_asset).visibility = if (canManage) View.VISIBLE else View.GONE
        view.findViewById<ImageButton>(R.id.btn_archive_asset).visibility = if (canManage) View.VISIBLE else View.GONE
        view.findViewById<ImageButton>(R.id.btn_delete_asset).visibility = if (canManage) View.VISIBLE else View.GONE

        textTitle.text = "账户详情"
        
        val fallbackTypeText = when (assetType) {
            0 -> "现金"
            1 -> "银行卡"
            2 -> "支付宝"
            3 -> "微信"
            else -> "其他"
        }
        val typeText = assetCategoryLabel.ifBlank { fallbackTypeText }
        textAssetInfo.text = String.format("%s · %s", typeText, assetName)

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
        val records = if (assetId != 0L) {
            databaseHelper.getRecordsByAssetId(assetId)
        } else {
            databaseHelper.getRecordsByAssetSource(assetName)
        }

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
