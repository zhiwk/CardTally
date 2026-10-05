package com.example.cardtally

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AssetAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.util.Money
import com.example.cardtally.model.Asset
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class ArchivedAssetsFragment : Fragment() {
    private lateinit var sectionsContainer: LinearLayout
    private lateinit var textEmpty: TextView
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_archived_assets, container, false)

        sectionsContainer = view.findViewById(R.id.archived_sections)
        textEmpty = view.findViewById(R.id.text_empty)
        view.findViewById<View>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        databaseHelper = DatabaseHelper(requireContext())

        loadAssets()

        return view
    }

    override fun onResume() {
        super.onResume()
        loadAssets()
        hideBottomNav()
    }

    override fun onPause() {
        super.onPause()
        showBottomNav()
    }

    private fun loadAssets() {
        val assets = databaseHelper.getArchivedAssets()
        if (assets.isEmpty()) {
            textEmpty.visibility = View.VISIBLE
            sectionsContainer.visibility = View.GONE
            sectionsContainer.removeAllViews()
        } else {
            textEmpty.visibility = View.GONE
            sectionsContainer.visibility = View.VISIBLE
            renderSections(assets)
        }
    }

    private fun renderSections(assets: List<Asset>) {
        val regularAssets = assets.filterNot(::isCreditAsset)
        val sections = listOf(
            R.string.asset_account_cash to regularAssets.filter {
                it.categoryLabel !in rechargeLabels &&
                    it.categoryLabel !in investmentLabels &&
                    it.categoryLabel !in receivableLabels &&
                    it.categoryLabel !in payableLabels
            },
            R.string.asset_account_credit to assets.filter(::isCreditAsset),
            R.string.asset_account_topup to assets.filter { it.categoryLabel in rechargeLabels },
            R.string.asset_account_investment to assets.filter { it.categoryLabel in investmentLabels },
            R.string.asset_account_receivable to assets.filter { it.categoryLabel in receivableLabels },
            R.string.asset_account_payable to assets.filter { it.categoryLabel in payableLabels }
        )

        sectionsContainer.removeAllViews()
        sections.filter { it.second.isNotEmpty() }.forEach { (titleRes, sectionAssets) ->
            val section = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = 12.dp
                }
            }
            val header = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = 40.dp
            }
            header.addView(TextView(requireContext()).apply {
                text = getString(titleRes)
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.onBackground_light))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })
            header.addView(TextView(requireContext()).apply {
                text = getString(R.string.asset_section_total, Money.formatYuan(sectionAssets.sumOf(::amountMinor)))
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.onSurfaceVariant_light))
            })
            section.addView(header)

            val card = MaterialCardView(requireContext()).apply {
                radius = 12.dp.toFloat()
                cardElevation = 0f
                strokeWidth = 0
                setStrokeColor(Color.TRANSPARENT)
                setCardBackgroundColor(com.example.cardtally.util.ThemeColorHelper.resolveCardSurface(requireContext()))
                layoutParams = LinearLayout.LayoutParams(-1, -2)
            }
            val recycler = RecyclerView(requireContext()).apply {
                layoutManager = LinearLayoutManager(requireContext())
                isNestedScrollingEnabled = false
                adapter = createAdapter(sectionAssets)
            }
            card.addView(recycler)
            section.addView(card)
            sectionsContainer.addView(section)
        }
    }

    private fun createAdapter(assets: List<Asset>): AssetAdapter {
        return AssetAdapter(assets, object : AssetAdapter.OnAssetActionListener {
            override fun onClick(asset: Asset) = Unit
            override fun onEdit(asset: Asset) = Unit

            override fun onDelete(asset: Asset) {
                showDeleteDialog(asset)
            }

            override fun onArchive(asset: Asset) {
                databaseHelper.unarchiveAsset(asset.id)
                Toast.makeText(requireContext(), R.string.toast_asset_unarchived, Toast.LENGTH_SHORT).show()
                loadAssets()
            }
        }, archivedMode = true, showAmount = true)
    }

    private fun isCreditAsset(asset: Asset): Boolean {
        return asset.categoryLabel in setOf("信用卡", "花呗", "白条", "借呗", "其他信用")
    }

    private val rechargeLabels = setOf("交通卡", "饭卡", "话费", "会员卡", "押金", "其他充值卡")
    private val investmentLabels = setOf("股票", "基金", "黄金", "其他理财")
    private val receivableLabels = setOf("借出", "其他应收")
    private val payableLabels = setOf("借入", "其他应付")
    private fun amountMinor(asset: Asset): Long = Money.toMinor(asset.amount) ?: 0L

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private fun showEditDialog(asset: Asset) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("编辑资产")

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_asset, null)
        builder.setView(view)

        val editName = view.findViewById<android.widget.EditText>(R.id.edit_asset_name)
        val editAmount = view.findViewById<android.widget.EditText>(R.id.edit_asset_amount)
        val radioGroupType = view.findViewById<android.widget.RadioGroup>(R.id.radio_group_asset_type)

        editName.setText(asset.name)
        editAmount.setText(Money.formatYuan(asset.amount))

        when (asset.type) {
            0 -> view.findViewById<android.widget.RadioButton>(R.id.radio_cash).isChecked = true
            1 -> view.findViewById<android.widget.RadioButton>(R.id.radio_bank).isChecked = true
            2 -> view.findViewById<android.widget.RadioButton>(R.id.radio_alipay).isChecked = true
            3 -> view.findViewById<android.widget.RadioButton>(R.id.radio_wechat).isChecked = true
            else -> view.findViewById<android.widget.RadioButton>(R.id.radio_other).isChecked = true
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
            .setTitle("删除资产")
            .setMessage("确定要删除\"${asset.name}\"吗？")
            .setPositiveButton("确定") { _, _ ->
                try {
                    databaseHelper.deleteArchivedAsset(asset.id)
                    Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
                    loadAssets()
                } catch (error: DatabaseHelper.AssetOperationException) {
                    val message = when (error.error) {
                        DatabaseHelper.AssetOperationError.IN_USE_BY_RECORDS ->
                            "该资产仍有关联记录，无法永久删除"
                        DatabaseHelper.AssetOperationError.NOT_ARCHIVED ->
                            "仅已归档资产可以永久删除"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                }
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
