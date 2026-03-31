# Home Page Framework Redesign

## TL;DR
> **Summary**: Redesign `HomeFragment` from a monolithic records-management page into a pure diary-style home experience with date header, month overview card, 3 recent records, Agent card, and FAB entry. All records management code is stripped and moved to the new `RecordsFragment` (handled in records-page-framework). Navigation is already 5-tab IA; no bottom-nav changes needed here.
> **Effort**: Medium
> **Parallel**: YES - 2 waves
> **Critical Path**: 1 -> 2 -> 4 -> 5

## Context
### Original Request
- Continue the previously discussed optimization/alignment work and lock the home page framework before implementation.

### Interview Summary
- Scope is home page redesign only.
- `HomeFragment` currently serves dual roles (首页 + 记录 tab) but user confirmed "记录 tab 独立" — records tab is standalone.
- Home page should be diary-style emotional entry: date header, month overview card, 3 recent records, Agent card, FAB.
- No period switching, no full records list, no filter dialog, no multi-select or drag-sort.
- FAB always opens `AddRecordFragment`.
- Agent card navigates to the Agent tab.
- 3 recent records navigate to `EditRecordFragment` on tap.

### Metis Review (gaps addressed)
- Defaulted to reusing existing `DatabaseHelper` query methods for month totals and recent records rather than creating new query paths.
- Defaulted to a lightweight `HomeViewModel` for month totals and recent records state only; no app-wide architectural changes.
- Guardrailed against redesigning the Agent conversation UI — that belongs to add-agent-page-framework.
- Guardrailed against creating a new layout file from scratch — adapt `fragment_home.xml` rather than replace it entirely to minimize navigation risk.

## Work Objectives
### Core Objective
- Deliver one implementation plan that upgrades the current dual-role `HomeFragment` into a pure diary-style home page with emotional entry design, month overview, recent records preview, Agent card, and FAB. All records management code is removed from this fragment.

### Deliverables
- Redesigned `HomeFragment` with date header, month overview card (income/expense/balance), 3 recent records list, Agent card, and FAB.
- `HomeViewModel` for month totals and recent records state.
- Updated `fragment_home.xml` layout reflecting the new home page design.
- Empty state with diary-style welcome message and "记下第一笔" CTA.
- Navigation: FAB → `AddRecordFragment`, Agent card → Agent tab, recent record tap → `EditRecordFragment`.

### Definition of Done (verifiable conditions with commands)
- `./gradlew.bat assembleDebug` succeeds after home page redesign.
- `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.HomeViewModelTest"` passes.
- `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest` passes with home page coverage.
- Agent card tap navigates to Agent tab.
- FAB tap navigates to `AddRecordFragment`.
- Recent record tap navigates to `EditRecordFragment`.
- Manual navigation is no longer required to validate plan acceptance; all critical paths are covered by agent-executed checks and stored evidence.

### Must Have
- Keep `MainActivity` bottom-nav fragment switching intact from `app/src/main/java/com/example/cardtally/MainActivity.kt:36`.
- Keep `DatabaseHelper` as the concrete data access layer from `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:14`.
- Keep FAB navigation to `AddRecordFragment` from `app/src/main/java/com/example/cardtally/HomeFragment.kt:103`.
- Keep bottom-nav/FAB hide-show animation behavior from `app/src/main/java/com/example/cardtally/HomeFragment.kt:449`.
- Date header displays current date in format "M月d日 E" (e.g., "3月31日 周二").
- Month overview card shows income total, expense total, and balance for current month.
- 3 recent records show category icon, amount, and brief description.
- Agent card shows管家-style message and navigates to Agent tab on tap.
- FAB opens `AddRecordFragment`.
- Empty state shows diary-style welcome with "记下第一笔" button that opens `AddRecordFragment`.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No Navigation Component, repository layer, dependency injection framework, coroutines migration, or app-wide MVVM retrofit.
- No records management code in `HomeFragment` — no record list with period switching, no filter dialog, no multi-select, no drag-sort.
- No complex statistics charts or dashboard-style data panels.
- No period switching buttons (month/year selectors).
- No bottom-nav changes — 5-tab IA is handled in records-page-framework.
- No vague QA such as "visually verify" or "manual smoke test" as acceptance criteria.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after with JUnit 4 + Robolectric + Espresso.
- QA policy: every task includes happy-path and failure/edge-path executable scenarios.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Shared foundations are extracted into Wave 1.

Wave 1: home page state model (ViewModel), updated layout structure, recent records query reuse.
Wave 2: home page UI assembly (date header, month card, recent records, Agent card), empty state, navigation wiring, instrumentation tests.

