package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the curated category icon sets: every curated name must resolve to a
 * bundled drawable, groups must be non-empty and duplicate-free, and the broad
 * themes must stay richly populated.
 */
class IconCategoryCatalogTest {

    @Test
    fun `every curated icon exists in the bundled catalog`() {
        val all = TablerIconCatalog.icons.toHashSet()
        val missing = IconCategoryCatalog.groups.flatMap { it.icons }.filter { it !in all }
        assertTrue("curated icons missing from TablerIconCatalog.icons: $missing", missing.isEmpty())
    }

    @Test
    fun `there are eleven groups with unique non-empty members`() {
        assertEquals(11, IconCategoryCatalog.groups.size)
        IconCategoryCatalog.groups.forEach { group ->
            assertTrue("group ${group.title} is empty", group.icons.isNotEmpty())
            assertEquals(
                "group ${group.title} contains duplicates",
                group.icons.size,
                group.icons.toSet().size
            )
        }
    }

    @Test
    fun `curated sets stay close to the target size`() {
        val total = IconCategoryCatalog.groups.sumOf { it.icons.size }
        assertTrue("total curated icons too small: $total", total >= 800)

        val wellPopulated = IconCategoryCatalog.groups.count { it.icons.size >= 80 }
        assertTrue("too few well-populated groups: $wellPopulated", wellPopulated >= 6)
    }
}
