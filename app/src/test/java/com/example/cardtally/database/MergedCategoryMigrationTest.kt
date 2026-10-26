package com.example.cardtally.database

import org.junit.Assert.*
import org.junit.Test

class MergedCategoryMigrationTest {
    @Test fun everyReferencedLeafHasAnExplicitTargetUnderTheNewDirectory() {
        ReferencedCategoryCatalog.entries.filter { it.parent != null }.forEach { entry ->
            val target = requireNotNull(MergedCategoryMigration.target(entry.type, entry.parent!!, entry.name)) { entry.pathKey }
            assertTrue(DefaultCategoryCatalog.entries.any { it.type == target.type && it.parent == null && it.name == target.parent })
        }
    }

    @Test fun mealTimesAndPlatformsAreMergedButBroadLegacyLabelsAreNotGuessed() {
        listOf("早餐", "午餐", "晚餐", "早午晚餐", "早中午餐").forEach {
            assertEquals(MergedCategoryMigration.Target(0, "餐饮食品", "餐饮"), MergedCategoryMigration.target(0, "餐饮", it))
        }
        assertEquals(MergedCategoryMigration.Target(0, "交通出行", "飞机高铁"), MergedCategoryMigration.target(0, "交通", "飞机高铁"))
        assertEquals(MergedCategoryMigration.Target(1, "工资奖金", "工资"), MergedCategoryMigration.target(1, "工作", "工资"))
        assertNull(MergedCategoryMigration.target(0, "餐饮", "自定义夜宵"))
        assertNull(MergedCategoryMigration.target(0, "家庭", "自定义家庭开支"))
    }
}
