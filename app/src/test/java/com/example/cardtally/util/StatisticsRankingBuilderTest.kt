package com.example.cardtally.util

import com.example.cardtally.adapter.StatisticsAdapter
import com.example.cardtally.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsRankingBuilderTest {
    @Test
    fun sameNamedLeavesStaySeparateUnderTheirOwnParents() {
        val food = Category(id = 1, name = "餐饮")
        val leisure = Category(id = 2, name = "娱乐")
        val foodOther = Category(id = 3, name = "其他", parentId = food.id)
        val leisureOther = Category(id = 4, name = "其他", parentId = leisure.id)
        val categories = listOf(food, leisure, foodOther, leisureOther).associateBy(Category::id)
        val rows = listOf(
            StatisticsRankingBuilder.CategoryRow(foodOther.id, "餐饮 / 其他", -20.0, 1, null),
            StatisticsRankingBuilder.CategoryRow(leisureOther.id, "娱乐 / 其他", -30.0, 2, null)
        )

        val (parents, total) = StatisticsRankingBuilder.buildById(rows, categories)

        assertEquals(50.0, total, 0.0)
        assertEquals(setOf(1L, 2L), parents.mapNotNull { it.categoryId }.toSet())
        assertEquals(setOf(3L, 4L), parents.flatMap { it.children }.mapNotNull { it.categoryId }.toSet())
        assertEquals(-20.0, parents.single { it.categoryId == food.id }.amount, 0.0)
        assertEquals(-30.0, parents.single { it.categoryId == leisure.id }.amount, 0.0)
    }

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
