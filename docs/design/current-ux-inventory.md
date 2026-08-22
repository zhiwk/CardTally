# Current UX Inventory

> Scope: runtime UX proven reachable from the current `MainActivity` navigation shell or a transaction initiated by a reachable surface. Source code is the authority. This inventory was frozen for Todo 2 of `cardtally-light-ux-redesign` on 2026-08-12. It describes current behavior, not the planned redesign.

## State Labels

| Label | Meaning |
| --- | --- |
| Current behavior | A runtime state or interaction implemented on a reachable path. |
| Approved new behavior | A plan-approved redesign behavior with no current runtime route. It is not an implementation claim. |
| Design-only primitive | A reusable future visual or interaction state with no current runtime authority. |

## Shell And Destination Variants

| Surface | Runtime source | Entry condition | Back and conditional navigation | State label |
| --- | --- | --- | --- | --- |
| Activity shell | `MainActivity.kt`, `activity_main.xml`, `bottom_nav_menu.xml` | App launch. First fragment is Home unless Quick Add is enabled. | Default shell back behavior pops the Fragment back stack, then delegates to the system. Route-level callbacks can override it; for example, Add Record routes system back through its own return logic. `nav_shell` and bottom navigation are visible only for Home, Ledger, Assets, Agent, and Me. | Current behavior |
| Five destinations | `bottom_nav_menu.xml`, `MainActivity.kt` | Assets visibility preference is on and AI entry preference is on. | Home, Ledger, Assets, Agent, Me. Selecting a destination replaces the container without adding a back-stack entry. | Current behavior |
| Four destinations | Same shell | Exactly one of Assets or Agent is hidden by its independent preference. | The corresponding menu item is hidden. No alternate route is introduced. | Current behavior |
| Three destinations | Same shell | Both Assets and Agent are hidden by their independent preferences. | Home, Ledger, and Me remain. | Current behavior |
| Quick Add cold start | `MainActivity.kt`, `AddRecordFragment.kt`, `fragment_add_record.xml` | `QuickAddHelper.getQuickAdd()` is true during a fresh launch. | Add Record has no back-stack entry on this path. Its close, cancel, and system back select Home. A normal Add Record entry instead pops its caller. | Current behavior |
| Locale transition overlay | `MainActivity.kt`, `activity_main.xml` | `LanguageHelper.consumePendingLocaleTransition()` returns true. | A pre-draw fade removes the overlay. This is an activity transition treatment, not a destination. | Current behavior |

## Reachable Fragment Routes

Each runtime Fragment appears once in this table. Shared layouts are recorded on the family row rather than repeated as separate routes.