### Dependency Matrix (full, all tasks)
| Task | Depends On | Enables |
|------|------------|---------|
| 1 | - | 2, 3, 4 |
| 2 | 1 | 3, 4 |
| 3 | 1 | 4 |
| 4 | 1, 2, 3 | 5, 6 |
| 5 | 4 | 6 |
| 6 | 4, 5 | Final Verification |

### Agent Dispatch Summary
| Wave | Task Count | Recommended Categories |
|------|------------|------------------------|
| 1 | 3 | unspecified-high, quick |
| 2 | 3 | unspecified-high, deep |
| Final | 4 | oracle, unspecified-high, deep |

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Add home page state management with `HomeViewModel`

  **What to do**: Create a `home` package with `HomeViewModel`, `HomeViewModelFactory`, and UI state models for the home page. `HomeViewModel` must: load current month totals (income, expense, balance) using existing `DatabaseHelper.getTotalsByDateRange()`; load the 3 most recent records using existing `DatabaseHelper.getRecordsByDateRange()` with limit 3 and descending date; expose observable state for date header text, month overview data (income/expense/balance), recent records list, and empty state flag. Add unit tests covering initial load, month totals calculation, and recent records ordering.

  **Must NOT do**: Do not introduce a repository abstraction, shared activity-scoped state, coroutines, or app-wide architectural retrofit. Do not create new database query methods — reuse existing `DatabaseHelper` public methods.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: creates the state layer for the home page redesign.
  - Skills: [`writing-plans`] - Reason: exact file structure and method responsibilities needed.
  - Omitted: [`brainstorming`] - Reason: product decisions already confirmed; this is implementation-shaping.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 2, 3, 4 | Blocked By: none

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:44` - Current fragment-local state to migrate into `HomeViewModel`.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:446` - Existing totals query to reuse for month totals.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt:462` - Existing records-by-date-range query to reuse for recent records.
  - Pattern: `app/src/main/java/com/example/cardtally/model/Record.kt:3` - Record model contract for recent records list.
  - API/Type: `app/src/main/java/com/example/cardtally/model/DateGroup.kt:3` - Grouped-list output shape; note home page uses flat list of 3 records, not grouped.

  **Acceptance Criteria** (agent-executable only):
  - [ ] New files exist for `HomeViewModel`, `HomeViewModelFactory`, and home UI-state models under `app/src/main/java/com/example/cardtally/home/`.
  - [ ] `HomeViewModel` exposes observable state for date header, month income/expense/balance, recent records list, and empty state.
  - [ ] `HomeViewModel` uses existing `DatabaseHelper` public methods (no new query methods).
  - [ ] `app/src/test/java/com/example/cardtally/home/HomeViewModelTest.kt` covers initial state, month totals load, recent records load, and empty state flag.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.home.HomeViewModelTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: ViewModel loads month totals and recent records on initialization
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.home.HomeViewModelTest.initialLoadPopulatesMonthTotalsAndRecentRecords"
    Expected: BUILD SUCCESSFUL and assertions confirm income/expense/balance values and exactly 3 recent records
    Evidence: .sisyphus/evidence/task-1-viewmodel-load.txt

  Scenario: Empty state flag is true when no records exist
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.home.HomeViewModelTest.emptyStateTrueWhenNoRecords"
    Expected: BUILD SUCCESSFUL and assertions confirm empty state flag is set correctly
    Evidence: .sisyphus/evidence/task-1-viewmodel-empty.txt
  ```

  **Commit**: YES | Message: `feat(home): add home page state layer` | Files: `app/src/main/java/com/example/cardtally/home/...`, `app/src/test/java/com/example/cardtally/home/HomeViewModelTest.kt`

