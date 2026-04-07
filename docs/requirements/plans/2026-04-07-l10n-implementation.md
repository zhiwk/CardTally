# L10n Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add app-level Chinese/English localization with a language switch in the profile/settings page.

**Architecture:** Use AndroidX app locales for immediate global switching, wire the setting from `SettingsFragment`, and move core visible copy into localized string resources. Keep the implementation minimal and avoid changing business logic or stored record data.

**Tech Stack:** Kotlin, Android Fragments, XML layouts, AppCompatDelegate, resource-based localization.

---

### Task 1: Add locale helper

**Files:**
- Create: `app/src/main/java/com/example/cardtally/util/LanguageHelper.kt`

### Task 2: Apply locale on startup

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/MainActivity.kt`

### Task 3: Add localized string resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values-zh-rCN/strings.xml`
- Create: `app/src/main/res/values-en/strings.xml`

### Task 4: Add language setting entry in profile page

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/java/com/example/cardtally/SettingsFragment.kt`

### Task 5: Localize core visible layouts and fragments

**Files:**
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/res/layout/fragment_add_record.xml`
- Modify: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/HomeFragment.kt`
- Modify: `app/src/main/res/layout/fragment_agent.xml`

### Task 6: Verify build

**Files:**
- Modify: none

Run: `./gradlew.bat assembleDebug`
