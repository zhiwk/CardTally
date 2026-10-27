package com.example.cardtally

import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Record
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.LedgerSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * On-device checks for the unified new/edit ledger form (UX13 + UX14 + UX15).
 *
 * Everything runs against the isolated verification build and a scratch database;
 * any ledger created here is deleted again in teardown.
 */
@RunWith(AndroidJUnit4::class)
class LedgerSetupFormTest {

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var databaseHelper: DatabaseHelper

    private val syntheticLedgerName = "ZzVerificationLedger"

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        databaseHelper.getLedgers()
            .filter { it.name.startsWith("ZzVerification") }
            .forEach { databaseHelper.deleteLedgers(setOf(it.id)) }
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    private fun launchSetup(action: (LedgerSetupFragment) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerSetupFragment())
                    .commitNow()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentById(R.id.fragment_container) as LedgerSetupFragment
                action(fragment)
            }
            instrumentation.waitForIdleSync()
        }
    }

    private fun launchEdit(ledgerId: Long, action: (LedgerSetupFragment) -> Unit) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerSetupFragment.newEditInstance(ledgerId))
                    .commitNow()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager
                    .findFragmentById(R.id.fragment_container) as LedgerSetupFragment
                action(fragment)
            }
            instrumentation.waitForIdleSync()
        }
    }

    @Test
    fun newLedgerForm_inflatesWithVisibleBasicInfoAndAssetCard() {
        launchSetup { fragment ->
            val view = requireNotNull(fragment.view)
            assertEquals(
                "name field must be visible",
                View.VISIBLE,
                view.findViewById<View>(R.id.ledger_setup_name).visibility
            )
            assertEquals(
                "icon row must be visible",
                View.VISIBLE,
                view.findViewById<View>(R.id.ledger_setup_icon_picker).visibility
            )
            assertEquals(
                "the asset card must be shown when creating",
                View.VISIBLE,
                view.findViewById<View>(R.id.ledger_setup_asset_relation_section).visibility
            )
            val independent = view.findViewById<RadioButton>(R.id.ledger_setup_independent_assets)
            val existing = view.findViewById<RadioButton>(R.id.ledger_setup_share_other_assets)
            assertTrue("the existing-group option must be the visible default", existing.isChecked)
            assertFalse("the independent option must not be preselected", independent.isChecked)
            val save = view.findViewById<View>(R.id.button_ledger_setup_save)
            val name = view.findViewById<EditText>(R.id.ledger_setup_name)
            assertFalse("empty names disable creation", save.isEnabled)
            name.setText("   ")
            assertFalse("whitespace names disable creation", save.isEnabled)
            name.setText(syntheticLedgerName)
            assertTrue("valid names enable creation", save.isEnabled)
            assertTrue(view.findViewById<View>(R.id.ledger_setup_create_note).isVisible)
            assertTrue(
                "the icon row must meet the 48dp touch target",
                view.findViewById<View>(R.id.ledger_setup_icon_picker).height >= dp(48f)
            )
            assertTrue(
                "the save button must meet the 52dp minimum",
                (view.findViewById<View>(R.id.button_ledger_setup_save) as TextView).minHeight >= dp(52f)
            )
        }
    }

    // --- UX15: the existing-group selection must be one clear control ---------

    @Test
    fun existingGroupMode_exposesOneFullWidthSelectionRowWithChevron() {
        launchSetup { fragment ->
            val view = requireNotNull(fragment.view)
            val groupRow = view.findViewById<View>(R.id.ledger_setup_group_row)
            val label = view.findViewById<TextView>(R.id.ledger_setup_choose_ledger)
            val name = view.findViewById<TextView>(R.id.ledger_setup_group_name)
            val summary = view.findViewById<TextView>(R.id.ledger_setup_group_summary)

            assertTrue("the selection row is visible in existing mode", groupRow.isVisible)
            assertTrue("the row must be clickable", groupRow.isClickable)
            assertTrue("the row must be focusable", groupRow.isFocusable)
            assertTrue(
                "the selection row must be at least 64dp tall (${groupRow.height})",
                groupRow.height >= dp(64f)
            )
            assertTrue(
                "the selection row must span the card width (${groupRow.width})",
                groupRow.width >= dp(300f)
            )
            assertTrue("the row must carry a label", label.text.isNotEmpty())
            assertTrue("the row must state the current group name", name.text.isNotEmpty())
            assertTrue("the row must summarise members and asset count", summary.text.isNotEmpty())
            assertTrue(
                "the row must describe itself once for accessibility",
                groupRow.contentDescription?.toString().orEmpty().contains(name.text.toString())
            )
            assertNotNull("the row must end with a decorative chevron", findChevron(groupRow))
        }
    }

    @Test
    fun existingGroupMode_doesNotRepeatTheGroupSummaryInTheModeRow() {
        launchSetup { fragment ->
            val view = requireNotNull(fragment.view)
            val modeRow = view.findViewById<View>(R.id.ledger_setup_existing_row)
            val groupName = view.findViewById<TextView>(R.id.ledger_setup_group_name).text.toString()
            val groupSummary = view.findViewById<TextView>(R.id.ledger_setup_group_summary).text.toString()

            val modeTexts = collectTexts(modeRow)
            assertFalse(
                "the mode row must not repeat the current group name ('$groupName'): $modeTexts",
                modeTexts.any { it == groupName }
            )
            assertFalse(
                "the mode row must not repeat the group summary ('$groupSummary'): $modeTexts",
                groupSummary.isNotEmpty() && modeTexts.any { it == groupSummary }
            )
            assertTrue(
                "the mode row must carry a generic hint",
                modeTexts.any { it == context.getString(R.string.ledger_asset_existing_group_hint) }
            )
        }
    }

    @Test
    fun togglingAssetGroupMode_hidesTheWholeRowAndKeepsTheSameGroup() {
        databaseHelper.createLedgerInAssetGroup(syntheticLedgerName, null)
        val selectedName = context.getString(R.string.ledger_asset_group_label, syntheticLedgerName)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LedgerSetupFragment()).commitNow()
            }
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.ledger_setup_group_row).performClick()
            }
            onView(withText(selectedName)).perform(click())
            scenario.onActivity { activity ->
                val view = (activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as LedgerSetupFragment).requireView()
                assertEquals(selectedName, view.findViewById<TextView>(R.id.ledger_setup_group_name).text.toString())
                view.findViewById<View>(R.id.ledger_setup_independent_row).performClick()
                assertFalse(view.findViewById<View>(R.id.ledger_setup_asset_list).isVisible)
                assertTrue(view.findViewById<RadioButton>(R.id.ledger_setup_independent_assets).isChecked)
                view.findViewById<View>(R.id.ledger_setup_existing_row).performClick()
                assertTrue(view.findViewById<View>(R.id.ledger_setup_asset_list).isVisible)
                assertEquals("non-default groups survive mode changes", selectedName,
                    view.findViewById<TextView>(R.id.ledger_setup_group_name).text.toString())
                view.findViewById<EditText>(R.id.ledger_setup_name).setText(syntheticLedgerName)
            }
            scenario.recreate()
            scenario.onActivity { activity ->
                assertEquals(selectedName, activity.findViewById<TextView>(R.id.ledger_setup_group_name).text.toString())
                assertEquals(syntheticLedgerName, activity.findViewById<EditText>(R.id.ledger_setup_name).text.toString())
                activity.onBackPressedDispatcher.onBackPressed()
            }
            onView(withText(R.string.ledger_setup_discard_title)).check(matches(isDisplayed()))
            onView(withText(R.string.ledger_setup_continue_editing)).perform(click())
            scenario.onActivity { activity ->
                assertEquals(syntheticLedgerName, activity.findViewById<EditText>(R.id.ledger_setup_name).text.toString())
                activity.findViewById<View>(R.id.button_ledger_setup_back).performClick()
            }
            onView(withText(R.string.ledger_setup_discard_title)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun groupRow_handlesLongNamesAt320dpWithoutLosingTheEntry() {
        val longName = "ZzVerificationLongLedgerName" + "很长的账本名称".repeat(4)
        databaseHelper.createLedgerInAssetGroup(longName, null)

        launchSetup { fragment ->
            val view = requireNotNull(fragment.view)
            val name = view.findViewById<TextView>(R.id.ledger_setup_group_name)
            name.text = longName
            name.maxLines = 2
            view.measure(
                View.MeasureSpec.makeMeasureSpec(dp(320f), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(dp(900f), View.MeasureSpec.AT_MOST)
            )
            view.layout(0, 0, dp(320f), view.measuredHeight)
            val groupRow = view.findViewById<View>(R.id.ledger_setup_group_row)
            assertTrue("the entry must stay enabled at 320dp", groupRow.isEnabled)
            assertTrue("the entry must keep its touch target", groupRow.height >= dp(64f))
            assertTrue(
                "a long name may wrap to at most two lines",
                name.lineCount in 1..2
            )
            val chevron = findChevron(groupRow)
            assertNotNull("the entry must keep its chevron", chevron)
            assertTrue(
                "the chevron must stay inside the row",
                chevron!!.getRight() <= groupRow.width
            )
        }
    }

    @Test
    fun newLedgerForm_createsAnIndependentGroupAndCleansUp() {
        launchSetup { fragment ->
            val view = requireNotNull(fragment.view)
            view.findViewById<EditText>(R.id.ledger_setup_name).setText(syntheticLedgerName)
            view.findViewById<View>(R.id.ledger_setup_independent_row).performClick()
            view.findViewById<View>(R.id.button_ledger_setup_save).performClick()
            view.findViewById<View>(R.id.button_ledger_setup_save).performClick()

            val created = databaseHelper.getLedgers().firstOrNull { it.name == syntheticLedgerName }
            assertNotNull("saving must create the ledger", created)
            assertEquals("repeated clicks must not create duplicate ledgers", 1,
                databaseHelper.getLedgers().count { it.name == syntheticLedgerName })
            assertEquals(
                "an independent ledger must own its own group root",
                created!!.id,
                databaseHelper.getAssetGroupRootId(created.id)
            )
            assertEquals(
                "an independent group starts empty",
                0,
                databaseHelper.getAssetGroups().first { it.rootLedgerId == created.id }.assetCount
            )
        }
    }

    @Test
    fun editLedgerForm_hidesTheWholeAssetCard() {
        val ledgerId = databaseHelper.createLedgerInAssetGroup(syntheticLedgerName, null)
        assertNotNull(ledgerId)
        val storedName = databaseHelper.getLedgers().first { it.id == ledgerId!! }.name

        launchEdit(ledgerId!!) { fragment ->
            val view = requireNotNull(fragment.view)
            assertEquals(
                "editing must not expose the asset relationship",
                View.GONE,
                view.findViewById<View>(R.id.ledger_setup_asset_relation_section).visibility
            )
            assertEquals(
                "editing keeps the ledger name",
                storedName,
                view.findViewById<EditText>(R.id.ledger_setup_name).text.toString()
            )
        }
    }

    @Test
    fun assetHistoryQuery_seesEveryLedgerThatSharesTheGroup() {
        LedgerSession.setCurrentId(context, databaseHelper.getMasterLedgerId())
        val assetId = databaseHelper.addAsset(Asset(name = "ZzVerificationCash", amount = 10.0, type = 0))

        val shared = databaseHelper.createLedgerInAssetGroup(
            syntheticLedgerName,
            databaseHelper.getMasterLedgerId()
        )
        assertNotNull(shared)
        LedgerSession.setCurrentId(context, shared!!)
        databaseHelper.addRecord(
            Record(
                date = "2026-09-11",
                amount = 3.0,
                category = "餐饮",
                type = 0,
                assetId = assetId
            )
        )

        LedgerSession.setCurrentId(context, databaseHelper.getMasterLedgerId())
        val history = databaseHelper.getAllRecordsByAssetId(assetId)
        assertEquals("the shared ledger record must appear in the asset history", 1, history.size)
        assertEquals(shared, history.first().ledgerId)
    }

    private fun collectTexts(view: View): List<String> {
        val texts = mutableListOf<String>()
        if (view is TextView) texts += view.text?.toString().orEmpty()
        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) texts += collectTexts(view.getChildAt(index))
        }
        return texts.filter { it.isNotEmpty() }
    }

    private fun findChevron(view: View): View? {
        if (view is android.widget.ImageView && view.drawable != null) return view
        if (view is android.view.ViewGroup) {
            for (index in 0 until view.childCount) {
                findChevron(view.getChildAt(index))?.let { return it }
            }
        }
        return null
    }

    private fun dp(value: Float): Int = (value * context.resources.displayMetrics.density).toInt()

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
    }
}
