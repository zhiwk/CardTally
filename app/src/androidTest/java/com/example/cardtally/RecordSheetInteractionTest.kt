package com.example.cardtally

import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UX09 end-to-end checks for switching from the in-app amount keypad to each
 * record sheet. The verification flavor owns the scratch database; no user
 * financial data or network service is touched.
 */
@RunWith(AndroidJUnit4::class)
class RecordSheetInteractionTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
        databaseHelper.addAsset(Asset(name = "ZzVerificationWallet", amount = 100.0))
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun portrait_amountKeypadSwitchesCleanlyToDateAndAssetSheets() {
        withRecordForm { scenario ->
            verifySheetSwitch(scenario, R.id.row_date, R.id.recycler_date_calendar, R.id.btn_confirm_date)
            verifySheetSwitch(scenario, R.id.row_asset, R.id.recycler_assets)
        }
    }

    @Test
    fun landscapeLargeFont_dateSheetKeepsConfirmationInTheViewport() {
        instrumentation.runOnMainSync {
            val configuration = Configuration(context.resources.configuration).apply {
                orientation = Configuration.ORIENTATION_LANDSCAPE
                fontScale = 2f
            }
            val configured = ContextThemeWrapper(
                context.createConfigurationContext(configuration),
                R.style.Theme_CardTally_Light
            )
            val density = configured.resources.displayMetrics.density
            val root = LayoutInflater.from(configured).inflate(
                R.layout.bottom_sheet_record_date,
                FrameLayout(configured),
                false
            )
            val width = (640 * density).toInt()
            val height = (320 * density).toInt()
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
            )
            root.layout(0, 0, width, height)
            val action = root.findViewById<View>(R.id.btn_confirm_date)
            assertTrue("large-font landscape action must retain a 48dp target", action.height >= dp(48f))
            assertTrue("large-font landscape action must stay inside the sheet", action.bottom <= root.height)
        }
    }

    private fun withRecordForm(checks: (ActivityScenario<MainActivity>) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment())
                    .commitNow()
            }
            instrumentation.waitForIdleSync()
            checks(scenario)
        }
    }

    private fun verifySheetSwitch(
        scenario: ActivityScenario<MainActivity>,
        rowId: Int,
        sheetContentId: Int,
        requiredActionId: Int? = null
    ) {
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as AddRecordFragment
            val root = requireNotNull(fragment.view)
            root.findViewById<View>(R.id.edit_amount).performClick()
            assertTrue(root.findViewById<View>(R.id.layout_amount_keypad).isShown)
            root.findViewById<View>(rowId).performClick()
            assertTrue(root.findViewById<View>(R.id.layout_amount_keypad).visibility == View.GONE)
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as AddRecordFragment
            val dialog = fragment.activeSheetDialogForTest
            assertNotNull("record sheet must be open", dialog)
            val decor = requireNotNull(dialog!!.window).decorView
            val closeId = if (sheetContentId == R.id.recycler_assets) R.id.btn_archive else R.id.btn_close_sheet
            val close = decor.findViewById<View>(closeId)
            val content = decor.findViewById<View>(sheetContentId)
            assertTrue("sheet close action must be visible", close.isShown)
            assertTrue("sheet content must be visible", content.visibility == View.VISIBLE)

            val visibleFrame = Rect().also(decor::getWindowVisibleDisplayFrame)
            val closeBounds = Rect()
            assertTrue("close action needs a visible rectangle", close.getGlobalVisibleRect(closeBounds))
            assertTrue("close action must remain inside the visible window", visibleFrame.contains(closeBounds))
            if (sheetContentId == R.id.recycler_date_calendar) {
                val calendar = content as androidx.recyclerview.widget.RecyclerView
                assertTrue("calendar must render a complete six-row month grid", calendar.adapter?.itemCount == 42)
            }
        }
        instrumentation.waitForIdleSync()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager
                .findFragmentById(R.id.fragment_container) as AddRecordFragment
            val decor = requireNotNull(fragment.activeSheetDialogForTest?.window).decorView
            requiredActionId?.let { actionId ->
                val action = decor.findViewById<View>(actionId)
                assertTrue("sheet action must be part of the live dialog", action.isShown)
                assertTrue("sheet action must keep a 48dp target", action.height >= dp(48f))
                action.performClick()
            }
            if (requiredActionId == null) {
                val closeId = if (sheetContentId == R.id.recycler_assets) R.id.btn_archive else R.id.btn_close_sheet
                decor.findViewById<View>(closeId).performClick()
            }
        }
        instrumentation.waitForIdleSync()
    }

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
    }
}
