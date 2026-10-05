package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.util.Money
import com.example.cardtally.util.IncomeExpenseColorScheme
import com.example.cardtally.util.ThemeColorHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton

class AssetFragment : Fragment() {
    interface AssetSelectionHost {
        fun onAssetSelected(asset: Asset)
        fun onAssetSelectionClosed()
        fun onNoAssetSelected() = Unit
    }

    private lateinit var recyclerAssets: RecyclerView
    private lateinit var recyclerPinnedAssets: RecyclerView
    private lateinit var recyclerCreditAssets: RecyclerView
    private lateinit var recyclerRechargeAssets: RecyclerView
    private lateinit var recyclerInvestmentAssets: RecyclerView
    private lateinit var recyclerReceivableAssets: RecyclerView
    private lateinit var recyclerPayableAssets: RecyclerView
    private lateinit var cardPinnedAssets: View
    private lateinit var cardAllAssets: View
    private lateinit var layoutFundsSection: View
    private lateinit var layoutCreditSection: View
    private lateinit var layoutRechargeSection: View
    private lateinit var layoutInvestmentSection: View
    private lateinit var layoutReceivableSection: View
    private lateinit var layoutPayableSection: View
    private lateinit var textEmpty: TextView
    private lateinit var textTotalAmount: TextView
    private lateinit var textTotalAssets: TextView
    private lateinit var textTotalLiabilities: TextView
    private lateinit var textFundsTotal: TextView
    private lateinit var textCreditTotal: TextView
    private lateinit var textRechargeTotal: TextView
    private lateinit var textInvestmentTotal: TextView
    private lateinit var textReceivableTotal: TextView
    private lateinit var textPayableTotal: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnArchive: ImageButton
    private lateinit var btnToggleAssetVisibility: ImageView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var contentView: View
    private var adapter: AssetAdapter? = null
    private var pinnedAdapter: AssetAdapter? = null
    private var creditAdapter: AssetAdapter? = null
    private val additionalAdapters = mutableMapOf<Int, AssetAdapter>()
    private var amountsVisible = true
    private var pickerMode = false
    private var excludedAssetId: Long? = null
    private var selectedAssetId: Long? = null
    private var pickerLedgerId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_asset_v2, container, false)
        contentView = view
        pickerMode = arguments?.getBoolean(ARG_PICKER_MODE, false) == true
        excludedAssetId = arguments?.getLong(ARG_EXCLUDED_ASSET_ID)?.takeIf { it > 0L }
        selectedAssetId = arguments?.getLong(ARG_SELECTED_ASSET_ID)?.takeIf { it > 0L }
        pickerLedgerId = arguments?.getLong(ARG_PICKER_LEDGER_ID)?.takeIf { it > 0L }
        amountsVisible = savedInstanceState?.getBoolean(KEY_AMOUNTS_VISIBLE, true) ?: true

        recyclerAssets = view.findViewById(R.id.recycler_assets)
        recyclerPinnedAssets = view.findViewById(R.id.recycler_pinned_assets)
        recyclerCreditAssets = view.findViewById(R.id.recycler_credit_assets)
        recyclerRechargeAssets = view.findViewById(R.id.recycler_recharge_assets)
        recyclerInvestmentAssets = view.findViewById(R.id.recycler_investment_assets)
        recyclerReceivableAssets = view.findViewById(R.id.recycler_receivable_assets)
        recyclerPayableAssets = view.findViewById(R.id.recycler_payable_assets)
        cardPinnedAssets = view.findViewById(R.id.card_pinned_assets)
        cardAllAssets = view.findViewById(R.id.card_all_assets)
        layoutFundsSection = view.findViewById(R.id.layout_funds_section)
        layoutCreditSection = view.findViewById(R.id.layout_credit_section)
        layoutRechargeSection = view.findViewById(R.id.layout_recharge_section)
        layoutInvestmentSection = view.findViewById(R.id.layout_investment_section)
        layoutReceivableSection = view.findViewById(R.id.layout_receivable_section)
        layoutPayableSection = view.findViewById(R.id.layout_payable_section)
        textEmpty = view.findViewById(R.id.text_empty)
        textTotalAmount = view.findViewById(R.id.text_total_amount)
        textTotalAssets = view.findViewById(R.id.text_total_assets)
        textTotalLiabilities = view.findViewById(R.id.text_total_liabilities)
        textFundsTotal = view.findViewById(R.id.text_funds_total)
        textCreditTotal = view.findViewById(R.id.text_credit_total)
        textRechargeTotal = view.findViewById(R.id.text_recharge_total)
        textInvestmentTotal = view.findViewById(R.id.text_investment_total)
        textReceivableTotal = view.findViewById(R.id.text_receivable_total)
        textPayableTotal = view.findViewById(R.id.text_payable_total)
        fabAdd = view.findViewById(R.id.fab_add)
        btnArchive = view.findViewById(R.id.btn_archive)
        btnToggleAssetVisibility = view.findViewById(R.id.btn_toggle_asset_visibility)
        updateVisibilityToggle()
        btnToggleAssetVisibility.setOnClickListener {
            amountsVisible = !amountsVisible
            updateVisibilityToggle()
            loadAssets()
        }

        databaseHelper = DatabaseHelper(requireContext())

        recyclerAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerPinnedAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerCreditAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerRechargeAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerInvestmentAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerReceivableAssets.layoutManager = LinearLayoutManager(requireContext())
        recyclerPayableAssets.layoutManager = LinearLayoutManager(requireContext())
        if (!pickerMode) {
            listOf(
                recyclerPinnedAssets,
                recyclerAssets,
                recyclerCreditAssets,
                recyclerRechargeAssets,
                recyclerInvestmentAssets,
                recyclerReceivableAssets,
                recyclerPayableAssets
            ).forEach(::attachAssetDragSorting)
        }

        loadAssets()

        if (pickerMode) {
            configurePickerMode(view)
        } else {
            fabAdd.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AssetTypeSelectFragment())
                    .addToBackStack("asset_type_select")
                    .commit()
            }

            btnArchive.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, ArchivedAssetsFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_AMOUNTS_VISIBLE, amountsVisible)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
    }

    private fun loadAssets() {
        val assets = databaseHelper.getAllAssets(pickerLedgerId).filter { it.id != excludedAssetId }
        val pinnedAssets = assets.filter { it.isPinned }
        val regularAssets = assets.filterNot { it.isPinned }
        val creditAssets = regularAssets.filter(::isCreditAsset)
        val fundAssets = regularAssets.filterNot(::isCreditAsset)
        val rechargeAssets = regularAssets.filter { it.categoryLabel in rechargeLabels }
        val investmentAssets = regularAssets.filter { it.categoryLabel in investmentLabels }
        val receivableAssets = regularAssets.filter { it.categoryLabel in receivableLabels }
        val payableAssets = regularAssets.filter { it.categoryLabel in payableLabels }
        val coreFundAssets = fundAssets.filterNot {
            it.categoryLabel in rechargeLabels || it.categoryLabel in investmentLabels ||
                it.categoryLabel in receivableLabels || it.categoryLabel in payableLabels
        }
        val totalMinor = databaseHelper.getTotalAssetsMinor()
        val includedAssets = assets.filter { it.includeInTotal }
        val totalAssetsMinor = includedAssets.filter { it.amount >= 0 }.sumOf(::amountMinor)
        val totalLiabilitiesMinor = includedAssets.filter { it.amount < 0 }.sumOf { -amountMinor(it) }
        val regularFundsTotalMinor = coreFundAssets.sumOf(::amountMinor)
        val regularCreditTotalMinor = creditAssets.filter { it.includeInTotal }.sumOf(::amountMinor)

        textTotalAmount.text = moneyText(totalMinor)
        textTotalAssets.text = moneyText(totalAssetsMinor)
        textTotalLiabilities.text = moneyText(totalLiabilitiesMinor)
        textTotalAssets.setTextColor(IncomeExpenseColorScheme.incomePrimary(requireContext()))
        textTotalLiabilities.setTextColor(IncomeExpenseColorScheme.expensePrimary(requireContext()))
        textFundsTotal.text = totalText(regularFundsTotalMinor)
        textCreditTotal.text = totalText(regularCreditTotalMinor)
        textRechargeTotal.text = sectionTotal(rechargeAssets)
        textInvestmentTotal.text = sectionTotal(investmentAssets)
        textReceivableTotal.text = sectionTotal(receivableAssets)
        textPayableTotal.text = sectionTotal(payableAssets)
        setAdapterAmountsVisibility()
        layoutFundsSection.visibility = if (coreFundAssets.isEmpty()) View.GONE else View.VISIBLE
        layoutCreditSection.visibility = if (creditAssets.isEmpty()) View.GONE else View.VISIBLE
        layoutRechargeSection.visibility = if (rechargeAssets.isEmpty()) View.GONE else View.VISIBLE
        layoutInvestmentSection.visibility = if (investmentAssets.isEmpty()) View.GONE else View.VISIBLE
        layoutReceivableSection.visibility = if (receivableAssets.isEmpty()) View.GONE else View.VISIBLE
        layoutPayableSection.visibility = if (payableAssets.isEmpty()) View.GONE else View.VISIBLE

        // Pinned assets live in their own card. They must not be counted or
        // visually mixed into the normal account list.
        cardPinnedAssets.visibility = if (pinnedAssets.isEmpty()) View.GONE else View.VISIBLE
        if (pinnedAssets.isNotEmpty()) {
            // Recreate and swap the adapter on every resume. Reusing an
            // adapter after returning from a child page can leave the nested
            // RecyclerView measured but without rebound child views.
            pinnedAdapter = createAssetAdapter(pinnedAssets)
            recyclerPinnedAssets.swapAdapter(pinnedAdapter, true)
            recyclerPinnedAssets.visibility = View.VISIBLE
        } else {
            pinnedAdapter = null
            recyclerPinnedAssets.adapter = null
            recyclerPinnedAssets.visibility = View.GONE
        }

        if (coreFundAssets.isEmpty()) {
            cardAllAssets.visibility = if (assets.isEmpty()) View.VISIBLE else View.GONE
            textEmpty.visibility = if (assets.isEmpty()) View.VISIBLE else View.GONE
            recyclerAssets.visibility = View.GONE
            adapter = null
            recyclerAssets.adapter = null
        } else {
            cardAllAssets.visibility = View.VISIBLE
            textEmpty.visibility = View.GONE
            recyclerAssets.visibility = View.VISIBLE

            if (adapter == null) {
                adapter = createAssetAdapter(coreFundAssets)
                recyclerAssets.adapter = adapter
            } else {
                adapter?.updateAssets(coreFundAssets)
                if (recyclerAssets.adapter == null) {
                    recyclerAssets.adapter = adapter
                }
            }
        }

        bindAdditionalSection(R.id.layout_recharge_section, R.id.recycler_recharge_assets, rechargeAssets)
        bindAdditionalSection(R.id.layout_investment_section, R.id.recycler_investment_assets, investmentAssets)
        bindAdditionalSection(R.id.layout_receivable_section, R.id.recycler_receivable_assets, receivableAssets)
        bindAdditionalSection(R.id.layout_payable_section, R.id.recycler_payable_assets, payableAssets)

        if (creditAssets.isEmpty()) {
            creditAdapter = null
            recyclerCreditAssets.adapter = null
        } else if (creditAdapter == null) {
            creditAdapter = createAssetAdapter(creditAssets)
            recyclerCreditAssets.adapter = creditAdapter
        } else {
            creditAdapter?.updateAssets(creditAssets)
            if (recyclerCreditAssets.adapter == null) recyclerCreditAssets.adapter = creditAdapter
        }
    }

    private fun isCreditAsset(asset: Asset): Boolean {
        return asset.categoryLabel in setOf("信用卡", "花呗", "白条", "借呗", "其他信用")
    }

    private val rechargeLabels = setOf("交通卡", "饭卡", "话费", "会员卡", "押金", "其他充值卡")
    private val investmentLabels = setOf("股票", "基金", "黄金", "其他理财")
    private val receivableLabels = setOf("借出", "其他应收")
    private val payableLabels = setOf("借入", "其他应付")

    private fun sectionTotal(assets: List<Asset>): String {
        return totalText(assets.sumOf(::amountMinor))
    }

    private fun amountMinor(asset: Asset): Long = Money.toMinor(asset.amount) ?: 0L

    private fun moneyText(amountMinor: Long): String =
        if (amountsVisible) "¥${Money.formatYuan(amountMinor)}" else "***"

    private fun totalText(amountMinor: Long): String =
        if (amountsVisible) "共计:¥${Money.formatYuan(amountMinor)}" else "共计:***"

    private fun setAdapterAmountsVisibility() {
        adapter?.setAmountsVisible(amountsVisible)
        pinnedAdapter?.setAmountsVisible(amountsVisible)
        creditAdapter?.setAmountsVisible(amountsVisible)
        additionalAdapters.values.forEach { it.setAmountsVisible(amountsVisible) }
    }

    private fun updateVisibilityToggle() {
        if (!::btnToggleAssetVisibility.isInitialized) return
        btnToggleAssetVisibility.setImageResource(
            if (amountsVisible) R.drawable.tabler_eye
            else R.drawable.tabler_eye_off
        )
        btnToggleAssetVisibility.contentDescription = if (amountsVisible) {
            "隐藏资产余额"
        } else {
            "显示资产余额"
        }
    }

    companion object {
        private const val KEY_AMOUNTS_VISIBLE = "asset_amounts_visible"
        private const val ARG_PICKER_MODE = "asset_picker_mode"
        private const val ARG_EXCLUDED_ASSET_ID = "excluded_asset_id"
        private const val ARG_SELECTED_ASSET_ID = "selected_asset_id"
        private const val ARG_PICKER_LEDGER_ID = "picker_ledger_id"
        private const val ARG_ALLOW_NO_ASSET = "allow_no_asset"

        fun newPickerInstance(excludedAssetId: Long?, selectedAssetId: Long?, ledgerId: Long? = null,
                              allowNoAsset: Boolean = false): AssetFragment =
            AssetFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_PICKER_MODE, true)
                    putBoolean(ARG_ALLOW_NO_ASSET, allowNoAsset)
                    excludedAssetId?.let { putLong(ARG_EXCLUDED_ASSET_ID, it) }
                    selectedAssetId?.let { putLong(ARG_SELECTED_ASSET_ID, it) }
                    ledgerId?.let { putLong(ARG_PICKER_LEDGER_ID, it) }
                }
            }
    }

    private fun bindAdditionalSection(sectionId: Int, recyclerId: Int, assets: List<Asset>) {
        val recycler = contentView.findViewById<RecyclerView>(recyclerId)
        if (assets.isEmpty()) {
            recycler.adapter = null
            additionalAdapters.remove(sectionId)
            return
        }
        val existing = additionalAdapters[sectionId]
        if (existing == null) {
            val created = createAssetAdapter(assets)
            additionalAdapters[sectionId] = created
            recycler.adapter = created
        } else {
            existing.updateAssets(assets)
            if (recycler.adapter == null) recycler.adapter = existing
        }
    }

    private fun attachAssetDragSorting(recycler: RecyclerView) {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            private var moved = false

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                moved = (recyclerView.adapter as? AssetAdapter)?.moveAsset(from, to) == true || moved
                return moved
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                if (moved) {
                    (recyclerView.adapter as? AssetAdapter)?.assetIdsInOrder()?.let(databaseHelper::updateAssetSortOrder)
                    moved = false
                }
            }
        }).attachToRecyclerView(recycler)
    }

    private fun createAssetAdapter(assets: List<Asset>): AssetAdapter {
        return AssetAdapter(assets, object : AssetAdapter.OnAssetActionListener {
            override fun onClick(asset: Asset) {
                if (pickerMode) {
                    (parentFragment as? AssetSelectionHost)?.onAssetSelected(asset)
                    return
                }
                val detailFragment = AssetRecordsFragment.newInstance(
                    asset.name, asset.amount, asset.type, asset.id, asset.categoryLabel
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit()
            }

            override fun onEdit(asset: Asset) {
                val editFragment = AddAssetFragment.newEditInstance(asset.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit()
            }

            override fun onDelete(asset: Asset) {
                databaseHelper.setAssetPinned(asset.id, !asset.isPinned)
                Toast.makeText(requireContext(), if (asset.isPinned) "已取消置顶" else "已置顶", Toast.LENGTH_SHORT).show()
                loadAssets()
            }

            override fun onArchive(asset: Asset) {
                databaseHelper.archiveAsset(asset.id)
                Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
                loadAssets()
            }
        }, pickerMode = pickerMode, selectedAssetId = selectedAssetId).also {
            it.setAmountsVisible(amountsVisible)
        }
    }

    private fun configurePickerMode(view: View) {
        view.findViewById<View>(R.id.card_asset_summary).visibility = View.GONE
        view.findViewById<View>(R.id.fab_add).visibility = View.GONE
        view.findViewById<TextView>(R.id.text_title).setText(R.string.record_asset_sheet_title)
        btnArchive.setImageResource(R.drawable.ic_close)
        btnArchive.contentDescription = getString(R.string.dialog_cancel)
        btnArchive.setOnClickListener {
            (parentFragment as? AssetSelectionHost)?.onAssetSelectionClosed()
        }
        configureNoAssetRow(view)
        view.findViewById<androidx.core.widget.NestedScrollView>(R.id.asset_content_scroll).apply {
            isNestedScrollingEnabled = true
            setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
        }
        view.findViewById<View>(R.id.asset_page_content).setPadding(
            (16 * resources.displayMetrics.density).toInt(),
            0,
            (16 * resources.displayMetrics.density).toInt(),
            0
        )
    }

    private fun configureNoAssetRow(view: View) {
        if (arguments?.getBoolean(ARG_ALLOW_NO_ASSET, false) != true) return
        view.findViewById<View>(R.id.card_record_asset_none).visibility = View.VISIBLE
        // Use the actual asset-row layout without creating a placeholder asset or category.
        val row = view.findViewById<View>(R.id.button_record_asset_none)
        val holder = AssetAdapter.AssetViewHolder(row)
        val selected = selectedAssetId == null
        holder.textAssetName.setText(R.string.record_asset_no_selection)
        holder.textAssetType.visibility = View.GONE
        holder.textAssetAmount.visibility = View.GONE
        holder.archivedActions.visibility = View.GONE
        holder.imageAssetIcon.setImageResource(R.drawable.tabler_wallet_off)
        holder.imageAssetIcon.setColorFilter(ThemeColorHelper.resolveColor(requireContext(), com.google.android.material.R.attr.colorPrimary))
        holder.iconContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.surface_container_low)
        )
        holder.iconAssetPinned.apply {
            visibility = if (selected) View.VISIBLE else View.GONE
            setImageResource(R.drawable.tabler_check)
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        holder.cardContent.apply {
            setCardBackgroundColor(if (selected) {
                ThemeColorHelper.resolveThemeAwareResource(requireContext(), R.color.primaryContainer_light)
            } else {
                android.graphics.Color.TRANSPARENT
            })
            isSelected = selected
        }
        row.isSelected = selected
        row.isFocusable = true
        row.setOnClickListener { (parentFragment as? AssetSelectionHost)?.onNoAssetSelected() }
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("添加资产")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_asset, null)
        builder.setView(view)

        val editName = view.findViewById<EditText>(R.id.edit_asset_name)
        val editAmount = view.findViewById<EditText>(R.id.edit_asset_amount)
        val radioGroupType = view.findViewById<RadioGroup>(R.id.radio_group_asset_type)

        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            val amountStr = editAmount.text.toString().trim()
            val type = when (radioGroupType.checkedRadioButtonId) {
                R.id.radio_cash -> 0
                R.id.radio_bank -> 1
                R.id.radio_alipay -> 2
                R.id.radio_wechat -> 3
                else -> 4
            }

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入资产名称", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = Money.evaluateYuanExpression(amountStr)?.let(Money::toMajorDouble) ?: run {
                Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val asset = Asset(name = name, amount = amount, type = type)
            val id = databaseHelper.addAsset(asset)
            if (id != -1L) {
                Toast.makeText(requireContext(), "添加成功", Toast.LENGTH_SHORT).show()
                loadAssets()
            } else {
                Toast.makeText(requireContext(), "添加失败", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showEditDialog(asset: Asset) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("编辑资产")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_asset, null)
        builder.setView(view)

        val editName = view.findViewById<EditText>(R.id.edit_asset_name)
        val editAmount = view.findViewById<EditText>(R.id.edit_asset_amount)
        val radioGroupType = view.findViewById<RadioGroup>(R.id.radio_group_asset_type)

        editName.setText(asset.name)
        editAmount.setText(Money.formatYuan(asset.amount))

        when (asset.type) {
            0 -> view.findViewById<RadioButton>(R.id.radio_cash).isChecked = true
            1 -> view.findViewById<RadioButton>(R.id.radio_bank).isChecked = true
            2 -> view.findViewById<RadioButton>(R.id.radio_alipay).isChecked = true
            3 -> view.findViewById<RadioButton>(R.id.radio_wechat).isChecked = true
            else -> view.findViewById<RadioButton>(R.id.radio_other).isChecked = true
        }

        builder.setPositiveButton("确定") { _, _ ->
            val name = editName.text.toString().trim()
            val amountStr = editAmount.text.toString().trim()
            val type = when (radioGroupType.checkedRadioButtonId) {
                R.id.radio_cash -> 0
                R.id.radio_bank -> 1
                R.id.radio_alipay -> 2
                R.id.radio_wechat -> 3
                else -> 4
            }

            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "请输入资产名称", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            val amount = Money.evaluateYuanExpression(amountStr)?.let(Money::toMajorDouble) ?: run {
                Toast.makeText(requireContext(), "请输入有效的金额", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }

            asset.name = name
            asset.amount = amount
            asset.type = type

            val rowsAffected = databaseHelper.updateAsset(asset)
            if (rowsAffected > 0) {
                Toast.makeText(requireContext(), "更新成功", Toast.LENGTH_SHORT).show()
                loadAssets()
            } else {
                Toast.makeText(requireContext(), "更新失败", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("取消", null)

        builder.show()
    }

    private fun showDeleteDialog(asset: Asset) {
        AlertDialog.Builder(requireContext())
            .setTitle("归档资产")
            .setMessage("确定要归档\"${asset.name}\"吗？")
            .setPositiveButton("确定") { _, _ ->
                databaseHelper.archiveAsset(asset.id)
                Toast.makeText(requireContext(), "已归档", Toast.LENGTH_SHORT).show()
                loadAssets()
            }
            .setNegativeButton("取消", null)
            .show()
    }

}