- [ ] 2. Create home page layout adapting `fragment_home.xml`

  **What to do**: Create `fragment_home_new.xml` as the new home page layout with the diary-style design. The layout must include: top area with date header (`text_date`) showing "M月d日 E" format; month overview card (`card_month_overview`) with three rows: income (`text_income`), expense (`text_expense`), balance (`text_balance`); recent records section (`layout_recent_records`) showing 3 `item_recent_record` items (category icon, amount, description); Agent card (`card_agent`) with管家-style message and "进入助手" CTA; FAB (`fab_add`) at bottom-right; empty state (`layout_empty_state`) with diary illustration and "记下第一笔" button. Reuse existing `item_record.xml` for record item structure but simplified for home page display (no swipe actions). Create `item_recent_record.xml` for the recent records list item.

  **Must NOT do**: Do not delete or replace `fragment_home.xml` yet — keep it for compatibility during implementation. Do not add period switching UI. Do not add filter or search buttons.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: layout creation and view ID conventions.
  - Skills: [`writing-plans`] - Reason: exact view IDs and structure needed.
  - Omitted: [`brainstorming`] - Reason: design decisions already confirmed in specs.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/res/layout/fragment_home.xml:1` - Existing layout to adapt rather than replace.
  - Pattern: `app/src/main/res/layout/item_record.xml:10` - Existing record item layout to simplify for recent records.
  - Pattern: `docs/stitch-guidance/home-page-spec.md:16` - Home page module清单 and layout structure.
  - Pattern: `docs/stitch-guidance/page-design-guide.md:28` - Home page layout structure diagram.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `fragment_home_new.xml` exists with `text_date`, `card_month_overview` (with `text_income`, `text_expense`, `text_balance`), `layout_recent_records`, `card_agent`, `fab_add`, and `layout_empty_state`.
  - [ ] `item_recent_record.xml` exists with category icon, amount, and description views.
  - [ ] `fragment_home.xml` remains unchanged during this task.
  - [ ] Layout compiles without errors when inflation is tested.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Home page layout inflates without errors
    Tool: Bash
    Steps: Create a minimal instrumentation test that inflates `fragment_home_new.xml` and asserts all required views are present.
    Expected: BUILD SUCCESSFUL and test confirms all required view IDs are found
    Evidence: .sisyphus/evidence/task-2-layout-inflate.txt
  ```

  **Commit**: YES | Message: `feat(home): add home page layout structure` | Files: `app/src/main/res/layout/fragment_home_new.xml`, `app/src/main/res/layout/item_recent_record.xml`

- [ ] 3. Create simplified home page record adapter for recent records

  **What to do**: Create a new `HomeRecentRecordAdapter` for the home page's 3-recent-records display. This adapter should: display category icon, amount (with sign color), and description (or "无备注" if blank); handle tap to navigate to `EditRecordFragment` via `OnItemClickListener`; be a simple flat list adapter (not grouped). Reuse existing `item_recent_record.xml`. Create unit tests for item count, view types, and click dispatch. Keep this adapter separate from `RecordsDateGroupAdapter` — home page uses a different, simpler adapter.

  **Must NOT do**: Do not use `DateGroupAdapter` or `RecordsDateGroupAdapter` for home page recent records. Do not add swipe actions to home page records.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: localized adapter creation with focused tests.
  - Skills: [`systematic-debugging`] - Reason: adapter touch/click regressions need explicit verification.
  - Omitted: [`brainstorming`] - Reason: adapter behavior fully decided.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 4 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt:221` - Existing record binding logic for icon, amount color/sign to reuse.
  - Pattern: `app/src/main/res/layout/item_recent_record.xml:10` - Layout IDs expected by the adapter.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/CategorySelectorAdapter.kt:16` - Existing adapter pattern for simple click dispatch.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/src/main/java/com/example/cardtally/adapter/HomeRecentRecordAdapter.kt` exists.
  - [ ] The adapter shows category icon, amount with sign color, and description.
  - [ ] Tap action dispatches record ID through `OnItemClickListener`.
  - [ ] `app/src/test/java/com/example/cardtally/adapter/HomeRecentRecordAdapterTest.kt` verifies item count, view types, and listener dispatch.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.HomeRecentRecordAdapterTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Adapter renders exactly 3 recent records with correct data
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.HomeRecentRecordAdapterTest.rendersThreeRecentRecordsWithCorrectData"
    Expected: BUILD SUCCESSFUL and assertions confirm exactly 3 items with correct icon, amount, and description
    Evidence: .sisyphus/evidence/task-3-adapter-render.txt

  Scenario: Record tap dispatches correct record ID
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.adapter.HomeRecentRecordAdapterTest.recordTapDispatchesCorrectRecordId"
    Expected: BUILD SUCCESSFUL and assertions confirm the correct record ID is passed to the click listener
    Evidence: .sisyphus/evidence/task-3-adapter-click.txt
  ```

  **Commit**: YES | Message: `feat(home): add simplified recent records adapter` | Files: `app/src/main/java/com/example/cardtally/adapter/HomeRecentRecordAdapter.kt`, `app/src/test/java/com/example/cardtally/adapter/HomeRecentRecordAdapterTest.kt`

