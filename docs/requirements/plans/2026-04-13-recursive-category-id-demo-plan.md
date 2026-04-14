# Recursive Category ID Demo Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Upgrade CardTally from flat category name storage to recursive categories with `records.category_id` as the canonical link, while keeping the current Fragment + XML + SQLite architecture and preserving flat category statistics.

**Architecture:** This demo should evolve the existing `DatabaseHelper` schema instead of replacing it. Categories become a recursive tree via `categories.parent_id`, records store the selected leaf through `category_id`, and records also keep display snapshots such as category name and path so old screens can render safely during migration and fallback handling. Settings should store a local max-depth preference with default `2` and hard cap `50`, and category management plus record entry should enforce that only leaf categories can be assigned to records.

**Tech Stack:** Kotlin, Fragment, XML layouts, SQLiteOpenHelper (`DatabaseHelper.kt`), SharedPreferences helpers, RecyclerView adapters, JUnit4, AndroidX instrumentation tests.

---

## Execution intent and zero-context handoff

This plan is for execution, not discussion. Assume the implementer has zero context, and do not make them reread the whole repo before starting.

Current repo facts that matter:

- Data is local only. There is no server, no Room, and no Navigation Component.
- `DatabaseHelper.kt` is still on database version `9` and stores `records.category` as plain text.
- `Category.kt` is currently only `id`, `name`, `type`, `icon`.
- `Record.kt` is currently only `category: String`, with no category ID or path snapshot.
- `AddRecordFragment.kt` and `EditRecordFragment.kt` both use `CategorySelectorAdapter.kt` and currently bind selection by category name.
- `CategoryManageFragment.kt` and `CategoryAdapter.kt` currently manage a flat list by income/expense type.
- `HomeFragment.kt`, `SearchFragment.kt`, `adapter/DateGroupAdapter.kt`, and `StatisticsFragment.kt` all still display or query category strings.
- Business rule from `docs/requirements/decisions/business_rules.md`: category statistics stay flat. Parent categories do not auto-roll-up child totals.

## Scope and non-goals

In scope for this demo:

- Recursive categories through `categories.parent_id`
- Canonical record link through `records.category_id`
- `category_path_snapshot` and compatible display fallback on records
- Settings entry for category max depth, default `2`, max `50`
- Tree-aware category management
- Leaf-only record assignment in add and edit record flows
- Compatibility updates for home, search, date-group display, and statistics
- Migration handling for legacy record names that do not resolve uniquely

Out of scope for this demo:

- New navigation structure or new detail pages
- Automatic parent roll-up in statistics
- Deleting a category subtree that is still risky to migrate safely
- Room migration, repository layer rewrite, or remote sync

## Required implementation rules

1. `records.category_id` becomes the canonical relationship for newly created or edited records.
2. Keep record-side snapshots for safe display. At minimum plan for `category_name_snapshot` and `category_path_snapshot`. If the team keeps legacy `category` temporarily, document it as compatibility only.
3. Records can only point to leaf categories.
4. Lowering the max-depth setting below the current deepest category level must be rejected with a user-facing message.
5. Statistics remain flat. Selecting parent `餐饮` does not include `餐饮/早餐` automatically.
6. Migration must not guess when a legacy category name maps to more than one possible recursive category. Ambiguous rows need an explicit unresolved state and visible fallback handling.

## Suggested file map

Primary code paths to touch:

