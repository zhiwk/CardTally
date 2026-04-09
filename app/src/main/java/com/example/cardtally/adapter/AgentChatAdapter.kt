package com.example.cardtally.adapter

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.R
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.util.ThemeColorHelper
import kotlin.math.roundToInt

class AgentChatAdapter : RecyclerView.Adapter<AgentChatAdapter.AgentChatViewHolder>() {

    private val messages = mutableListOf<AiChatMessage>()
    private var streamingMessageIndex: Int = -1

    /**
     * Replaces all messages (used for initial load or complete refresh).
     */
    fun submitMessages(newMessages: List<AiChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        streamingMessageIndex = -1
        notifyDataSetChanged()
    }

    /**
     * Appends a new message to the list.
     */
    fun appendMessage(message: AiChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    /**
     * Sets up a streaming message placeholder. Returns the index of the streaming message.
     */
    fun startStreamingMessage(): Int {
        val message = AiChatMessage(
            role = AiChatRole.ASSISTANT,
            content = ""
        )
        messages.add(message)
        streamingMessageIndex = messages.size - 1
        notifyItemInserted(streamingMessageIndex)
        return streamingMessageIndex
    }

    /**
     * Updates the content of the currently streaming message.
     */
    fun updateStreamingContent(content: String) {
        if (streamingMessageIndex >= 0 && streamingMessageIndex < messages.size) {
            val updatedMessage = messages[streamingMessageIndex].copy(content = content)
            messages[streamingMessageIndex] = updatedMessage
            notifyItemChanged(streamingMessageIndex, PAYLOAD_STREAMING_CONTENT)
        }
    }

    /**
     * Finalizes the streaming message with final content.
     * If isError is true, marks it as an error message.
     */
    fun finalizeStreamingMessage(finalContent: String, isError: Boolean = false) {
        if (streamingMessageIndex >= 0 && streamingMessageIndex < messages.size) {
            val finalMessage = messages[streamingMessageIndex].copy(
                content = finalContent,
                isError = isError
            )
            messages[streamingMessageIndex] = finalMessage
            notifyItemChanged(streamingMessageIndex)
            streamingMessageIndex = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_message, parent, false)
        return AgentChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgentChatViewHolder, position: Int) {
        bindViewHolder(holder, position, false)
    }

    override fun onBindViewHolder(
        holder: AgentChatViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_STREAMING_CONTENT)) {
            // Partial update - only update text content
            val message = messages[position]
            holder.textMessage.text = message.content
        } else {
            bindViewHolder(holder, position, false)
        }
    }

    private fun bindViewHolder(holder: AgentChatViewHolder, position: Int, isPartial: Boolean) {
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
                ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnPrimaryContainer)
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
                if (message.isError) {
                    ThemeColorHelper.resolveThemeAwareResource(context, R.color.error_primary)
                } else {
                    ThemeColorHelper.resolveColor(context, com.google.android.material.R.attr.colorOnSurface)
                }
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

    companion object {
        private const val PAYLOAD_STREAMING_CONTENT = "streaming_content"
    }
}
