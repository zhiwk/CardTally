# Bottom Nav Stitch Alignment Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make the app's bottom navigation visually align more closely with the approved Stitch bottom-card design.

**Architecture:** Keep the existing activity-level `BottomNavigationView` structure, but restyle the shell to behave like a full-width, top-rounded bottom card instead of a floating inset card. Update only the shared resources that control the nav shell, active indicator, and bottom-safe-area spacing.

**Tech Stack:** Android XML layouts, Material Components, resource dimensions, color state lists.

---

### Task 1: Restyle the shared bottom nav shell

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/values/styles.xml`

**Step 1:** Convert the nav shell from an inset floating card to a bottom-attached card with top-only rounded corners.

**Step 2:** Tune the active indicator so the selected tab reads as a soft pill instead of a generic highlight.

### Task 2: Rebalance shared spacing tokens

**Files:**
- Modify: `app/src/main/res/values/dimens.xml`

**Step 1:** Reduce bottom padding tokens that were sized for the old floating shell.

**Step 2:** Keep enough space for scroll content, the primary FAB, and the Agent composer above the new nav height.

### Task 3: Make nav colors theme-aware

**Files:**
- Modify: `app/src/main/res/color/bottom_nav_item_colors.xml`

**Step 1:** Replace hardcoded light colors with theme attributes so selected/unselected states stay correct across themes.
