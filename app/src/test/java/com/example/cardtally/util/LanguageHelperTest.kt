package com.example.cardtally.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageHelperTest {

    @Test
    fun preferredLanguage_explicitAppSettingWinsOverSystem() {
        assertEquals("zh-CN", LanguageHelper.resolvePreferredLanguageTag("zh-CN", "en-US"))
        assertEquals("en", LanguageHelper.resolvePreferredLanguageTag("en", "zh-CN"))
    }

    @Test
    fun preferredLanguage_withoutAppSettingFollowsSystem() {
        assertEquals("en", LanguageHelper.resolvePreferredLanguageTag(null, "en-US"))
        assertEquals("zh-CN", LanguageHelper.resolvePreferredLanguageTag(null, "zh-TW"))
        assertEquals("en", LanguageHelper.resolvePreferredLanguageTag("", "en-GB"))
    }

    @Test
    fun preferredLanguage_unsupportedSystemFallsBackToChinese() {
        assertEquals("zh-CN", LanguageHelper.resolvePreferredLanguageTag(null, "ja-JP"))
    }

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
