package com.example.cardtally.util

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryHierarchySettingsHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("category_hierarchy_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun getCategoryMaxDepth_defaultsToTwoWhenMissing() {
        assertEquals(2, CategoryHierarchySettingsHelper.getCategoryMaxDepth(context))
    }

    @Test
    fun getCategoryMaxDepth_sanitizesStoredValuesIntoSupportedRange() {
        context.getSharedPreferences("category_hierarchy_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("category_hierarchy_max_depth", 0)
            .commit()

        assertEquals(1, CategoryHierarchySettingsHelper.getCategoryMaxDepth(context))

        context.getSharedPreferences("category_hierarchy_settings_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("category_hierarchy_max_depth", 99)
            .commit()

        assertEquals(50, CategoryHierarchySettingsHelper.getCategoryMaxDepth(context))
    }
}
