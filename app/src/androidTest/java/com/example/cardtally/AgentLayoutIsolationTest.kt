package com.example.cardtally

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.AgentChatAdapter
import com.example.cardtally.model.AiChatMessage
import com.example.cardtally.model.AiChatRole
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Layout-only tests: no Activity, preferences, database, or network access. */
@RunWith(AndroidJUnit4::class)
class AgentLayoutIsolationTest {
    private fun context(scale: Float): ContextThemeWrapper {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(base.resources.configuration).apply { fontScale = scale }
        return ContextThemeWrapper(base.createConfigurationContext(configuration), R.style.Theme_CardTally_Light)
    }

    private fun measure(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    @Test fun configuredPage_largeFontAndMultilineComposer_doNotOverlap() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (scale in listOf(1f, 1.3f, 2f)) {
                val ctx = context(scale)
                val density = ctx.resources.displayMetrics.density
                val root = LayoutInflater.from(ctx).inflate(R.layout.fragment_agent, FrameLayout(ctx), false)
                root.findViewById<View>(R.id.layout_agent_config_missing).visibility = View.GONE
                root.findViewById<EditText>(R.id.edit_agent_message).setText("Test line one\nTest line two\nTest line three")
                measure(root, (360 * density).toInt(), (640 * density).toInt())
                val header = root.findViewById<View>(R.id.appbar)
                val content = root.findViewById<View>(R.id.layout_agent_content)
                val composer = root.findViewById<View>(R.id.layout_agent_composer_container)
                assertTrue("header overlaps content at $scale", header.bottom <= content.top)
                assertTrue("content overlaps composer at $scale", content.bottom <= composer.top)
                assertTrue("composer outside page at $scale", composer.bottom <= root.height)
                assertTrue("content collapsed at $scale", content.height > 0)
            }
        }
    }

    @Test fun configurationCard_largeFont_canScrollWithoutCoveringComposer() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val ctx = context(2f)
            val density = ctx.resources.displayMetrics.density
            val root = LayoutInflater.from(ctx).inflate(R.layout.fragment_agent, FrameLayout(ctx), false)
            val card = root.findViewById<View>(R.id.layout_agent_config_missing)
            card.visibility = View.VISIBLE
            measure(root, (360 * density).toInt(), (520 * density).toInt())
            val content = root.findViewById<View>(R.id.layout_agent_content)
            assertTrue(card.bottom <= content.height - content.paddingBottom)
            assertTrue("Configuration action must remain reachable by scrolling", card.canScrollVertically(1))
        }
    }

    @Test fun longMessages_andStreamingPayload_fitAndKeepText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (scale in listOf(1f, 2f)) {
                val ctx = context(scale)
                val parent = FrameLayout(ctx)
                val adapter = AgentChatAdapter()
                val longText = "Synthetic test message. 合成测试内容。".repeat(30)
                adapter.submitMessages(listOf(
                    AiChatMessage(role = AiChatRole.USER, content = longText),
                    AiChatMessage(role = AiChatRole.ASSISTANT, content = longText, isError = true)))
                for (position in 0..1) {
                    val holder = adapter.onCreateViewHolder(parent, 0)
                    adapter.onBindViewHolder(holder, position)
                    val width = (328 * ctx.resources.displayMetrics.density).toInt()
                    holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    holder.itemView.layout(0, 0, width, holder.itemView.measuredHeight)
                    assertEquals(longText, holder.textMessage.text.toString())
                    assertTrue(holder.textMessage.left >= 0 && holder.textMessage.right <= width)
                    assertTrue(holder.textMessage.lineCount > 1)
                }
                val index = adapter.startStreamingMessage()
                val holder = adapter.onCreateViewHolder(parent, 0)
                adapter.onBindViewHolder(holder, index)
                adapter.updateStreamingContent(longText)
                adapter.onBindViewHolder(holder, index, mutableListOf("streaming_content"))
                assertEquals(longText, holder.textMessage.text.toString())
            }
        }
    }
}
