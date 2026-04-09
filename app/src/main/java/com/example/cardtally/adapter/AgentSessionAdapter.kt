package com.example.cardtally.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors

data class AgentSessionListItem(
    val sessionId: Long,
    val title: String,
    val preview: String,
    val timestamp: String,
    val isCurrent: Boolean
)

class AgentSessionAdapter(
    private val listener: SessionActionListener
) : RecyclerView.Adapter<AgentSessionAdapter.AgentSessionViewHolder>() {

    interface SessionActionListener {
        fun onSessionSelected(item: AgentSessionListItem)
        fun onSessionLongPressed(item: AgentSessionListItem)
    }

    private val items = mutableListOf<AgentSessionListItem>()
    var interactionsEnabled: Boolean = true

    fun submitItems(newItems: List<AgentSessionListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentSessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_session, parent, false)
        return AgentSessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgentSessionViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.textTitle.text = item.title
        holder.textPreview.text = item.preview
        holder.textTimestamp.text = item.timestamp
        holder.viewIndicator.visibility = if (item.isCurrent) View.VISIBLE else View.INVISIBLE

        val backgroundColor = MaterialColors.getColor(
            holder.cardView,
            if (item.isCurrent) com.google.android.material.R.attr.colorPrimaryContainer
            else com.google.android.material.R.attr.colorSurfaceContainerLow
        )
        val strokeColor = MaterialColors.getColor(
            holder.cardView,
            if (item.isCurrent) com.google.android.material.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOutlineVariant
        )
        val titleColor = MaterialColors.getColor(
            holder.textTitle,
            if (item.isCurrent) com.google.android.material.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOnSurface
        )

        holder.cardView.setCardBackgroundColor(backgroundColor)
        holder.cardView.strokeColor = strokeColor
        holder.cardView.strokeWidth = if (item.isCurrent) 2.dp(context) else 1.dp(context)
        holder.textTitle.setTextColor(titleColor)

        holder.cardView.isEnabled = interactionsEnabled
        holder.cardView.alpha = if (interactionsEnabled) 1f else 0.72f
        holder.cardView.setOnClickListener {
            if (interactionsEnabled) {
                listener.onSessionSelected(item)
            }
        }
        holder.cardView.setOnLongClickListener {
            if (interactionsEnabled) {
                listener.onSessionLongPressed(item)
                true
            } else {
                false
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class AgentSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.card_agent_session)
        val viewIndicator: View = itemView.findViewById(R.id.view_agent_session_indicator)
        val textTitle: TextView = itemView.findViewById(R.id.text_agent_session_title)
        val textTimestamp: TextView = itemView.findViewById(R.id.text_agent_session_timestamp)
        val textPreview: TextView = itemView.findViewById(R.id.text_agent_session_preview)
    }

    private fun Int.dp(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }
}
