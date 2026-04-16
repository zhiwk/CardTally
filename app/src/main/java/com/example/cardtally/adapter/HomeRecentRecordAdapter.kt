package com.example.cardtally.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.Record
import com.example.cardtally.util.SwipeToEditDeleteHelper
import com.example.cardtally.util.ThemeColorHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeRecentRecordAdapter(
    private var records: List<Record>,
    private val listener: Listener
) : RecyclerView.Adapter<HomeRecentRecordAdapter.RecordViewHolder>() {

    interface Listener {
        fun onEdit(record: Record)
        fun onDelete(record: Record)
    }

    fun updateRecords(newRecords: List<Record>) {
        records = newRecords
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_recent_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position], listener)
    }

    override fun getItemCount(): Int = records.size

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val actionContainer: LinearLayout = itemView.findViewById(R.id.layout_actions)
        private val contentCard: View = itemView.findViewById(R.id.layout_content_card)
        private val iconContainer: View = itemView.findViewById(R.id.view_icon_container)
        private val iconView: ImageView = itemView.findViewById(R.id.image_icon)
        private val titleView: TextView = itemView.findViewById(R.id.text_title)
        private val subtitleView: TextView = itemView.findViewById(R.id.text_subtitle)
        private val amountView: TextView = itemView.findViewById(R.id.text_amount)
        private val deleteButton: View = itemView.findViewById(R.id.btn_delete)
        private var swipeHelper: SwipeToEditDeleteHelper? = null

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

        fun bind(record: Record, listener: Listener) {
            val context = itemView.context
            val title = record.description?.takeIf { it.isNotBlank() }
                ?: record.categoryNameSnapshot?.takeIf { it.isNotBlank() }
                ?: record.category
            val categoryLabel = record.categoryPathSnapshot?.takeIf { it.isNotBlank() }
                ?: record.category

            titleView.text = title
            subtitleView.text = buildSubtitle(record, categoryLabel)
            iconView.setImageResource(categoryIcons[record.category] ?: R.drawable.ic_category_other)
            iconContainer.backgroundTintList = ColorStateList.valueOf(
                resolveIconBackgroundColor(record.category, context)
            )

            if (record.type == 0) {
                amountView.text = String.format(Locale.US, "-¥%.2f", record.amount)
                amountView.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.expense_primary))
            } else {
                amountView.text = String.format(Locale.US, "+¥%.2f", record.amount)
                amountView.setTextColor(ThemeColorHelper.resolveThemeAwareResource(context, R.color.income_primary))
            }

            actionContainer.visibility = View.VISIBLE
            contentCard.translationX = 0f
            swipeHelper = SwipeToEditDeleteHelper(
                contentCard,
                actionContainer,
                onEdit = { listener.onEdit(record) },
                onDelete = { listener.onDelete(record) },
                onClick = { listener.onEdit(record) }
            )

            itemView.setOnLongClickListener {
                listener.onDelete(record)
                true
            }
            deleteButton.setOnClickListener { listener.onDelete(record) }
        }

        private fun buildSubtitle(record: Record, categoryLabel: String): String {
            val dayLabel = formatDayLabel(record.date)
            val leadingLabel = record.assetSource?.takeIf { it.isNotBlank() }
                ?: categoryLabel.substringAfterLast('/')
            return "$leadingLabel • $dayLabel"
        }

        private fun formatDayLabel(date: String): String {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val parsedDate = format.parse(date) ?: return date
                val targetCalendar = Calendar.getInstance().apply { time = parsedDate }
                val todayCalendar = Calendar.getInstance()
                val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

                when {
                    isSameDay(targetCalendar, todayCalendar) -> itemView.context.getString(R.string.home_record_today)
                    isSameDay(targetCalendar, yesterdayCalendar) -> itemView.context.getString(R.string.home_record_yesterday)
                    else -> SimpleDateFormat(
                        itemView.context.getString(R.string.home_recent_record_date_pattern),
                        Locale.getDefault()
                    ).format(parsedDate)
                }
            } catch (_: Exception) {
                date
            }
        }

        private fun isSameDay(first: Calendar, second: Calendar): Boolean {
            return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
        }

        private fun resolveIconBackgroundColor(category: String, context: android.content.Context): Int {
            return when (category) {
                "餐饮" -> ThemeColorHelper.resolveThemeAwareResource(context, R.color.warning_container)
                "购物" -> ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorSecondaryContainer)
                "工资" -> ThemeColorHelper.resolveThemeAwareResource(context, R.color.success_container)
                "交通" -> ThemeColorHelper.resolveThemeAwareResource(context, R.color.error_container)
                "住房" -> ThemeColorHelper.resolveThemeAwareResource(context, R.color.info_container)
                else -> ThemeColorHelper.resolveThemeAwareResource(context, R.color.editorial_surface_low)
            }
        }
    }
}
