package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Record
import com.example.cardtally.util.Money

class RecordAdapter(
    private var records: List<Record>,
    private val listener: OnRecordActionListener? = null
) : RecyclerView.Adapter<RecordAdapter.RecordViewHolder>() {

    interface OnRecordActionListener {
        fun onEdit(record: Record)
        fun onDelete(record: Record)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val record = records[position]
        holder.textDate.text = record.date
        holder.textCategory.text = if (record.type == 2) {
            listOfNotNull(record.assetSource, record.destinationAssetSource).joinToString(" → ")
        } else record.category

        val amountText = if (record.type == 2) {
            holder.textAmount.setTextColor(0xFF444444.toInt())
            Money.formatYuan(record.amount)
        } else if (record.type == 0) {
            holder.textAmount.setTextColor(0xFFF44336.toInt())
            "-${Money.formatYuan(record.amount)}"
        } else {
            holder.textAmount.setTextColor(0xFF4CAF50.toInt())
            "+${Money.formatYuan(record.amount)}"
        }
        holder.textAmount.text = amountText

        if (record.type == 2 && record.fee > 0L) {
            holder.textFee.text = holder.itemView.context.getString(
                R.string.record_fee_included,
                "¥${Money.formatYuan(record.fee)}"
            )
            holder.textFee.visibility = View.VISIBLE
        } else {
            holder.textFee.visibility = View.GONE
        }

        if (!record.description.isNullOrEmpty()) {
            holder.textDescription.text = record.description
            holder.textDescription.visibility = View.VISIBLE
        } else {
            holder.textDescription.visibility = View.GONE
        }

        holder.btnEdit.setOnClickListener {
            listener?.onEdit(record)
        }

        holder.btnDelete.setOnClickListener {
            listener?.onDelete(record)
        }
    }

    override fun getItemCount(): Int = records.size

    fun updateRecords(newRecords: List<Record>) {
        records = newRecords
        notifyDataSetChanged()
    }

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textDate: TextView = itemView.findViewById(R.id.text_date)
        val textCategory: TextView = itemView.findViewById(R.id.text_category)
        val textDescription: TextView = itemView.findViewById(R.id.text_description)
        val textAmount: TextView = itemView.findViewById(R.id.text_amount)
        val textFee: TextView = itemView.findViewById(R.id.text_fee)
        val btnEdit: Button = itemView.findViewById(R.id.btn_edit)
        val btnDelete: Button = itemView.findViewById(R.id.btn_delete)
    }
}