| Route | Entry and source chain | Layout and adapters | Back, state, and behavior |
| --- | --- | --- | --- |
| Home | Shell `nav_home`, or normal cold start, via `MainActivity.kt`. | `fragment_home.xml`; `HomeRecentRecordAdapter` uses `item_home_recent_record.xml`. | Top level. Ledger affordance selects Ledger. Agent note selects Agent only when the AI entry preference is enabled. FAB opens Add Record on the back stack. Empty latest-record result shows `text_empty`; populated results support edit and delete confirmation. |
| Ledger, Statistics and Details | Shell `nav_statistics`, or Home Ledger affordance, via `MainActivity.kt` and `HomeFragment.kt`. The active Ledger is `StatisticsFragment`, not a `RecordsFragment`. | `fragment_statistics.xml`; `StatisticsAdapter` uses `item_statistics.xml`; `DateGroupAdapter` uses `item_date_header.xml` and `item_record.xml`; chart legend uses `item_ledger_chart_legend.xml`. | Top level. The mode selector opens the active PopupMenu for expense statistics, income statistics, or record details. The range selector opens the period sheet. FAB opens Add Record on the back stack. Statistics and Details each show a deterministic empty state when their filtered data is empty. Record details open Edit Record or a delete confirmation. |
| Add Record and Edit Record | Add: Home FAB, Ledger FAB, or Quick Add cold start. Edit: Home, Ledger Details, and Asset Records record actions. | Both use `fragment_add_record.xml`. Record asset sheet uses `RecordAssetSheetAdapter` and `item_record_asset_sheet.xml`. Record category sheet uses `RecordCategoryTreeAdapter` and `item_record_category_tree.xml`. | Secondary route, so navigation is hidden. Add normal entry pops its caller after close, cancel, or save. Quick Add cold start returns to Home. Edit is created with `record_id` and returns through the back stack after update or delete. The amount defaults to `0.00` in Add Record. Both forms use the same date, asset, and category sheets. A category can be confirmed only when it is a leaf. Validation feedback is a Toast. |
| Assets | Shell `nav_asset` when `AssetDisplayHelper.getShowAsset()` is true. | `fragment_asset.xml`; `AssetAdapter` uses `item_asset.xml`. | Top level. Empty assets show `text_empty`. FAB opens Add Asset. Archive action opens Archived Assets. A populated asset opens Asset Records, edit opens Edit Asset, archive updates the list, and an active-list row delete asks for confirmation. |
| Add Asset and Edit Asset | Add: Assets FAB. Edit: active asset row action. | Both use `fragment_add_asset.xml`. | Secondary route, so navigation is hidden. Back, successful add or update pops the caller. Required name and numeric amount validation use Toast feedback. Edit exposes a delete action that immediately calls the current asset deletion helper, shows a success Toast, and pops the caller without a confirmation dialog. |
| Asset Records | Active or archived asset row tap, created with asset name, amount, and type. | `fragment_asset_records.xml`; `DateGroupAdapter` renders grouped records. | Secondary route, so navigation is hidden. Empty records show `text_empty`. Edit opens Edit Record. Delete is immediate through the current record deletion helper and then reloads the list. Record lookup is name based through `assetSource`, which is existing accepted behavior rather than immutable asset identity. |
| Archived Assets | Assets archive action. | `fragment_archived_assets.xml`; `AssetAdapter` uses `item_asset.xml`. | Secondary route, so navigation is hidden. Empty archive shows `text_empty`. A row can open Asset Records, open the legacy active edit dialog, restore, or open permanent-delete confirmation. |
| Agent chat | Shell `nav_agent` only when the AI entry preference is enabled. Home Agent note can select the same destination. | `fragment_agent.xml`; `AgentChatAdapter` uses `item_agent_message.xml`; `AgentSessionAdapter` uses `item_agent_session.xml`. | Top level unless the session drawer is open. Incomplete MiniMax configuration shows the configuration-required state and a settings affordance. Send state disables composer and session actions, shows progress while streaming, and updates status for sending, receiving, ready, or classified error. Sessions and messages persist locally. A partial assistant reply is retained when cancellation, timeout, interruption, or another failure occurs after content arrives. If the AI entry preference is turned off while Agent resumes, it selects Home. |
| AI configuration | Me API configuration card or Agent configuration-required affordance. | `fragment_ai_assistant_settings.xml`. | Secondary route, so navigation is hidden. Existing API key, model, and full request URL are loaded, saved locally, and shown with a saved-value status. |
| Me | Shell `nav_settings`. | `fragment_settings.xml`; settings item layouts are `item_setting.xml`, `item_setting_switch.xml`, `item_setting_arrow.xml`, and `item_setting_divider.xml`. | Top level. It persists Quick Add, AI-entry visibility, Asset visibility, category-depth limit, and language. It opens AI configuration, Category Management, and Theme Settings. Language and category-depth use dialogs. Toggling AI or Assets updates the shell menu visibility. |
| Theme Settings | Me theme card. | `fragment_theme_settings.xml`. | Secondary route. The current implementation still exposes Light, Dark, and System choices and recreates the activity after a choice. This is current runtime behavior even though the later redesign plan supersedes it. |
| Category Management | Me category-management card. | `fragment_category_manage.xml`; `CategoryAdapter` uses `item_category.xml`. | Secondary route, so navigation is hidden. Expense and income tabs reload the tree. Empty tree shows `text_empty`. Add and edit use the category dialog. Delete uses confirmation and preserves database rejection feedback for invalid parent, cycles, depth, has-children, and in-use cases. |

## Active Sheets, Popups, Drawers, And Dialogs

