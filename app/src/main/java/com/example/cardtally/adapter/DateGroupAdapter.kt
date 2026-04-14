package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.SwipeToEditDeleteHelper
import com.example.cardtally.util.ThemeColorHelper

class DateGroupAdapter(
    private var dateGroups: List<DateGroup>,
    private val listener: OnRecordActionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_DATE_HEADER = 0
        const val TYPE_RECORD = 1
    }

    interface OnRecordActionListener {
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
                (holder as DateHeaderViewHolder).bind(dateGroup.date)
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
                    selectedRecords.contains(record)
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

        fun bind(date: String) {
            textDate.text = date
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
        private val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        private var swipeHelper: SwipeToEditDeleteHelper? = null
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
            isSelected: Boolean
        ) {
            currentRecord = record
            val categoryLabel = record.categoryPathSnapshot?.takeIf { it.isNotBlank() } ?: record.category
            textCategory.text = categoryLabel
            textTime.text = record.assetSource?.uppercase().orEmpty().ifEmpty { "CURATED ENTRY" }
            imageIcon.setImageResource(categoryIcons[record.category] ?: R.drawable.ic_category_other)

            if (!record.description.isNullOrEmpty()) {
                textDescription.text = record.description
                textDescription.visibility = View.VISIBLE
            } else {
                textDescription.visibility = View.GONE
            }

            val context = itemView.context
            val categoryColors = mapOf(
                "餐饮" to ThemeColorHelper.resolveThemeAwareResource(context, R.color.warning_container),
                "购物" to ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer),
                "工资" to ThemeColorHelper.resolveThemeAwareResource(context, R.color.success_container),
                "交通" to ThemeColorHelper.resolveThemeAwareResource(context, R.color.error_container),
                "住房" to ThemeColorHelper.resolveThemeAwareResource(context, R.color.info_container),
                "娱乐" to ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_surface_low)
            )
            viewIcon.backgroundTintList = ColorStateList.valueOf(
                categoryColors[record.category]
                    ?: ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_surface_low)
            )

            val amountText = if (record.type == 0) {
                textAmount.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.expense_primary))
                String.format("-¥%.2f", record.amount)
            } else {
                textAmount.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.income_primary))
                String.format("+¥%.2f", record.amount)
            }
            textAmount.text = amountText

            if (isMultiSelect) {
                layoutActions.visibility = View.GONE
                swipeHelper = null

                if (isSelected) {
                    cardContent.setBackgroundColor(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
                    )
                } else {
                    cardContent.setBackgroundColor(
                        ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSurface)
                    )
                }

                itemView.setOnClickListener {
                    currentRecord?.let { listener.onToggleMultiSelect(it) }
                }
            } else {
                cardContent.setBackgroundColor(
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSurface)
                )

                swipeHelper = SwipeToEditDeleteHelper(
                    cardContent,
                    layoutActions,
                    onEdit = { listener.onEdit(record) },
                    onDelete = { listener.onDelete(record) },
                    onClick = { listener.onEdit(record) }
                )

                itemView.setOnLongClickListener {
                    currentRecord?.let { listener.onEnterMultiSelectMode(it) }
                    true
                }
            }
        }
    }
}
