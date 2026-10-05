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
import com.example.cardtally.ai.AiRecordAssistant
import com.example.cardtally.ai.AiRecordEngine
import com.example.cardtally.adapter.AgentChatAdapter
import com.example.cardtally.adapter.AgentSessionAdapter
import com.example.cardtally.adapter.AgentSessionListItem
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.network.AiChatSender
import com.example.cardtally.network.MiniMaxChatResult
import com.example.cardtally.network.MiniMaxClient
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.state.AgentScreenState
import com.example.cardtally.state.AiRequestIdentity
import com.example.cardtally.state.InFlightAiLifecycle
import com.example.cardtally.util.AgentSessionTitleHelper
import com.example.cardtally.util.AiAssistantSettingsHelper
import com.example.cardtally.util.FloatingNavLayoutHelper
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
    private lateinit var layoutComposerContainer: View
    private lateinit var layoutComposerShell: View
    private lateinit var imageStop: ImageView

    private lateinit var databaseHelper: DatabaseHelper
    private var miniMaxClient: AiChatSender = createDefaultSender()
    private val requestLifecycle = InFlightAiLifecycle()
    private val chatMessages = mutableListOf<AiChatMessage>()
    private lateinit var chatAdapter: AgentChatAdapter
    private lateinit var sessionAdapter: AgentSessionAdapter
    private lateinit var closeDrawerCallback: OnBackPressedCallback
    internal var isSending = false
        private set
    internal var hasActiveStream = false
        private set
    private var recordTurn = false
    private var recordAssistant: AiRecordAssistant? = null
    private lateinit var recordActionPanel: View
    private lateinit var recordActionText: TextView
    private var currentSessionId = 0L
    private var isSessionDrawerOpen = false
    // Monotonically increasing request identity. A callback is only applied when
    // its captured requestId matches this value, so a stopped request A cannot
    // overwrite or finish a later request B. Stale ids are simply ignored.
    private val requestIdentity = AiRequestIdentity()
    private var restoredAgentState: AgentScreenState? = null

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
        imageStop = view.findViewById(R.id.image_agent_stop)
        layoutComposerContainer = view.findViewById(R.id.layout_agent_composer_container)
        layoutComposerShell = view.findViewById(R.id.layout_agent_composer_shell)

        recordActionPanel = view.findViewById(R.id.layout_ai_record_action)
        recordActionText = view.findViewById(R.id.text_ai_record_action)
        view.findViewById<View>(R.id.button_ai_record_review).setOnClickListener { recordAssistant?.review() }
        view.findViewById<View>(R.id.button_ai_record_reject).setOnClickListener { recordAssistant?.reject() }

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
        applyComposerGapAboveBottomNav()
        restoredAgentState = AgentScreenState.readFrom(savedInstanceState)
        initializeChatState(restoredAgentState!!)
        editMessage.setText(restoredAgentState!!.composerText)
        refreshChatUi()
        view.post { restoreAgentViewState() }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentAgentState().writeTo(outState)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.setBottomNavigationTemporarilyHidden(isSessionDrawerOpen)
        applyComposerGapAboveBottomNav()

        if (!AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext()) ||
            !AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
            forceStopStreamingIfNeeded()
            // Keep the user on the assistant page and show the configuration
            // state instead of replacing the Fragment from onResume().
            refreshChatUi()
            return
        }

        ensureGreetingMessageIfNeeded(currentSessionId)
        loadCurrentSessionMessages()
        refreshSessionList()
        refreshChatUi()
    }

    override fun onPause() {
        // Leaving the page invalidates every pending approval, including theme/config/session changes.
        if (recordTurn && isSending) stopActiveStreamByUser()
        recordAssistant = null // Provider-private tool context never survives leaving the page.
        super.onPause()
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.setBottomNavigationTemporarilyHidden(false)
        if (isSending) {
            stopActiveStreamByUser()
        }
        super.onDestroyView()
    }

    private fun refreshChatUi() {
        val configComplete = AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())
        layoutConfigMissing.visibility = if (configComplete) View.GONE else View.VISIBLE
        recyclerMessages.visibility = View.VISIBLE

        textStatus.text = if (configComplete) {
            getString(
                if (AiAssistantSettingsHelper.getRecordToolsEnabled(requireContext())) R.string.ai_record_ready else R.string.agent_status_ready,
                AiAssistantSettingsHelper.getModel(requireContext())
            )
        } else {
            getString(R.string.agent_status_configuration_required)
        }

        updateSendingState(isSending, hasActiveStream)
    }

    private fun applyComposerGapAboveBottomNav() {
        // MainActivity already places the fragment above navigation; consume the gap only once.
        layoutComposerContainer.setPaddingRelative(
            layoutComposerContainer.paddingStart,
            layoutComposerContainer.paddingTop,
            layoutComposerContainer.paddingEnd,
            resources.getDimensionPixelSize(R.dimen.spacing_s)
        )
    }

    /**
     * Drives the composer from an instrumentation test without depending on the
     * real IME. Only text is written; no persistence or network side effect.
     */
    internal fun setComposerTextForTest(text: String) {
        editMessage.setText(text)
    }

    /** Rendered chat rows, for instrumentation assertions on the send lifecycle. */
    internal fun messageSnapshotForTest(): List<Pair<AiChatRole, String>> =
        chatAdapter.messageSnapshot()

    /** Cancels and clears any in-flight request without touching the user draft. */
    internal fun forceStopForTest() {
        if (isSending) stopActiveStreamByUser()
    }

    internal fun sendCurrentMessage() {
        if (isSending) {
            if (hasActiveStream) {
                stopActiveStreamByUser()
            }
            return
        }

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

        // Reject an unusable request URL before the request is registered as in
        // flight: the client would otherwise report that failure synchronously.
        if (!AiAssistantSettingsHelper.getMiniMaxConfig(requireContext()).hasValidRequestUrl()) {
            editMessage.error = getString(R.string.agent_error_invalid_url)
            openAiSettings()
            return
        }

        editMessage.error = null
        editMessage.setText("")

        recordTurn = AiAssistantSettingsHelper.getRecordToolsEnabled(requireContext())
        val createdAt = System.currentTimeMillis()
        val userMessage = AiChatMessage(
            sessionId = currentSessionId,
            role = AiChatRole.USER,
            content = content,
            createdAt = createdAt,
            isLocalOnly = recordTurn
        )
        databaseHelper.addAiChatMessage(userMessage)
        chatMessages.add(userMessage)
        chatAdapter.appendMessage(userMessage)
        refreshSessionList()
        scrollToBottom()

        val placeholderMessage = AiChatMessage(role = AiChatRole.ASSISTANT, content = "")
        chatMessages.add(placeholderMessage)
        chatAdapter.startStreamingMessage()

        val requestId = requestIdentity.begin(currentSessionId)
        val requestSessionId = currentSessionId

        isSending = true
        hasActiveStream = false
        updateSendingState(isSending, hasActiveStream)

        // Register the request as in flight before the sender runs. A sender may
        // report a failure synchronously (which has already been ruled out for the
        // predictable cases above), and the terminal handler must then be able to
        // clear the busy state instead of being overwritten by a late markStarted.
        requestLifecycle.registerPending()

        val handle = if (recordTurn) {
            val ledgerId = requireNotNull(databaseHelper.getCurrentLedger()).id
            val config = AiAssistantSettingsHelper.getMiniMaxConfig(requireContext())
            val previousContext = recordAssistant?.contextForNextTurn(config, ledgerId, requestSessionId)
            recordAssistant = AiRecordAssistant(
                requireContext(), AiAssistantSettingsHelper.getMiniMaxConfig(requireContext()),
                ledgerId, requestSessionId,
                onPending = { plan -> showRecordAction(plan) },
                onLocalResult = { receipt -> deliverAiResult(requestId, requestSessionId,
                    MiniMaxChatResult.StreamingChunk(receipt)) },
                onComplete = { result -> deliverAiResult(requestId, requestSessionId, result) }
            )
            recordAssistant!!.start(content, previousContext)
        } else {
            miniMaxClient.sendChat(
                config = AiAssistantSettingsHelper.getMiniMaxConfig(requireContext()),
                messages = chatMessages.filter { !it.isError && !it.isLocalOnly && it.content.isNotBlank() }.toList()
            ) { result -> deliverAiResult(requestId, requestSessionId, result) }
        }
        requestLifecycle.markStarted(handle)
    }

    private fun showRecordAction(plan: AiRecordEngine.Plan?) {
        recordActionPanel.visibility = if (plan == null) View.GONE else View.VISIBLE
        if (plan != null && isSending) {
            recordActionText.text = getString(R.string.ai_record_pending, plan.title)
            hasActiveStream = true // Keep the existing stop action available while awaiting approval.
            updateSendingState(true, true)
            textStatus.setText(R.string.ai_record_waiting)
        }
    }

    /**
     * Applies an AI callback only while it still belongs to the active request and
     * session. Kept separate from the sender lambda so the ordering contract (a
     * synchronous failure can arrive before [requestLifecycle.markStarted]) is
     * explicit and testable.
     */
    internal fun deliverAiResult(
        requestId: Long,
        requestSessionId: Long,
        result: MiniMaxChatResult
    ) {
        activity?.runOnUiThread {
            if (!isAdded) {
                return@runOnUiThread
            }

            if (!isCurrentRequest(requestId, requestSessionId)) {
                return@runOnUiThread
            }

            when (result) {
                is MiniMaxChatResult.StreamingChunk -> {
                    if (!isSending) return@runOnUiThread
                    if (!hasActiveStream) {
                        hasActiveStream = true
                        updateSendingState(isSending, hasActiveStream)
                    }
                    // Update both adapter and chatMessages with partial content
                    val partialContent = result.partialContent
                    chatAdapter.updateStreamingContent(partialContent, result.reasoning.orEmpty())
                    // Update chatMessages to keep it in sync
                    val lastIndex = chatMessages.size - 1
                    if (lastIndex >= 0 && chatMessages[lastIndex].role == AiChatRole.ASSISTANT) {
                        chatMessages[lastIndex] = AiChatMessage(
                            role = AiChatRole.ASSISTANT,
                            content = partialContent,
                            reasoning = result.reasoning?.takeIf { it.isNotBlank() },
                            isLocalOnly = recordTurn
                        )
                    }
                    scrollToBottom()
                }

                is MiniMaxChatResult.StreamingDone -> {
                    completeCurrentRequest(requestId, requestSessionId)
                    val finalMessage = AiChatMessage(
                        sessionId = requestSessionId,
                        role = AiChatRole.ASSISTANT,
                        content = result.finalContent,
                        reasoning = result.reasoning?.takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis(),
                        isLocalOnly = recordTurn
                    )
                    chatMessages[chatMessages.size - 1] = finalMessage
                    chatAdapter.finalizeStreamingMessage(result.finalContent, result.reasoning.orEmpty(), isError = false)
                    databaseHelper.addAiChatMessage(finalMessage)
                    refreshSessionList()
                    updateSendingState(isSending, hasActiveStream)
                }

                is MiniMaxChatResult.Success -> {
                    completeCurrentRequest(requestId, requestSessionId)
                    val finalMessage = AiChatMessage(
                        sessionId = requestSessionId,
                        role = AiChatRole.ASSISTANT,
                        content = result.reply,
                        reasoning = result.reasoning?.takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis(),
                        isLocalOnly = recordTurn
                    )
                    chatMessages[chatMessages.size - 1] = finalMessage
                    chatAdapter.finalizeStreamingMessage(result.reply, result.reasoning.orEmpty(), isError = false)
                    databaseHelper.addAiChatMessage(finalMessage)
                    refreshSessionList()
                    updateSendingState(isSending, hasActiveStream)
                }

                is MiniMaxChatResult.Failure -> {
                    completeCurrentRequest(requestId, requestSessionId)
                    val finalMessage = handleErrorResult(result, requestSessionId)
                    databaseHelper.addAiChatMessage(finalMessage)
                    refreshSessionList()
                    updateSendingState(isSending, hasActiveStream)
                }
            }
        }
    }

    /** Guards against stale callbacks: a request is only applied while it is current. */
    private fun isCurrentRequest(requestId: Long, requestSessionId: Long): Boolean {
        return requestIdentity.isCurrent(requestId, requestSessionId)
    }

    /** Marks the active request finished / expected to no longer receive callbacks. */
    private fun completeCurrentRequest(requestId: Long, requestSessionId: Long) {
        if (requestIdentity.isCurrent(requestId, requestSessionId)) {
            requestLifecycle.markFinished()
            hasActiveStream = false
            isSending = false
            requestIdentity.complete(requestId)
        }
    }

    /** Invalidates the current request so its remaining callbacks are dropped. */
    private fun invalidateCurrentRequest() {
        requestIdentity.invalidate()
    }

    private fun handleErrorResult(result: MiniMaxChatResult.Failure, requestSessionId: Long): AiChatMessage {
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
                sessionId = requestSessionId,
                role = AiChatRole.ASSISTANT,
                content = contentWithContext,
                isError = true,
                createdAt = System.currentTimeMillis(),
                isLocalOnly = recordTurn
            )
            chatMessages[chatMessages.size - 1] = finalMessage
            chatAdapter.finalizeStreamingMessage(contentWithContext, isError = true)
            scrollToBottom()
            return finalMessage
        } else {
            val errorContent = getErrorMessage(result)
            val errorMessage = AiChatMessage(
                sessionId = requestSessionId,
                role = AiChatRole.ASSISTANT,
                content = errorContent,
                isError = true,
                createdAt = System.currentTimeMillis(),
                isLocalOnly = recordTurn
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
        val idle = !sending
        val establishing = sending && !streaming
        val streamingActive = sending && streaming

        editMessage.isEnabled = configComplete && idle
        buttonMenu.isEnabled = idle
        buttonNewSession.isEnabled = idle
        sessionAdapter.interactionsEnabled = idle
        buttonMenu.alpha = if (idle) 1f else 0.4f
        buttonNewSession.alpha = if (idle) 1f else 0.4f

        val sendEnabled = configComplete && (idle || streamingActive)
        buttonSend.isEnabled = sendEnabled
        buttonSend.alpha = if (configComplete) 1f else 0.4f
        progressSending.visibility = if (establishing) View.VISIBLE else View.GONE
        imageSend.visibility = if (idle) View.VISIBLE else View.INVISIBLE
        imageStop.visibility = if (streamingActive) View.VISIBLE else View.GONE
        buttonSend.contentDescription = when {
            streamingActive -> getString(R.string.agent_stop_content_description)
            establishing -> getString(R.string.agent_status_sending)
            else -> getString(R.string.agent_send_content_description)
        }

        textStatus.text = when {
            !configComplete -> getString(R.string.agent_status_configuration_required)
            streaming -> getString(R.string.agent_status_receiving)
            sending -> getString(R.string.agent_status_sending)
            else -> getString(
                if (AiAssistantSettingsHelper.getRecordToolsEnabled(requireContext())) R.string.ai_record_ready else R.string.agent_status_ready,
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

    private fun initializeChatState(restoredState: AgentScreenState) {
        val sessions = databaseHelper.getAiChatSessions()
        val savedSessionId = AiAssistantSettingsHelper.getActiveSessionId(requireContext())
        currentSessionId = sessions.firstOrNull { it.id == restoredState.activeSessionId }?.id
            ?: sessions.firstOrNull { it.id == savedSessionId }?.id
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
        chatMessages.addAll(getSessionMessagesWithCurrentGreeting(currentSessionId))
        renderMessages()
    }

    private fun getSessionMessagesWithCurrentGreeting(sessionId: Long): List<AiChatMessage> {
        val messages = databaseHelper.getAiChatMessages(sessionId)
        val first = messages.firstOrNull() ?: return messages
        // Older app-generated greetings were persisted as assistant messages.
        // Refresh only the exact opening greeting, without rewriting chat history.
        if (first.role != AiChatRole.ASSISTANT || first.isError || first.isLocalOnly ||
            !first.reasoning.isNullOrBlank() || first.content !in legacyGreetings) {
            return messages
        }
        return messages.toMutableList().apply {
            this[0] = first.copy(content = getString(R.string.agent_greeting))
        }
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
            val sessionMessages = getSessionMessagesWithCurrentGreeting(session.id)
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
        if (!isSending) {
            return
        }
        stopActiveStreamByUser()
    }

    private fun stopActiveStreamByUser() {
        if (!isSending) {
            return
        }
        persistCurrentAssistantDraftBeforeForcedStop()
        invalidateCurrentRequest()
        hasActiveStream = false
        isSending = false
        requestLifecycle.cancelAndReset()
        updateSendingState(sending = false, streaming = false)
    }

    private fun persistCurrentAssistantDraftBeforeForcedStop() {
        if (currentSessionId <= 0L || chatMessages.isEmpty()) {
            return
        }

        val lastMessage = chatMessages.last()
        if (lastMessage.role != AiChatRole.ASSISTANT) {
            return
        }

        if (lastMessage.content.isBlank()) {
            // Still only an empty placeholder: drop it from the adapter and the
            // in-memory draft rather than persisting a blank error message.
            chatMessages.removeAt(chatMessages.size - 1)
            chatAdapter.discardStreamingPlaceholder()
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
            createdAt = System.currentTimeMillis(),
            isLocalOnly = recordTurn
        )
        chatMessages[chatMessages.size - 1] = finalMessage
        // Keep the adapter bubble and the persisted message in sync so the stop
        // state is visible immediately and consistent after a re-entry.
        chatAdapter.finalizeStreamingMessage(persistedContent, isError = true)
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
        (activity as? MainActivity)?.setBottomNavigationTemporarilyHidden(true)
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
        (activity as? MainActivity)?.setBottomNavigationTemporarilyHidden(false)
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

    private fun currentAgentState(): AgentScreenState {
        val messageScroll = recyclerScrollState(recyclerMessages)
        val sessionScroll = recyclerScrollState(recyclerSessions)
        return AgentScreenState(
            activeSessionId = currentSessionId,
            composerText = editMessage.text.toString(),
            isSessionDrawerOpen = isSessionDrawerOpen,
            messageScrollPosition = messageScroll?.first ?: 0,
            messageScrollOffset = messageScroll?.second ?: 0,
            sessionScrollPosition = sessionScroll?.first ?: 0,
            sessionScrollOffset = sessionScroll?.second ?: 0
        )
    }

    private fun restoreAgentViewState() {
        val state = restoredAgentState ?: return
        (recyclerMessages.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(state.messageScrollPosition, state.messageScrollOffset)
        (recyclerSessions.layoutManager as LinearLayoutManager)
            .scrollToPositionWithOffset(state.sessionScrollPosition, state.sessionScrollOffset)
        if (state.isSessionDrawerOpen &&
            AiAssistantSettingsHelper.getAiAssistantEnabled(requireContext()) &&
            AiAssistantSettingsHelper.isMiniMaxConfigComplete(requireContext())) {
            openSessionDrawer()
        }
    }

    private fun recyclerScrollState(recyclerView: RecyclerView): Pair<Int, Int>? {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return null
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position == RecyclerView.NO_POSITION) return null
        return position to (layoutManager.findViewByPosition(position)?.top ?: 0)
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

    companion object {
        private val legacyGreetings = setOf(
            "你好，我已接入 MiniMax 文本对话。你可以先问我预算思路、消费复盘，或让我们一起梳理一段财务想法。",
            "Hello. MiniMax text chat is ready here. Ask for budget thinking, spending reflections, or help structuring a financial thought."
        )

        // Injectable seam so a fake AiChatSender can drive stop/restart/late-callback
        // orchestration deterministically in tests. Production uses MiniMaxClient.
        @Volatile
        internal var senderFactory: () -> AiChatSender = { MiniMaxClient() }

        internal fun createDefaultSender(): AiChatSender = senderFactory.invoke()
    }
}
