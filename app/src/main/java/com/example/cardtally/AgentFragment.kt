package com.example.cardtally

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cardtally.adapter.AgentChatAdapter
import com.example.cardtally.adapter.AgentSessionAdapter
import com.example.cardtally.adapter.AgentSessionListItem
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.network.MiniMaxChatResult
import com.example.cardtally.network.MiniMaxClient
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.util.AgentSessionTitleHelper
import com.example.cardtally.util.AiAssistantSettingsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AgentFragment : Fragment() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var recyclerSessions: RecyclerView
    private lateinit var layoutConfigMissing: View
    private lateinit var layoutSessionDrawer: View
    private lateinit var textStatus: TextView
    private lateinit var editMessage: EditText
    private lateinit var buttonSend: View
    private lateinit var buttonMenu: View
    private lateinit var buttonNewSession: View
    private lateinit var buttonOpenSettings: View
    private lateinit var progressSending: ProgressBar
    private lateinit var overlaySessionDrawer: View
    private lateinit var imageSend: ImageView

    private lateinit var databaseHelper: DatabaseHelper
    private val miniMaxClient = MiniMaxClient()
    private val chatMessages = mutableListOf<AiChatMessage>()
    private lateinit var chatAdapter: AgentChatAdapter
    private lateinit var sessionAdapter: AgentSessionAdapter
    private lateinit var closeDrawerCallback: OnBackPressedCallback
    private var isSending = false
    private var hasActiveStream = false
    private var currentSessionId = 0L
    private var isSessionDrawerOpen = false
    private var suppressNextTerminalCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_agent, container, false)

        databaseHelper = DatabaseHelper(requireContext())
        recyclerMessages = view.findViewById(R.id.recycler_agent_messages)
        recyclerSessions = view.findViewById(R.id.recycler_agent_sessions)
        layoutConfigMissing = view.findViewById(R.id.layout_agent_config_missing)
        layoutSessionDrawer = view.findViewById(R.id.layout_agent_session_drawer)
        textStatus = view.findViewById(R.id.text_agent_status)
        editMessage = view.findViewById(R.id.edit_agent_message)
        buttonSend = view.findViewById(R.id.button_agent_send)
        buttonMenu = view.findViewById(R.id.button_agent_menu)
        buttonNewSession = view.findViewById(R.id.button_agent_new_session)
        buttonOpenSettings = view.findViewById(R.id.button_open_ai_settings)
        progressSending = view.findViewById(R.id.progress_agent_sending)
        overlaySessionDrawer = view.findViewById(R.id.view_agent_session_overlay)
        imageSend = view.findViewById(R.id.image_agent_send)

        chatAdapter = AgentChatAdapter()
        recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        recyclerMessages.adapter = chatAdapter

        sessionAdapter = AgentSessionAdapter(object : AgentSessionAdapter.SessionActionListener {
            override fun onSessionSelected(item: AgentSessionListItem) {
                if (item.sessionId == currentSessionId) {
                    closeSessionDrawer()
                    return
                }
                switchToSession(item.sessionId)
            }

            override fun onSessionLongPressed(item: AgentSessionListItem) {
                showRenameSessionDialog(item)
            }
        })
        recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        recyclerSessions.adapter = sessionAdapter

        closeDrawerCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeSessionDrawer()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, closeDrawerCallback)

        buttonMenu.setOnClickListener {
            if (isSending) return@setOnClickListener
            if (isSessionDrawerOpen) closeSessionDrawer() else openSessionDrawer()
        }

        buttonNewSession.setOnClickListener {
            if (isSending) return@setOnClickListener
            showCreateSessionDialog()
        }

        overlaySessionDrawer.setOnClickListener {
            closeSessionDrawer()
        }

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

        configureSessionDrawerWidth()
        initializeChatState()
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

        ensureGreetingMessageIfNeeded(currentSessionId)
        loadCurrentSessionMessages()
        refreshSessionList()
        refreshChatUi()
    }

    override fun onDestroyView() {
        if (hasActiveStream) {
            persistCurrentAssistantDraftBeforeForcedStop()
            suppressNextTerminalCallback = true
            miniMaxClient.cancel()
        }
        super.onDestroyView()
    }

    private fun refreshChatUi() {
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        layoutConfigMissing.visibility = if (configComplete) View.GONE else View.VISIBLE
        recyclerMessages.visibility = View.VISIBLE

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

        suppressNextTerminalCallback = false
        closeSessionDrawer()

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

        val createdAt = System.currentTimeMillis()
        val userMessage = AiChatMessage(
            sessionId = currentSessionId,
            role = AiChatRole.USER,
            content = content,
            createdAt = createdAt
        )
        databaseHelper.addAiChatMessage(userMessage)
        chatMessages.add(userMessage)
        chatAdapter.appendMessage(userMessage)
        refreshSessionList()
        scrollToBottom()

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
                if (suppressNextTerminalCallback && result !is MiniMaxChatResult.StreamingChunk) {
                    suppressNextTerminalCallback = false
                    return@runOnUiThread
                }

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
                        val finalMessage = AiChatMessage(
                            sessionId = currentSessionId,
                            role = AiChatRole.ASSISTANT,
                            content = result.finalContent,
                            createdAt = System.currentTimeMillis()
                        )
                        chatMessages[chatMessages.size - 1] = finalMessage
                        chatAdapter.finalizeStreamingMessage(result.finalContent, isError = false)
                        databaseHelper.addAiChatMessage(finalMessage)
                        refreshSessionList()
                        updateSendingState(isSending, hasActiveStream)
                    }

                    is MiniMaxChatResult.Success -> {
                        hasActiveStream = false
                        isSending = false
                        val finalMessage = AiChatMessage(
                            sessionId = currentSessionId,
                            role = AiChatRole.ASSISTANT,
                            content = result.reply,
                            createdAt = System.currentTimeMillis()
                        )
                        chatMessages[chatMessages.size - 1] = finalMessage
                        chatAdapter.finalizeStreamingMessage(result.reply, isError = false)
                        databaseHelper.addAiChatMessage(finalMessage)
                        refreshSessionList()
                        updateSendingState(isSending, hasActiveStream)
                    }

                    is MiniMaxChatResult.Failure -> {
                        hasActiveStream = false
                        isSending = false
                        val finalMessage = handleErrorResult(result)
                        databaseHelper.addAiChatMessage(finalMessage)
                        refreshSessionList()
                        updateSendingState(isSending, hasActiveStream)
                    }
                }
            }
        }
    }

    private fun handleErrorResult(result: MiniMaxChatResult.Failure): AiChatMessage {
        val partialContent = result.detail?.takeIf {
            (result.type == MiniMaxErrorType.CANCELLED ||
             result.type == MiniMaxErrorType.INTERRUPTED ||
             result.type == MiniMaxErrorType.TIMEOUT) &&
            it.isNotBlank() &&
            !it.contains("java.net") &&
            !it.contains("IOException")
        }

        if (partialContent != null) {
            val errorTypeString = when (result.type) {
                MiniMaxErrorType.CANCELLED -> getString(R.string.agent_error_cancelled)
                MiniMaxErrorType.INTERRUPTED -> getString(R.string.agent_error_interrupted)
                MiniMaxErrorType.TIMEOUT -> getString(R.string.agent_error_timeout)
                else -> ""
            }
            val contentWithContext = getString(
                R.string.agent_error_with_partial_content,
                partialContent,
                errorTypeString
            )
            val finalMessage = AiChatMessage(
                sessionId = currentSessionId,
                role = AiChatRole.ASSISTANT,
                content = contentWithContext,
                isError = true,
                createdAt = System.currentTimeMillis()
            )
            chatMessages[chatMessages.size - 1] = finalMessage
            chatAdapter.finalizeStreamingMessage(contentWithContext, isError = true)
            scrollToBottom()
            return finalMessage
        } else {
            val errorContent = getErrorMessage(result)
            val errorMessage = AiChatMessage(
                sessionId = currentSessionId,
                role = AiChatRole.ASSISTANT,
                content = errorContent,
                isError = true,
                createdAt = System.currentTimeMillis()
            )
            chatMessages[chatMessages.size - 1] = errorMessage
            chatAdapter.finalizeStreamingMessage(errorContent, isError = true)
            scrollToBottom()
            return errorMessage
        }
    }

    private fun updateSendingState(sending: Boolean, streaming: Boolean) {
        isSending = sending
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        editMessage.isEnabled = configComplete && !sending
        buttonSend.isEnabled = configComplete
        buttonMenu.isEnabled = !sending
        buttonNewSession.isEnabled = !sending
        sessionAdapter.interactionsEnabled = !sending
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

    private fun initializeChatState() {
        val sessions = databaseHelper.getAiChatSessions()
        val savedSessionId = AiAssistantSettingsHelper.getActiveSessionId(requireContext())
        currentSessionId = sessions.firstOrNull { it.id == savedSessionId }?.id
            ?: sessions.firstOrNull()?.id
            ?: createSessionInternal(AgentSessionTitleHelper.createDefaultTitle())

        AiAssistantSettingsHelper.saveActiveSessionId(requireContext(), currentSessionId)
        ensureGreetingMessageIfNeeded(currentSessionId)
        loadCurrentSessionMessages()
        refreshSessionList()
        closeSessionDrawer(immediate = true)
    }

    private fun loadCurrentSessionMessages() {
        if (currentSessionId <= 0L) {
            chatMessages.clear()
            renderMessages()
            return
        }

        chatMessages.clear()
        chatMessages.addAll(databaseHelper.getAiChatMessages(currentSessionId))
        renderMessages()
    }

    private fun ensureGreetingMessageIfNeeded(sessionId: Long) {
        if (sessionId <= 0L || !AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
            return
        }

        val existingMessages = databaseHelper.getAiChatMessages(sessionId)
        if (existingMessages.isNotEmpty()) {
            return
        }

        databaseHelper.addAiChatMessage(
            AiChatMessage(
                sessionId = sessionId,
                role = AiChatRole.ASSISTANT,
                content = getString(R.string.agent_greeting),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun refreshSessionList() {
        val items = databaseHelper.getAiChatSessions().map { session ->
            val sessionMessages = databaseHelper.getAiChatMessages(session.id)
            AgentSessionListItem(
                sessionId = session.id,
                title = session.title,
                preview = sessionMessages.lastOrNull()?.content?.replace("\n", " ")
                    ?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.agent_session_empty_state),
                timestamp = formatSessionTimestamp(session.updatedAt),
                isCurrent = session.id == currentSessionId
            )
        }
        sessionAdapter.submitItems(items)
    }

    private fun switchToSession(sessionId: Long) {
        if (sessionId <= 0L || sessionId == currentSessionId) {
            closeSessionDrawer()
            return
        }

        forceStopStreamingIfNeeded()
        currentSessionId = sessionId
        AiAssistantSettingsHelper.saveActiveSessionId(requireContext(), currentSessionId)
        ensureGreetingMessageIfNeeded(currentSessionId)
        loadCurrentSessionMessages()
        refreshSessionList()
        refreshChatUi()
        closeSessionDrawer()
    }

    private fun showCreateSessionDialog() {
        showSessionNameDialog(
            title = getString(R.string.agent_session_create_title),
            positiveButtonText = getString(R.string.agent_session_create_confirm),
            initialValue = AgentSessionTitleHelper.createDefaultTitle(),
            onConfirm = { sessionTitle ->
                forceStopStreamingIfNeeded()
                currentSessionId = createSessionInternal(sessionTitle)
                AiAssistantSettingsHelper.saveActiveSessionId(requireContext(), currentSessionId)
                ensureGreetingMessageIfNeeded(currentSessionId)
                loadCurrentSessionMessages()
                refreshSessionList()
                refreshChatUi()
                closeSessionDrawer()
            }
        )
    }

    private fun showRenameSessionDialog(item: AgentSessionListItem) {
        showSessionNameDialog(
            title = getString(R.string.agent_session_rename_title),
            positiveButtonText = getString(R.string.agent_session_rename_confirm),
            initialValue = item.title,
            onConfirm = { newTitle ->
                databaseHelper.updateAiChatSessionTitle(item.sessionId, newTitle)
                refreshSessionList()
            }
        )
    }

    private fun showSessionNameDialog(
        title: String,
        positiveButtonText: String,
        initialValue: String,
        onConfirm: (String) -> Unit
    ) {
        val input = EditText(requireContext()).apply {
            setText(initialValue)
            setSelection(text.length)
            hint = getString(R.string.agent_session_name_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            maxLines = 1
        }

        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.spacing_xl)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.spacing_m)
        val container = FrameLayout(requireContext()).apply {
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(container)
            .setNegativeButton(R.string.dialog_cancel, null)
            .setPositiveButton(positiveButtonText, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val trimmedTitle = input.text.toString().trim()
                if (trimmedTitle.isBlank()) {
                    input.error = getString(R.string.agent_session_name_empty)
                } else {
                    onConfirm(trimmedTitle)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun createSessionInternal(title: String): Long {
        val sessionTitle = title.trim()
        val fallbackTitle = AgentSessionTitleHelper.createDefaultTitle()
        return databaseHelper.addAiChatSession(
            title = sessionTitle.ifBlank { fallbackTitle },
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun forceStopStreamingIfNeeded() {
        if (!hasActiveStream) {
            return
        }

        persistCurrentAssistantDraftBeforeForcedStop()
        suppressNextTerminalCallback = true
        hasActiveStream = false
        isSending = false
        miniMaxClient.cancel()
        updateSendingState(isSending, hasActiveStream)
    }

    private fun persistCurrentAssistantDraftBeforeForcedStop() {
        if (currentSessionId <= 0L || chatMessages.isEmpty()) {
            return
        }

        val lastMessage = chatMessages.last()
        if (lastMessage.role != AiChatRole.ASSISTANT || lastMessage.content.isBlank()) {
            return
        }

        val persistedContent = getString(
            R.string.agent_error_with_partial_content,
            lastMessage.content,
            getString(R.string.agent_error_cancelled)
        )
        val finalMessage = lastMessage.copy(
            sessionId = currentSessionId,
            content = persistedContent,
            isError = true,
            createdAt = System.currentTimeMillis()
        )
        chatMessages[chatMessages.size - 1] = finalMessage
        databaseHelper.addAiChatMessage(finalMessage)
        refreshSessionList()
    }

    private fun configureSessionDrawerWidth() {
        layoutSessionDrawer.post {
            val targetWidth = (resources.displayMetrics.widthPixels * 0.68f).toInt()
            layoutSessionDrawer.layoutParams = layoutSessionDrawer.layoutParams.apply {
                width = targetWidth
            }
            layoutSessionDrawer.translationX = -targetWidth.toFloat()
        }
    }

    private fun openSessionDrawer() {
        if (isSessionDrawerOpen) {
            return
        }

        isSessionDrawerOpen = true
        closeDrawerCallback.isEnabled = true
        overlaySessionDrawer.alpha = 0f
        overlaySessionDrawer.visibility = View.VISIBLE
        overlaySessionDrawer.animate().alpha(1f).setDuration(180L).start()

        layoutSessionDrawer.visibility = View.VISIBLE
        layoutSessionDrawer.animate()
            .translationX(0f)
            .setDuration(220L)
            .start()
    }

    private fun closeSessionDrawer(immediate: Boolean = false) {
        if (!isSessionDrawerOpen && !immediate) {
            return
        }

        isSessionDrawerOpen = false
        closeDrawerCallback.isEnabled = false
        val closedTranslation = -layoutSessionDrawer.width.toFloat()

        if (immediate) {
            overlaySessionDrawer.visibility = View.GONE
            overlaySessionDrawer.alpha = 0f
            layoutSessionDrawer.translationX = closedTranslation
            layoutSessionDrawer.visibility = View.GONE
            return
        }

        overlaySessionDrawer.animate()
            .alpha(0f)
            .setDuration(180L)
            .withEndAction {
                overlaySessionDrawer.visibility = View.GONE
            }
            .start()

        layoutSessionDrawer.animate()
            .translationX(closedTranslation)
            .setDuration(220L)
            .withEndAction {
                layoutSessionDrawer.visibility = View.GONE
            }
            .start()
    }

    private fun formatSessionTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val sameDayFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val monthDayFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())
        val fullDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return when {
            now - timestamp < 24 * 60 * 60 * 1000L && fullDateFormatter.format(Date(now)) == fullDateFormatter.format(Date(timestamp)) -> {
                sameDayFormatter.format(Date(timestamp))
            }
            else -> monthDayFormatter.format(Date(timestamp))
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
