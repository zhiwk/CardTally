package com.example.cardtally

import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.database.DatabaseHelper
import com.example.cardtally.model.Asset
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.DefaultRecordAssetPreferences
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordAssetSelectionMemoryTest {
    private lateinit var context: Context
    private lateinit var databaseHelper: DatabaseHelper
    private var expenseAssetId = 0L
    private var incomeAssetId = 0L
    private var transferSourceAssetId = 0L
    private var transferDestinationAssetId = 0L

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        databaseHelper = DatabaseHelper(context)
        expenseAssetId = databaseHelper.addAsset(Asset(name = "默认支出"))
        incomeAssetId = databaseHelper.addAsset(Asset(name = "默认收入"))
        transferSourceAssetId = databaseHelper.addAsset(Asset(name = "转出账户"))
        transferDestinationAssetId = databaseHelper.addAsset(Asset(name = "转入账户"))
        DefaultRecordAssetPreferences.saveExpenseAssetId(context, expenseAssetId)
        DefaultRecordAssetPreferences.saveIncomeAssetId(context, incomeAssetId)
    }

    @After
    fun tearDown() {
        DefaultRecordAssetPreferences.saveExpenseAssetId(context, null)
        DefaultRecordAssetPreferences.saveIncomeAssetId(context, null)
        databaseHelper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun typeSwitches_keepIndependentExpenseIncomeAndTransferSelections() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            scenario.onActivity { activity ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AddRecordFragment())
                    .commitNow()
                val root = requireNotNull(
                    activity.supportFragmentManager.findFragmentById(R.id.fragment_container)?.view
                )
                assertEquals("默认支出", root.findViewById<TextView>(R.id.text_asset_single_value).text)

                root.findViewById<TextView>(R.id.btn_transfer).performClick()
                assertEquals(
                    activity.getString(R.string.record_transfer_from_asset_hint),
                    root.findViewById<TextView>(R.id.text_asset_value).text
                )
                assertEquals(
                    activity.getString(R.string.record_transfer_to_asset_hint),
                    root.findViewById<TextView>(R.id.text_destination_asset_value).text
                )

                val fragment = activity.supportFragmentManager.findFragmentById(R.id.fragment_container) as AddRecordFragment
                fragment.onAssetPickerSelected(transferSourceAssetId, selectDestination = false)
                fragment.onAssetPickerSelected(transferDestinationAssetId, selectDestination = true)

                root.findViewById<TextView>(R.id.btn_income).performClick()
                assertEquals("默认收入", root.findViewById<TextView>(R.id.text_asset_single_value).text)

                root.findViewById<TextView>(R.id.btn_expense).performClick()
                assertEquals("默认支出", root.findViewById<TextView>(R.id.text_asset_single_value).text)

                root.findViewById<TextView>(R.id.btn_transfer).performClick()
                assertEquals("转出账户", root.findViewById<TextView>(R.id.text_asset_value).text)
                assertEquals("转入账户", root.findViewById<TextView>(R.id.text_destination_asset_value).text)
            }
        }
    }
}
