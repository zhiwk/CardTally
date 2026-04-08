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

        updateSendingState(isSending)
    }

    private fun sendCurrentMessage() {
        if (isSending) {
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

        chatMessages.add(AiChatMessage(role = AiChatRole.USER, content = content))
        renderMessages()
        updateSendingState(true)

        miniMaxClient.sendChat(
            config = AiAssistantSettingsHelper.getMiniMaxConfig(requireContext()),
            messages = chatMessages.toList()
        ) { result ->
            activity?.runOnUiThread {
                if (!isAdded) {
                    return@runOnUiThread
                }

                updateSendingState(false)

                when (result) {
                    is MiniMaxChatResult.Success -> {
                        chatMessages.add(
                            AiChatMessage(
                                role = AiChatRole.ASSISTANT,
                                content = result.reply
                            )
                        )
                    }

                    is MiniMaxChatResult.Failure -> {
                        chatMessages.add(
                            AiChatMessage(
                                role = AiChatRole.ASSISTANT,
                                content = getErrorMessage(result),
                                isError = true
                            )
                        )
                    }
                }

                renderMessages()
            }
        }
    }

    private fun updateSendingState(sending: Boolean) {
        isSending = sending
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        editMessage.isEnabled = configComplete && !sending
        buttonSend.isEnabled = configComplete && !sending
        progressSending.visibility = if (sending) View.VISIBLE else View.GONE
        imageSend.visibility = if (sending) View.INVISIBLE else View.VISIBLE

        textStatus.text = when {
            !configComplete -> getString(R.string.agent_status_configuration_required)
            sending -> getString(R.string.agent_status_sending)
            else -> getString(
                R.string.agent_status_ready,
                AiAssistantSettingsHelper.getModel(requireContext())
            )
        }
    }

    private fun renderMessages() {
        chatAdapter.submitMessages(chatMessages)
        recyclerMessages.post {
            if (chatAdapter.itemCount > 0) {
                recyclerMessages.scrollToPosition(chatAdapter.itemCount - 1)
            }
        }
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
        }
    }

    private fun openAiSettings() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, AiAssistantSettingsFragment())
            .addToBackStack(null)
            .commit()
    }
}
