# Surface Traceability Audit

> Task: `2. Freeze active surface, transition, state, and behavior-preservation ledgers`  
> Date: 2026-08-12  
> Verdict: **PASS, with source-backed inventory coverage**

## Authority And Method

The audit used CodeGraph source and call-path results first. The Kotlin LSP was unavailable because `kotlin-lsp` is not installed, so no installation was requested or performed. The LSP installation decision was recorded as declined. Non-indexed documents used only for scope and rule reconciliation were the plan, collaboration snapshot, navigation menu, and business-rule decision.

The canonical inventory is [`docs/design/current-ux-inventory.md`](../../../docs/design/current-ux-inventory.md). It maps all proved live Fragment routes once, then maps their shared layouts, adapters, sheets, dialogs, PopupMenu, and drawer separately to avoid duplicating a route for each component.

## Source Traceability

| Audit domain | Proven source paths | Result |
| --- | --- | --- |
| Activity shell and conditional destinations | `app/src/main/java/com/example/cardtally/MainActivity.kt`; `app/src/main/res/layout/activity_main.xml`; `app/src/main/res/menu/bottom_nav_menu.xml` | Five declared destinations are Home, Ledger, Assets, Agent, and Me. Assets and Agent independently become hidden, producing the required 4 and 3 destination variants. |
| Quick Add | `MainActivity.kt`; `AddRecordFragment.kt`; `fragment_add_record.xml` | Cold start selects Add Record when Quick Add is enabled. With no back-stack entry, Add Record back selects Home. Normal entry is a back-stack route. |
| Home and records | `HomeFragment.kt`; `HomeRecentRecordAdapter.kt`; `fragment_home.xml`; `item_home_recent_record.xml` | Home opens Ledger, conditional Agent, Add Record, Edit Record, and record deletion confirmation. |
| Ledger | `StatisticsFragment.kt`; `StatisticsAdapter.kt`; `DateGroupAdapter.kt`; `LedgerCalendarAdapter.kt`; `fragment_statistics.xml`; `item_statistics.xml`; `item_date_header.xml`; `item_record.xml`; `item_ledger_chart_legend.xml`; `bottom_sheet_ledger_period.xml`; `item_ledger_calendar_day.xml` | `StatisticsFragment` is the single active Ledger. It supplies statistics and detail modes, the range sheet, the mode PopupMenu, Add Record, Edit Record, and delete confirmation. No `RecordsFragment` was found as an active destination. |
| Record form family | `AddRecordFragment.kt`; `EditRecordFragment.kt`; `RecordAssetSheetAdapter.kt`; `RecordCategoryTreeAdapter.kt`; `fragment_add_record.xml`; `bottom_sheet_record_date.xml`; `bottom_sheet_record_assets.xml`; `bottom_sheet_record_category.xml` | Add and Edit share `fragment_add_record.xml` and all three record sheets. Category confirmation rejects a non-leaf category. |
| Asset family | `AssetFragment.kt`; `AddAssetFragment.kt`; `EditAssetFragment.kt`; `AssetRecordsFragment.kt`; `ArchivedAssetsFragment.kt`; `AssetAdapter.kt`; `fragment_asset.xml`; `fragment_add_asset.xml`; `fragment_asset_records.xml`; `fragment_archived_assets.xml` | Add and Edit Asset share `fragment_add_asset.xml`. Active-list and archived-row deletion each use confirmation before permanent deletion and list reload. The reachable Edit Asset form is distinct: its delete button immediately calls `DatabaseHelper.deleteAsset`, shows a success Toast, and pops the back stack without confirmation. |
| Agent family | `AgentFragment.kt`; `AiAssistantSettingsFragment.kt`; `AgentChatAdapter.kt`; `AgentSessionAdapter.kt`; `fragment_agent.xml`; `fragment_ai_assistant_settings.xml` | Agent has configuration-required, ready, sending, receiving, classified failure, persisted-session, partial-reply, session drawer, new-session, rename, and AI configuration states. Drawer open hides the shell navigation and drawer close restores it. |
| Me and categories | `SettingsFragment.kt`; `ThemeSettingsFragment.kt`; `CategoryManageFragment.kt`; `CategoryAdapter.kt`; `IconPickerAdapter.kt`; `fragment_settings.xml`; `fragment_theme_settings.xml`; `fragment_category_manage.xml`; `dialog_add_category.xml`; `dialog_icon_picker.xml` | Me reaches AI configuration, category management, theme settings, language dialog, and category-depth dialog. Category management reaches add/edit, icon picker, and protected deletion. |

## Omission And Duplicate Check

| Check | Evidence | Result |
| --- | --- | --- |
| Every shell destination mapped once | The destination `when` in `MainActivity` names Home, Asset, Statistics, Agent, and Settings. Each has one route row in the canonical inventory. | Pass |
| Secondary record routes mapped | Home, Ledger Details, and Asset Records each instantiate Edit Record. Home and Ledger instantiate Add Record. | Pass |
| Shared layouts not double-counted as routes | Add/Edit Record are one family row with `fragment_add_record.xml`. Add/Edit Asset are one family row with `fragment_add_asset.xml`. | Pass |
| All live sheet, popup, drawer, and dialog families mapped | Three record sheets, active Ledger period sheet, Ledger PopupMenu, Agent drawer and session dialogs, Me dialogs, category dialogs, row-level destructive confirmations, and Edit Asset's immediate-delete exception have individual component or route rows. | Pass |
| State applicability recorded | Populated, empty, validation, destructive, loading, error, disabled, conditional navigation, approved new behavior, and design-only primitive states are labeled in the state matrix. | Pass |
| Dormant Search excluded | CodeGraph found only the self-contained `SearchFragment` factory and implementation. It is absent from `MainActivity` and reachable transactions. | Pass |
| Dormant Categories excluded | CodeGraph found the isolated `CategoriesFragment` implementation with no proved caller. The live category path is `SettingsFragment` to `CategoryManageFragment`. | Pass |
| Stale Ledger mode sheet excluded | `StatisticsFragment` calls the active PopupMenu for mode and inflates the period sheet. No live `R.layout.bottom_sheet_ledger_mode` caller was proven. | Pass |
| Legacy asset dialogs excluded | Active Assets interactions route to Add Asset and Edit Asset Fragments. The private `AssetFragment` add and edit dialog helpers had no proved caller. | Pass |
| Asset deletion entry points distinguished | CodeGraph traced `DatabaseHelper.deleteAsset` to three live callers: confirmation paths from active-list and archived rows, plus `EditAssetFragment.deleteAsset`. Only the Edit Asset caller deletes immediately, shows a Toast, and pops. | Pass |

## Dirty-Worktree Provenance

At the time of verification, `git status --short` showed a modified `AGENTS.md`, an untracked `.omo/` tree, the untracked inventory, and two untracked decision documents. This Todo 2 evidence is limited to `docs/design/current-ux-inventory.md` and `.omo/evidence/task-2-cardtally-light-ux-redesign/surface-traceability.md`; the evidence file is reported through the untracked `.omo/` directory. This statement records the observable artifact scope, not authorship or historical change attribution.

## Risks And Boundaries

1. `ThemeSettingsFragment` is currently reachable and is intentionally included. The later plan approves removing its route, but this inventory does not claim that work is complete.
2. The Agent tab currently follows the AI-entry preference only. The plan's later enabled-and-configured predicate is approved new behavior, not a current implementation claim.
3. If Agent is active when its preference is turned off, its next resume selects Home. No equivalent source-backed active-Assets fallback was found. No source-backed loading state exists for synchronous SQLite lists. The only current loading treatment is Agent sending and streaming.
