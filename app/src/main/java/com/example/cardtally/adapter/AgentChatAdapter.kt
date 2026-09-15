package com.example.cardtally.adapter

import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    private val expandedReasoning = mutableSetOf<Long>()

    /**
     * Replaces all messages (used for initial load or complete refresh).
     */
    fun submitMessages(newMessages: List<AiChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        streamingMessageIndex = -1
        expandedReasoning.clear()
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
    fun updateStreamingContent(content: String, reasoning: String = "") {
        if (streamingMessageIndex >= 0 && streamingMessageIndex < messages.size) {
            val updatedMessage = messages[streamingMessageIndex].copy(
                content = content,
                reasoning = reasoning.takeIf { it.isNotBlank() }
            )
            messages[streamingMessageIndex] = updatedMessage
            notifyItemChanged(streamingMessageIndex)
        }
    }

    /**
     * Finalizes the streaming message with final content and reasoning.
     * If isError is true, marks it as an error message.
     */
    fun finalizeStreamingMessage(finalContent: String, reasoning: String = "", isError: Boolean = false) {
        if (streamingMessageIndex >= 0 && streamingMessageIndex < messages.size) {
            val finalMessage = messages[streamingMessageIndex].copy(
                content = finalContent,
                reasoning = reasoning.takeIf { it.isNotBlank() },
                isError = isError
            )
            messages[streamingMessageIndex] = finalMessage
            notifyItemChanged(streamingMessageIndex)
            streamingMessageIndex = -1
        }
    }

    /**
     * Removes the streaming placeholder entirely without persisting it. Used when
     * a request is stopped before any assistant content arrived, so the empty
     * bubble does not linger in the list.
     */
    fun discardStreamingPlaceholder() {
        if (streamingMessageIndex in 0 until messages.size) {
            messages.removeAt(streamingMessageIndex)
            notifyItemRemoved(streamingMessageIndex)
            streamingMessageIndex = -1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgentChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agent_message, parent, false)
        return AgentChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AgentChatViewHolder, position: Int) {
        bindMessage(holder, position)
    }

    override fun onBindViewHolder(
        holder: AgentChatViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        bindMessage(holder, position)
    }

    private fun bindMessage(holder: AgentChatViewHolder, position: Int) {
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

        bindReasoning(holder, message, position)
    }

    private fun bindReasoning(holder: AgentChatViewHolder, message: AiChatMessage, position: Int) {
        val context = holder.itemView.context
        val reasoning = message.reasoning.orEmpty()
        val hasReasoning = message.role == AiChatRole.ASSISTANT && reasoning.isNotBlank()
        holder.layoutReasoning.visibility = if (hasReasoning) View.VISIBLE else View.GONE
        if (!hasReasoning) return

        holder.textReasoningBody.text = reasoning
        val isStreaming = position == streamingMessageIndex
        // Auto-expand while only thinking is arriving; collapse once the answer starts.
        val expanded = if (isStreaming && message.content.isBlank()) {
            true
        } else {
            expandedReasoning.contains(message.id)
        }
        applyReasoningExpanded(holder, expanded)
        holder.rowReasoningHeader.contentDescription = context.getString(
            if (expanded) R.string.agent_reasoning_collapse else R.string.agent_reasoning_expand
        )
        holder.rowReasoningHeader.setOnClickListener {
            // Fall back to the bind-time position when the holder is not attached
            // to a RecyclerView (e.g. tests binding the holder directly).
            val adapterPosition = holder.adapterPosition
                .takeIf { it != RecyclerView.NO_POSITION }
                ?: position
            val id = messages.getOrNull(adapterPosition)?.id ?: return@setOnClickListener
            if (expandedReasoning.contains(id)) {
                expandedReasoning.remove(id)
            } else {
                expandedReasoning.add(id)
            }
            notifyItemChanged(adapterPosition)
        }
    }

    private fun applyReasoningExpanded(holder: AgentChatViewHolder, expanded: Boolean) {
        holder.textReasoningBody.maxLines = if (expanded) Int.MAX_VALUE else 2
        holder.textReasoningBody.ellipsize = if (expanded) null else TextUtils.TruncateAt.END
        holder.imageReasoningChevron.setImageResource(
            if (expanded) R.drawable.tabler_chevron_up else R.drawable.tabler_chevron_down
        )
    }

    override fun getItemCount(): Int = messages.size

    /**
     * Read-only snapshot of the rendered rows, used by instrumentation tests to
     * assert which assistant text actually reached the adapter.
     */
    internal fun messageSnapshot(): List<Pair<AiChatRole, String>> =
        messages.map { it.role to it.content }

    internal fun reasoningSnapshot(): List<String?> =
        messages.map { it.reasoning }

    class AgentChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.layout_agent_message_container)
        val textRole: TextView = itemView.findViewById(R.id.text_agent_message_role)
        val layoutReasoning: LinearLayout = itemView.findViewById(R.id.layout_agent_reasoning)
        val rowReasoningHeader: LinearLayout = itemView.findViewById(R.id.row_agent_reasoning_header)
        val textReasoningBody: TextView = itemView.findViewById(R.id.text_agent_reasoning_body)
        val imageReasoningChevron: ImageView = itemView.findViewById(R.id.image_agent_reasoning_chevron)
        val textMessage: TextView = itemView.findViewById(R.id.text_agent_message_body)
    }

    private fun Int.dp(viewContext: android.content.Context): Int {
        return (this * viewContext.resources.displayMetrics.density).roundToInt()
    }
}
