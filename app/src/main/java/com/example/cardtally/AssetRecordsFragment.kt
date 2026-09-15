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
import com.example.cardtally.util.Money
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
        view.findViewById<ImageButton>(R.id.btn_more_asset).setOnClickListener { anchor ->
            showAssetMoreMenu(anchor)
        }
        view.findViewById<View>(R.id.action_transfer).setOnClickListener {
            Toast.makeText(requireContext(), R.string.toast_transfer_not_configured, Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.action_record).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AddRecordFragment())
                .addToBackStack(null)
                .commit()
        }
        view.findViewById<TextView>(R.id.text_asset_balance).text = "¥${Money.formatYuan(assetAmount)}"

        databaseHelper = DatabaseHelper(requireContext())
        val canManage = assetId == 0L || databaseHelper.canManageAsset(assetId)
        view.findViewById<ImageButton>(R.id.btn_edit_asset).visibility = if (canManage) View.VISIBLE else View.GONE
        view.findViewById<ImageButton>(R.id.btn_more_asset).visibility = if (canManage) View.VISIBLE else View.GONE

        textTitle.text = getString(R.string.asset_records_title)
        
        val fallbackTypeText = when (assetType) {
            0 -> getString(R.string.asset_type_cash)
            1 -> getString(R.string.asset_type_bank)
            2 -> getString(R.string.asset_type_alipay)
            3 -> getString(R.string.asset_type_wechat)
            else -> getString(R.string.ledger_chart_other)
        }
        val typeText = assetCategoryLabel.ifBlank { fallbackTypeText }
        textAssetInfo.text = String.format("%s · %s", typeText, assetName)

        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())

        loadRecords()

        return view
    }

    private fun showAssetMoreMenu(anchor: View) {
        if (assetId == 0L) return
        val popup = android.widget.PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.menu_asset_records, popup.menu)
        val isPinned = databaseHelper.getAllAssets().firstOrNull { it.id == assetId }?.isPinned == true
        popup.menu.findItem(R.id.action_pin_asset).title = getString(
            if (isPinned) R.string.menu_asset_unpin else R.string.menu_asset_pin
        )
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin_asset -> {
                    databaseHelper.setAssetPinned(assetId, !isPinned)
                    Toast.makeText(
                        requireContext(),
                        if (isPinned) R.string.toast_asset_unpinned else R.string.toast_asset_pinned,
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                R.id.action_archive_asset -> {
                    showArchiveAssetDialog()
                    true
                }
                R.id.action_delete_asset -> {
                    showDeleteAssetDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showArchiveAssetDialog() {
        if (assetId == 0L) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.asset_dialog_archive_title)
            .setMessage(getString(R.string.asset_dialog_archive_message, assetName))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.asset_dialog_archive_confirm) { _, _ ->
                databaseHelper.archiveAsset(assetId)
                Toast.makeText(requireContext(), R.string.toast_asset_archived, Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .show()
    }

    private fun showDeleteAssetDialog() {
        if (assetId == 0L) return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.asset_dialog_delete_title)
            .setMessage(getString(R.string.asset_dialog_delete_message, assetName))
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(R.string.asset_dialog_delete_confirm) { _, _ ->
                try {
                    if (databaseHelper.deleteAsset(assetId)) {
                        Toast.makeText(requireContext(), R.string.toast_asset_deleted, Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        Toast.makeText(requireContext(), R.string.toast_asset_delete_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (error: DatabaseHelper.AssetOperationException) {
                    Toast.makeText(requireContext(), R.string.error_asset_in_use_by_records, Toast.LENGTH_LONG).show()
                }
            }
            .show()
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
            // Asset history is a property of the asset id: every ledger that
            // references it, including transfers in and out, each record once.
            databaseHelper.getAllRecordsByAssetId(assetId)
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

            val ledgerNames = databaseHelper.getLedgerNamesByIds(
                records.mapNotNull { it.ledgerId }.toSet()
            )
            records.forEach { record ->
                record.ledgerName = record.ledgerId?.let { ledgerNames[it] }
                    ?: getString(R.string.asset_records_unknown_ledger)
            }
            val dateGroups = groupRecordsByDate(records)
            val categoryIconsById = databaseHelper.getAllCategories()
                .filter { !it.icon.isNullOrEmpty() }
                .associate { it.id to it.icon.orEmpty() }

            if (adapter == null) {
                adapter = DateGroupAdapter(
                    dateGroups,
                    object : DateGroupAdapter.OnRecordActionListener {
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
                    },
                    categoryIconsById = categoryIconsById,
                    showAssetRoute = false
                )
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
