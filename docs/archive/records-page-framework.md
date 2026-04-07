# Records Page Framework Redesign

## TL;DR
> **Summary**: Create a new standalone `RecordsFragment` as the dedicated records tab, decoupled from `HomeFragment`, with full search, filter, swipe edit/delete, and add flows. The existing `HomeFragment` becomes the pure home page. Navigation updates to the 5-tab IA structure (首页/记录/资产/Agent/我的) are part of this scope. Ship with minimal test infrastructure, critical-path coverage, and CI.
> **Effort**: Large
> **Parallel**: YES - 2 waves
> **Critical Path**: 1 -> 2 -> 3 -> 5 -> 6 -> 8

## Context
### Original Request
- Continue the previously discussed optimization/alignment work and lock the records-page framework before implementation.

### Interview Summary
- Scope is records page only.
- `HomeFragment` remains the navigation entry for the records tab; no Navigation Component migration.
- V1 is a full-management records experience: search, filter, edit, delete, period switching, totals, and FAB add entry all stay in scope.
- Interaction policy is fixed: keep swipe edit/delete; remove long-press multi-select and drag sorting from the records page.
- Add-record entry is fixed to FAB only.
- Testing policy is fixed: include full testing-system planning in this work, not as a later follow-up.

### Metis Review (gaps addressed)
- Defaulted to JUnit 4 for consistency with `app/build.gradle:50` and to avoid Android JUnit 5 plugin churn in the same scope.
- Defaulted to a fragment-scoped `RecordsViewModel` for records-page state only; the rest of the app stays on the current fragment-plus-helper structure.
- Defaulted to keeping `SearchFragment` as the v1 dedicated search result surface, but replaced its current in-memory query path with database-backed filtering and the simplified adapter.
- Guardrailed against scope creep: no repository pattern, no Navigation Component, no schema migration, no Material 3 redesign, no asset-page refactor.

## Work Objectives
### Core Objective
- Deliver one implementation plan that creates a new standalone `RecordsFragment` as the dedicated 记录 tab, decoupled from `HomeFragment`, with full search, filter, swipe edit/delete, and add flows. The existing `HomeFragment` is stripped of records management code and redesigned separately in the home-page-framework plan. Navigation updates to the 5-tab IA structure (首页/记录/资产/Agent/我的) are part of this scope.

### Deliverables
- New `RecordsFragment` as the standalone 记录 tab entry point with period switching, search, filter, swipe edit/delete, and FAB add.
- Database-backed records query path supporting date-range, keyword, and category filtering.
- Simplified `RecordsDateGroupAdapter` for records flows with swipe edit/delete only.
- Modernized `SearchFragment` sharing the same records interaction rules.
- Updated bottom navigation with 5-tab IA (nav_records added, nav_home remains home-only).
- Test source sets, dependencies, Jacoco reporting, and GitHub Actions workflow.
- Critical-path unit, Robolectric, and Espresso coverage for records behavior.

### Definition of Done (verifiable conditions with commands)
- `./gradlew.bat testDebugUnitTest` passes with records query/state/adapter coverage.
- `./gradlew.bat connectedDebugAndroidTest` passes with records-page Espresso coverage.
- `./gradlew.bat jacocoTestReport` generates a coverage report without task failure.
- `./gradlew.bat assembleDebug` succeeds after records-page refactor.
- Manual navigation is no longer required to validate plan acceptance; all critical paths are covered by agent-executed checks and stored evidence.

### Must Have
- Keep `MainActivity` bottom-nav fragment switching intact from `app/src/main/java/com/example/cardtally/MainActivity.kt:36` — extended for `nav_records`.
- Keep `DatabaseHelper` as the concrete data access layer from `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:14`.
- Keep grouped-by-date rendering based on `app/src/main/java/com/example/cardtally/model/DateGroup.kt:3`.
- Keep swipe edit/delete interaction rooted in `app/src/main/java/com/example/cardtally/util/SwipeToEditDeleteHelper.kt:13`.
- Keep FAB-only add-record entry consistent with `app/src/main/res/layout/fragment_home.xml:273` and `app/src/main/java/com/example/cardtally/HomeFragment.kt:103`.
- Add `nav_records` to `bottom_nav_menu.xml` between `nav_home` and `nav_asset`.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No Navigation Component, repository layer, dependency injection framework, coroutines migration, or app-wide MVVM retrofit.
- No database schema change or migration; `sort_order` may become unused in records flow, but the column stays.
- No refactor of `AssetFragment`, `AssetRecordsFragment`, `StatisticsFragment`, `SettingsFragment`, `AddRecordFragment`, or `EditRecordFragment` beyond compatibility fixes required by records-page navigation/tests.
- No retention of long-press multi-select or drag sorting in the records-page user flow.
- No vague QA such as "visually verify" or "manual smoke test" as acceptance criteria.
- HomeFragment must NOT retain any records management code (list state, adapter, filter dialog) — all records concerns belong to RecordsFragment.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after with JUnit 4 + Robolectric + Espresso + Jacoco.
- QA policy: every task includes happy-path and failure/edge-path executable scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Shared foundations are extracted into Wave 1.

