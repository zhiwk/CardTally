package com.example.cardtally.database

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.testing.IsolatedTestGuard
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperNegativeAssetTest {
    private lateinit var context: Context
    private lateinit var helper: DatabaseHelper

    @Before
    fun setUp() {
        IsolatedTestGuard.requireIsolatedBuild()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        helper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        helper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun negativeAssetBalance_canBeCreatedReadAndCounted() {
        val assetId = helper.addAsset(Asset(name = "Credit balance", amount = -125.50))
        assertTrue(assetId > 0)
        assertEquals(-125.50, helper.getAllAssets().single { it.id == assetId }.amount, 0.001)
        assertEquals(-125.50, helper.getTotalAssets(), 0.001)
    }
}