| Surface | Owner and entry | Layout or adapter | Dismissal and state | State label |
| --- | --- | --- | --- | --- |
| Record date sheet | Add Record and Edit Record date row | `bottom_sheet_record_date.xml` | Close dismisses. Today sets the picker. Confirm updates the form date and dismisses. | Current behavior |
| Record asset sheet | Add Record and Edit Record asset row | `bottom_sheet_record_assets.xml`, `RecordAssetSheetAdapter`, `item_record_asset_sheet.xml` | Close dismisses. Selecting an asset, including the no-asset row, updates the form and dismisses. | Current behavior |
| Record category sheet | Add Record and Edit Record category row | `bottom_sheet_record_category.xml`, `RecordCategoryTreeAdapter`, `item_record_category_tree.xml` | Parent rows expand or collapse. A leaf selection is only applied on confirm. Close dismisses. | Current behavior |
| Ledger period sheet | Ledger range selector | `bottom_sheet_ledger_period.xml`, `LedgerCalendarAdapter`, `item_ledger_calendar_day.xml` | It selects the custom range and returns rendering to the active Ledger mode. It is the live Ledger sheet. | Current behavior |
| Ledger mode PopupMenu | Ledger mode selector | Programmatic `PopupMenu` in `StatisticsFragment.kt` | Selects expense statistics, income statistics, or record details. The toolbar menu button is disabled, so it is not a second live popup route. | Current behavior |
| Agent session drawer | Agent menu button | In `fragment_agent.xml`; `AgentSessionAdapter`, `item_agent_session.xml` | Overlay tap and system back close it. Opening calls `MainActivity.setBottomNavigationTemporarilyHidden(true)`; closing restores navigation. | Current behavior |
| Agent session create and rename dialogs | Agent plus button, or session long press | Programmatic `AlertDialog` with one text input | Blank titles remain invalid. Create stops an active stream if needed, creates and activates a persisted session, and closes the drawer. Rename persists the new title. | Current behavior |
| Agent configuration-required treatment | Agent when `isMiniMaxConfigComplete()` is false | `fragment_agent.xml` | Settings button opens AI configuration. The message list remains rendered while sending is disabled. | Current behavior |
| Language dialog | Me language card | Programmatic single-choice `AlertDialog` | A selection updates the app locale. Cancel dismisses. | Current behavior |
| Category-depth dialog | Me category-depth card | Programmatic `AlertDialog` with numeric input | Invalid or lower-than-existing-tree depth produces feedback. Confirm persists a sanitized valid limit. | Current behavior |
| Category add and edit dialog | Category Management add action or category edit action | `dialog_add_category.xml`, `spinner_item_small.xml`, `spinner_dropdown_item.xml` | Name is required. Parent candidates exclude the edited category and its descendants. Save reports database rule failures and reloads the tree after success. | Current behavior |
| Category icon picker | Category add and edit icon action | `dialog_icon_picker.xml`, `IconPickerAdapter`, `item_icon.xml` | Selecting an icon updates the parent dialog's preview. | Current behavior |
| Record delete confirmation | Home and Ledger Details | Programmatic `AlertDialog` | Confirm calls current `DatabaseHelper.deleteRecord` behavior, then reloads the owner list. Cancel dismisses. | Current behavior |
| Asset row delete confirmations | Assets active-list row and Archived Assets row | Programmatic `AlertDialog` | Confirm performs the current permanent deletion path, then reloads the owner list. This does not apply to the reachable Edit Asset form. | Current behavior |
| Archived Asset edit dialog | Archived Assets row edit action | `dialog_asset.xml` | Existing archive-only edit treatment. Required name and numeric amount validate in the dialog. | Current behavior |
| Category delete confirmation | Category Management row action | Programmatic `AlertDialog` | Confirm attempts deletion and exposes protected-tree or in-use feedback when rejected. | Current behavior |

## State Matrix

