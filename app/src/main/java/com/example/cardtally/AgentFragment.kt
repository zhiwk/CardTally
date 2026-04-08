package com.example.cardtally

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AgentChatAdapter
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.network.MiniMaxChatResult
import com.example.cardtally.network.MiniMaxClient
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.util.AiAssistantSettingsHelper

class AgentFragment : Fragment() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var layoutConfigMissing: View
    private lateinit var textStatus: TextView
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: View
    private lateinit var buttonOpenSettings: View
    private lateinit var progressSending: ProgressBar
    private lateinit var imageSend: ImageView

    private val miniMaxClient = MiniMaxClient()
    private val chatMessages = mutableListOf<AiChatMessage>()
    private lateinit var chatAdapter: AgentChatAdapter
    private var isSending = false
    private var hasActiveStream = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agent, container, false)

        recyclerMessages = view.findViewById(R.id.recycler_agent_messages)
        layoutConfigMissing = view.findViewById(R.id.layout_agent_config_missing)
        textStatus = view.findViewById(R.id.text_agent_status)
        editMessage = view.findViewById(R.id.edit_agent_message)
        buttonSend = view.findViewById(R.id.button_agent_send)
        buttonOpenSettings = view.findViewById(R.id.button_open_ai_settings)
        progressSending = view.findViewById(R.id.progress_agent_sending)
        imageSend = view.findViewById(R.id.image_agent_send)

        chatAdapter = AgentChatAdapter()
        recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        recyclerMessages.adapter = chatAdapter

        buttonOpenSettings.setOnClickListener {
            openAiSettings()
        }

        buttonSend.setOnClickListener {
            sendCurrentMessage()
        }

        editMessage.setOnEditorActionListener { _, actionId, event ->
            if (
                actionId == EditorInfo.IME_ACTION_SEND ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                sendCurrentMessage()
                true
            } else {
                false
            }
        }

        refreshChatUi()
        return view
    }

    override fun onResume() {
        super.onResume()

        if (!AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext())) {
            requireActivity()
                .findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.nav_home
            return
        }

        refreshChatUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any active request when fragment is destroyed
        if (hasActiveStream) {
            miniMaxClient.cancel()
            // The callback will handle preserving partial content
        }
    }

    private fun refreshChatUi() {
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        layoutConfigMissing.visibility = if (configComplete) View.GONE else View.VISIBLE
        recyclerMessages.visibility = if (configComplete) View.VISIBLE else View.GONE

        if (configComplete && chatMessages.isEmpty()) {
            chatMessages.add(
                AiChatMessage(
                    role = AiChatRole.ASSISTANT,
                    content = getString(R.string.agent_greeting)
                )
            )
            renderMessages()
        }

        textStatus.text = if (configComplete) {
            getString(
                R.string.agent_status_ready,
                AiAssistantSettingsHelper.getModel(requireContext())
            )
        } else {
            getString(R.string.agent_status_configuration_required)
        }

        updateSendingState(isSending, hasActiveStream)
    }

    private fun sendCurrentMessage() {
        if (isSending) {
            // If already sending, treat as cancel - let the callback handle cleanup
            if (hasActiveStream) {
                miniMaxClient.cancel()
            }
            return
        }

        if (!AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
            refreshChatUi()
            openAiSettings()
            return
        }

        val content = editMessage.text.toString().trim()
        if (content.isBlank()) {
            editMessage.error = getString(R.string.agent_error_empty_message)
            return
        }

        editMessage.error = null
        editMessage.setText("")

        // Add user message to both list and adapter
        val userMessage = AiChatMessage(role = AiChatRole.USER, content = content)
        chatMessages.add(userMessage)
        chatAdapter.appendMessage(userMessage)
        scrollToBottom()

        // Start streaming message placeholder in both list and adapter
        val placeholderMessage = AiChatMessage(role = AiChatRole.ASSISTANT, content = "")
        chatMessages.add(placeholderMessage)
        chatAdapter.startStreamingMessage()

        isSending = true
        hasActiveStream = true
        updateSendingState(isSending, hasActiveStream)

        miniMaxClient.sendChat(
            config = AiAssistantSettingsHelper.getMiniMaxConfig(requireContext()),
            messages = chatMessages.filter { !it.isError && it.content.isNotBlank() }.toList()
        ) { result ->
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }

                when (result) {
                    is MiniMaxChatResult.StreamingChunk -> {
                        // Update both adapter and chatMessages with partial content
                        val partialContent = result.partialContent
                        chatAdapter.updateStreamingContent(partialContent)
                        // Update chatMessages to keep it in sync
                        val lastIndex = chatMessages.size - 1
                        if (lastIndex >= 0 && chatMessages[lastIndex].role == AiChatRole.ASSISTANT) {
                            chatMessages[lastIndex] = AiChatMessage(
                                role = AiChatRole.ASSISTANT,
                                content = partialContent
                            )
                        }
                        scrollToBottom()
                    }

                    is MiniMaxChatResult.StreamingDone -> {
                        hasActiveStream = false
                        isSending = false
                        // Finalize with complete content
                        val finalMessage = AiChatMessage(
                            role = AiChatRole.ASSISTANT,
                            content = result.finalContent
                        )
                        chatMessages[chatMessages.size - 1] = finalMessage
                        chatAdapter.finalizeStreamingMessage(result.finalContent, isError = false)
                        updateSendingState(isSending, hasActiveStream)
                    }

                    is MiniMaxChatResult.Success -> {
                        // Non-streaming success
                        hasActiveStream = false
                        isSending = false
                        val finalMessage = AiChatMessage(
                            role = AiChatRole.ASSISTANT,
                            content = result.reply
                        )
                        chatMessages[chatMessages.size - 1] = finalMessage
                        chatAdapter.finalizeStreamingMessage(result.reply, isError = false)
                        updateSendingState(isSending, hasActiveStream)
                    }

                    is MiniMaxChatResult.Failure -> {
                        hasActiveStream = false
                        isSending = false
                        handleErrorResult(result)
                        updateSendingState(isSending, hasActiveStream)
                    }
                }
            }
        }
    }

    private fun handleErrorResult(result: MiniMaxChatResult.Failure) {
        // Check if there's partial content to preserve (from cancellation/interruption/timeout)
        val partialContent = result.detail?.takeIf {
            (result.type == MiniMaxErrorType.CANCELLED ||
             result.type == MiniMaxErrorType.INTERRUPTED ||
             result.type == MiniMaxErrorType.TIMEOUT) &&
            it.isNotBlank() &&
            !it.contains("java.net") &&
            !it.contains("IOException")
        }

        if (partialContent != null) {
            // Preserve the partial content as the assistant message
            // The error context is implicit in the isError flag
            val errorTypeString = when (result.type) {
                MiniMaxErrorType.CANCELLED -> getString(R.string.agent_error_cancelled)
                MiniMaxErrorType.INTERRUPTED -> getString(R.string.agent_error_interrupted)
                MiniMaxErrorType.TIMEOUT -> getString(R.string.agent_error_timeout)
                else -> ""
            }
            // Combine partial content with error indicator
            val contentWithContext = getString(
                R.string.agent_error_with_partial_content,
                partialContent,
                errorTypeString
            )
            val finalMessage = AiChatMessage(
                role = AiChatRole.ASSISTANT,
                content = contentWithContext,
                isError = true
            )
            chatMessages[chatMessages.size - 1] = finalMessage
            chatAdapter.finalizeStreamingMessage(contentWithContext, isError = true)
        } else {
            // No partial content, show error message
            val errorContent = getErrorMessage(result)
            val errorMessage = AiChatMessage(
                role = AiChatRole.ASSISTANT,
                content = errorContent,
                isError = true
            )
            chatMessages[chatMessages.size - 1] = errorMessage
            chatAdapter.finalizeStreamingMessage(errorContent, isError = true)
        }

        scrollToBottom()
    }

    private fun updateSendingState(sending: Boolean, streaming: Boolean) {
        isSending = sending
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        editMessage.isEnabled = configComplete && !sending
        buttonSend.isEnabled = configComplete
        progressSending.visibility = if (streaming) View.VISIBLE else View.GONE
        imageSend.visibility = if (streaming) View.INVISIBLE else View.VISIBLE

        textStatus.text = when {
            !configComplete -> getString(R.string.agent_status_configuration_required)
            streaming -> getString(R.string.agent_status_receiving)
            sending -> getString(R.string.agent_status_sending)
            else -> getString(
                R.string.agent_status_ready,
                AiAssistantSettingsHelper.getModel(requireContext())
            )
        }
    }

    private fun scrollToBottom() {
        recyclerMessages.post {
            if (chatAdapter.itemCount > 0) {
                recyclerMessages.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
    }

    private fun renderMessages() {
        chatAdapter.submitMessages(chatMessages)
        scrollToBottom()
    }

    private fun getErrorMessage(result: MiniMaxChatResult.Failure): String {
        return when (result.type) {
            MiniMaxErrorType.INVALID_CONFIG -> getString(R.string.agent_error_config_missing)
            MiniMaxErrorType.INVALID_URL -> getString(R.string.agent_error_invalid_url)
            MiniMaxErrorType.NETWORK -> getString(R.string.agent_error_network)
            MiniMaxErrorType.AUTH -> getString(R.string.agent_error_auth)
            MiniMaxErrorType.RATE_LIMIT -> getString(R.string.agent_error_rate_limit)
            MiniMaxErrorType.INSUFFICIENT_BALANCE -> getString(R.string.agent_error_insufficient_balance)
            MiniMaxErrorType.INVALID_REQUEST -> getString(R.string.agent_error_invalid_request)
            MiniMaxErrorType.HTTP -> {
                if (result.detail.isNullOrBlank()) {
                    getString(R.string.agent_error_http)
                } else {
                    getString(R.string.agent_error_http_with_detail, result.detail)
                }
            }
            MiniMaxErrorType.PARSE -> getString(R.string.agent_error_parse)
            MiniMaxErrorType.EMPTY_REPLY -> getString(R.string.agent_error_empty_reply)
            MiniMaxErrorType.TIMEOUT -> getString(R.string.agent_error_timeout)
            MiniMaxErrorType.CANCELLED -> getString(R.string.agent_error_cancelled)
            MiniMaxErrorType.INTERRUPTED -> getString(R.string.agent_error_interrupted)
        }
    }

    private fun openAiSettings() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AiAssistantSettingsFragment())
            .addToBackStack(null)
            .commit()
    }
}