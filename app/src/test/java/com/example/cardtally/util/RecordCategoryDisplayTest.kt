package com.example.cardtally.util

import com.example.cardtally.model.Category
import com.example.cardtally.model.Record
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordCategoryDisplayTest {
    @Test fun currentPathWinsWithoutChangingHistoricalSnapshots() {
        val paths = RecordCategoryDisplay.paths(listOf(
            Category(id = 1, name = "餐饮食品"),
            Category(id = 2, name = "餐饮", parentId = 1)
        ))
        val record = Record(categoryId = 2, category = "早午晚餐",
            categoryNameSnapshot = "早午晚餐", categoryPathSnapshot = "餐饮 / 早午晚餐")
        val before = record.copy()
        assertEquals("餐饮食品 / 餐饮", RecordCategoryDisplay.label(record, paths))
        assertEquals(before, record)
    }

    @Test fun missingOrUnboundCategoryFallsBackToHistoricalPath() {
        val record = Record(categoryId = 99, category = "旧名称",
            categoryNameSnapshot = "旧叶子", categoryPathSnapshot = "旧父类 / 旧叶子")
        assertEquals("旧父类 / 旧叶子", RecordCategoryDisplay.label(record, emptyMap()))
        assertEquals("旧父类 / 旧叶子", RecordCategoryDisplay.label(record.copy(categoryId = null), emptyMap()))
    }

    @Test fun blankSnapshotsFallBackToNameThenLegacyCategory() {
        val record = Record(category = "历史分类", categoryNameSnapshot = "历史叶子", categoryPathSnapshot = " ")
        assertEquals("历史叶子", RecordCategoryDisplay.label(record, emptyMap()))
        assertEquals("历史分类", RecordCategoryDisplay.label(record.copy(categoryNameSnapshot = ""), emptyMap()))
    }

    @Test fun sameNamedLeavesUseTheirOwnIdsAndNewParents() {
        val categories = listOf(Category(id = 1, name = "甲"), Category(id = 2, name = "乙"),
            Category(id = 3, name = "同名", parentId = 1), Category(id = 4, name = "同名", parentId = 2))
        val record = Record(categoryId = 3)
        assertEquals("甲 / 同名", RecordCategoryDisplay.label(record, RecordCategoryDisplay.paths(categories)))
        assertEquals("乙 / 同名", RecordCategoryDisplay.label(record.copy(categoryId = 4), RecordCategoryDisplay.paths(categories)))
        val moved = categories.map { if (it.id == 3L) it.copy(parentId = 2) else it }
        assertEquals("乙 / 同名", RecordCategoryDisplay.label(record, RecordCategoryDisplay.paths(moved)))
    }
}