Wave 1: testing foundation, records query contract, records state holder, simplified records adapter.
Wave 2: records shell refactor, search flow modernization, CI/coverage, integration hardening.

### Dependency Matrix (full, all tasks)
| Task | Depends On | Enables |
|------|------------|---------|
| 1 | - | 2, 3, 5, 6, 7 |
| 2 | 1 | 3, 5, 6 |
| 3 | 1, 2 | 5, 6 |
| 4 | 1 | 5, 6, 7 |
| 5 | 1, 2, 3, 4 | 7, 8 |
| 6 | 1, 2, 3, 4 | 7, 8 |
| 7 | 1, 4, 5, 6 | 8 |
| 8 | 1, 5, 6, 7 | Final Verification |

### Agent Dispatch Summary
| Wave | Task Count | Recommended Categories |
|------|------------|------------------------|
| 1 | 4 | unspecified-high, quick |
| 2 | 4 | unspecified-high, deep |
| Final | 4 | oracle, unspecified-high, deep |

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Establish Android test foundation for records work

  **What to do**: Update `app/build.gradle` to support the records-page test stack without changing the app architecture: keep JUnit 4, add Robolectric, AndroidX core-testing, Espresso contrib/rules, Jacoco, and any lifecycle artifacts required for a fragment-scoped `ViewModel`; enable Android resources in local unit tests; create `app/src/test/java/com/example/cardtally/` and `app/src/androidTest/java/com/example/cardtally/`; add one smoke test per source set plus `.github/workflows/android-tests.yml` that runs assemble, unit tests, instrumentation tests, and Jacoco report generation.
  **Must NOT do**: Do not introduce JUnit 5, DI, custom Gradle plugins beyond `jacoco`, or CI jobs unrelated to Android build/test/coverage.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: touches Gradle, Android test config, and CI in one coordinated task.
  - Skills: [`writing-plans`] - Reason: keeps file/command specificity tight while modifying build and CI surfaces.
  - Omitted: [`test-driven-development`] - Reason: this task creates the test foundation itself before feature TDD can start.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2, 3, 4, 5, 6, 7, 8 | Blocked By: none

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/build.gradle:1` - Current Android app module config; keep plugin style and dependency declaration style consistent.
  - Pattern: `app/build.gradle:17` - Existing instrumentation runner declaration to preserve.
  - Pattern: `app/build.gradle:45` - Existing test dependency block to extend rather than replace.
  - Pattern: `README.md:27` - Existing build command documentation already uses Gradle wrapper conventions.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/build.gradle` includes Jacoco plus explicit dependencies for Robolectric, AndroidX core-testing, Espresso contrib/rules, and lifecycle/ViewModel support while preserving JUnit 4.
  - [ ] `app/src/test/java/com/example/cardtally/` and `app/src/androidTest/java/com/example/cardtally/` exist with compiling smoke tests.
  - [ ] `.github/workflows/android-tests.yml` runs `assembleDebug`, `testDebugUnitTest`, `connectedDebugAndroidTest`, and `jacocoTestReport` on pushes and pull requests.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.SmokeTest"` passes.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.NavigationSmokeTest` passes on an attached emulator/device.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Unit-test foundation compiles and runs
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.SmokeTest"
    Expected: BUILD SUCCESSFUL and a generated XML report under app/build/test-results/testDebugUnitTest/
    Evidence: .sisyphus/evidence/task-1-test-foundation-unit.txt

  Scenario: Instrumentation source set is runnable
    Tool: Bash
    Steps: ./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.NavigationSmokeTest
    Expected: BUILD SUCCESSFUL and a generated XML report under app/build/outputs/androidTest-results/
    Evidence: .sisyphus/evidence/task-1-test-foundation-android.txt
  ```

  **Commit**: YES | Message: `test(records): add Android test foundation and coverage pipeline` | Files: `app/build.gradle`, `app/src/test/java/com/example/cardtally/...`, `app/src/androidTest/java/com/example/cardtally/...`, `.github/workflows/android-tests.yml`

- [ ] 2. Add database-backed records search and filter queries

  **What to do**: Extend `DatabaseHelper` with a single records-page query path that supports date-range filtering plus optional keyword and category filters, and keep existing public methods unchanged for callers outside the records redesign. Implement SQL-backed matching for the same fields currently searched in memory by `SearchFragment`: category, description, numeric amount, formatted amount, asset source, and date. Add Robolectric-backed tests that seed SQLite with representative records and validate period boundaries, multi-category filters, empty results, and deletion-after-filter refresh behavior.
  **Must NOT do**: Do not change database schema, do not remove `getAllRecords()` or `getRecordsByDateRange()`, and do not push filtering back into Kotlin collection `.filter {}` for the new records-page path.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: requires careful SQLite query assembly plus deterministic Robolectric coverage.
  - Skills: [`systematic-debugging`] - Reason: SQL edge cases and date-range behavior are easy to regress silently.
  - Omitted: [`brainstorming`] - Reason: product decisions are already fixed; this is an implementation-shaping task.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 3, 5, 6 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:196` - Existing all-record query ordering; preserve descending date and ascending `sort_order` semantics.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:446` - Existing totals-by-date-range query pattern to mirror for date scoping.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:462` - Existing records-by-date-range query path; keep this intact for legacy callers.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:77` - Current in-memory search behavior that the new SQL-backed query must replace semantically.
  - API/Type: `app/src/main/java/com/example/cardtally/model/Record.kt:3` - Returned model contract for all query methods.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `DatabaseHelper` exposes one new public records-page query method that accepts `startDate`, `endDate`, optional `keyword`, and `categories`.
  - [ ] The new query preserves sort order: date descending, then `sort_order` ascending within a date.
  - [ ] `app/src/test/java/com/example/cardtally/database/DatabaseHelperTest.kt` covers keyword hit, category filter hit, empty result, and date-boundary inclusion/exclusion cases.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.database.DatabaseHelperTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: SQL-backed keyword and category filter returns only matching rows
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.database.DatabaseHelperTest.filteredQueryReturnsExpectedRecords"
    Expected: BUILD SUCCESSFUL and the targeted test asserts exact matching record IDs/order
    Evidence: .sisyphus/evidence/task-2-query-filter.txt

  Scenario: Date boundary handling excludes out-of-range records
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.database.DatabaseHelperTest.dateRangeQueryHonorsInclusiveBoundaries"
    Expected: BUILD SUCCESSFUL and the targeted test proves only start/end inclusive records are returned
    Evidence: .sisyphus/evidence/task-2-query-boundary.txt
  ```

  **Commit**: YES | Message: `feat(records): add database-backed search and filter queries` | Files: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`, `app/src/test/java/com/example/cardtally/database/DatabaseHelperTest.kt`

- [ ] 3. Introduce fragment-scoped records state management

  **What to do**: Add a `records` package for `RecordsViewModel`, `RecordsViewModelFactory`, and immutable UI/state models that own the active period, selected categories, current keyword, totals, and grouped record list. The `ViewModel` must use `DatabaseHelper` directly through the factory, expose observable state for `HomeFragment` and `SearchFragment`, and provide explicit actions for initial load, period switch, filter apply/clear, keyword submit, and record delete refresh.
  **Must NOT do**: Do not introduce a repository abstraction, shared activity-scoped state, coroutines migration, or app-wide architectural retrofit.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: this is the architecture anchor for the rest of the records-page redesign.
  - Skills: [`writing-plans`] - Reason: the task needs exact file structure and method responsibilities, not open-ended experimentation.
  - Omitted: [`executing-plans`] - Reason: the plan itself already provides the execution breakdown.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5, 6 | Blocked By: 1, 2

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:44` - Current fragment-local state (`currentYear`, `currentMonth`, `currentPeriod`) to migrate into the `ViewModel`.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:229` - Current `loadRecords()` responsibilities to split into state and rendering.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:26` - Existing keyword state ownership that must move out of fragment fields.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:446` - Totals query APIs the `ViewModel` must continue to use.
  - API/Type: `app/src/main/java/com/example/cardtally/model/DateGroup.kt:3` - Grouped-list output shape for UI state.

  **Acceptance Criteria** (agent-executable only):
  - [ ] New files exist for `RecordsViewModel`, `RecordsViewModelFactory`, and records UI-state models under `app/src/main/java/com/example/cardtally/records/`.
  - [ ] `RecordsViewModel` exposes observable state consumed by fragments without holding view references.
  - [ ] `RecordsViewModel` has explicit methods for `loadRecords()`, `setPeriod(...)`, `setKeyword(...)`, `applyCategoryFilter(...)`, `clearCategoryFilter()`, and `deleteRecord(...)` refresh behavior.
  - [ ] `app/src/test/java/com/example/cardtally/records/RecordsViewModelTest.kt` verifies initial state, period change, filter application, keyword update, and delete refresh.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.records.RecordsViewModelTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: ViewModel period switch recalculates records and totals
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.records.RecordsViewModelTest.periodChangeReloadsRecordsAndTotals"
    Expected: BUILD SUCCESSFUL and assertions confirm updated grouped list plus totals after period change
    Evidence: .sisyphus/evidence/task-3-viewmodel-period.txt

  Scenario: Clearing filters restores full period-scoped result set
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.records.RecordsViewModelTest.clearFilterRestoresUnfilteredResults"
    Expected: BUILD SUCCESSFUL and assertions confirm category filters are removed while period boundaries remain active
    Evidence: .sisyphus/evidence/task-3-viewmodel-clear-filter.txt
  ```

  **Commit**: YES | Message: `feat(records): add records state layer` | Files: `app/src/main/java/com/example/cardtally/records/...`, `app/src/test/java/com/example/cardtally/records/RecordsViewModelTest.kt`

