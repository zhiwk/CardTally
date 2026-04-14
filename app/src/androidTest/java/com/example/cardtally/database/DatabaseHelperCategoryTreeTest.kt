package com.example.cardtally.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import com.example.cardtally.util.CategoryHierarchySettingsHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseHelperCategoryTreeTest {

    private lateinit var databaseHelper: DatabaseHelper

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase("CardTally.db")
        CategoryHierarchySettingsHelper.saveCategoryMaxDepth(context, 3)
        databaseHelper = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        databaseHelper.close()
        context.deleteDatabase("CardTally.db")
    }

    @Test
    fun getCategoryTreeByType_returnsDepthFirstHierarchy() {
        val parentId = databaseHelper.addCategory(Category(name = "父分类", type = 0))
        val childId = databaseHelper.addCategory(Category(name = "子分类", type = 0, parentId = parentId))
        databaseHelper.addCategory(Category(name = "孙分类", type = 0, parentId = childId))

        val categories = databaseHelper.getCategoryTreeByType(0)
        val hierarchyNames = categories
            .filter { it.name in setOf("父分类", "子分类", "孙分类") }
            .map { it.name }

        assertEquals(listOf("父分类", "子分类", "孙分类"), hierarchyNames)
    }

    private fun expectCategoryOperationException(
        block: () -> Unit
    ): DatabaseHelper.CategoryOperationException {
        return try {
            block()
            fail("Expected CategoryOperationException")
            throw IllegalStateException("Unreachable")
        } catch (exception: DatabaseHelper.CategoryOperationException) {
            exception
        }
    }

    @Test
    fun addCategory_rejectsParentFromAnotherType() {
        val expenseParentId = databaseHelper.addCategory(Category(name = "支出父类", type = 0))

        val exception = expectCategoryOperationException {
            databaseHelper.addCategory(Category(name = "收入子类", type = 1, parentId = expenseParentId))
        }

        assertEquals(DatabaseHelper.CategoryOperationError.PARENT_TYPE_MISMATCH, exception.error)
    }

    @Test
    fun updateCategory_rejectsDescendantCycle() {
        val parentId = databaseHelper.addCategory(Category(name = "父类", type = 0))
        val childId = databaseHelper.addCategory(Category(name = "子类", type = 0, parentId = parentId))
        val parent = databaseHelper.getCategoryTreeByType(0).first { it.id == parentId }

        parent.parentId = childId

        val exception = expectCategoryOperationException {
            databaseHelper.updateCategory(parent)
        }

        assertEquals(DatabaseHelper.CategoryOperationError.DESCENDANT_CYCLE, exception.error)
    }

    @Test
    fun addCategory_rejectsDepthAboveConfiguredLimit() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        CategoryHierarchySettingsHelper.saveCategoryMaxDepth(context, 2)
        val parentId = databaseHelper.addCategory(Category(name = "父类", type = 0))
        val childId = databaseHelper.addCategory(Category(name = "子类", type = 0, parentId = parentId))

        val exception = expectCategoryOperationException {
            databaseHelper.addCategory(Category(name = "孙类", type = 0, parentId = childId))
        }

        assertEquals(DatabaseHelper.CategoryOperationError.MAX_DEPTH_EXCEEDED, exception.error)
    }

    @Test
    fun deleteCategory_rejectsWhenCategoryHasChildren() {
        val parentId = databaseHelper.addCategory(Category(name = "父类", type = 0))
        databaseHelper.addCategory(Category(name = "子类", type = 0, parentId = parentId))

        val exception = expectCategoryOperationException {
            databaseHelper.deleteCategory(parentId)
        }

        assertEquals(DatabaseHelper.CategoryOperationError.HAS_CHILDREN, exception.error)
    }

    @Test
    fun deleteCategory_rejectsWhenReferencedByRecordCategoryId() {
        val categoryId = databaseHelper.addCategory(Category(name = "被引用分类", type = 0))
        databaseHelper.addRecord(
            Record(
                date = "2026-04-14",
                amount = 20.0,
                category = "被引用分类",
                categoryId = categoryId,
                categoryNameSnapshot = "被引用分类",
                categoryPathSnapshot = "被引用分类",
                type = 0
            )
        )

        val exception = expectCategoryOperationException {
            databaseHelper.deleteCategory(categoryId)
        }

        assertEquals(DatabaseHelper.CategoryOperationError.IN_USE_BY_RECORDS, exception.error)
    }
}
