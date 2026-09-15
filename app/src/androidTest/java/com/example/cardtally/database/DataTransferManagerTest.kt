package com.example.cardtally.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.testing.IsolatedTestGuard
import com.example.cardtally.util.DataTransferManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataTransferManagerTest {
    private lateinit var context: Context
    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        databaseHelper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun exportAndImport_preservesNegativeAssetAndMergesWithoutDuplication() {
        val assetId = databaseHelper.addAsset(Asset(name = "Liability", amount = -123.45))
        assertTrue(assetId > 0)
        val manager = DataTransferManager(context)
        val json = manager.exportJson()
        manager.close()
        databaseHelper.close()
        context.deleteDatabase("CardTally.db")

        val imported = DataTransferManager(context)
        val first = imported.importJson(json)
        assertEquals(1, first.assets)
        val verificationHelper = DatabaseHelper(context)
        assertEquals(-123.45, verificationHelper.getAllAssets().single { it.name == "Liability" }.amount, 0.001)
        verificationHelper.close()
        val second = imported.importJson(json)
        assertEquals(0, second.assets)
        imported.close()
    }
}
