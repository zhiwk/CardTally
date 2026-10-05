package com.example.cardtally.util

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResourceParityTest {
    @Test
    fun darkPalette_coversExistingTokensAndMaintainsTextContrast() {
        fun colors(path: String): Map<String, String> {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(File(path)).getElementsByTagName("color")
            return (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent.trim()
            }
        }
        val light = colors("src/main/res/values/colors_light.xml")
        val dark = colors("src/main/res/values-night/colors.xml")
        assertTrue(dark.keys.containsAll(light.keys))
        fun luminance(hex: String): Double {
            val value = hex.removePrefix("#").toLong(16)
            fun channel(shift: Int): Double {
                val c = ((value shr shift) and 255) / 255.0
                return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
        listOf("onSurface_light", "onSurfaceVariant_light", "income_primary", "expense_primary").forEach { name ->
            val ratio = (luminance(dark.getValue(name)) + 0.05) / (luminance(dark.getValue("surface_light")) + 0.05)
            assertTrue("$name contrast is $ratio", ratio >= 4.5)
        }
    }
}