- [ ] 4. Fork and simplify the grouped records adapter

  **What to do**: Create a new `RecordsDateGroupAdapter` dedicated to the records-page and search-page flows. Copy only the grouped date-header rendering, record binding, click-to-edit, and swipe edit/delete behavior from `DateGroupAdapter`; remove all multi-select state, selected-record bookkeeping, drag helpers, and long-press entry points. Keep the legacy `DateGroupAdapter` untouched so `AssetRecordsFragment` continues to work during this records-page redesign.
  **Must NOT do**: Do not mutate `AssetRecordsFragment` to the new adapter in this task, do not delete `DateGroupAdapter`, and do not keep any public API in the new adapter for multi-select or drag sorting.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: the task is localized to adapter extraction plus focused tests once decisions are fixed.
  - Skills: [`systematic-debugging`] - Reason: adapter touch/click regressions are subtle and need explicit verification.
  - Omitted: [`brainstorming`] - Reason: adapter behavior is fully decided already.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 5, 6, 7 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt:16` - Existing grouped adapter structure to fork from.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt:96` - Minimal update API to preserve (`updateDateGroups`).
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt:221` - Existing record binding logic to keep for category icon, amount sign/color, and swipe hookup.
  - Pattern: `app/src/main/java/com/example/cardtally/util/SwipeToEditDeleteHelper.kt:13` - Swipe behavior contract the new adapter must keep.
  - Pattern: `app/src/main/res/layout/item_record.xml:10` - Layout IDs expected by the swipe helper.
  - Pattern: `app/src/main/res/layout/item_date_header.xml:1` - Existing date-header layout to reuse as-is.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/src/main/java/com/example/cardtally/adapter/RecordsDateGroupAdapter.kt` exists and contains no multi-select or drag APIs.
  - [ ] The new adapter supports date headers, record rows, record tap -> edit, and swipe actions -> edit/delete.
  - [ ] `DateGroupAdapter.kt` remains available for `AssetRecordsFragment` compatibility.
  - [ ] `app/src/test/java/com/example/cardtally/adapter/RecordsDateGroupAdapterTest.kt` verifies item counts, view types, description visibility, and listener dispatch.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.RecordsDateGroupAdapterTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Grouped adapter renders date headers and records without multi-select state
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.RecordsDateGroupAdapterTest.rendersGroupedRowsWithoutMultiSelectApis"
    Expected: BUILD SUCCESSFUL and assertions confirm item count/view types with no selected-state behavior
    Evidence: .sisyphus/evidence/task-4-adapter-grouping.txt

  Scenario: Record row click dispatches edit action while blank description stays hidden
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.RecordsDateGroupAdapterTest.recordClickDispatchesEditAndHidesBlankDescription"
    Expected: BUILD SUCCESSFUL and assertions confirm edit callback fires and `text_description` visibility is `GONE` for empty descriptions
    Evidence: .sisyphus/evidence/task-4-adapter-click.txt
  ```

  **Commit**: YES | Message: `refactor(records): add simplified grouped records adapter` | Files: `app/src/main/java/com/example/cardtally/adapter/RecordsDateGroupAdapter.kt`, `app/src/test/java/com/example/cardtally/adapter/RecordsDateGroupAdapterTest.kt`

- [ ] 5. Create new `RecordsFragment` and update bottom navigation to 5-tab IA

  **What to do**: Create a new `RecordsFragment` as the standalone 记录 tab entry point, using `RecordsViewModel` and `RecordsDateGroupAdapter`. Add a new `nav_records` menu item to `bottom_nav_menu.xml` with title "记录", icon appropriate for records. Update `MainActivity` to handle `nav_records` and inflate `RecordsFragment` as the 记录 tab destination. The new `fragment_records.xml` layout should include: period header with month/year text and left/right arrows, search button (`btn_search`) and filter button (`btn_filter`) in the header action row, totals row (收入/支出/余额), `RecyclerView` with `RecordsDateGroupAdapter`, FAB for add, and empty state view. Remove all records management code from `HomeFragment` as part of this task — the home page will be redesigned separately in the home-page-framework plan. Implement swipe edit/delete, period switching, category filter dialog with `清空` action, row tap to edit, and FAB to `AddRecordFragment`.

  **Must NOT do**: Do not implement the home page redesign (date header, month overview card, 3 recent records, Agent card) — that belongs to the home-page-framework plan. Do not add Navigation Component. Do not create a repository layer. Do not keep any records list state in `HomeFragment`.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: coordinates new fragment creation, bottom nav restructure, and navigation behavior.
  - Skills: [`systematic-debugging`] - Reason: fragment lifecycle and bottom-nav visibility regressions are likely.
  - Omitted: [`test-driven-development`] - Reason: this task depends on the already-established test base and includes instrumentation verification directly.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 7, 8 | Blocked By: 1, 2, 3, 4

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/res/menu/bottom_nav_menu.xml:1` - Existing bottom nav menu structure to extend with `nav_records`.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Existing navigation switch behavior to extend for `nav_records`.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:103` - FAB navigation contract to reuse for `RecordsFragment`.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:336` - Existing delete-confirm dialog pattern to reuse.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/RecordsDateGroupAdapter.kt:16` - New adapter created in Task 4 to use in `RecordsFragment`.
  - Pattern: `app/src/main/java/com/example/cardtally/records/RecordsViewModel.kt:3` - ViewModel created in Task 3 to observe in `RecordsFragment`.
  - Pattern: `app/src/main/res/layout/fragment_home.xml:58` - Header action area layout to adapt for `fragment_records.xml`.
  - Pattern: `app/src/main/res/layout/fragment_home.xml:273` - FAB position and treatment to reuse.
  - API/Type: `app/src/main/java/com/example/cardtally/model/DateGroup.kt:3` - Grouped-list output shape for adapter.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/src/main/java/com/example/cardtally/RecordsFragment.kt` exists and is the 记录 tab entry point.
  - [ ] `bottom_nav_menu.xml` includes `nav_records` with title "记录" between `nav_home` and `nav_asset`.
  - [ ] `MainActivity` handles `R.id.nav_records` and inflates `RecordsFragment`.
  - [ ] `RecordsFragment` observes `RecordsViewModel` state for records list, totals, empty state, and active filters.
  - [ ] `RecordsFragment` uses `RecordsDateGroupAdapter` and has FAB that opens `AddRecordFragment`.
  - [ ] `HomeFragment` no longer contains records management code (list state, adapter, filter dialog — these move to `RecordsFragment`).
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.RecordsFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Records tab shows grouped data and FAB navigates to add-record
    Tool: Bash
    Steps: Seed one record in test setup; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.RecordsFragmentTest#fabNavigatesToAddRecord`; inside the test call `onView(withId(R.id.fab_add)).perform(click())` and assert an Add Record screen view such as `R.id.btn_save` is displayed.
    Expected: BUILD SUCCESSFUL and Espresso proves `R.id.fab_add` always opens `AddRecordFragment`
    Evidence: .sisyphus/evidence/task-5-records-fab.txt

  Scenario: Applying and clearing category filter updates the list state
    Tool: Bash
    Steps: Seed records for at least `餐饮` and `交通`; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.RecordsFragmentTest#filterDialogAppliesAndClearsCategories`; inside the test call `onView(withId(R.id.btn_filter)).perform(click())`, choose `餐饮`, confirm, assert only `餐饮` rows remain, reopen the dialog, tap `清空`, and assert both categories return.
    Expected: BUILD SUCCESSFUL and Espresso proves category filtering plus `清空` restoration work on `R.id.recycler_records`
    Evidence: .sisyphus/evidence/task-5-records-filter.txt
  ```

  **Commit**: YES | Message: `feat(records): add standalone records fragment and 5-tab bottom nav` | Files: `app/src/main/java/com/example/cardtally/RecordsFragment.kt`, `app/src/main/res/layout/fragment_records.xml`, `app/src/main/res/menu/bottom_nav_menu.xml`, `app/src/main/java/com/example/cardtally/MainActivity.kt`, `app/src/main/java/com/example/cardtally/HomeFragment.kt`, `app/src/androidTest/java/com/example/cardtally/RecordsFragmentTest.kt`

- [ ] 6. Modernize `SearchFragment` as the records search surface

  **What to do**: Keep `SearchFragment` as the v1 dedicated search-result screen, but replace its current `keyword -> getAllRecords() -> Kotlin filter` flow with the new `RecordsViewModel` + SQL-backed query path. Expand `SearchFragment.newInstance(...)` to accept the active period and selected categories from the records page, update `fragment_search.xml` to include an inline keyword input with ID `edit_keyword` plus a submit action with ID `btn_search_submit` above the list, keep bottom-nav hidden on this screen, and reuse `RecordsDateGroupAdapter` for click/swipe behavior parity with the records page.
  **Must NOT do**: Do not leave the old in-memory `.filter {}` search path in place, do not add drag or multi-select, and do not make search reachable only through a dialog.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines fragment args, shared state semantics, layout changes, and UI tests.
  - Skills: [`systematic-debugging`] - Reason: search scope and delete-refresh behavior need deterministic coverage.
  - Omitted: [`brainstorming`] - Reason: the dedicated-search-screen decision is already fixed for v1.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 8 | Blocked By: 1, 2, 3, 4

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:28` - Existing fragment-argument factory pattern to extend.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:77` - Current in-memory search logic to remove.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:98` - Current edit/delete adapter wiring to preserve semantically.
  - Pattern: `app/src/main/res/layout/fragment_search.xml:13` - Existing top section to upgrade into inline search controls.
  - Pattern: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt:47` - Existing `newInstance(recordId)` edit navigation contract to preserve.
  - Pattern: `app/src/main/java/com/example/cardtally/util/SwipeToEditDeleteHelper.kt:51` - Action-button behavior that search results must keep.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `SearchFragment` no longer calls `databaseHelper.getAllRecords()` for keyword filtering.
  - [ ] `SearchFragment.newInstance(...)` accepts and restores the active period plus selected categories from the records page.
  - [ ] `fragment_search.xml` includes `edit_keyword` and `btn_search_submit` above the results list.
  - [ ] Search results use `RecordsDateGroupAdapter` and preserve edit/delete actions.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.SearchFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Search returns only records matching keyword within current scope
    Tool: Bash
    Steps: Seed records spanning two periods and two categories; launch `SearchFragment` with a fixed period plus selected categories; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.SearchFragmentTest#searchUsesScopedSqlBackedResults`; inside the test call `onView(withId(R.id.edit_keyword)).perform(replaceText("午餐"))`, `onView(withId(R.id.btn_search_submit)).perform(click())`, and assert only scoped matches appear in `R.id.recycler_records`.
    Expected: BUILD SUCCESSFUL and Espresso proves keyword search respects incoming period/category scope instead of global in-memory search
    Evidence: .sisyphus/evidence/task-6-search-results.txt

  Scenario: Deleting a search result refreshes the result list without leaving the screen
    Tool: Bash
    Steps: Seed two matching search results; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.SearchFragmentTest#deleteFromSearchRefreshesResults`; inside the test swipe the first `R.id.recycler_records` item left, tap `R.id.btn_delete`, confirm the dialog, and assert the fragment title plus remaining result still exist.
    Expected: BUILD SUCCESSFUL and Espresso proves delete refreshes results in place without leaving `SearchFragment`
    Evidence: .sisyphus/evidence/task-6-search-delete.txt
  ```

  **Commit**: YES | Message: `feat(records): modernize search fragment flow` | Files: `app/src/main/java/com/example/cardtally/SearchFragment.kt`, `app/src/main/res/layout/fragment_search.xml`, `app/src/androidTest/java/com/example/cardtally/SearchFragmentTest.kt`

- [ ] 7. Harden records interactions and protect legacy compatibility

  **What to do**: Finish the records redesign by removing dead gesture-era code paths from the records flow, backfilling edge-case tests, and proving adjacent legacy flows still work. Specifically: ensure `HomeFragment` and `SearchFragment` contain no active multi-select or drag code; add tests for period switching, deleting the final record in a date group, empty-state rendering after filter/search misses, and swipe delete refresh behavior; add one regression test proving `AssetRecordsFragment` still renders through the legacy `DateGroupAdapter` path after the new records adapter is introduced.
  **Must NOT do**: Do not refactor `AssetRecordsFragment` to the new adapter, do not delete `RecordDragCallback.kt` just for cleanup if it is harmlessly unused, and do not widen scope into asset-page redesign.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this is a regression-hardening task spanning multiple fragments and edge-case verification.
  - Skills: [`requesting-code-review`] - Reason: the point is confidence and compatibility, not feature expansion.
  - Omitted: [`brainstorming`] - Reason: this is a verification-and-hardening pass.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 8 | Blocked By: 1, 4, 5, 6

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:266` - Old multi-select mode behavior that must be fully absent from the redesigned flow.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:297` - Old `RecordDragCallback` hookup that must not remain active in records flow.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:423` - Old bulk-delete helper that must not remain wired.
  - Pattern: `app/src/main/java/com/example/cardtally/AssetRecordsFragment.kt:107` - Legacy adapter usage that must stay functional.
  - Pattern: `app/src/main/java/com/example/cardtally/util/RecordDragCallback.kt:9` - Legacy drag utility to leave untouched unless references safely drop to zero.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:141` - Existing delete-confirm refresh pattern to preserve in the redesigned search flow.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `HomeFragment` and `SearchFragment` contain no active multi-select or drag-sort execution path.
  - [ ] `app/src/androidTest/java/com/example/cardtally/HomeFragmentTest.kt` includes period-switch and delete-last-record edge coverage.
  - [ ] `app/src/androidTest/java/com/example/cardtally/SearchFragmentTest.kt` includes empty-result coverage for no-match searches.
  - [ ] `app/src/androidTest/java/com/example/cardtally/AssetRecordsFragmentTest.kt` verifies legacy asset-record rendering still works.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest,com.example.cardtally.SearchFragmentTest,com.example.cardtally.AssetRecordsFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Deleting the final record in a date group removes both row and header
    Tool: Bash
    Steps: Seed exactly one record for date `2026-03-31`; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#deleteLastRecordRemovesDateGroupHeader`; inside the test swipe the only row left, tap `R.id.btn_delete`, confirm, then assert both the record text and header text `2026-03-31` disappear while `R.id.text_empty` becomes visible.
    Expected: BUILD SUCCESSFUL and Espresso proves deleting the final row removes the orphaned date header and shows the empty state
    Evidence: .sisyphus/evidence/task-7-delete-last-group.txt

  Scenario: Legacy asset-record list still renders on the old adapter path
    Tool: Bash
    Steps: Seed two records for asset `现金`; launch `AssetRecordsFragment.newInstance("现金", 100.0, 0)`; run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AssetRecordsFragmentTest#assetRecordsStillRenderWithLegacyAdapter`; inside the test assert `R.id.recycler_records` is displayed and at least one grouped row is visible.
    Expected: BUILD SUCCESSFUL and Espresso proves the legacy asset-record screen still renders through `DateGroupAdapter`
    Evidence: .sisyphus/evidence/task-7-asset-compat.txt
  ```

  **Commit**: YES | Message: `test(records): harden records edge cases and compatibility` | Files: `app/src/androidTest/java/com/example/cardtally/HomeFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/SearchFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/AssetRecordsFragmentTest.kt`, `app/src/main/java/com/example/cardtally/HomeFragment.kt`, `app/src/main/java/com/example/cardtally/SearchFragment.kt`

- [ ] 8. Finalize CI, coverage, and full-suite verification

  **What to do**: After all feature and test code is in place, tighten the Android CI workflow and coverage reporting to run the actual records suite, not just smoke tests. Make `.github/workflows/android-tests.yml` boot an emulator, run the targeted records instrumentation classes plus the full unit-test suite, publish Jacoco artifacts, and fail the job on any records-suite regression. Ensure Gradle has a stable `jacocoTestReport` configuration that captures the new records packages and excludes generated Android boilerplate.
  **Must NOT do**: Do not set unrealistic coverage gates, do not add unrelated release/deploy steps, and do not create a second CI workflow for the same Android verification path.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines Gradle reporting, GitHub Actions, and end-to-end verification behavior.
  - Skills: [`requesting-code-review`] - Reason: final CI/coverage wiring benefits from a reviewer mindset.
  - Omitted: [`executing-plans`] - Reason: this is still part of the plan’s implementation scope, not a handoff.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Final Verification | Blocked By: 1, 5, 6, 7

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/build.gradle:6` - Android block where `testOptions` and Jacoco wiring must live.
  - Pattern: `app/build.gradle:45` - Dependency/reporting area to keep coherent after all test additions.
  - Pattern: `.sisyphus/plans/records-page-framework.md:24` - Fixed command-level definition of done this workflow must satisfy.
  - Pattern: `README.md:27` - Existing build command story; preserve wrapper-based command usage.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `.github/workflows/android-tests.yml` runs the records unit and instrumentation suites plus `jacocoTestReport` in one pipeline.
  - [ ] Jacoco artifacts are uploaded or persisted by CI for later review.
  - [ ] `./gradlew.bat testDebugUnitTest connectedDebugAndroidTest jacocoTestReport assembleDebug` completes successfully in a prepared local/emulator environment.
  - [ ] CI fails when any targeted records test fails.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Local full records suite completes successfully
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest connectedDebugAndroidTest jacocoTestReport assembleDebug
    Expected: BUILD SUCCESSFUL and reports generated for unit tests, instrumentation tests, and Jacoco coverage
    Evidence: .sisyphus/evidence/task-8-full-suite.txt

  Scenario: CI workflow executes records verification path
    Tool: Bash
    Steps: gh workflow run android-tests.yml && gh run watch
    Expected: GitHub Actions run completes successfully with artifacts for test results and coverage
    Evidence: .sisyphus/evidence/task-8-ci-run.txt
  ```

  **Commit**: YES | Message: `test(records): finalize CI and coverage verification` | Files: `app/build.gradle`, `.github/workflows/android-tests.yml`

## Final Verification Wave (MANDATORY - after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit - oracle
- [ ] F2. Code Quality Review - unspecified-high
- [ ] F3. Real Manual QA - unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check - deep

## Commit Strategy
- Commit 1: `test(records): add Android test foundation and coverage pipeline`
- Commit 2: `feat(records): add records query and state layer`
- Commit 3: `refactor(records): simplify grouped record interactions`
- Commit 4: `feat(records): refactor records tab shell and search flow`
- Commit 5: `test(records): add integration coverage and CI hardening`

## Success Criteria
- Records tab still opens through `MainActivity` bottom navigation and remains the default non-quick-add landing flow.
- Records page supports period switching, totals, search, filter, swipe edit/delete, record editing, and FAB add without long-press or drag-sort behavior.
- Search no longer loads the full record table into memory solely for keyword filtering.
- Asset-record flows remain functional on the existing adapter path until separately redesigned.
- The repository gains runnable unit, Robolectric, Espresso, coverage, and CI support for the records domain.