- `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- `app/src/main/java/com/example/cardtally/model/Category.kt`
- `app/src/main/java/com/example/cardtally/model/Record.kt`
- `app/src/main/java/com/example/cardtally/CategoryManageFragment.kt`
- `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
- `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
- `app/src/main/java/com/example/cardtally/HomeFragment.kt`
- `app/src/main/java/com/example/cardtally/SearchFragment.kt`
- `app/src/main/java/com/example/cardtally/StatisticsFragment.kt`
- `app/src/main/java/com/example/cardtally/adapter/CategoryAdapter.kt`
- `app/src/main/java/com/example/cardtally/adapter/CategorySelectorAdapter.kt`
- `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt`
- `app/src/main/java/com/example/cardtally/SettingsFragment.kt`
- `app/src/main/res/layout/fragment_settings.xml`
- `app/src/main/res/layout/fragment_category_manage.xml`
- `app/src/main/res/layout/fragment_add_record.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-en/strings.xml`

Likely new files:

- `app/src/main/java/com/example/cardtally/util/CategoryHierarchySettingsHelper.kt`
- `app/src/main/java/com/example/cardtally/model/CategoryDepthInfo.kt` or similar small model for validation/query results
- `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryMigrationTest.kt`
- `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt`
- `app/src/test/java/com/example/cardtally/util/CategoryHierarchySettingsHelperTest.kt`

If UI complexity grows, it is acceptable to add one focused adapter or small helper for tree rows, but do not add new pages.

---

### Task 1: Add failing tests for recursive schema, snapshots, and migration outcomes

**Files:**
- Modify: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperAgentChatSessionTest.kt` only if extracting shared setup is useful
- Create: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryMigrationTest.kt`
- Create: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt`
- Test target: `app/src/androidTest/java/com/example/cardtally/database/`

**Step 1: Write a failing migration test for upgraded schemas**

Cover these cases:

- database upgrade adds `categories.parent_id`
- database upgrade adds `records.category_id`
- database upgrade adds `records.category_name_snapshot`
- database upgrade adds `records.category_path_snapshot`
- existing plain-text records still load after upgrade

**Step 2: Write a failing migration test for ambiguous legacy names**

Seed old-style data such as two leaf categories with the same name under different parents, then upgrade. Assert that the migrated record does **not** silently bind to one of them. The plan should expect one of these explicit outcomes:

- `category_id` stays `NULL`, while snapshots preserve the old display value, or
- a dedicated unresolved flag or unresolved-note column is added

Prefer the first option unless code review proves a second field is required.

**Step 3: Write a failing query test for leaf-only assignment and flat statistics**

Assert that:

- parent categories can exist
- only leaf categories are returned by the record picker query
- statistics query groups by the exact assigned category record, not by parent
- a parent total remains separate from child totals

**Step 4: Run instrumentation tests to confirm failure**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Expected: the new tests fail because schema columns and recursive queries do not exist yet.

**Step 5: Commit**

```bash
git add app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryMigrationTest.kt app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt
git commit -m "test: define recursive category database expectations"
```

---

### Task 2: Implement schema and model foundations for recursive categories

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- Modify: `app/src/main/java/com/example/cardtally/model/Category.kt`
- Modify: `app/src/main/java/com/example/cardtally/model/Record.kt`
- Test: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryMigrationTest.kt`
- Test: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt`

**Step 1: Bump the database version and define new columns**

Add at least:

- `categories.parent_id INTEGER`
- `records.category_id INTEGER`
- `records.category_name_snapshot TEXT`
- `records.category_path_snapshot TEXT`

Do not drop old columns in this demo. Keep migration additive.

**Step 2: Extend the models minimally**

Update `Category.kt` with recursive metadata:

```kotlin
data class Category(
    var id: Long = 0,
    var name: String = "",
    var type: Int = 0,
    var icon: String? = null,
    var parentId: Long? = null
)
```

Update `Record.kt` with canonical link plus snapshots:

```kotlin
data class Record(
    var id: Long = 0,
    var date: String = "",
    var amount: Double = 0.0,
    var category: String = "",
    var categoryId: Long? = null,
    var categoryNameSnapshot: String = "",
    var categoryPathSnapshot: String = "",
    var type: Int = 0,
    var description: String? = null,
    var assetSource: String? = null,
    var sortOrder: Int = 0
)
```

Keep `category` temporarily if needed for old call sites. Document it in code comments as compatibility only.

**Step 3: Implement upgrade logic**

In `onUpgrade`, add additive `ALTER TABLE` statements and a data migration pass that:

- fills snapshots from legacy category strings
- resolves `category_id` only when exactly one matching leaf category exists for the same type
- leaves `category_id` null for ambiguous or missing matches
- never invents a parent mapping

