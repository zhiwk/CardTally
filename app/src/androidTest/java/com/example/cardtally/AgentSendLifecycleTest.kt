package com.example.cardtally

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import com.example.cardtally.network.AiChatSender
import com.example.cardtally.network.MiniMaxChatResult
import com.example.cardtally.network.MiniMaxConfig
import com.example.cardtally.network.MiniMaxErrorType
import com.example.cardtally.network.MiniMaxRequestHandle
import com.example.cardtally.util.AiAssistantSettingsHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real AgentFragment send lifecycle with a fake [AiChatSender] — the
 * only place the stop/restart/late-callback ordering and the synchronous-failure
 * race can be observed end to end.
 *
 * Safety: no network request is made, the API key is replaced before the test and
 * cleared afterwards, and only synthetic chat text is written. Financial data is
 * never read or modified.
 */
@RunWith(AndroidJUnit4::class)
class AgentSendLifecycleTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    /** Records how the sender was invoked and lets the test choose sync/async. */
    private class FakeSender(
        private val behaviour: (callback: (MiniMaxChatResult) -> Unit) -> Unit
    ) : AiChatSender {
        val handle = MiniMaxRequestHandle { }

        override fun sendChat(
            config: MiniMaxConfig,
            messages: List<AiChatMessage>,
            callback: (MiniMaxChatResult) -> Unit
        ): MiniMaxRequestHandle {
            behaviour(callback)
            return handle
        }
    }

    /** Captures the callback of the most recent send so the test can fire it. */
    private class CapturingSender : AiChatSender {
        private var callback: ((MiniMaxChatResult) -> Unit)? = null
        val handle = MiniMaxRequestHandle { }

        override fun sendChat(
            config: MiniMaxConfig,
            messages: List<AiChatMessage>,
            callback: (MiniMaxChatResult) -> Unit
        ): MiniMaxRequestHandle {
            this.callback = callback
            return handle
        }

        fun emit(result: MiniMaxChatResult) {
            callback?.invoke(result)
        }
    }

    @Before
    fun setUp() {
        AiAssistantSettingsHelper.saveRecordToolsEnabled(context, false)
        AiAssistantSettingsHelper.saveConfiguration(context, MiniMaxConfig(
            apiKey = "test-key-no-network",
            model = "test-model",
            requestUrl = "https://service.example/v1/chat/completions"
        ))
        AiAssistantSettingsHelper.saveAiAssistantEnabled(context, true)
    }

    @After
    fun tearDown() {
        AgentFragment.senderFactory = { com.example.cardtally.network.MiniMaxClient() }
        AiAssistantSettingsHelper.saveApiKey(context, "")
        AiAssistantSettingsHelper.saveAiAssistantEnabled(context, false)
    }

    private fun agentOf(scenario: ActivityScenario<MainActivity>): AgentFragment {
        var fragment: AgentFragment? = null
        scenario.onActivity { activity ->
            fragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as? AgentFragment
        }
        return requireNotNull(fragment) { "AgentFragment must be attached" }
    }

    /**
     * Runs [action] on the main thread, lets the main looper drain, then runs
     * [assertion]. [between] runs on the instrumentation thread in between, so it
     * can use runOnMainSync to deliver a fake callback.
     */
    private fun withAgent(
        sender: AiChatSender,
        action: (AgentFragment) -> Unit,
        between: (() -> Unit)? = null,
        assertion: (AgentFragment) -> Unit
    ) {
        AgentFragment.senderFactory = { sender }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AgentFragment())
                    .commitNow()
            }
            instrumentation.waitForIdleSync()
            val agent = agentOf(scenario)
            scenario.onActivity { action(agent) }
            instrumentation.waitForIdleSync()
            between?.invoke()
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertion(agent) }
        }
    }

    private fun assistantTexts(fragment: AgentFragment): List<String> =
        fragment.messageSnapshotForTest()
            .filter { it.first == AiChatRole.ASSISTANT }
            .map { it.second }

    @Test
    fun missingConfiguration_showsRecoveryAndNeverInvokesSender() {
        AiAssistantSettingsHelper.saveApiKey(context, "")
        var sendCount = 0
        val sender = FakeSender { sendCount += 1 }

        withAgent(
            sender = sender,
            action = { fragment ->
                val view = requireNotNull(fragment.view)
                assertEquals(
                    "missing configuration must expose the local recovery card",
                    android.view.View.VISIBLE,
                    view.findViewById<android.view.View>(R.id.layout_agent_config_missing).visibility
                )
                assertFalse(
                    "composer must be disabled until configuration is complete",
                    view.findViewById<android.widget.EditText>(R.id.edit_agent_message).isEnabled
                )
                assertTrue(
                    "the settings recovery action must remain reachable",
                    view.findViewById<android.view.View>(R.id.button_open_ai_settings).isClickable
                )
                fragment.setComposerTextForTest("must not leave device")
                fragment.sendCurrentMessage()
            },
            assertion = { fragment ->
                assertEquals("sender must not be invoked without configuration", 0, sendCount)
                assertFalse("missing configuration is an idle state", fragment.isSending)
            }
        )
    }

    @Test
    fun synchronousFailure_leavesComposerUsableAndLifecycleIdle() {
        // Reported before sendChat returns its handle: the ordering that previously
        // stranded the in-flight lifecycle as loading forever.
        val sender = FakeSender { callback ->
            callback(MiniMaxChatResult.Failure(MiniMaxErrorType.INVALID_URL, "not-a-url"))
        }

        withAgent(
            sender = sender,
            action = { fragment ->
                fragment.setComposerTextForTest("synthetic probe")
                fragment.sendCurrentMessage()
            },
            assertion = { fragment ->
                assertFalse("stuck busy state after a synchronous failure", fragment.isSending)
                assertTrue("failure must be rendered", assistantTexts(fragment).any { it.isNotBlank() })
            }
        )
    }

    @Test
    fun asynchronousFailure_clearsBusyStateAfterCallbackArrives() {
        val sender = CapturingSender()

        withAgent(
            sender = sender,
            action = { fragment ->
                fragment.setComposerTextForTest("synthetic probe")
                fragment.sendCurrentMessage()
                // While the request is genuinely establishing, the page is busy.
                assertTrue("request must be in flight before the callback", fragment.isSending)
            },
            between = {
                instrumentation.runOnMainSync {
                    sender.emit(MiniMaxChatResult.Failure(MiniMaxErrorType.NETWORK, "offline"))
                }
            },
            assertion = { fragment ->
                assertFalse("busy state must clear once the failure is reported", fragment.isSending)
            }
        )
    }

    @Test
    fun streamingChunkThenDone_rendersFinalTextAndReleasesComposer() {
        val sender = CapturingSender()

        withAgent(
            sender = sender,
            action = { fragment ->
                fragment.setComposerTextForTest("synthetic probe")
                fragment.sendCurrentMessage()
                assertTrue("streaming request must be busy", fragment.isSending)
            },
            between = {
                instrumentation.runOnMainSync {
                    sender.emit(MiniMaxChatResult.StreamingChunk("partial"))
                    sender.emit(MiniMaxChatResult.StreamingDone("final answer"))
                }
            },
            assertion = { fragment ->
                assertFalse("stream finished, composer must be usable", fragment.isSending)
                assertTrue(
                    "final text must be rendered",
                    assistantTexts(fragment).contains("final answer")
                )
            }
        )
    }

    @Test
    fun stoppedRequest_ignoresLateCallbacks() {
        val sender = CapturingSender()

        withAgent(
            sender = sender,
            action = { fragment ->
                fragment.setComposerTextForTest("synthetic probe")
                fragment.sendCurrentMessage()
                fragment.forceStopForTest()
                assertFalse("stop must release the busy state", fragment.isSending)
            },
            between = {
                // A late terminal for the stopped request must not resurrect it.
                instrumentation.runOnMainSync {
                    sender.emit(MiniMaxChatResult.StreamingDone("stale answer"))
                }
            },
            assertion = { fragment ->
                assertFalse("late callback must not mark the page busy", fragment.isSending)
                assertFalse(
                    "stopped request text must not be finalised",
                    assistantTexts(fragment).contains("stale answer")
                )
            }
        )
    }
}