- [ ] 4. Assemble home page UI in `HomeFragment` and strip records management code

  **What to do**: Refactor `HomeFragment` to become the pure diary-style home page. Remove all records management code: delete `ItemTouchHelper`, `RecordDragCallback`, multi-select state, selected-delete mode, `showSearchDialog()`, `showDeleteSelectedDialog()`, and all related imports. Add observation of `HomeViewModel` for date header text, month overview data, recent records list, and empty state. Wire the UI: `text_date` shows current date; `card_month_overview` shows income/expense/balance; `layout_recent_records` shows 3 records via `HomeRecentRecordAdapter`; `card_agent` tap navigates to Agent tab (using `MainActivity.navigateToAgent()` or similar — coordinate with add-agent-plan); FAB navigates to `AddRecordFragment`; empty state "记下第一笔" also navigates to `AddRecordFragment`. Update `HomeFragment` to use `fragment_home_new.xml` layout. Add `navigateToAgent()` method to `MainActivity` if not already present (coordinate with add-agent-plan).

  **Must NOT do**: Do not add any records management functionality back. Do not add period switching. Do not add filter or search UI. Do not use `RecordsDateGroupAdapter` or `DateGroupAdapter` for home page.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: coordinates fragment state observation, layout swap, and navigation behavior.
  - Skills: [`systematic-debugging`] - Reason: fragment lifecycle and navigation regressions are likely.
  - Omitted: [`test-driven-development`] - Reason: this task depends on the established state and adapter base.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 5, 6 | Blocked By: 1, 2, 3

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:44` - Fragment-local state to remove.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:266` - Old multi-select mode to remove.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:297` - Old `RecordDragCallback` hookup to remove.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:336` - Old bulk-delete helper to remove.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:103` - FAB navigation to preserve and reuse.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Bottom-nav switch behavior to preserve.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/HomeRecentRecordAdapter.kt:16` - New adapter to use in `HomeFragment`.
  - Pattern: `app/src/main/java/com/example/cardtally/home/HomeViewModel.kt:3` - New ViewModel to observe.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `HomeFragment` no longer imports or constructs `ItemTouchHelper`, `RecordDragCallback`, or any multi-select state.
  - [ ] `HomeFragment` observes `HomeViewModel` state for date header, month totals, recent records, and empty state.
  - [ ] `text_date` displays current date in "M月d日 E" format.
  - [ ] `card_month_overview` displays month income, expense, and balance.
  - [ ] Recent records section shows exactly 3 records via `HomeRecentRecordAdapter`.
  - [ ] Agent card tap navigates to Agent tab.
  - [ ] FAB tap navigates to `AddRecordFragment`.
  - [ ] Empty state "记下第一笔" tap navigates to `AddRecordFragment`.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Home page shows date header with current date
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#dateHeaderShowsCurrentDate`; inside the test assert `R.id.text_date` contains the current month and day in Chinese format.
    Expected: BUILD SUCCESSFUL and Espresso confirms date header shows today's date
    Evidence: .sisyphus/evidence/task-4-home-date-header.txt

  Scenario: FAB navigates to add-record page
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#fabNavigatesToAddRecord`; inside the test call `onView(withId(R.id.fab_add)).perform(click())` and assert an Add Record screen view such as `R.id.btn_save` is displayed.
    Expected: BUILD SUCCESSFUL and Espresso confirms FAB opens AddRecordFragment
    Evidence: .sisyphus/evidence/task-4-home-fab.txt

  Scenario: Agent card tap navigates to Agent tab
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#agentCardNavigatesToAgent`; inside the test call `onView(withId(R.id.card_agent)).perform(click())` and assert the Agent tab is active (e.g., `R.id.nav_agent` is checked or Agent conversation view is displayed).
    Expected: BUILD SUCCESSFUL and Espresso confirms Agent card navigates to Agent tab
    Evidence: .sisyphus/evidence/task-4-home-agent-card.txt
  ```

  **Commit**: YES | Message: `feat(home): redesign home fragment as diary-style home page` | Files: `app/src/main/java/com/example/cardtally/HomeFragment.kt`, `app/src/main/res/layout/fragment_home_new.xml`, `app/src/main/java/com/example/cardtally/MainActivity.kt` (if navigateToAgent added)

- [ ] 5. Coordinate with add-agent-plan for `navigateToAgent()` method

  **What to do**: This task is a coordination point with the add-agent-page-framework plan. After both plans are confirmed, verify that `MainActivity` has a `navigateToAgent()` method (or equivalent) that `HomeFragment` can call for the Agent card tap action. If the add-agent-plan adds this method, no changes needed here. If not, add a simple `navigateToAgent()` method to `MainActivity` that switches to the Agent tab. This task depends on add-agent-plan being in execution or completed.

  **Must NOT do**: Do not implement the Agent conversation UI here. Do not add complex navigation logic.

  **Recommended Agent Profile**:
  - Category: `quick` - Reason: simple coordination and method addition if needed.
  - Skills: [`systematic-debugging`] - Reason: navigation behavior verification.
  - Omitted: [`brainstorming`] - Reason: coordination task.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 6 | Blocked By: 4, add-agent-plan in execution

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Existing navigation switch behavior to extend with Agent tab.
  - Pattern: `.sisyphus/plans/add-agent-page-framework.md` - Agent page plan for coordination.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `MainActivity` has `navigateToAgent()` method that switches to Agent tab.
  - [ ] `HomeFragment` calls `navigateToAgent()` on Agent card tap.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Home page Agent card navigates to Agent tab via MainActivity
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#agentCardNavigatesToAgent`; confirm it passes.
    Expected: BUILD SUCCESSFUL and navigation to Agent tab works
    Evidence: .sisyphus/evidence/task-5-agent-nav.txt
  ```

  **Commit**: YES | Message: `feat(home): add navigateToAgent coordination` | Files: `app/src/main/java/com/example/cardtally/MainActivity.kt` (if method added)

- [ ] 6. Finalize home page integration and run full verification

  **What to do**: After all home page code is in place, run the full home page test suite to verify: date header updates correctly, month overview shows accurate totals, recent records display correctly, Agent card navigation works, FAB and empty state navigation to add-record work, and empty state shows when no records exist. Add any missing instrumentation tests for edge cases (e.g., no records -> empty state shown, partial month data). Verify `HomeFragment` contains no records management code paths.

  **Must NOT do**: Do not add features beyond home page scope. Do not set unrealistic coverage gates.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: regression-hardening task spanning multiple fragments and edge-case verification.
  - Skills: [`requesting-code-review`] - Reason: verification and confidence focus.
  - Omitted: [`brainstorming`] - Reason: verification pass.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Final Verification | Blocked By: 4, 5

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:266` - Old multi-select mode to confirm absent.
  - Pattern: `app/src/main/java/com/example/cardtally/HomeFragment.kt:297` - Old `RecordDragCallback` to confirm absent.
  - Pattern: `app/src/main/java/com/example/cardtally/SearchFragment.kt:77` - Old in-memory search logic; home page should have no search.
  - Pattern: `.sisyphus/plans/records-page-framework.md` - Records page plan that created `RecordsFragment`; home page should not duplicate.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `HomeFragment` contains no active records management code paths.
  - [ ] `HomeFragment` contains no `ItemTouchHelper`, `RecordDragCallback`, or multi-select state imports.
  - [ ] `app/src/androidTest/java/com/example/cardtally/HomeFragmentTest.kt` includes empty-state, partial-data, and navigation coverage.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest` passes.
  - [ ] `./gradlew.bat assembleDebug` succeeds after home page redesign.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Home page shows empty state when no records exist
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#emptyStateShownWhenNoRecords`; assert `R.id.layout_empty_state` is visible and `R.id.layout_recent_records` is hidden.
    Expected: BUILD SUCCESSFUL and Espresso confirms empty state displays correctly
    Evidence: .sisyphus/evidence/task-6-home-empty-state.txt

  Scenario: Recent record tap navigates to edit record page
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.HomeFragmentTest#recentRecordTapNavigatesToEditRecord`; assert the edit record screen is displayed.
    Expected: BUILD SUCCESSFUL and Espresso confirms navigation to EditRecordFragment
    Evidence: .sisyphus/evidence/task-6-home-record-tap.txt
  ```

  **Commit**: YES | Message: `test(home): finalize home page integration and coverage` | Files: `app/src/androidTest/java/com/example/cardtally/HomeFragmentTest.kt`, `app/src/main/java/com/example/cardtally/HomeFragment.kt`

## Final Verification Wave (MANDATORY - after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit - oracle
- [ ] F2. Code Quality Review - unspecified-high
- [ ] F3. Real Manual QA - unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check - deep

## Commit Strategy
- Commit 1: `feat(home): add home page state layer`
- Commit 2: `feat(home): add home page layout structure`
- Commit 3: `feat(home): add simplified recent records adapter`
- Commit 4: `feat(home): redesign home fragment as diary-style home page`
- Commit 5: `test(home): finalize home page integration and coverage`

## Success Criteria
- Home page displays current date header in "M月d日 E" format.
- Month overview card shows current month's income, expense, and balance.
- Exactly 3 most recent records display with category icon, amount, and description.
- Agent card is visible and tap navigates to Agent tab.
- FAB and empty state "记下第一笔" both navigate to `AddRecordFragment`.
- Recent record tap navigates to `EditRecordFragment`.
- `HomeFragment` contains no records management code (no list state, no adapter for full records, no filter dialog, no multi-select, no drag-sort).
- Empty state displays when no records exist.
- Build succeeds after home page redesign.
