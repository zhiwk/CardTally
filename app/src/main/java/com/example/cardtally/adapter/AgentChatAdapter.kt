package com.example.cardtally.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import kotlin.math.roundToInt

class AgentChatAdapter : RecyclerView.Adapter<AgentChatAdapter.AgentChatViewHolder>() {

    private val messages = mutableListOf<AiChatMessage>()

    fun submitMessages(newMessages: List<AiChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_message, parent, false)
        return AgentChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgentChatViewHolder, position: Int) {
        val message = messages[position]
        val context = holder.itemView.context
        val bubbleLayoutParams = holder.textMessage.layoutParams as LinearLayout.LayoutParams
        val roleLayoutParams = holder.textRole.layoutParams as LinearLayout.LayoutParams
        val roleOffset = 12.dp(context)

        holder.textMessage.text = message.content

        if (message.role == AiChatRole.USER) {
            holder.container.gravity = Gravity.END
            holder.textRole.text = context.getString(R.string.agent_role_you)
            roleLayoutParams.marginStart = 0
            roleLayoutParams.marginEnd = roleOffset
            bubbleLayoutParams.marginStart = 72.dp(context)
            bubbleLayoutParams.marginEnd = 0
            holder.textMessage.setBackgroundResource(R.drawable.bg_agent_bubble_user)
            holder.textMessage.backgroundTintList = null
            holder.textMessage.setTextColor(
                ContextCompat.getColor(context, R.color.onPrimaryContainer_light)
            )
        } else {
            holder.container.gravity = Gravity.START
            holder.textRole.text = context.getString(R.string.agent_role_assistant)
            roleLayoutParams.marginStart = roleOffset
            roleLayoutParams.marginEnd = 0
            bubbleLayoutParams.marginStart = 0
            bubbleLayoutParams.marginEnd = 44.dp(context)
            holder.textMessage.setBackgroundResource(R.drawable.bg_agent_bubble_assistant)
            holder.textMessage.backgroundTintList = null
            holder.textMessage.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (message.isError) R.color.error_primary else R.color.onSurface_light
                )
            )
        }

        holder.textRole.layoutParams = roleLayoutParams
        holder.textMessage.layoutParams = bubbleLayoutParams
    }

    override fun getItemCount(): Int = messages.size

    class AgentChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.layout_agent_message_container)
        val textRole: TextView = itemView.findViewById(R.id.text_agent_message_role)
        val textMessage: TextView = itemView.findViewById(R.id.text_agent_message_body)
    }

    private fun Int.dp(viewContext: android.content.Context): Int {
        return (this * viewContext.resources.displayMetrics.density).roundToInt()
    }
}
