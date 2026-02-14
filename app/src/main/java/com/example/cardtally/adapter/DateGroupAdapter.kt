package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.DateGroup
import com.example.cardtally.model.Record
import com.example.cardtally.util.SwipeToEditDeleteHelper

class DateGroupAdapter(
    private var dateGroups: List<DateGroup>,
    private val listener: OnRecordActionListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_RECORD = 1
    }

    interface OnRecordActionListener {
        fun onEdit(record: Record)
        fun onDelete(record: Record)
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0
        for (dateGroup in dateGroups) {
            if (position == currentPosition) {
                return TYPE_DATE_HEADER
            }
            currentPosition++
            if (position < currentPosition + dateGroup.records.size) {
                return TYPE_RECORD
            }
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
                (holder as RecordViewHolder).bind(record, listener)
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

    class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textDate: TextView = itemView.findViewById(R.id.text_date)

        fun bind(date: String) {
            textDate.text = date
        }
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardContent: CardView = itemView.findViewById(R.id.card_content)
        private val layoutActions: View = itemView.findViewById(R.id.layout_actions)
        private val textCategory: TextView = itemView.findViewById(R.id.text_category)
        private val textDescription: TextView = itemView.findViewById(R.id.text_description)
        private val textAssetSource: TextView = itemView.findViewById(R.id.text_asset_source)
        private val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        private val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)

        private var swipeHelper: SwipeToEditDeleteHelper? = null

        fun bind(record: Record, listener: OnRecordActionListener) {
            textCategory.text = record.category

            if (!record.description.isNullOrEmpty()) {
                textDescription.text = record.description
                textDescription.visibility = View.VISIBLE
                textCategory.layoutParams = (textCategory.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    topMargin = 0
                }
            } else {
                textDescription.visibility = View.GONE
                textCategory.layoutParams = (textCategory.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    topMargin = 0
                }
            }

            if (!record.assetSource.isNullOrEmpty()) {
                textAssetSource.text = record.assetSource
                textAssetSource.visibility = View.VISIBLE
            } else {
                textAssetSource.visibility = View.GONE
            }

            val amountText = if (record.type == 0) {
                textAmount.setTextColor(0xFFF44336.toInt())
                String.format("-%.2f", record.amount)
            } else {
                textAmount.setTextColor(0xFF4CAF50.toInt())
                String.format("+%.2f", record.amount)
            }
            textAmount.text = amountText

            swipeHelper = SwipeToEditDeleteHelper(
                cardContent,
                layoutActions,
                onEdit = { listener.onEdit(record) },
                onDelete = { listener.onDelete(record) }
            )
        }
    }
}
