package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Asset
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperRecordDeletionTest {

    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseHelper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun deleteExpenseAndUndo_restoresExactRecordAndBalanceOnlyOnce() {
        val assetId = databaseHelper.addAsset(Asset(name = ASSET_NAME, amount = 1_000.0))
        val categoryId = databaseHelper.addCategory(Category(name = "Breakfast", type = EXPENSE_TYPE))
        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "Breakfast",
                categoryId = categoryId,
                categoryNameSnapshot = "Breakfast snapshot",
                categoryPathSnapshot = "Food / Breakfast snapshot",
                type = EXPENSE_TYPE,
                description = "Fixture note",
                assetSource = ASSET_NAME
            )
        )
        val expectedRecord = databaseHelper.getRecordById(recordId)

        val token = databaseHelper.deleteRecord(recordId, DELETED_AT)
            ?: throw AssertionError("Expected deletion token")

        assertNull(databaseHelper.getRecordById(recordId))
        assertEquals(1_000.0, activeAssetAmount(assetId), 0.0)
        assertTrue(databaseHelper.undoRecordDeletion(token, DELETED_AT + 1))
        assertEquals(expectedRecord, databaseHelper.getRecordById(recordId))
        assertEquals(900.0, activeAssetAmount(assetId), 0.0)
        assertFalse(databaseHelper.undoRecordDeletion(token, DELETED_AT + 2))
        assertEquals(900.0, activeAssetAmount(assetId), 0.0)
    }

    @Test
    fun deleteIncomeAndUndo_restoresIncomeBalanceSemantics() {
        val assetId = databaseHelper.addAsset(Asset(name = ASSET_NAME, amount = 1_000.0))
        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "Salary",
                type = INCOME_TYPE,
                assetSource = ASSET_NAME
            )
        )

        val token = databaseHelper.deleteRecord(recordId, DELETED_AT)
            ?: throw AssertionError("Expected deletion token")

        assertEquals(1_000.0, activeAssetAmount(assetId), 0.0)
        assertTrue(databaseHelper.undoRecordDeletion(token, DELETED_AT + 1))
        assertEquals(1_100.0, activeAssetAmount(assetId), 0.0)
    }

    @Test
    fun undoRecordDeletion_afterExpirationIsNoOp() {
        val assetId = databaseHelper.addAsset(Asset(name = ASSET_NAME, amount = 1_000.0))
        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 100.0,
                category = "Food",
                type = EXPENSE_TYPE,
                assetSource = ASSET_NAME
            )
        )
        val token = databaseHelper.deleteRecord(recordId, DELETED_AT)
            ?: throw AssertionError("Expected deletion token")

        assertFalse(databaseHelper.undoRecordDeletion(token, token.expiresAtEpochMs))
        assertNull(databaseHelper.getRecordById(recordId))
        assertEquals(1_000.0, activeAssetAmount(assetId), 0.0)
    }

    @Test
    fun protectedCategoryDeletion_stillRejectsChildrenAndRecordReferences() {
        val parentId = databaseHelper.addCategory(Category(name = "Parent", type = EXPENSE_TYPE))
        databaseHelper.addCategory(Category(name = "Child", type = EXPENSE_TYPE, parentId = parentId))
        val referencedId = databaseHelper.addCategory(Category(name = "Referenced", type = EXPENSE_TYPE))
        databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 20.0,
                category = "Referenced",
                categoryId = referencedId,
                type = EXPENSE_TYPE
            )
        )

        assertEquals(
            DatabaseHelper.CategoryOperationError.HAS_CHILDREN,
            categoryDeletionError(parentId)
        )
        assertEquals(
            DatabaseHelper.CategoryOperationError.IN_USE_BY_RECORDS,
            categoryDeletionError(referencedId)
        )
    }

    @Test
    fun activeAssetArchivesAndArchivedAssetWithRecordsCannotBePermanentlyDeleted() {
        val assetId = databaseHelper.addAsset(Asset(name = ASSET_NAME, amount = 1_000.0))
        databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 20.0,
                category = "Food",
                type = EXPENSE_TYPE,
                assetSource = ASSET_NAME
            )
        )

        databaseHelper.archiveAsset(assetId)

        assertTrue(databaseHelper.getArchivedAssets().any { it.id == assetId })
        val exception = try {
            databaseHelper.deleteArchivedAsset(assetId)
            fail("Expected AssetOperationException")
            throw IllegalStateException("Unreachable")
        } catch (error: DatabaseHelper.AssetOperationException) {
            error
        }
        assertEquals(DatabaseHelper.AssetOperationError.IN_USE_BY_RECORDS, exception.error)
        assertTrue(databaseHelper.getArchivedAssets().any { it.id == assetId })
    }

    @Test
    fun archivedAssetWithoutRecords_canBePermanentlyDeleted() {
        val assetId = databaseHelper.addAsset(
            Asset(name = "Unused archive", amount = 25.0, isArchived = true)
        )

        assertTrue(databaseHelper.deleteArchivedAsset(assetId))

        assertFalse(databaseHelper.getArchivedAssets().any { it.id == assetId })
    }

    @Test
    fun archivedAssetWithPendingRecordUndo_cannotBePermanentlyDeleted() {
        val assetId = databaseHelper.addAsset(Asset(name = ASSET_NAME, amount = 1_000.0))
        val recordId = databaseHelper.addRecord(
            Record(
                date = "2026-08-15",
                amount = 20.0,
                category = "Food",
                type = EXPENSE_TYPE,
                assetSource = ASSET_NAME
            )
        )
        databaseHelper.deleteRecord(recordId)
            ?: throw AssertionError("Expected deletion token")
        databaseHelper.archiveAsset(assetId)

        val exception = try {
            databaseHelper.deleteArchivedAsset(assetId)
            fail("Expected AssetOperationException")
            throw IllegalStateException("Unreachable")
        } catch (error: DatabaseHelper.AssetOperationException) {
            error
        }

        assertEquals(DatabaseHelper.AssetOperationError.IN_USE_BY_RECORDS, exception.error)
        assertTrue(databaseHelper.getArchivedAssets().any { it.id == assetId })
    }

    private fun activeAssetAmount(assetId: Long): Double {
        return databaseHelper.getAllAssets().first { it.id == assetId }.amount
    }

    private fun categoryDeletionError(categoryId: Long): DatabaseHelper.CategoryOperationError {
        return try {
            databaseHelper.deleteCategory(categoryId)
            fail("Expected CategoryOperationException")
            throw IllegalStateException("Unreachable")
        } catch (error: DatabaseHelper.CategoryOperationException) {
            error.error
        }
    }

    companion object {
        private const val DATABASE_NAME = "CardTally.db"
        private const val ASSET_NAME = "Test wallet"
        private const val EXPENSE_TYPE = 0
        private const val INCOME_TYPE = 1
        private const val DELETED_AT = 10_000L
    }
}
