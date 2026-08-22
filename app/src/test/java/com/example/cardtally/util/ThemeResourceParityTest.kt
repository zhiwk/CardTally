package com.example.cardtally.util

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResourceParityTest {

    @Test
    fun representativeColors_haveNoNightOverrides() {
        // Given: representative page, text, and card colors used by the light theme
        val representativeColors = listOf(
            "background_light",
            "onBackground_light",
            "surface_light",
            "surface_container_low"
        )
        val valuesDirectory = File("src/main/res/values")
        val nightDirectory = File("src/main/res/values-night")
        val baseResources = valuesDirectory.listFiles()
            .orEmpty()
            .filter { it.extension == "xml" }
            .joinToString(separator = "\n") { it.readText() }

        // When / Then: each color exists in base resources and none can be overridden at night
        representativeColors.forEach { colorName ->
            assertTrue(baseResources.contains("name=\"$colorName\""))
        }
        assertFalse(nightDirectory.listFiles().orEmpty().any { it.extension == "xml" })
    }
}
