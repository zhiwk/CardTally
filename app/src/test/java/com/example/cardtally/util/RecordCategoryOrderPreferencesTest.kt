package com.example.cardtally.util

import com.example.cardtally.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordCategoryOrderPreferencesTest {

    @Test
    fun orderCategories_keepsSavedIdsAndAppendsNewCategories() {
        val categories = listOf(
            Category(id = 1, name = "购物"),
            Category(id = 2, name = "餐饮"),
            Category(id = 3, name = "居住")
        )

        val ordered = RecordCategoryOrderPreferences.orderCategories(categories, listOf(2, 99, 1))

        assertEquals(listOf(2L, 1L, 3L), ordered.map { it.id })
    }

    @Test
    fun orderCategories_doesNotMutateTheSourceOrder() {
        val categories = listOf(Category(id = 1), Category(id = 2))

        RecordCategoryOrderPreferences.orderCategories(categories, listOf(2))

        assertEquals(listOf(1L, 2L), categories.map { it.id })
    }
}
