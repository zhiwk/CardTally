# Assets And My Page Framework Redesign

## TL;DR
> **Summary**: Rebuild the current assets tab and settings tab into the new v1 `资产` and `我的` page frameworks without changing the app's fragment-based navigation model. Assets keeps standalone add/edit pages plus record-list detail; My becomes a rebuilt settings-and-management shell with Statistics removed from bottom navigation.
> **Deliverables**:
> - Simplified assets tab with standalone asset forms and archived-assets flow
> - Rebuilt `我的` tab with retained preferences and management entries
> - Bottom-nav update removing `统计` and renaming `设置` to `我的`
> - Minimal page-scope Android test bootstrap and navigation/regression coverage
> **Effort**: Large
> **Parallel**: YES - 2 waves
> **Critical Path**: 1 -> 2 -> 3 -> 5 -> 6 -> 7

## Context
### Original Request
- Continue alignment after records-page planning because several other pages still had no frozen framework.

### Interview Summary
- This round covers `资产 + 我的` only.
- `首页` already has a confirmed spec and is not being replanned now.
- Assets decisions are fixed: add/edit assets use standalone secondary pages only; inline dialogs are retired; asset detail in v1 stays as an account-specific record list.
- My-page decisions are fixed: rebuild from the current `SettingsFragment`; bottom tab becomes `我的`; `统计` is removed from bottom navigation; keep theme, category management, quick-add, and about/preferences management.

### Metis Review (gaps addressed)
- The plan does not assume records-page test infrastructure already exists; it includes a minimal bootstrap/normalization task for page-scope Android tests.
- Asset types are fixed to built-in options (`现金 / 银行卡 / 支付宝 / 微信 / 其他`) and are not made user-configurable in this scope.
- Theme and category management remain secondary pages reached from the rebuilt My shell, rather than becoming same-page inline modules.
- Statistics is treated as deferred capability for v1; this plan removes the tab and does not re-home it elsewhere.

## Work Objectives
### Core Objective
- Deliver one execution plan that replaces the current mixed-mode asset/settings surfaces with a stable v1 assets shell and a rebuilt My shell, while preserving existing fragment navigation conventions and limiting change to assets/my-related code paths.

### Deliverables
- Assets tab rebuilt around `AssetFragment`, `AddAssetFragment`, `EditAssetFragment`, `ArchivedAssetsFragment`, and `AssetRecordsFragment` with no inline asset dialogs.
- My tab rebuilt from `SettingsFragment` semantics into a `我的` shell with theme/category/quick-add/about entries and no asset-tab visibility toggle.
- Bottom navigation and `MainActivity` updated from `首页 / 资产 / 统计 / 设置` to the v1 structure needed for this phase.
- Page-scope Android instrumentation/unit tests for assets/my navigation and edge states.

### Definition of Done (verifiable conditions with commands)
- `./gradlew.bat assembleDebug` succeeds after assets/my refactor.
- `./gradlew.bat testDebugUnitTest` succeeds for any new helper/view-state tests added in this plan.
- `./gradlew.bat connectedDebugAndroidTest` succeeds for assets/my navigation and empty-state coverage.
- The app bottom nav no longer displays `统计` or `设置`.
- Assets page no longer contains inline add/edit dialogs in active code paths.

### Must Have
- Keep fragment transaction conventions consistent with `app/src/main/java/com/example/cardtally/MainActivity.kt:36`.
- Keep `DatabaseHelper` as the concrete asset/category/preferences-adjacent data layer already used by fragments.
- Keep `AssetRecordsFragment` as the v1 asset-detail destination.
- Keep theme and category management available from the rebuilt My page.
- Keep `QuickAddHelper` as the storage mechanism for quick-add preference.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No changes to `Agent`, `AddRecord`, `Home`, or records-page implementation beyond compatibility-safe navigation label updates.
- No richer asset ledger, balance history chart, or account analytics page in v1.
- No inline add/edit asset dialogs on assets-active screens.
- No user-customizable asset types.
- No reintroduction of `统计` as a bottom-nav placeholder.
- No retention of the `显示资产页` toggle or any bottom-nav item visibility preference for assets.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after with minimal Android test bootstrap if shared foundation is absent.
- QA policy: every task includes executable happy-path and edge-path checks.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Shared dependencies are extracted into Wave 1.

Wave 1: test bootstrap/normalization, assets shell cleanup, archived/detail alignment, My shell design anchors.
Wave 2: bottom-nav restructure, My secondary management flow, assets/my regression tests, integration hardening.