| State or rule | Applies to | Label | Current meaning |
| --- | --- | --- | --- |
| Populated list | Home, Ledger Details, Assets, Asset Records, Archived Assets, Category Management, Agent sessions and messages | Current behavior | The listed adapter is bound or refreshed from SQLite-backed data. |
| Empty list | Home, Ledger Statistics, Ledger Details, Assets, Asset Records, Archived Assets, Category Management | Current behavior | The relevant `text_empty` is shown and the RecyclerView is hidden. |
| Validation feedback | Add/Edit Record, Add/Edit Asset, AI session name, category add/edit, category depth | Current behavior | Current UI uses Toasts, input error, or a retained dialog when input is invalid. |
| Destructive confirmation | Records, active and archived asset rows, categories | Current behavior | These deletion entry points use confirmation dialogs. The reachable Edit Asset form is the exception: it deletes immediately, shows a Toast, and pops. Current record deletion retains the database's balance rollback behavior. |
| Loading | Agent streaming only | Current behavior | Sending and receiving status plus progress are implemented. Synchronous SQLite screens do not provide a loading state. |
| AI error and partial reply | Agent | Current behavior | Configuration-required, network, timeout, cancellation, interruption, and other classified failures are handled. Received partial assistant content is retained. |
| Disabled interaction | Agent while sending or streaming | Current behavior | Composer, session drawer, session creation, and session actions are disabled as defined by `updateSendingState`. |
| Conditional shell destination | Assets and Agent | Current behavior | Assets follows the asset visibility preference. Agent follows the AI-entry preference. Neither predicate currently requires complete MiniMax configuration. |
| Home Preferences | Me secondary route | Approved new behavior | Todo 15 approves this preference surface. It has no current Fragment, layout, or live caller. |
| Ledger startup preference | Me secondary preference | Approved new behavior | Todo 3 and Todo 15 approve startup choice behavior. It has no current route or persisted runtime implementation. |
| Home overview sources and AI overview | Home and Home Preferences | Approved new behavior | Todo 3 and Todo 10 specify future local, greeting, custom, and explicit AI overview behavior. No current runtime route or request is claimed here. |
| Skeleton, Snackbar Undo, custom keypad, saved-state restoration | Future redesign families | Design-only primitive | These are plan or design-system concerns. No current source route proves them as active UX. |

## Behavior-Preservation Ledger

| Invariant | Current evidence and required preservation boundary |
| --- | --- |
| Local-first financial data | `DatabaseHelper` backs records, assets, categories, Agent sessions, and Agent messages with local SQLite. The app must not require cloud storage for bookkeeping. |
| Leaf-only record categories | Add Record and Edit Record only confirm a category when it has no children. Existing record category ID and path snapshot behavior must remain intact. |
| Flat statistics | The business decision requires exact category aggregation. Child categories must not roll into their parent in Ledger statistics. |
| Quick Add return | Cold-start Add Record returns to Home because it has no back-stack entry. A normal Add Record entry returns to the invoking surface. |
| Persistent AI chat | Agent restores the saved active session, persists sessions and messages in SQLite, and keeps the current session across page changes or app restart. |
| Partial AI replies | When content has arrived before cancellation, timeout, interruption, or another terminal failure, the partial assistant content is retained as an error-state message rather than discarded. |
| Current record deletion accounting | The binding business rule says deleting an expense returns its amount to the associated asset balance, without a second confirmation. This inventory does not introduce undo behavior. |
| Name-based asset association | Asset-record lookup uses stored asset name through `assetSource`. It is current behavior and must not be described as immutable asset-ID linkage. |

## Explicit Exclusions

| Excluded artifact | Source present | Live-caller finding | Runtime-authority decision |
| --- | --- | --- | --- |
| `SearchFragment` and `fragment_search.xml` | Yes | No caller or shell destination was proven. The only reference is the fragment's own factory and implementation. | Dormant. Excluded from active UX. |
| `CategoriesFragment` and `fragment_categories.xml` | Yes | No caller or shell destination was proven. | Dormant. Excluded from active UX. Category Management is the reachable category route. |
| `bottom_sheet_ledger_mode.xml` | Yes | No live layout reference was proven. `StatisticsFragment` uses a PopupMenu for mode selection and `bottom_sheet_ledger_period.xml` for period selection. | Stale. Excluded from active UX. |
| Legacy add and edit asset dialogs in `AssetFragment` | Yes, as private helper methods using `dialog_asset.xml` | Assets binds its FAB and edit actions to the reachable Add Asset and Edit Asset Fragments. No caller was proven for these private dialog helpers. | Unreachable helper paths. Excluded from active UX. |
| Historical Stitch exports | Removed | They were design references, not Android runtime callers. | Excluded from runtime and current design work. |
