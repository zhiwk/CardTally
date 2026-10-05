package com.example.cardtally

import android.os.Bundle
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import java.text.Collator
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.state.AssetType
import com.example.cardtally.util.AssetTypeIconCatalog
import com.example.cardtally.util.BankIconCatalog
import com.example.cardtally.util.ThemeColorHelper

/** A local bank list shared by savings cards and credit cards. */
class BankSelectFragment : Fragment() {
    companion object {
        const val RESULT_KEY = "asset_bank_selection"
        const val RESULT_ICON_NAME = "bank_icon_name"
        private const val ARG_CATEGORY = "card_category"
        private const val ARG_RETURN_SELECTION = "return_selection"

        fun newInstance(category: String, returnSelection: Boolean = false) = BankSelectFragment().apply {
            arguments = bundleOf(ARG_CATEGORY to category, ARG_RETURN_SELECTION to returnSelection)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_bank_select, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val category = if (arguments?.getString(ARG_CATEGORY) == "信用卡") "信用卡" else "储蓄卡"
        view.findViewById<TextView>(R.id.text_title).setText(
            if (category == "信用卡") R.string.bank_select_credit else R.string.bank_select_savings
        )
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<RecyclerView>(R.id.recycler_banks).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = BankAdapter(requireContext()) { bank ->
                if (arguments?.getBoolean(ARG_RETURN_SELECTION) == true) {
                    parentFragmentManager.setFragmentResult(RESULT_KEY, bundleOf(RESULT_ICON_NAME to bank.iconName))
                    parentFragmentManager.popBackStack()
                } else {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AddAssetFragment.newInstance(AssetType.BANK, category, bank.iconName))
                        .addToBackStack("asset_add")
                        .commit()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        requireActivity().findViewById<View>(R.id.nav_shell)?.visibility = View.VISIBLE
    }

    private class BankAdapter(context: Context, private val onSelected: (BankIconCatalog.Bank) -> Unit) : RecyclerView.Adapter<RowHolder>() {
        private data class Row(val heading: String? = null, val bank: BankIconCatalog.Bank? = null)
        private val rows = buildList {
            add(Row(heading = "common"))
            BankIconCatalog.common.forEach { add(Row(bank = it)) }
            val collator = Collator.getInstance(Locale.CHINA)
            BankIconCatalog.banks.groupBy { it.section }.toSortedMap().forEach { (section, banks) ->
                add(Row(heading = section))
                banks.sortedWith { a, b -> collator.compare(context.getString(a.nameRes), context.getString(b.nameRes)) }
                    .forEach { add(Row(bank = it)) }
            }
            add(Row(heading = "*"))
            add(Row(bank = BankIconCatalog.other))
        }

        override fun getItemCount() = rows.size
        override fun getItemViewType(position: Int) = if (rows[position].bank == null) 0 else 1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowHolder = RowHolder(
            LayoutInflater.from(parent.context).inflate(
                if (viewType == 0) R.layout.item_bank_section else R.layout.item_bank_option, parent, false
            )
        )

        override fun onBindViewHolder(holder: RowHolder, position: Int) {
            val row = rows[position]
            val bank = row.bank
            val context = holder.itemView.context
            if (bank == null) {
                holder.itemView.findViewById<TextView>(R.id.text_section).text =
                    if (row.heading == "common") context.getString(R.string.bank_group_common) else row.heading
            } else {
                val radius = 12f * context.resources.displayMetrics.density
                val top = if (position == 0 || rows[position - 1].bank == null) radius else 0f
                val bottom = if (position == rows.lastIndex || rows[position + 1].bank == null) radius else 0f
                val surface = GradientDrawable().apply {
                    setColor(ThemeColorHelper.resolveCardSurface(context))
                    cornerRadii = floatArrayOf(top, top, top, top, bottom, bottom, bottom, bottom)
                }
                val mask = GradientDrawable().apply {
                    setColor(android.graphics.Color.WHITE)
                    cornerRadii = surface.cornerRadii
                }
                holder.itemView.background = RippleDrawable(
                    ColorStateList.valueOf(ThemeColorHelper.resolveThemeAwareResource(context, R.color.surface_container_low)),
                    surface, mask
                )
                holder.itemView.findViewById<TextView>(R.id.option_label).setText(bank.nameRes)
                AssetTypeIconCatalog.bindIcon(
                    holder.itemView.findViewById<ImageView>(R.id.option_icon), bank.iconRes, 36,
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorPrimary)
                )
                holder.itemView.setOnClickListener { onSelected(bank) }
            }
        }
    }

    private class RowHolder(view: View) : RecyclerView.ViewHolder(view)
}