### Dependency Matrix (full, all tasks)
| Task | Depends On | Enables |
|------|------------|---------|
| 1 | - | 2, 3, 4, 5, 6, 7 |
| 2 | 1 | 3, 7 |
| 3 | 1, 2 | 7 |
| 4 | 1 | 5, 6, 7 |
| 5 | 1, 4 | 6, 7 |
| 6 | 1, 2, 3, 5 | 7 |
| 7 | 1, 2, 3, 5, 6 | Final Verification |

### Agent Dispatch Summary
| Wave | Task Count | Recommended Categories |
|------|------------|------------------------|
| 1 | 4 | unspecified-high, quick |
| 2 | 3 | unspecified-high, deep |
| Final | 4 | oracle, unspecified-high, deep |

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Bootstrap or normalize minimal Android test support for assets/my flows

  **What to do**: Audit the current repo for page-test support and make this plan self-sufficient. If `app/src/test/java/com/example/cardtally/` and `app/src/androidTest/java/com/example/cardtally/` plus required AndroidX test dependencies already exist from prior work, extend them; otherwise create them here. Add only the minimum dependencies/helpers needed for fragment/navigation tests around assets and my pages, plus one smoke test proving the suite is runnable.
  **Must NOT do**: Do not assume records-page test work already landed; do not add full coverage/CI redesign in this task; do not introduce JUnit 5.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines Gradle, Android test setup, and navigation test prerequisites.
  - Skills: [`systematic-debugging`] - Reason: Android test bootstrapping fails silently if setup is incomplete.
  - Omitted: [`brainstorming`] - Reason: all product decisions are already fixed.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2, 3, 4, 5, 6, 7 | Blocked By: none

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/build.gradle:1` - Existing Android app module configuration to extend.
  - Pattern: `app/build.gradle:17` - Existing instrumentation runner declaration to preserve.
  - Pattern: `.sisyphus/plans/records-page-framework.md` - Records-page plan assumes fuller shared infrastructure but has not yet been executed.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:16` - Activity host used by bottom-nav instrumentation checks.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/build.gradle` contains the minimum AndroidX/Espresso test dependencies required for assets/my instrumentation coverage.
  - [ ] `app/src/androidTest/java/com/example/cardtally/` exists with a smoke/navigation test class.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetsMySmokeTest` passes on an attached emulator/device.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Assets/My instrumentation smoke suite runs
    Tool: Bash
    Steps: ./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetsMySmokeTest
    Expected: BUILD SUCCESSFUL and XML results are produced for the smoke class
    Evidence: .sisyphus/evidence/task-1-assets-my-smoke.txt

  Scenario: Local unit-test task still runs after bootstrap
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest
    Expected: BUILD SUCCESSFUL with no new Gradle/test task breakage
    Evidence: .sisyphus/evidence/task-1-assets-my-unit.txt
  ```

  **Commit**: YES | Message: `test(shell): bootstrap assets-my navigation coverage` | Files: `app/build.gradle`, `app/src/androidTest/java/com/example/cardtally/...`, `app/src/test/java/com/example/cardtally/...`

- [ ] 2. Rebuild the assets tab around standalone asset forms only

  **What to do**: Refactor `AssetFragment` into a pure assets shell: keep total balance, asset list, archive entry, FAB add entry, and asset-card click-through to `AssetRecordsFragment`, but remove all active inline add/edit dialog code paths and any dead helper methods tied to `dialog_asset.xml`. Keep `AssetAdapter` swipe actions, but ensure edit always routes to `EditAssetFragment` and add always routes to `AddAssetFragment`. Retain the fixed asset-type taxonomy already used by add/edit pages.
  **Must NOT do**: Do not reintroduce quick dialogs, do not redesign asset types, and do not convert asset detail into a ledger view.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: coordinates fragment cleanup, adapter behavior, and UI shell preservation.
  - Skills: [`systematic-debugging`] - Reason: dead dialog code and list actions can leave hidden regressions.
  - Omitted: [`test-driven-development`] - Reason: this task sits on the bootstrap from task 1 and includes instrumentation verification directly.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 3, 6, 7 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/AssetFragment.kt:25` - Current assets-tab shell to simplify.
  - Pattern: `app/src/main/java/com/example/cardtally/AssetFragment.kt:97` - Existing FAB -> `AddAssetFragment` navigation that becomes the only add path.
  - Pattern: `app/src/main/java/com/example/cardtally/AssetFragment.kt:121` - Existing load/update list flow to preserve.
  - Pattern: `app/src/main/java/com/example/cardtally/AssetFragment.kt:174` - Inline add/edit dialog helpers to remove from active flow.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/AssetAdapter.kt:15` - Existing swipe/click adapter contract to preserve.
  - Pattern: `app/src/main/res/layout/fragment_asset.xml:59` - Header action area with archive button.
  - Pattern: `app/src/main/res/layout/fragment_asset.xml:142` - Existing FAB positioning and styling.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `AssetFragment` contains no active `showAddDialog()` or `showEditDialog()` path.
  - [ ] FAB always opens `AddAssetFragment`; swipe edit always opens `EditAssetFragment`.
  - [ ] Tapping an asset row still opens `AssetRecordsFragment`.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Asset FAB opens standalone add page
    Tool: Bash
    Steps: Seed one asset-free state; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetFragmentTest#fabOpensAddAssetFragment`; inside the test call `onView(withId(R.id.fab_add)).perform(click())` and assert a unique Add Asset view such as `R.id.btn_save` is displayed.
    Expected: BUILD SUCCESSFUL and Espresso proves add flow is a fragment transition, not a dialog
    Evidence: .sisyphus/evidence/task-2-asset-fab.txt

  Scenario: Asset row tap still opens asset-specific records
    Tool: Bash
    Steps: Seed asset `现金账户`; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetFragmentTest#assetRowOpensAssetRecords`; inside the test tap the first item in `R.id.recycler_assets` and assert `AssetRecordsFragment` content is shown.
    Expected: BUILD SUCCESSFUL and Espresso proves the v1 detail destination remains the filtered record list
    Evidence: .sisyphus/evidence/task-2-asset-detail.txt
  ```

  **Commit**: YES | Message: `refactor(asset): remove inline dialog flows` | Files: `app/src/main/java/com/example/cardtally/AssetFragment.kt`, `app/src/main/java/com/example/cardtally/adapter/AssetAdapter.kt`, `app/src/androidTest/java/com/example/cardtally/AssetFragmentTest.kt`

- [ ] 3. Align archived-assets and asset-edit flows to the same standalone model

  **What to do**: Refactor `ArchivedAssetsFragment` so archived items use the same standalone edit path as active assets. Replace inline edit dialog usage with `EditAssetFragment.newInstance(asset.id)`, keep unarchive available from swipe/archive action, keep delete confirmation explicit, and ensure returning from edit/delete/unarchive refreshes the archived list and empty state. Remove archived-page dependencies on `dialog_asset.xml` from active code paths.
  **Must NOT do**: Do not merge archived assets back into the main list page, do not add a custom archived edit form, and do not change `EditAssetFragment` into a dialog.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: mostly localized flow alignment once the main assets shell policy is fixed.
  - Skills: [`systematic-debugging`] - Reason: archived edge states and return navigation are easy to miss.
  - Omitted: [`brainstorming`] - Reason: archived-flow behavior is already decided by product alignment.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 6, 7 | Blocked By: 1, 2

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt:18` - Archived-assets shell to align.
  - Pattern: `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt:73` - Existing archived adapter action wiring.
  - Pattern: `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt:103` - Inline edit dialog path to retire.
  - Pattern: `app/src/main/java/com/example/cardtally/EditAssetFragment.kt:31` - Canonical standalone edit flow to reuse.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:617` - Archived-asset query path to preserve.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `ArchivedAssetsFragment` routes edit to `EditAssetFragment.newInstance(asset.id)`.
  - [ ] No active inline asset edit dialog remains in archived-assets flow.
  - [ ] Unarchive and delete both refresh the archived list and show the empty state when the final archived item is removed.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.ArchivedAssetsFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Archived asset edit uses standalone page
    Tool: Bash
    Steps: Seed one archived asset; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.ArchivedAssetsFragmentTest#editArchivedAssetUsesStandaloneFragment`; inside the test swipe/tap edit and assert `EditAssetFragment` with `R.id.btn_update` is shown.
    Expected: BUILD SUCCESSFUL and Espresso proves archived edit no longer uses an inline dialog
    Evidence: .sisyphus/evidence/task-3-archived-edit.txt

  Scenario: Removing last archived asset shows empty state
    Tool: Bash
    Steps: Seed exactly one archived asset; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.ArchivedAssetsFragmentTest#removingLastArchivedAssetShowsEmptyState`; unarchive or delete it in the test and assert `R.id.text_empty` becomes visible.
    Expected: BUILD SUCCESSFUL and Espresso proves archived edge-state handling is correct
    Evidence: .sisyphus/evidence/task-3-archived-empty.txt
  ```

  **Commit**: YES | Message: `feat(asset): align archived asset flows` | Files: `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt`, `app/src/androidTest/java/com/example/cardtally/ArchivedAssetsFragmentTest.kt`

- [ ] 4. Rebuild the old settings shell into the new My-page shell

  **What to do**: Use `SettingsFragment` as the rebuild base for the new `我的` shell. Keep the existing editorial profile/header feel from `fragment_settings.xml`, but reorganize it into explicit sections for preferences, management, and about. Preserve quick-add as a toggle backed by `QuickAddHelper`; keep theme and category management as navigational entries; add a lightweight about entry in the shell; remove any trace of the asset-tab visibility preference from the UI.
  **Must NOT do**: Do not keep the page branded as `设置`; do not inline theme/category management into the shell; do not add new unimplemented preferences such as reminders just because the design guide mentions them.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines page-structure redesign, preference retention, and layout cleanup.
  - Skills: [`writing-plans`] - Reason: sectioning and exact entry composition need precision more than experimentation.
  - Omitted: [`brainstorming`] - Reason: the My-page structure has already been chosen.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5, 6, 7 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/SettingsFragment.kt:15` - Current settings-shell behavior to rebuild.
  - Pattern: `app/src/main/res/layout/fragment_settings.xml:16` - Existing editorial profile/header shell to preserve stylistically.
  - Pattern: `app/src/main/res/layout/fragment_settings.xml:77` - Existing quick-add card pattern to retain semantically.
  - Pattern: `app/src/main/res/layout/fragment_settings.xml:133` - Existing asset-visibility card to retire.
  - Pattern: `app/src/main/java/com/example/cardtally/util/QuickAddHelper.kt` - Quick-add storage contract to preserve.
  - External: `docs/stitch-guidance/page-design-guide.md` - My-page sectioning target (`头像 -> 偏好设置 -> 管理 -> 关于`).

  **Acceptance Criteria** (agent-executable only):
  - [ ] The My shell contains sections for preferences, management, and about.
  - [ ] Quick-add remains user-configurable from this shell.
  - [ ] Theme and category management remain navigable secondary pages from this shell.
  - [ ] The UI no longer exposes `显示资产页`.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MyPageTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: My page keeps quick-add and opens theme/category management
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MyPageTest#myPageShowsExpectedEntries`; inside the test select the My tab, assert the quick-add switch exists, tap the theme entry and assert `ThemeSettingsFragment` loads, then return and tap the category entry to assert `CategoryManageFragment` loads.
    Expected: BUILD SUCCESSFUL and Espresso proves the rebuilt shell preserves required management entries
    Evidence: .sisyphus/evidence/task-4-my-shell.txt

  Scenario: My page no longer exposes asset visibility preference
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MyPageTest#myPageDoesNotShowAssetVisibilityToggle`; inside the test select the My tab and assert no view with the old asset-toggle text or ID is displayed.
    Expected: BUILD SUCCESSFUL and Espresso proves the retired preference is absent
    Evidence: .sisyphus/evidence/task-4-my-no-asset-toggle.txt
  ```

  **Commit**: YES | Message: `feat(my): rebuild my shell` | Files: `app/src/main/java/com/example/cardtally/SettingsFragment.kt`, `app/src/main/res/layout/fragment_settings.xml`, `app/src/androidTest/java/com/example/cardtally/MyPageTest.kt`

- [ ] 5. Keep theme and category management as secondary pages under My

  **What to do**: Align `ThemeSettingsFragment` and `CategoryManageFragment` with the new My shell contract without turning them into inline modules. Keep existing fragment navigation from the shell, make sure labels/back-stack behavior stay coherent after the tab rename, and add a simple about destination only if the shell uses a secondary page for it; otherwise keep About as static shell content. Do not expand theme scope beyond the current light/dark/system behavior.
  **Must NOT do**: Do not implement extra theme color variants, do not redesign category management behavior, and do not create a new category system.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: this is primarily a containment and consistency task around already-existing secondary pages.
  - Skills: [`systematic-debugging`] - Reason: back-stack and navigation regressions are the main risk.
  - Omitted: [`requesting-code-review`] - Reason: that belongs in the final verification wave, not this isolated alignment task.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 7 | Blocked By: 1, 4

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/SettingsFragment.kt:53` - Current category-entry navigation pattern.
  - Pattern: `app/src/main/java/com/example/cardtally/SettingsFragment.kt:60` - Current theme-entry navigation pattern.
  - Pattern: `app/src/main/java/com/example/cardtally/ThemeSettingsFragment.kt:13` - Existing theme page limited to light/dark/system.
  - Pattern: `app/src/main/java/com/example/cardtally/CategoryManageFragment.kt:23` - Existing category-management page to preserve as secondary flow.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Theme entry from My opens `ThemeSettingsFragment` and still saves light/dark/system through `ThemeHelper`.
  - [ ] Category entry from My opens `CategoryManageFragment` and preserves existing CRUD behavior.
  - [ ] No new theme modes or category-system behaviors are introduced.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MySecondaryPagesTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Theme selection still persists through My secondary flow
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MySecondaryPagesTest#themeFlowStillPersistsSelection`; inside the test open theme settings from My, select `R.id.radio_system`, and assert the selection remains after recreation/back navigation.
    Expected: BUILD SUCCESSFUL and Espresso proves theme behavior stayed within the old supported set
    Evidence: .sisyphus/evidence/task-5-theme-flow.txt

  Scenario: Category management is still reachable from My
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MySecondaryPagesTest#categoryManagementIsReachableFromMy`; inside the test open category management and assert `R.id.recycler_categories` is displayed.
    Expected: BUILD SUCCESSFUL and Espresso proves management subpages remain wired after shell rebuild
    Evidence: .sisyphus/evidence/task-5-category-flow.txt
  ```

  **Commit**: YES | Message: `feat(my): preserve secondary management pages` | Files: `app/src/main/java/com/example/cardtally/ThemeSettingsFragment.kt`, `app/src/main/java/com/example/cardtally/CategoryManageFragment.kt`, `app/src/androidTest/java/com/example/cardtally/MySecondaryPagesTest.kt`

- [ ] 6. Restructure bottom navigation from Settings/Statistics to My-only v1 nav

  **What to do**: Update `bottom_nav_menu.xml` and `MainActivity` so the app no longer exposes `统计` in bottom navigation and the old settings destination becomes `我的`. Keep the current fragment transaction style, preserve the assets tab, and ensure the renamed My tab still loads the rebuilt shell. Remove any runtime menu-visibility logic tied to `AssetDisplayHelper`, and retire the helper from active navigation control.
  **Must NOT do**: Do not add a placeholder statistics tab, do not change unrelated tab behavior, and do not hide the assets tab behind preferences anymore.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: navigation changes touch central app shell behavior and can regress every tab.
  - Skills: [`systematic-debugging`] - Reason: app-shell navigation errors are high impact and easy to miss.
  - Omitted: [`brainstorming`] - Reason: nav structure was already decided in alignment.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 7 | Blocked By: 1, 2, 3, 4, 5

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Current bottom-nav dispatch logic to update.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:71` - Asset visibility update logic to retire from active nav behavior.
  - Pattern: `app/src/main/res/menu/bottom_nav_menu.xml:3` - Current four-tab menu containing `首页 / 资产 / 统计 / 设置`.
  - Pattern: `app/src/main/java/com/example/cardtally/SettingsFragment.kt:48` - Asset-visibility preference logic that must stop controlling nav.
  - External: `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md` - IA direction that removes Statistics from the main v1 nav.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `bottom_nav_menu.xml` no longer contains `nav_statistics` and no longer labels the last tab as `设置`.
  - [ ] `MainActivity` no longer routes to `StatisticsFragment` from bottom navigation.
  - [ ] `MainActivity` no longer hides/shows the asset tab using `AssetDisplayHelper` at runtime.
  - [ ] Selecting the last bottom-nav item opens the rebuilt My shell.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.BottomNavTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Bottom navigation no longer shows Statistics and opens My
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.BottomNavTest#bottomNavUsesMyInsteadOfSettingsAndStatistics`; inside the test assert no bottom-nav item labeled `统计` exists, tap the last tab, and assert My shell content is displayed.
    Expected: BUILD SUCCESSFUL and Espresso proves the app shell reflects the new IA decision
    Evidence: .sisyphus/evidence/task-6-bottom-nav.txt

  Scenario: Assets tab remains visible without preference gating
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.BottomNavTest#assetsTabIsAlwaysVisible`; inside the test assert the asset tab exists without first toggling any preference.
    Expected: BUILD SUCCESSFUL and Espresso proves nav no longer depends on `AssetDisplayHelper`
    Evidence: .sisyphus/evidence/task-6-asset-tab.txt
  ```

  **Commit**: YES | Message: `feat(my): update bottom navigation for my tab` | Files: `app/src/main/java/com/example/cardtally/MainActivity.kt`, `app/src/main/res/menu/bottom_nav_menu.xml`, `app/src/main/java/com/example/cardtally/SettingsFragment.kt`, `app/src/androidTest/java/com/example/cardtally/BottomNavTest.kt`

- [ ] 7. Add assets/my regression coverage and integration hardening

  **What to do**: Finish the combined redesign by running and tightening the assets/my suite across the main edge cases: empty assets state, delete last archived asset, quick-add toggle persistence, My-page absence of the asset-visibility preference, and stable returns from add/edit asset pages. Add any small compatibility fixes needed so these tests pass, but do not expand scope into new features.
  **Must NOT do**: Do not reopen product decisions, do not add statistics relocation, and do not widen this into a full app-wide shell rewrite.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: final hardening spans multiple fragments and cross-page regression behavior.
  - Skills: [`requesting-code-review`] - Reason: this task is about confidence, regression control, and scope fidelity.
  - Omitted: [`brainstorming`] - Reason: the framework is already frozen.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Final Verification | Blocked By: 1, 2, 3, 5, 6

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/AssetFragment.kt:127` - Existing empty-state behavior to preserve for assets.
  - Pattern: `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt:65` - Archived empty-state branch to preserve.
  - Pattern: `app/src/main/java/com/example/cardtally/SettingsFragment.kt:39` - Existing quick-add load path to preserve after rebuild.
  - Pattern: `app/src/main/java/com/example/cardtally/AddAssetFragment.kt:61` - Save-and-return behavior to preserve.
  - Pattern: `app/src/main/java/com/example/cardtally/EditAssetFragment.kt:105` - Update/delete-and-return behavior to preserve.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Assets empty state appears when there are no active assets.
  - [ ] Archived empty state appears after the final archived asset is removed.
  - [ ] Quick-add toggle still persists after navigating away from and back to My.
  - [ ] Add/edit asset flows return to the originating list and refresh data.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetFragmentTest,com.example.cardtally.ArchivedAssetsFragmentTest,com.example.cardtally.MyPageTest,com.example.cardtally.BottomNavTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Quick-add preference persists through My page navigation
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.MyPageTest#quickAddPreferencePersists`; inside the test toggle `R.id.switch_quick_add`, leave the My tab, return, and assert the switch state is preserved.
    Expected: BUILD SUCCESSFUL and Espresso proves quick-add preference remains functional after the shell rebuild
    Evidence: .sisyphus/evidence/task-7-quick-add.txt

  Scenario: Add asset returns and refreshes the list
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetFragmentTest#addingAssetReturnsAndRefreshesList`; inside the test open Add Asset, save a named asset, return, and assert it appears in `R.id.recycler_assets`.
    Expected: BUILD SUCCESSFUL and Espresso proves the standalone asset form integrates cleanly with the main assets shell
    Evidence: .sisyphus/evidence/task-7-add-asset-refresh.txt
  ```

  **Commit**: YES | Message: `test(my-asset): add regression coverage and hardening` | Files: `app/src/androidTest/java/com/example/cardtally/AssetFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/ArchivedAssetsFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/MyPageTest.kt`, `app/src/androidTest/java/com/example/cardtally/BottomNavTest.kt`, related assets/my fragments as needed

## Final Verification Wave (MANDATORY - after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit - oracle
- [ ] F2. Code Quality Review - unspecified-high
- [ ] F3. Real Manual QA - unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check - deep

## Commit Strategy
- Commit 1: `test(shell): bootstrap assets-my navigation coverage`
- Commit 2: `refactor(asset): remove inline dialog flows`
- Commit 3: `feat(asset): align archived and detail navigation`
- Commit 4: `feat(my): rebuild my shell and bottom navigation`
- Commit 5: `test(my-asset): add regression coverage and integration hardening`

## Success Criteria
- The assets tab presents one clear model: list page + standalone secondary pages + record-list detail.
- The My tab replaces the old Settings tab naming and removes the asset-visibility toggle from user preferences.
- Bottom navigation no longer exposes `统计`, and app navigation remains stable.
- Archived assets, asset detail, theme settings, and category management still work through the new shell structure.
- Assets/my pages gain runnable automated coverage without waiting for a separate future test migration.
