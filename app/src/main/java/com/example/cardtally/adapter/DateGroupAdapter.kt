package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money
import com.example.cardtally.util.LedgerDisplayHelper
import com.example.cardtally.util.TablerIconCatalog
import com.example.cardtally.util.ThemeColorHelper
import com.example.cardtally.util.IncomeExpenseColorScheme

class DateGroupAdapter(
    private var dateGroups: List<DateGroup>,
    private val listener: OnRecordActionListener,
    private val categoryIconsById: Map<Long, String> = emptyMap(),
    private val categoryIconsByName: Map<String, String> = emptyMap(),
    private val showTypeSubtitle: Boolean = true,
    /**
     * When false the asset column keeps the *ledger* provenance label instead of
     * the asset/transfer route. Only the asset-detail history uses this, so other
     * bill and search pages keep their existing asset label semantics.
     */
    private val showAssetRoute: Boolean = true
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_RECORD = 1
    }

    interface OnRecordActionListener {
        fun onOpenDetails(record: Record)
        fun onEdit(record: Record)
        fun onDelete(record: Record)
        fun onMultiSelectChanged(selectedCount: Int)
        fun onDeleteSelected(records: List<Record>)
        fun onEnterMultiSelectMode(record: Record)
        fun onToggleMultiSelect(record: Record)
    }

    private var isMultiSelect = false
    private val selectedRecords = mutableSetOf<Record>()

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0
        for (dateGroup in dateGroups) {
            if (position == currentPosition) return TYPE_DATE_HEADER
            currentPosition++
            if (position < currentPosition + dateGroup.records.size) return TYPE_RECORD
            currentPosition += dateGroup.records.size
        }
        return TYPE_RECORD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_date_header, parent, false)
                DateHeaderViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_record, parent, false)
                RecordViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = 0
        for (dateGroup in dateGroups) {
            if (position == currentPosition) {
                (holder as DateHeaderViewHolder).bind(dateGroup)
                return
            }
            currentPosition++
            if (position < currentPosition + dateGroup.records.size) {
                val recordIndex = position - currentPosition
                val record = dateGroup.records[recordIndex]
                (holder as RecordViewHolder).bind(
                    record,
                    listener,
                    isMultiSelect,
                    selectedRecords.contains(record),
                    categoryIconsById = categoryIconsById,
                    categoryIconsByName = categoryIconsByName,
                    showTypeSubtitle = showTypeSubtitle,
                    showAssetRoute = showAssetRoute,
                    showDivider = recordIndex < dateGroup.records.lastIndex,
                    roundTopCorners = recordIndex == 0,
                    roundBottomCorners = recordIndex == dateGroup.records.lastIndex
                )
                return
            }
            currentPosition += dateGroup.records.size
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        for (dateGroup in dateGroups) {
            count += 1 + dateGroup.records.size
        }
        return count
    }

    fun updateDateGroups(newDateGroups: List<DateGroup>) {
        dateGroups = newDateGroups
        notifyDataSetChanged()
    }

    fun getDateGroups(): List<DateGroup> = dateGroups

    fun moveItem(fromPosition: Int, toPosition: Int) {
        var fromCurrentPosition = 0
        var fromDateGroupIndex = -1
        var fromRecordIndex = -1

        for (i in dateGroups.indices) {
            if (fromPosition == fromCurrentPosition) return
            fromCurrentPosition++
            if (fromPosition < fromCurrentPosition + dateGroups[i].records.size) {
                fromDateGroupIndex = i
                fromRecordIndex = fromPosition - fromCurrentPosition
                break
            }
            fromCurrentPosition += dateGroups[i].records.size
        }

        var toCurrentPosition = 0
        var toDateGroupIndex = -1
        var toRecordIndex = -1

        for (i in dateGroups.indices) {
            if (toPosition == toCurrentPosition) return
            toCurrentPosition++
            if (toPosition < toCurrentPosition + dateGroups[i].records.size) {
                toDateGroupIndex = i
                toRecordIndex = toPosition - toCurrentPosition
                break
            }
            toCurrentPosition += dateGroups[i].records.size
        }

        if (fromDateGroupIndex == -1 || toDateGroupIndex == -1) return
        if (fromDateGroupIndex != toDateGroupIndex) return

        val dateGroup = dateGroups[fromDateGroupIndex]
        val records = dateGroup.records.toMutableList()
        val record = records.removeAt(fromRecordIndex)
        records.add(toRecordIndex, record)

        for (i in records.indices) {
            records[i].sortOrder = i
        }

        val newDateGroup = DateGroup(dateGroup.date, records)
        val newDateGroups = dateGroups.toMutableList()
        newDateGroups[fromDateGroupIndex] = newDateGroup
        dateGroups = newDateGroups

        notifyItemMoved(fromPosition, toPosition)
    }

    fun getDateForPosition(position: Int): String? {
        var currentPosition = 0
        for (dateGroup in dateGroups) {
            if (position == currentPosition) return null
            currentPosition++
            if (position < currentPosition + dateGroup.records.size) return dateGroup.date
            currentPosition += dateGroup.records.size
        }
        return null
    }

    fun getRecordAtPosition(position: Int): Record? {
        var currentPosition = 0
        for (dateGroup in dateGroups) {
            if (position == currentPosition) return null
            currentPosition++
            if (position < currentPosition + dateGroup.records.size) {
                val recordIndex = position - currentPosition
                return dateGroup.records[recordIndex]
            }
            currentPosition += dateGroup.records.size
        }
        return null
    }

    fun isMultiSelectMode(): Boolean = isMultiSelect

    fun toggleMultiSelect(record: Record) {
        if (selectedRecords.contains(record)) {
            selectedRecords.remove(record)
        } else {
            selectedRecords.add(record)
        }

        if (selectedRecords.isEmpty()) {
            isMultiSelect = false
        }

        listener.onMultiSelectChanged(selectedRecords.size)
        notifyDataSetChanged()
    }

    fun enterMultiSelectMode(record: Record) {
        isMultiSelect = true
        selectedRecords.clear()
        selectedRecords.add(record)
        listener.onMultiSelectChanged(selectedRecords.size)
        notifyDataSetChanged()
    }

    fun exitMultiSelectMode() {
        isMultiSelect = false
        selectedRecords.clear()
        listener.onMultiSelectChanged(0)
        notifyDataSetChanged()
    }

    fun getSelectedRecords(): List<Record> = selectedRecords.toList()

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.text_date)
        private val textDateTotal: TextView = itemView.findViewById(R.id.text_date_total)

        fun bind(dateGroup: DateGroup) {
            textDate.text = LedgerDisplayHelper.formatDateHeader(dateGroup.date)
            val expense = dateGroup.records.filter { it.type == 0 }.sumOf { it.amount }
            val income = dateGroup.records.filter { it.type == 1 }.sumOf { it.amount }
            textDateTotal.text = itemView.context.getString(
                R.string.ledger_day_income_expense,
                expense,
                income
            )
        }
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContent: LinearLayout = itemView.findViewById(R.id.card_content)
        private val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        private val viewIcon: View = itemView.findViewById(R.id.view_icon)
        private val imageIcon: ImageView = itemView.findViewById(R.id.image_icon)
        private val textCategory: TextView = itemView.findViewById(R.id.text_category)
        private val textTime: TextView = itemView.findViewById(R.id.text_time)
        private val textDescription: TextView = itemView.findViewById(R.id.text_description)
        private val textAsset: TextView = itemView.findViewById(R.id.text_asset)
        private val textFee: TextView = itemView.findViewById(R.id.text_fee)
        private val textLedger: TextView = itemView.findViewById(R.id.text_ledger)
        private val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        private val recordDivider: View = itemView.findViewById(R.id.view_record_divider)

        private val rowLayoutController = RecordRowLayoutController(itemView)
        private var currentRecord: Record? = null
        private val categoryIcons = mapOf(
            "餐饮" to R.drawable.ic_category_food,
            "交通" to R.drawable.ic_category_transport,
            "购物" to R.drawable.ic_category_shopping,
            "娱乐" to R.drawable.ic_category_entertainment,
            "医疗" to R.drawable.ic_category_medical,
            "教育" to R.drawable.ic_category_education,
            "住房" to R.drawable.ic_category_housing,
            "工资" to R.drawable.ic_category_salary,
            "奖金" to R.drawable.ic_category_bonus,
            "其他" to R.drawable.ic_category_other
        )

        fun bind(
            record: Record,
            listener: OnRecordActionListener,
            isMultiSelect: Boolean,
            isSelected: Boolean,
            categoryIconsById: Map<Long, String> = emptyMap(),
            categoryIconsByName: Map<String, String> = emptyMap(),
            showTypeSubtitle: Boolean = true,
            showAssetRoute: Boolean = true,
            showDivider: Boolean = false,
            roundTopCorners: Boolean = false,
            roundBottomCorners: Boolean = false,
            parentProvidesBackground: Boolean = false
        ) {
            currentRecord = record
            recordDivider.visibility = if (showDivider) View.VISIBLE else View.GONE
            val transferRoute = listOfNotNull(
                record.assetSource?.takeIf { it.isNotBlank() },
                record.destinationAssetSource?.takeIf { it.isNotBlank() }
            ).joinToString(" → ")
            textCategory.text = if (record.type == 2) {
                transferRoute.ifBlank { itemView.context.getString(R.string.record_type_transfer) }
            } else {
                record.categoryPathSnapshot?.takeIf { it.isNotBlank() }
                    ?: record.categoryNameSnapshot?.takeIf { it.isNotBlank() }
                    ?: record.category
            }
            textTime.text = buildSubtitle(record)
            textTime.visibility = if (showTypeSubtitle) View.VISIBLE else View.GONE
            textAsset.text = if (record.type == 2) {
                ""
            } else {
                record.assetSource?.takeIf { it.isNotBlank() }.orEmpty()
            }
            textAsset.visibility = if (record.type != 2 && showAssetRoute && !textAsset.text.isNullOrBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            if (record.type == 2 && record.fee > 0L) {
                textFee.text = itemView.context.getString(
                    R.string.record_fee_included,
                    "¥${Money.formatYuan(record.fee)}"
                )
                textFee.visibility = View.VISIBLE
            } else {
                textFee.visibility = View.GONE
            }
            // Asset-detail mode shows which ledger each record belongs to; other
            // pages keep the asset/route label untouched.
            textLedger.text = record.ledgerName?.takeIf { it.isNotBlank() }.orEmpty()
            textLedger.visibility = if (!showAssetRoute && textLedger.text.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
            val context = itemView.context
            val idIconName = record.categoryId?.let(categoryIconsById::get)
            val nameIconName = record.categoryNameSnapshot?.let(categoryIconsByName::get)
                ?: categoryIconsByName[record.category]
            val iconName = idIconName
                ?.takeUnless { it == "tabler_category" || it == "ic_category_other" }
                ?: nameIconName
            val iconResource = if (record.type == 2) {
                R.drawable.tabler_transfer
            } else iconName?.let { TablerIconCatalog.resourceId(context, it) }
                ?.takeIf { it != 0 }
                ?: categoryIcons[record.category]
                ?: R.drawable.ic_category_other
            imageIcon.setImageResource(iconResource)

            textDescription.text = record.description?.takeIf { it.isNotBlank() }.orEmpty()
            textDescription.visibility = if (textDescription.text.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            viewIcon.backgroundTintList = ColorStateList.valueOf(
                if (record.type == 2) {
                    ThemeColorHelper.resolveThemeAwareResource(context, R.color.warning_container)
                } else if (record.type == 1) {
                    IncomeExpenseColorScheme.incomeContainer(context)
                } else {
                    IncomeExpenseColorScheme.expenseContainer(context)
                }
            )

            val amountText = if (record.type == 2) {
                textAmount.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.onSurface_light))
                "¥${Money.formatYuan(record.amount)}"
            } else if (record.type == 0) {
                textAmount.setTextColor(IncomeExpenseColorScheme.expensePrimary(context))
                "-¥${Money.formatYuan(record.amount)}"
            } else {
                textAmount.setTextColor(IncomeExpenseColorScheme.incomePrimary(context))
                "+¥${Money.formatYuan(record.amount)}"
            }
            textAmount.text = amountText
            // Long amounts or enlarged fonts must not squeeze the name: the row
            // re-applies its fitting rules on every layout (first bind, recycle,
            // font or orientation change).
            rowLayoutController.attach()
            rowLayoutController.applyLayout()

            layoutActions.visibility = View.GONE
            cardContent.setOnTouchListener(null)
            cardContent.translationX = 0f
            cardContent.setOnClickListener(null)
            cardContent.setOnLongClickListener(null)
            itemView.setOnClickListener(null)
            itemView.setOnLongClickListener(null)

            // Date-group cards already paint the neutral fill; adding it again compounds opacity.
            // Recompute on every bind so reuse by a standalone list restores its own background.
            val neutralCardColor = if (parentProvidesBackground) android.graphics.Color.TRANSPARENT
                else ThemeColorHelper.resolveCardSurface(context)
            if (isMultiSelect) {
                layoutActions.visibility = View.GONE

                if (isSelected) {
                    setCardBackground(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer),
                        roundTopCorners,
                        roundBottomCorners
                    )
                } else {
                    setCardBackground(
                        neutralCardColor,
                        roundTopCorners,
                        roundBottomCorners
                    )
                }

                cardContent.setOnClickListener {
                    currentRecord?.let { listener.onToggleMultiSelect(it) }
                }
            } else {
                setCardBackground(
                    neutralCardColor,
                    roundTopCorners,
                    roundBottomCorners
                )

                cardContent.setOnClickListener { listener.onOpenDetails(record) }

                cardContent.setOnLongClickListener {
                    currentRecord?.let { listener.onEnterMultiSelectMode(it) }
                    true
                }
            }
        }

        private fun buildSubtitle(record: Record): String {
            return if (record.type == 2) {
                "转账"
            } else if (record.type == 1) {
                itemView.context.getString(R.string.record_type_income)
            } else {
                itemView.context.getString(R.string.record_type_expense)
            }
        }

        private fun setCardBackground(color: Int, roundTop: Boolean, roundBottom: Boolean) {
            val radius = 12f * itemView.resources.displayMetrics.density
            val topLeft = if (roundTop) radius else 0f
            val topRight = if (roundTop) radius else 0f
            val bottomRight = if (roundBottom) radius else 0f
            val bottomLeft = if (roundBottom) radius else 0f
            cardContent.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(color)
                cornerRadii = floatArrayOf(
                    topLeft, topLeft,
                    topRight, topRight,
                    bottomRight, bottomRight,
                    bottomLeft, bottomLeft
                )
            }
        }
    }
}