**Step 4: Update insert and read methods**

Update `addRecord`, `updateRecord`, `getAllRecords`, `getRecordById`, `getRecordsByDateRange`, and other record readers so they persist and hydrate the new fields.

Use one shared display rule in `DatabaseHelper` or a small helper:

- preferred display path: resolved category path from current category tree
- fallback display path: `category_path_snapshot`
- last fallback: legacy `category`

**Step 5: Add focused category queries for recursion**

At minimum add methods equivalent to:

- `getCategoryById(id)`
- `getCategoriesByParent(type, parentId)`
- `getLeafCategoriesByType(type)`
- `getCategoryDepth(type)` or `getMaxCategoryDepthByType(type)`
- `hasChildCategories(categoryId)`
- `buildCategoryPath(categoryId)`

**Step 6: Run the instrumentation tests again**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Expected: migration and query tests pass or fail only on UI-facing tasks not yet implemented.

**Step 7: Commit**

```bash
git add app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt app/src/main/java/com/example/cardtally/model/Category.kt app/src/main/java/com/example/cardtally/model/Record.kt app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryMigrationTest.kt app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt
git commit -m "feat: add recursive category schema foundation"
```

---

### Task 3: Add category hierarchy settings with max-depth guardrails

**Files:**
- Create: `app/src/main/java/com/example/cardtally/util/CategoryHierarchySettingsHelper.kt`
- Create: `app/src/test/java/com/example/cardtally/util/CategoryHierarchySettingsHelperTest.kt`
- Modify: `app/src/main/java/com/example/cardtally/SettingsFragment.kt`
- Modify: `app/src/main/res/layout/fragment_settings.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Step 1: Write failing unit tests for settings persistence**

Test these rules:

- default max depth is `2`
- max accepted value is `50`
- values lower than `1` clamp or reject per helper design
- values above `50` are rejected or clamped consistently

Prefer explicit rejection in UI and safe getter normalization in helper.

**Step 2: Implement the settings helper**

Follow the repo pattern used by `QuickAddHelper.kt` and `AiAssistantSettingsHelper.kt`. Keep it local `SharedPreferences`, with a tiny API:

- `getCategoryMaxDepth(context): Int`
- `saveCategoryMaxDepth(context, depth: Int)`

**Step 3: Add the settings entry in the existing settings page**

Update `fragment_settings.xml` and `SettingsFragment.kt` to expose a max-depth item inside the current settings screen. Do not create a new page. A simple dialog with numeric input is enough for this demo.

**Step 4: Enforce the lower-bound rejection rule**

Before saving a smaller depth, query the current maximum category depth from `DatabaseHelper`. If the existing tree is deeper than the requested value, reject the save and show a clear message.

**Step 5: Run unit tests**

Run:

```powershell
.\gradlew.bat testDebugUnitTest
```

Expected: helper tests pass.

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/cardtally/util/CategoryHierarchySettingsHelper.kt app/src/test/java/com/example/cardtally/util/CategoryHierarchySettingsHelperTest.kt app/src/main/java/com/example/cardtally/SettingsFragment.kt app/src/main/res/layout/fragment_settings.xml app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "feat: add category hierarchy depth setting"
```

---

### Task 4: Make category management tree-aware and deletion-safe

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/CategoryManageFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/adapter/CategoryAdapter.kt`
- Modify: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- Modify: `app/src/main/res/layout/fragment_category_manage.xml`
- Modify: `app/src/main/res/layout/item_category.xml`
- Modify: `app/src/main/res/layout/dialog_add_category.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Test: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt`

**Step 1: Extend database tests for tree operations**

Add failing tests for:

- adding a child category with `parent_id`
- rejecting child depth beyond the saved max depth
- detecting whether a category has children
- blocking delete when category has children or linked records

**Step 2: Add tree-capable database methods**

Implement minimal methods such as:

- `addCategory(category, parentId)` or use `Category.parentId`
- `getCategoriesForManagement(type)` ordered for tree rendering
- `canDeleteCategory(categoryId)` returning reason codes if useful
- `getCategoryChildren(categoryId)`

**Step 3: Update the management UI**

Keep `CategoryManageFragment` as the entry page, but make it tree-aware:

- show indentation or path subtitle for child rows
- allow adding a root or child category from the same dialog flow
- show the chosen parent in add and edit dialogs
- prevent selecting an invalid parent that would exceed depth

Do not add a new detail page. A dialog-driven flow is enough.

**Step 4: Make deletion explicitly conservative**

For this demo, block delete when the category has child categories or any linked records, including unresolved legacy rows that still display the same snapshot. Do not attempt risky cascade delete.

**Step 5: Run tests**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
```

