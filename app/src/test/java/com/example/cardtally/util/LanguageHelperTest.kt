package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageHelperTest {

    @Test
    fun normalizeLanguageTag_mapsEnglishVariantsToEnglish() {
        assertEquals(LanguageHelper.LANGUAGE_ENGLISH, LanguageHelper.normalizeLanguageTag("en-US"))
    }

    @Test
    fun normalizeLanguageTag_mapsChineseAndUnknownToChinese() {
        assertEquals(LanguageHelper.LANGUAGE_CHINESE, LanguageHelper.normalizeLanguageTag("zh-TW"))
        assertEquals(LanguageHelper.LANGUAGE_CHINESE, LanguageHelper.normalizeLanguageTag("ja-JP"))
    }
}
