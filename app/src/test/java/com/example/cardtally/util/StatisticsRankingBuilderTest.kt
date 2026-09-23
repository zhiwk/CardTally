package com.example.cardtally.util

import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsRankingBuilderTest {
    @Test
    fun aggregatesLeavesIntoRootAndSortsByAbsoluteAmount() {
        val food = Category(id = 1, name = "餐饮", icon = "food")
        val breakfast = Category(id = 2, name = "早餐", parentId = food.id, icon = "coffee")
        val transit = Category(id = 3, name = "交通", icon = "bus")
        val result = StatisticsRankingBuilder.build(
            normalizedStats = mapOf("早餐" to -30.0, "交通" to -50.0),
            entryCounts = mapOf("早餐" to 2, "交通" to 1),
            categoryIcons = mapOf("早餐" to "coffee", "交通" to "bus"),
            categoriesById = listOf(food, breakfast, transit).associateBy(Category::id),
            categoriesByName = listOf(food, breakfast, transit).associateBy(Category::name)
        )

        assertEquals(80.0, result.second, 0.0)
        assertEquals("交通", result.first.first().label)
        val foodItem = result.first.first { it.label == "餐饮" }
        assertEquals(-30.0, foodItem.amount, 0.0)
        assertEquals(2, foodItem.entryCount)
        assertEquals(StatisticsAdapter.ItemType.PRIMARY_CHILD, foodItem.children.single().type)
        assertEquals("早餐", foodItem.children.single().label)
    }

    @Test
    fun missingCategoryMetadata_keepsFlatCategoryAsRoot() {
        val result = StatisticsRankingBuilder.build(
            mapOf("餐饮" to 12.0), emptyMap(), emptyMap(), emptyMap(), emptyMap()
        )
        assertTrue(result.first.single().children.isEmpty())
        assertEquals("餐饮", result.first.single().label)
    }
}