Expected: recursive category query tests pass.

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/cardtally/CategoryManageFragment.kt app/src/main/java/com/example/cardtally/adapter/CategoryAdapter.kt app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt app/src/main/res/layout/fragment_category_manage.xml app/src/main/res/layout/item_category.xml app/src/main/res/layout/dialog_add_category.xml app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt
git commit -m "feat: add tree-aware category management"
```

---

### Task 5: Switch add and edit record flows to leaf-only category assignment

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/adapter/CategorySelectorAdapter.kt`
- Modify: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- Modify: `app/src/main/res/layout/fragment_add_record.xml`
- Modify: `app/src/main/res/layout/item_category_selector.xml`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Step 1: Add a failing database or fragment-facing test for leaf picker data**

At minimum ensure `getLeafCategoriesByType(type)` excludes parents and returns enough display information for the picker, such as `name`, `path`, `icon`, and `id`.

**Step 2: Update the selector adapter contract**

Stop treating category name as the unique identity. Selection must track `Category.id`, and the rendered label should help distinguish same-name leaves by path.

**Step 3: Update add record save logic**

`AddRecordFragment.kt` should save:

- `categoryId = selectedCategory.id`
- `categoryNameSnapshot = selectedCategory.name`
- `categoryPathSnapshot = resolved full path`
- compatibility `category` string only if still required by remaining code during transition

If there is no selected leaf category, block save.

**Step 4: Update edit record loading and update logic**

`EditRecordFragment.kt` must prefer `record.categoryId` when restoring selection. If the ID is null because the row came from an ambiguous legacy migration, show the snapshot path and require the user to reselect a leaf before saving.

**Step 5: Run targeted verification**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Then manually verify:

