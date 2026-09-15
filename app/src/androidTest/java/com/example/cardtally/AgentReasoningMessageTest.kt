package com.example.cardtally

import android.view.ContextThemeWrapper
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.AgentChatAdapter
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AgentReasoningMessageTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun themedContext() = ContextThemeWrapper(
        instrumentation.targetContext,
        R.style.Theme_CardTally_Light
    )

    private fun bindFirst(message: AiChatMessage): AgentChatAdapter.AgentChatViewHolder {
        val adapter = AgentChatAdapter()
        adapter.submitMessages(listOf(message))
        val parent = FrameLayout(themedContext())
        val holder = adapter.onCreateViewHolder(parent, 0)
        adapter.onBindViewHolder(holder, 0)
        return holder
    }

    @Test
    fun assistantReasoning_showsCollapsedByDefaultAndExpandsOnTap() {
        instrumentation.runOnMainSync {
            val adapterContext = themedContext()
            val adapter = AgentChatAdapter()
            adapter.submitMessages(
                listOf(
                    AiChatMessage(
                        id = 7L,
                        role = AiChatRole.ASSISTANT,
                        content = "answer",
                        reasoning = "thinking text",
                        createdAt = 1L
                    )
                )
            )
            val parent = FrameLayout(adapterContext)
            val holder = adapter.onCreateViewHolder(parent, 0)
            adapter.onBindViewHolder(holder, 0)

            assertEquals(View.VISIBLE, holder.layoutReasoning.visibility)
            assertEquals("thinking text", holder.textReasoningBody.text.toString())
            assertEquals("collapsed by default shows a short preview", 2, holder.textReasoningBody.maxLines)

            holder.rowReasoningHeader.performClick()
            adapter.onBindViewHolder(holder, 0)
            assertEquals(Int.MAX_VALUE, holder.textReasoningBody.maxLines)
        }
    }

    @Test
    fun messageWithoutReasoning_hidesTheReasoningBlock() {
        instrumentation.runOnMainSync {
            val holder = bindFirst(
                AiChatMessage(id = 8L, role = AiChatRole.ASSISTANT, content = "answer", createdAt = 1L)
            )
            assertEquals(View.GONE, holder.layoutReasoning.visibility)
        }
    }

    @Test
    fun userMessage_neverShowsReasoning() {
        instrumentation.runOnMainSync {
            val holder = bindFirst(
                AiChatMessage(
                    id = 9L,
                    role = AiChatRole.USER,
                    content = "question",
                    reasoning = "should not matter",
                    createdAt = 1L
                )
            )
            assertEquals(View.GONE, holder.layoutReasoning.visibility)
        }
    }

    @Test
    fun streamingReasoning_autoExpandsUntilAnswerStarts() {
        instrumentation.runOnMainSync {
            val adapterContext = themedContext()
            val adapter = AgentChatAdapter()
            adapter.startStreamingMessage()
            adapter.updateStreamingContent("", "thinking...")
            val parent = FrameLayout(adapterContext)
            val holder = adapter.onCreateViewHolder(parent, 0)

            adapter.onBindViewHolder(holder, 0)
            assertEquals(View.VISIBLE, holder.layoutReasoning.visibility)
            assertEquals(Int.MAX_VALUE, holder.textReasoningBody.maxLines)

            adapter.updateStreamingContent("answer", "thinking...")
            adapter.onBindViewHolder(holder, 0)
            assertTrue(
                "reasoning collapses once the answer starts",
                holder.textReasoningBody.maxLines == 2
            )
        }
    }
}
