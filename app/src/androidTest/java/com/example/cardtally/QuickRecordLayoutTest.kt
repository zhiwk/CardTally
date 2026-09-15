package com.example.cardtally

import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.CategorySelectorAdapter
import com.example.cardtally.adapter.RecordCategoryGroupAdapter
import com.example.cardtally.model.Category
import com.example.cardtally.util.RecordEntryMode
import com.example.cardtally.util.RecordEntryModePreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickRecordLayoutTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun themedContext() = ContextThemeWrapper(
        instrumentation.targetContext,
        R.style.Theme_CardTally_Light
    )

    private fun dp(value: Int): Int =
        (value * instrumentation.targetContext.resources.displayMetrics.density).toInt()

    @Test
    fun quickRecordLayout_hostsInlineGridAndPinnedKeypadWithoutStandardActions() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val container = FrameLayout(ctx)
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, container, false)

            assertNotNull(
                "quick layout must expose the inline category grid",
                screen.findViewById<RecyclerView>(R.id.recycler_quick_categories)
            )
            val keypad = screen.findViewById<View>(R.id.layout_amount_keypad)
            assertNotNull("quick layout must host the pinned keypad", keypad)
            assertEquals(
                "the pinned keypad is visible without opening it",
                View.VISIBLE,
                keypad!!.visibility
            )
            assertNull(
                "quick layout must not reuse the standard bottom action bar",
                screen.findViewById<View>(R.id.layout_buttons)
            )
            assertNotNull("quick layout keeps the close button", screen.findViewById<View>(R.id.btn_close))
            assertNotNull("quick layout keeps the transfer swap control", screen.findViewById<View>(R.id.btn_swap_transfer_assets))
            assertNotNull("source account row needs a disclosure indicator", screen.findViewById<View>(R.id.image_source_asset_chevron))
            assertNotNull("destination account row needs a disclosure indicator", screen.findViewById<View>(R.id.image_destination_asset_chevron))
            assertNotNull("source account card needs a current balance", screen.findViewById<View>(R.id.text_source_asset_balance))
            assertNotNull("destination account card needs a current balance", screen.findViewById<View>(R.id.text_destination_asset_balance))

            val panel = screen.findViewById<View>(R.id.quick_record_panel)
            assertNotNull("amount/note/date/asset/photo must live in one combined panel", panel)
            assertNotNull("note field must sit in the combined panel", panel!!.findViewById<View>(R.id.edit_description))
            assertNotNull("amount field must sit in the combined panel", panel.findViewById<View>(R.id.edit_amount))
            assertNotNull("date control must sit in the combined panel", panel.findViewById<View>(R.id.row_date))
            assertNotNull("photo control must sit in the combined panel", panel.findViewById<View>(R.id.btn_take_photo))
            assertNotNull("photo count must sit in the combined panel", panel.findViewById<View>(R.id.text_photo_count))
            assertNotNull("single-asset row must sit in the combined panel", panel.findViewById<View>(R.id.row_asset_single))
            assertNotNull("fee row must sit in the combined panel", panel.findViewById<View>(R.id.row_fee))
            assertNotNull("fee field must exist", panel.findViewById<View>(R.id.edit_fee))
            assertNotNull(
                "quick layout must offer the transfer account cards",
                screen.findViewById<View>(R.id.transfer_accounts_block)
            )
            assertTrue(
                "quick transfer accounts must stay inside one card",
                screen.findViewById<View>(R.id.transfer_accounts_block)
                    is com.google.android.material.card.MaterialCardView
            )
            assertNotNull(
                "quick transfer account card must expose the direct adaptive container",
                screen.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.scroll_transfer_accounts)
            )
            assertTrue(
                "source account name must be single-line marquee",
                screen.findViewById<TextView>(R.id.text_asset_value).isSingleLine
            )
            assertTrue(
                "destination account name must be single-line marquee",
                screen.findViewById<TextView>(R.id.text_destination_asset_value).isSingleLine
            )
            assertEquals(
                "source and destination account values must use the same text size",
                screen.findViewById<TextView>(R.id.text_asset_value).textSize,
                screen.findViewById<TextView>(R.id.text_destination_asset_value).textSize,
                0.01f
            )
            assertEquals(
                "transfer card must not reserve bottom padding below the destination card",
                0,
                screen.findViewById<View>(R.id.scroll_transfer_accounts).paddingBottom
            )
            assertEquals(
                "source account card height must follow its content",
                ViewGroup.LayoutParams.WRAP_CONTENT,
                screen.findViewById<View>(R.id.row_asset).layoutParams.height
            )
            assertEquals(
                "destination account card height must follow its content",
                ViewGroup.LayoutParams.WRAP_CONTENT,
                screen.findViewById<View>(R.id.row_destination_asset).layoutParams.height
            )
            val destinationParams = screen.findViewById<View>(R.id.row_destination_asset)
                .layoutParams as ConstraintLayout.LayoutParams
            assertEquals(
                "destination card must be anchored to the adaptive container bottom",
                ConstraintLayout.LayoutParams.PARENT_ID,
                destinationParams.bottomToBottom
            )
            assertEquals(
                "destination card must be anchored to the adaptive container end",
                ConstraintLayout.LayoutParams.PARENT_ID,
                destinationParams.endToEnd
            )
            val flowParams = screen.findViewById<View>(R.id.transfer_flow).layoutParams
            assertEquals(
                "flow curve must stretch across the card gap horizontally",
                0,
                flowParams.width
            )
            assertEquals(
                "flow curve must follow the adaptive card height",
                0,
                flowParams.height
            )
        }
    }

    @Test
    fun sharedEntryShell_exposesTransferCardsAndFeeField() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val container = FrameLayout(ctx)
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, container, false)
            assertNotNull("shared shell needs the transfer cards block", screen.findViewById<View>(R.id.transfer_accounts_block))
            assertNotNull("shared shell needs the source card", screen.findViewById<View>(R.id.row_asset))
            assertNotNull("shared shell needs the destination card", screen.findViewById<View>(R.id.row_destination_asset))
            assertNotNull("shared shell needs the swap control", screen.findViewById<View>(R.id.btn_swap_transfer_assets))
            assertNotNull("shared shell needs the fee row", screen.findViewById<View>(R.id.row_fee))
            assertNotNull("shared shell needs the fee field", screen.findViewById<View>(R.id.edit_fee))
            assertNotNull("shared shell needs the single-asset row", screen.findViewById<View>(R.id.row_asset_single))
            assertNotNull("shared shell needs the standard category cards", screen.findViewById<View>(R.id.recycler_standard_categories))
        }
    }

    @Test
    fun keypadTargetSwitch_editsTheSelectedNumericField() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val root = LinearLayout(ctx)
            val keypad = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.layout_amount_keypad_quick, root, false)
            root.addView(keypad)
            val amount = android.widget.EditText(ctx)
            val fee = android.widget.EditText(ctx)
            root.addView(amount)
            root.addView(fee)

            val controller = com.example.cardtally.util.AmountKeypadController(
                ctx,
                amount,
                keypad,
                null,
                alwaysVisible = true,
                onConfirm = {}
            )
            controller.bind()
            controller.bindTarget(fee)

            controller.selectTarget(fee)
            keypad.findViewById<View>(R.id.keypad_5).performClick()
            assertEquals("5", fee.text.toString())
            assertEquals("", amount.text.toString())

            controller.selectTarget(amount)
            keypad.findViewById<View>(R.id.keypad_7).performClick()
            assertEquals("7", amount.text.toString())
            assertEquals("5", fee.text.toString())
        }
    }

    @Test
    fun entryModes_reorderIndependentCategoryUnits() {
        val categories = listOf(
            Category(id = 1, name = "购物"),
            Category(id = 2, name = "餐饮"),
            Category(id = 3, name = "超市", parentId = 1),
            Category(id = 4, name = "外卖", parentId = 2)
        )
        val quickAdapter = CategorySelectorAdapter(categories, null) { }
        val standardAdapter = RecordCategoryGroupAdapter(categories, null) { }

        quickAdapter.moveCategory(0, 1)
        standardAdapter.moveParent(0, 1)

        assertEquals(listOf(4L, 3L), quickAdapter.categoryIdsInOrder())
        assertEquals(listOf(2L, 1L), standardAdapter.parentIdsInOrder())
    }

    @Test
    fun quickLayout_keepsKeypadPinnedToBottomWhenCategoryCardIsHidden() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val container = FrameLayout(ctx)
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, container, false)
            val width = dp(360)
            val height = dp(640)

            fun measureAndLayout() {
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, width, height)
            }

            measureAndLayout()
            val keypad = screen.findViewById<View>(R.id.layout_amount_keypad)
            val panel = screen.findViewById<View>(R.id.quick_record_panel)
            assertTrue(
                "expense/income: keypad must sit on the bottom edge",
                Math.abs(keypad.bottom - height) <= 1
            )
            assertTrue("expense/income: panel must stay above the keypad", panel.bottom <= keypad.top)

            screen.findViewById<View>(R.id.card_quick_categories).visibility = View.GONE
            measureAndLayout()
            assertTrue(
                "transfer: keypad must stay on the bottom edge when the category card is hidden",
                Math.abs(keypad.bottom - height) <= 1
            )
            assertTrue("transfer: panel must stay above the keypad", panel.bottom <= keypad.top)
        }
    }

    @Test
    fun transferCard_resizesWithTheAvailableHeightWithoutClippingAccounts() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val container = FrameLayout(ctx)
            val screen = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.fragment_add_record_quick, container, false)
            val transferCard = screen.findViewById<View>(R.id.transfer_accounts_block)
            val categoryCard = screen.findViewById<View>(R.id.card_quick_categories)
            val panel = screen.findViewById<View>(R.id.quick_record_panel)
            val keypad = screen.findViewById<View>(R.id.layout_amount_keypad)
            val accounts = screen.findViewById<View>(R.id.scroll_transfer_accounts)
            val source = screen.findViewById<View>(R.id.row_asset)
            val destination = screen.findViewById<View>(R.id.row_destination_asset)
            val flow = screen.findViewById<View>(R.id.transfer_flow)

            transferCard.visibility = View.VISIBLE
            categoryCard.visibility = View.GONE
            keypad.visibility = View.GONE

            fun measureAndLayout(heightDp: Int) {
                screen.measure(
                    View.MeasureSpec.makeMeasureSpec(dp(360), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(heightDp), View.MeasureSpec.EXACTLY)
                )
                screen.layout(0, 0, dp(360), dp(heightDp))
            }

            measureAndLayout(640)
            val fullHeight = transferCard.height
            val fullFlowHeight = flow.height

            measureAndLayout(400)
            assertTrue("transfer card must shrink with the available height", transferCard.height < fullHeight)
            assertEquals(
                "transfer card must end at the form panel with its configured bottom margin",
                panel.top - dp(12),
                transferCard.bottom
            )
            assertTrue("source account must remain inside the transfer card", source.top >= accounts.paddingTop)
            assertTrue(
                "destination account must remain inside the transfer card",
                destination.bottom <= accounts.height
            )
            assertTrue("flow curve must shrink with the available height", flow.height < fullFlowHeight)
            assertEquals(
                "flow curve must fill the adaptive container height",
                accounts.height - accounts.paddingTop - accounts.paddingBottom,
                flow.height
            )
        }
    }

    @Test
    fun switchingFromTransferNoteToIncome_restoresAmountFocusAndQuickLayout() {
        val originalMode = RecordEntryModePreferences.getMode(instrumentation.targetContext)
        RecordEntryModePreferences.saveMode(instrumentation.targetContext, RecordEntryMode.QUICK)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    activity.supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AddRecordFragment())
                        .commitNow()

                    val root = requireNotNull(
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                    )
                    val note = root.findViewById<android.widget.EditText>(R.id.edit_description)
                    val amount = root.findViewById<android.widget.EditText>(R.id.edit_amount)
                    val keypad = root.findViewById<View>(R.id.layout_amount_keypad)
                    val transferBlock = root.findViewById<View>(R.id.transfer_accounts_block)
                    val panel = root.findViewById<View>(R.id.quick_record_panel)

                    root.findViewById<View>(R.id.btn_transfer).performClick()
                    note.requestFocus()
                    assertTrue("note must receive focus before switching type", note.hasFocus())
                    val notePanelParams = panel.layoutParams as ConstraintLayout.LayoutParams
                    assertEquals(
                        "transfer note panel must remain pinned above the amount keypad",
                        keypad.id,
                        notePanelParams.bottomToTop
                    )
                    assertEquals(
                        "transfer note panel must not add a top constraint",
                        ConstraintLayout.LayoutParams.UNSET,
                        notePanelParams.topToBottom
                    )
                    val transferParams = transferBlock.layoutParams as ConstraintLayout.LayoutParams
                    assertEquals(
                        "transfer card must fill the shared content area",
                        0,
                        transferParams.height
                    )
                    assertEquals(
                        "transfer card must end at the form panel",
                        panel.id,
                        transferParams.bottomToTop
                    )
                    assertTrue(
                        "transfer content must register as a scroll container so the window resizes instead of panning",
                        root.findViewById<View>(R.id.scroll_transfer_accounts).isScrollContainer
                    )
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = requireNotNull(
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                    )
                    val note = root.findViewById<android.widget.EditText>(R.id.edit_description)

                    root.findViewById<View>(R.id.btn_income).performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = requireNotNull(
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                    )
                    val note = root.findViewById<android.widget.EditText>(R.id.edit_description)
                    val amount = root.findViewById<android.widget.EditText>(R.id.edit_amount)
                    val keypad = root.findViewById<View>(R.id.layout_amount_keypad)
                    val transferBlock = root.findViewById<View>(R.id.transfer_accounts_block)
                    val panel = root.findViewById<View>(R.id.quick_record_panel)

                    assertFalse("income must clear the note focus", note.hasFocus())
                    assertTrue("income must activate the amount input", amount.hasFocus())
                    assertTrue("quick amount keypad must be restored", keypad.isShown)
                    assertEquals("transfer accounts must be hidden for income", View.GONE, transferBlock.visibility)
                    val panelParams = panel.layoutParams as ConstraintLayout.LayoutParams
                    assertEquals(
                        "income panel must remain pinned above the amount keypad",
                        keypad.id,
                        panelParams.bottomToTop
                    )
                    assertEquals(
                        "income panel must not inherit the transfer-card top constraint",
                        ConstraintLayout.LayoutParams.UNSET,
                        panelParams.topToBottom
                    )
                    val categoryParams = root.findViewById<View>(R.id.card_quick_categories)
                        .layoutParams as ConstraintLayout.LayoutParams
                    assertEquals(
                        "income category card must fill down to the form panel",
                        panel.id,
                        categoryParams.bottomToTop
                    )

                    root.findViewById<View>(R.id.btn_expense).performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = requireNotNull(
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                    )
                    val panel = root.findViewById<View>(R.id.quick_record_panel)
                    val categoryCard = root.findViewById<View>(R.id.card_quick_categories)
                    val panelParams = panel.layoutParams as ConstraintLayout.LayoutParams
                    val categoryParams = categoryCard.layoutParams as ConstraintLayout.LayoutParams

                    assertEquals("expense must hide transfer accounts", View.GONE,
                        root.findViewById<View>(R.id.transfer_accounts_block).visibility)
                    assertEquals(
                        "expense panel must not inherit the transfer-card top constraint",
                        ConstraintLayout.LayoutParams.UNSET,
                        panelParams.topToBottom
                    )
                    assertEquals(
                        "expense category card must fill down to the form panel",
                        panel.id,
                        categoryParams.bottomToTop
                    )
                }
            }
        } finally {
            RecordEntryModePreferences.saveMode(instrumentation.targetContext, originalMode)
        }
    }

    @Test
    fun quickKeypad_integratesSaveAndSaveAndAddAsReadableTargets() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val root = LinearLayout(ctx)
            val keypad = android.view.LayoutInflater.from(ctx)
                .inflate(R.layout.layout_amount_keypad_quick, root, false)
            keypad.visibility = View.VISIBLE
            root.addView(keypad)
            val width = dp(360)
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(600), View.MeasureSpec.AT_MOST)
            )
            root.layout(0, 0, width, root.measuredHeight)

            val digitIds = listOf(
                R.id.keypad_0, R.id.keypad_1, R.id.keypad_2, R.id.keypad_3, R.id.keypad_4,
                R.id.keypad_5, R.id.keypad_6, R.id.keypad_7, R.id.keypad_8, R.id.keypad_9,
                R.id.keypad_dot, R.id.keypad_minus, R.id.keypad_plus, R.id.keypad_delete
            )
            digitIds.forEach { id ->
                val key = keypad.findViewById<View>(id)
                assertNotNull("quick keypad must keep key $id", key)
                assertTrue(
                    "quick keypad key $id must be at least 48dp",
                    key!!.width >= dp(48) && key.height >= dp(48)
                )
            }

            val save = keypad.findViewById<TextView>(R.id.btn_save)
            val saveAndAdd = keypad.findViewById<TextView>(R.id.btn_save_and_add)
            assertNotNull("quick keypad must integrate the complete action", save)
            assertNotNull("quick keypad must integrate the save-and-add action", saveAndAdd)
            assertTrue("complete action needs a label", save!!.text.isNotBlank())
            assertTrue("save-and-add action needs a label", saveAndAdd!!.text.isNotBlank())
            assertTrue(
                "both integrated actions must be at least 48dp",
                save.width >= dp(48) && save.height >= dp(48) &&
                    saveAndAdd.width >= dp(48) && saveAndAdd.height >= dp(48)
            )
        }
    }

    @Test
    fun categoryGrid_onlyExposesLeafCategoriesAndSelectsStableId() {
        instrumentation.runOnMainSync {
            val ctx = themedContext()
            val categories = listOf(
                Category(id = 1, name = "Food", type = 0, parentId = null),
                Category(id = 2, name = "Breakfast", type = 0, parentId = 1),
                Category(id = 3, name = "Lunch", type = 0, parentId = 1),
                Category(id = 4, name = "Travel", type = 0, parentId = null)
            )
            val selected = mutableListOf<Long>()
            val adapter = CategorySelectorAdapter(categories, null) { selected += it.id }

            assertEquals("parent categories must not be selectable", 3, adapter.itemCount)

            val recycler = RecyclerView(ctx).apply {
                layoutManager = GridLayoutManager(ctx, 3)
                this.adapter = adapter
            }
            val container = FrameLayout(ctx)
            container.addView(recycler)
            val width = dp(360)
            container.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(400), View.MeasureSpec.AT_MOST)
            )
            container.layout(0, 0, width, container.measuredHeight)

            val rendered = (0 until recycler.childCount).map { recycler.getChildViewHolder(recycler.getChildAt(it)).adapterPosition }
            assertTrue("all three leaves must be rendered", rendered.containsAll(listOf(0, 1, 2)))

            recycler.getChildAt(rendered.indexOf(0)).performClick()
            assertTrue("only leaf ids are reported", selected.isNotEmpty() && selected.all { it in listOf(2L, 3L, 4L) })
            assertEquals("selection is tracked by stable id", selected.last(), adapter.getSelectedCategoryId())
        }
    }
}