- add expense with root and child categories present, only leaves are selectable
- edit an existing migrated record and confirm the correct leaf stays selected
- ambiguous legacy record shows readable fallback text and can be repaired by explicit reselection

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/cardtally/AddRecordFragment.kt app/src/main/java/com/example/cardtally/EditRecordFragment.kt app/src/main/java/com/example/cardtally/adapter/CategorySelectorAdapter.kt app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt app/src/main/res/layout/fragment_add_record.xml app/src/main/res/layout/item_category_selector.xml app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "feat: assign record categories by leaf id"
```

---

### Task 6: Update home, search, and date-group display to use snapshots and path fallback safely

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/HomeFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/SearchFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt`
- Modify: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`

**Step 1: Add a failing query test for display fallback**

Cover three record states:

- fully resolved `category_id`
- migrated unresolved record with only snapshots
- older compatibility record with only legacy `category`

Assert that each still renders a stable category label and path.

**Step 2: Centralize display text resolution**

Do not duplicate fallback logic across three fragments. Add one reusable method in `DatabaseHelper` or a dedicated small helper that returns the display category name and display path for a `Record`.

**Step 3: Update search behavior**

`SearchFragment.kt` currently searches `record.category`. Extend it to search:

- current display path
- name snapshot
- legacy category string

This preserves backward-compatible keyword behavior when users search by old flat names or new paths.

**Step 4: Update list presentation**

`DateGroupAdapter.kt` should show a meaningful label for same-name child categories. If UI space is tight, keep the main title as leaf name and use the secondary line for the path.

**Step 5: Verify manually and with build**

Run:

```powershell
.\gradlew.bat assembleDebug
```

Manually verify home and search lists with:

- resolved child categories
- unresolved migrated rows
- duplicate leaf names under different parents

**Step 6: Commit**

```bash
git add app/src/main/java/com/example/cardtally/HomeFragment.kt app/src/main/java/com/example/cardtally/SearchFragment.kt app/src/main/java/com/example/cardtally/adapter/DateGroupAdapter.kt app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt
git commit -m "feat: add recursive category display compatibility"
```

---

### Task 7: Keep statistics flat while moving to category ID based reads

**Files:**
- Modify: `app/src/main/java/com/example/cardtally/StatisticsFragment.kt`
- Modify: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- Test: `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt`

**Step 1: Write or extend failing tests for flat recursive statistics**

Use a tree like:

- `餐饮`
  - `早餐`
  - `午餐`

Assert that stats report exact buckets for the assigned leaf records and do not auto-merge them into `餐饮`.

**Step 2: Update statistics queries**

Replace string-only grouping with a category-aware query that still returns flat buckets. For display, prefer `category_path_snapshot` or resolved path so duplicate leaf names remain distinguishable.

If the UI adapter must stay `Map<String, Double>`, use exact display path strings as keys. Do not sum parents and children together.

**Step 3: Verify the screen still works**

Run:

```powershell
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleDebug
```

Manually verify the statistics screen shows separate entries for sibling leaves and does not create an automatic parent total.

**Step 4: Commit**

```bash
git add app/src/main/java/com/example/cardtally/StatisticsFragment.kt app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecursiveCategoryQueryTest.kt
git commit -m "fix: keep recursive category statistics flat"
```

---

### Task 8: Final regression pass, migration checks, and manual demo script

**Files:**
- Modify: any touched files above if regressions appear
- Optional docs note: add a short execution summary to the eventual task PR, not to this plan file

**Step 1: Re-run full required verification serially**

Run in this order, one command at a time:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat assembleDebug
```

**Step 2: Manual migration script on a debug build**

Prepare a local database with:

- one unique legacy category name that can resolve safely
- one duplicate legacy category name under different parents
- one deleted or missing legacy category name

Verify post-upgrade behavior:

- unique names get `category_id`
- ambiguous names stay unresolved but still display snapshots
- missing names still display snapshots and can be repaired by editing the record

**Step 3: Manual category-depth validation**

Verify:

- default max depth starts at `2`
- values over `50` are rejected or normalized per helper contract
- lowering below actual tree depth is rejected
- adding a child beyond the saved max depth is blocked

**Step 4: Manual CRUD safety checks**

Verify:

- parent categories cannot be assigned to records
- delete blocks when category has children
- delete blocks when category has linked records
- record add, edit, delete still preserve current asset rollback rules

**Step 5: Final commit**

```bash
git add app/src/main/java/com/example/cardtally app/src/main/res app/src/test app/src/androidTest
git commit -m "feat: deliver recursive category id demo"
```

---

## Notes for the implementer

- Start from `DatabaseHelper.kt`, not from the UI. The UI currently depends on category strings everywhere, so schema and model groundwork must land first.
- Keep migration additive and reversible where possible. Do not remove the legacy `category` field in the same demo.
- Be strict with ambiguous migration. Preserving a visible unresolved state is safer than binding a record to the wrong category.
- Reuse the current settings page and category management page. Do not create extra navigation or detail-page work.
- If duplicate leaf names exist under different parents, every display surface should prefer a path such as `餐饮 / 早餐` over a bare name.
- For this demo, a parent category can exist for organization and management, but records must always point to a leaf.
- For this demo, statistics stay flat even after the recursive tree lands.

## Minimal execution order

1. Database tests
2. Schema and model migration
3. Settings helper and max-depth UI
4. Tree-aware category management
5. Add and edit record flows
6. Home, search, and date-group compatibility
7. Statistics flat behavior
8. Full serial verification

Plan complete and saved to `docs/requirements/plans/2026-04-13-recursive-category-id-demo-plan.md`. Two execution options:

**1. Subagent-Driven (this session)**, dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Parallel Session (separate)**, open a new session with executing-plans, batch execution with checkpoints
