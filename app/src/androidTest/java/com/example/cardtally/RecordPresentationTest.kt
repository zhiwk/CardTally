package com.example.cardtally

import android.graphics.Typeface
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.adapter.DateGroupAdapter
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.LedgerSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordPresentationTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Test
    fun recordRows_showCategoryAndNoteOnSeparateLines_andTransferRouteAsTitle() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(
                instrumentation.targetContext,
                R.style.Theme_CardTally_Light
            )
            val parent = FrameLayout(context)
            val view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
            val holder = DateGroupAdapter.RecordViewHolder(view)
            val listener = object : DateGroupAdapter.OnRecordActionListener {
                override fun onOpenDetails(record: Record) = Unit
                override fun onEdit(record: Record) = Unit
                override fun onDelete(record: Record) = Unit
                override fun onMultiSelectChanged(selectedCount: Int) = Unit
                override fun onDeleteSelected(records: List<Record>) = Unit
                override fun onEnterMultiSelectMode(record: Record) = Unit
                override fun onToggleMultiSelect(record: Record) = Unit
            }

            holder.bind(
                Record(
                    category = "餐饮",
                    categoryNameSnapshot = "早餐",
                    description = "和同事一起吃饭",
                    type = 0,
                    assetSource = "招商银行"
                ),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals("早餐", view.findViewById<TextView>(R.id.text_category).text.toString())
            assertEquals(
                Typeface.NORMAL,
                view.findViewById<TextView>(R.id.text_category).typeface.style
            )
            assertEquals("和同事一起吃饭", view.findViewById<TextView>(R.id.text_description).text.toString())
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_description).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_asset).visibility)

            holder.bind(
                Record(
                    category = "资产转资产",
                    categoryNameSnapshot = "不应显示",
                    description = null,
                    type = 2,
                    assetSource = "余额宝",
                    destinationAssetSource = "支付宝一般基金"
                ),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals(
                "余额宝 → 支付宝一般基金",
                view.findViewById<TextView>(R.id.text_category).text.toString()
            )
            assertEquals(
                Typeface.NORMAL,
                view.findViewById<TextView>(R.id.text_category).typeface.style
            )
            assertEquals(View.GONE, view.findViewById<View>(R.id.text_description).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.text_asset).visibility)

            holder.bind(
                Record(category = "工资", description = "本月工资", type = 1),
                listener,
                isMultiSelect = false,
                isSelected = false,
                showTypeSubtitle = false
            )
            assertEquals("工资", view.findViewById<TextView>(R.id.text_category).text.toString())
            assertEquals("本月工资", view.findViewById<TextView>(R.id.text_description).text.toString())
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.text_description).visibility)
        }
    }

    @Test
    fun recordClick_opensDetailsWithoutRevealingSwipeActions() {
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_CardTally_Light)
            val root = LayoutInflater.from(context).inflate(R.layout.item_record, FrameLayout(context), false)
            var openedId = 0L
            val listener = object : DateGroupAdapter.OnRecordActionListener {
                override fun onOpenDetails(record: Record) { openedId = record.id }
                override fun onEdit(record: Record) { throw AssertionError("Click must open details") }
                override fun onDelete(record: Record) = Unit
                override fun onMultiSelectChanged(selectedCount: Int) = Unit
                override fun onDeleteSelected(records: List<Record>) = Unit
                override fun onEnterMultiSelectMode(record: Record) = Unit
                override fun onToggleMultiSelect(record: Record) = Unit
            }
            DateGroupAdapter.RecordViewHolder(root).bind(Record(id = 42, category = "Fixture"), listener, false, false)
            root.findViewById<View>(R.id.card_content).performClick()
            assertEquals(42L, openedId)
            assertEquals(View.GONE, root.findViewById<View>(R.id.layout_actions).visibility)
            assertEquals(0f, root.findViewById<View>(R.id.card_content).translationX, 0f)
        }
    }

    @Test
    fun crossLedgerDetails_editAndSaveAgain_keepLedgerDateAndAccount() = withIsolatedDatabase { helper ->
        val currentLedger = helper.getCurrentLedger()!!.id
        val otherLedger = helper.addLedgerWithAssetPool("Detail fixture ledger")
        DatabaseHelper(instrumentation.targetContext, otherLedger).use { scoped ->
            val assetId = scoped.addAsset(Asset(name = "Detail fixture wallet", amount = 100.0))
            val categoryId = scoped.addCategory(Category(name = "Detail fixture category", type = 1))
            val id = scoped.addRecord(Record(date = "2026-08-03", amount = 8.0, type = 1,
                category = "Detail fixture category", categoryId = categoryId,
                assetId = assetId, assetSource = "Detail fixture wallet", description = "Detail fixture note"))
            assertTrue(id > 0)
            withDetail(id) { scenario ->
                scenario.onActivity { activity ->
                    val root = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                    assertTrue(allText(root).contains("Detail fixture ledger"))
                    assertTrue(allText(root).contains("Detail fixture note"))
                    assertEquals(View.GONE, root.findViewById<View>(R.id.recycler_detail_photos).visibility)
                    root.findViewById<View>(R.id.button_detail_edit).performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val root = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)!!.requireView()
                    root.findViewById<EditText>(R.id.edit_amount).setText("9")
                    root.findViewById<View>(R.id.btn_save_and_add).performClick()
                }
                instrumentation.waitForIdleSync()
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container)
                    assertTrue(fragment is AddRecordFragment && fragment !is EditRecordFragment)
                    val root = fragment!!.requireView()
                    assertEquals("Detail fixture wallet", root.findViewById<TextView>(R.id.text_asset_single_value).text.toString())
                    assertEquals(activity.getString(R.string.record_date_display, 8, 3), root.findViewById<TextView>(R.id.text_date).text.toString())
                    root.findViewById<EditText>(R.id.edit_amount).setText("2")
                    root.findViewById<View>(R.id.keypad_confirm).performClick()
                }
                instrumentation.waitForIdleSync()
                assertEquals(9.0, scoped.getRecordById(id)!!.amount, 0.0)
                assertEquals(2, scoped.getAllRecords().size)
                assertTrue(scoped.getAllRecords().all { it.date == "2026-08-03" && it.assetId == assetId && it.type == 1 })
                assertEquals(currentLedger, LedgerSession.getCurrentId(instrumentation.targetContext))
            }
        }
    }

    @Test
    fun detailDelete_cancelKeepsRecord_confirmReturnsAndRefundsExpense() = withIsolatedDatabase { helper ->
        val assetId = helper.addAsset(Asset(name = "Deletion fixture wallet", amount = 100.0))
        val categoryId = helper.addCategory(Category(name = "Deletion fixture category", type = 0))
        val id = helper.addRecord(Record(date = "2026-08-03", amount = 8.0, type = 0,
            category = "Deletion fixture category", categoryId = categoryId, assetId = assetId))
        withDetail(id) { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as RecordDetailFragment
                fragment.requireView().findViewById<View>(R.id.button_detail_delete).performClick()
                detailDeleteDialog(fragment).getButton(android.content.DialogInterface.BUTTON_NEGATIVE).performClick()
                assertTrue(helper.getRecordById(id) != null)
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as RecordDetailFragment
                fragment.requireView().findViewById<View>(R.id.button_detail_delete).performClick()
                detailDeleteDialog(fragment).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertNull(helper.getRecordById(id))
            assertEquals(100.0, helper.getAllAssets().first { it.id == assetId }.amount, 0.0)
            scenario.onActivity { activity ->
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is LedgerFragment)
            }
        }
    }

    @Test
    fun detailDelete_transferWithoutUndoTokenStillSucceeds() = withIsolatedDatabase { helper ->
        val source = helper.addAsset(Asset(name = "Transfer source", amount = 100.0))
        val destination = helper.addAsset(Asset(name = "Transfer destination", amount = 0.0))
        val id = helper.addRecord(Record(date = "2026-08-03", amount = 8.0, type = 2,
            assetId = source, destinationAssetId = destination, fee = 1.0))
        withDetail(id) { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as RecordDetailFragment
                fragment.requireView().findViewById<View>(R.id.button_detail_delete).performClick()
                detailDeleteDialog(fragment).getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick()
            }
            instrumentation.waitForIdleSync()
            assertNull(helper.getRecordById(id))
            assertEquals(100.0, helper.getAllAssets().first { it.id == source }.amount, 0.0)
            assertEquals(0.0, helper.getAllAssets().first { it.id == destination }.amount, 0.0)
            scenario.onActivity { activity ->
                assertTrue(activity.supportFragmentManager.findFragmentById(R.id.fragment_container) is LedgerFragment)
            }
        }
    }

    private fun withIsolatedDatabase(check: (DatabaseHelper) -> Unit) {
        IsolatedTestGuard.requireIsolatedBuild()
        val context = instrumentation.targetContext
        val previousLedger = LedgerSession.getCurrentId(context)
        context.deleteDatabase("CardTally.db")
        LedgerSession.setCurrentId(context, 1L)
        try {
            DatabaseHelper(context).use { helper ->
                helper.getCurrentLedger()
                check(helper)
            }
        } finally {
            context.deleteDatabase("CardTally.db")
            LedgerSession.setCurrentId(context, previousLedger ?: 1L)
        }
    }

    private fun withDetail(id: Long, check: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerFragment()).commitNow()
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RecordDetailFragment.newInstance(id))
                    .addToBackStack(null).commit()
            }
            instrumentation.waitForIdleSync()
            check(scenario)
        }
    }

    private fun detailDeleteDialog(fragment: RecordDetailFragment): androidx.appcompat.app.AlertDialog {
        val field = RecordDetailFragment::class.java.getDeclaredField("deleteDialog").apply { isAccessible = true }
        return field.get(fragment) as androidx.appcompat.app.AlertDialog
    }

    private fun allText(view: View): String = when (view) {
        is TextView -> view.text.toString()
        is ViewGroup -> (0 until view.childCount).joinToString(" ") { allText(view.getChildAt(it)) }
        else -> ""
    }

}
