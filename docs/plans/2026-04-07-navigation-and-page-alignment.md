# Navigation And Page Alignment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Align the main bottom navigation and the `home`, `records`, `assets`, `agent`, and `me` screens with the updated Stitch layouts without changing the underlying app architecture.

**Architecture:** Keep `MainActivity` as the single owner of bottom navigation and update each Fragment/layout pair to respect the shared floating nav geometry. Preserve current Fragment routing and data sources, and limit Kotlin changes to wiring existing actions into the revised layout structure.

**Tech Stack:** Kotlin, Android Fragments, XML layouts, Material Components, RecyclerView, Gradle.

---

### Task 1: Align Shared Bottom Navigation

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/menu/bottom_nav_menu.xml`

**Step 1:** Update navigation shell spacing, height, and visual balance to better match the Stitch floating pill.

**Step 2:** Update bottom navigation labels to the approved Chinese copy.

**Step 3:** Re-read both files to confirm the shell still anchors at the activity level and no page duplicates navigation.

### Task 2: Align Home Shell And CTA

**Files:**
- Modify: `app/src/main/res/layout/fragment_home.xml`

**Step 1:** Update scroll padding and content spacing so the page clears the shared nav shell.

**Step 2:** Move the FAB to the approved position above the bottom nav.

**Step 3:** Keep existing view IDs so `HomeFragment.kt` continues to work unchanged.

### Task 3: Align Records Shell And CTA

**Files:**
- Modify: `app/src/main/res/layout/fragment_statistics.xml`

**Step 1:** Update header spacing and list bottom padding to match the Stitch records layout.

**Step 2:** Move the add FAB to the same shared CTA position used by the design.

**Step 3:** Preserve current RecyclerView and empty-state IDs so `StatisticsFragment.kt` remains valid.

### Task 4: Align Assets Page CTA Strategy

**Files:**
- Modify: `app/src/main/res/layout/fragment_asset.xml`

**Step 1:** Update page spacing and bottom reserve area to reflect the new design.

**Step 2:** Keep the inline `New Asset` CTA as the primary action and leave the hidden FAB unused.

**Step 3:** Preserve existing IDs for archive and add actions so `AssetFragment.kt` keeps working.

### Task 5: Align Agent Bottom Input Stack

**Files:**
- Modify: `app/src/main/res/layout/fragment_agent.xml`

**Step 1:** Increase content bottom reserve to account for both the input pill and the shared nav shell.

**Step 2:** Restyle and reposition the bottom input row so it floats above the nav like the Stitch mock.

**Step 3:** Keep the current fragment static and avoid introducing unsupported chat logic.

### Task 6: Align Me Page Structure

**Files:**
- Modify: `app/src/main/res/layout/fragment_settings.xml`

**Step 1:** Tighten the page structure to better match the Stitch `me` composition.

**Step 2:** Preserve existing switches and click targets.

**Step 3:** Avoid introducing new unsupported settings entries.

### Task 7: Align Shared List Presentation

**Files:**
- Modify: `app/src/main/res/layout/item_date_header.xml`
- Modify: `app/src/main/res/layout/item_record.xml`
- Modify: `app/src/main/res/layout/item_asset.xml`
- Modify: `app/src/main/res/layout/item_statistics.xml`

**Step 1:** Remove heavy separation treatments and increase editorial spacing.

**Step 2:** Update typography hierarchy and surface treatment to resemble the Stitch lists.

**Step 3:** Keep all existing view IDs used by adapters intact.

### Task 8: Verify Build

**Files:**
- Modify: none

**Step 1:** Run `./gradlew.bat assembleDebug`.

**Step 2:** Fix any resource or layout compile issues introduced by the UI changes.

**Step 3:** Re-run the build until it passes.
