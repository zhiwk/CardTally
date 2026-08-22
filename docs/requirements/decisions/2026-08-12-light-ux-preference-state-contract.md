# Light UX Preference and State Contract

> Effective date: 2026-08-12
> Status: approved implementation contract for Todo 3 of `cardtally-light-ux-redesign`
> Scope: new Home and Ledger preferences, amount keypad behavior, recreation state, migration, and privacy handling

## 1. Authority and storage boundaries

This document defines the behavior that Todos 5 through 15 must implement. New Home and Ledger values use their own `SharedPreferences` files. They do not share a file with existing theme, language, quick-add, category-depth, or AI configuration preferences.

The helper API owns sanitization. Every getter returns a legal value, even when stored data is missing, has the wrong type, is blank, or contains an unknown enum token. Repairing one managed key may update that key and its schema-version key only. It must not clear, rewrite, or remove an unrelated preference in the same file or another file.

## 2. Home preferences

Preference file: `home_ux_prefs`

| Key | Type | Legal stored values | Default | Invalid or wrong-type fallback | Persistence and migration |
| --- | --- | --- | --- | --- | --- |
| `home_ux_schema_version` | `Int` | `1` | `1` | Treat as `0`, run migration, then write `1` | New file starts at `1`. A value below `1` migrates missing managed keys. A value above `1` leaves all values intact and uses read-time fallbacks. |
| `home_summary_layout` | `String` | `one_primary_two_secondary`, `two_by_two`, `primary_with_three_secondary`, `compact_list` | `one_primary_two_secondary` | Return and persist `one_primary_two_secondary` | Missing key becomes the default during migration. |
| `home_overview_source` | `String` | `local_rule`, `greeting`, `custom`, `ai` | `local_rule` | Return and persist `local_rule` | Missing key becomes the default during migration. |
| `home_overview_custom_sentence` | `String` | Any trimmed string of 0 through 280 Unicode code points | Empty string | Return and persist empty string for a wrong type or text longer than 280 code points. Preserve valid empty text. | No legacy source exists. It is not read unless source is `custom`. |
| `home_overview_ai_sentence` | `String` | Any trimmed nonempty string of 1 through 280 Unicode code points | Empty string | Return and persist empty string for a wrong type, blank value, or text longer than 280 code points. | No legacy source exists. |
| `home_overview_ai_fingerprint` | `String` | A lowercase, 64-character SHA-256 hexadecimal digest | Empty string | Return and persist empty string | No legacy source exists. |
| `home_overview_ai_generated_at_epoch_ms` | `Long` | `0` through `Long.MAX_VALUE` | `0` | Return and persist `0` | No legacy source exists. |

`one_primary_two_secondary` is the mandatory first-run Home summary layout. The other three layout identifiers are the complete supported set. A UI must not invent a fifth layout identifier.

Overview source behavior is exact:

1. `local_rule` renders the deterministic local sentence from the current local aggregates. It makes no network request.
2. `greeting` renders the localized greeting. It makes no network request.
3. `custom` renders `home_overview_custom_sentence`. Empty custom text renders the localized empty-custom prompt and makes no network request.
4. `ai` renders the valid cached AI sentence only when its fingerprint equals the current aggregate-input fingerprint. A missing or mismatched cache renders the localized stale or unavailable state. Only an explicit Generate or Refresh action may start an AI overview request.

The fingerprint is the SHA-256 digest of a canonical UTF-8 representation of the approved aggregate overview input, in this exact field order: selected period label, aggregate income, aggregate expense, aggregate balance, then up to five flat leaf-category name and amount pairs sorted by descending absolute amount and then ascending category name. It contains no API key, record identifier, raw record, note, asset name, category path, chat content, or session identifier. A new valid AI response writes the sentence, fingerprint, and generation timestamp together. If a response belongs to an older fingerprint, it is discarded and does not replace the current cache.

## 3. Ledger preferences

Preference file: `ledger_ux_prefs`

| Key | Type | Legal stored values | Default | Invalid or wrong-type fallback | Persistence and migration |
| --- | --- | --- | --- | --- | --- |
| `ledger_ux_schema_version` | `Int` | `1` | `1` | Treat as `0`, run migration, then write `1` | New file starts at `1`. A value below `1` migrates missing managed keys. A value above `1` leaves all values intact and uses read-time fallbacks. |
| `ledger_startup_behavior` | `String` | `details`, `statistics`, `remember_last` | `details` | Return and persist `details` | Missing key becomes the default during migration. |
| `ledger_last_view` | `String` | `details`, `statistics_expense`, `statistics_income` | `details` | Return and persist `details` | Write after a user changes the Ledger view. Do not write merely because a startup default rendered. |

Ledger startup resolution is deterministic:

1. `details` opens `details`.
2. `statistics` opens `statistics_expense`.
3. `remember_last` opens the sanitized `ledger_last_view`.

The current Ledger view is not a replacement for transient filters. Period preset, custom range, statistics type, and chart mode belong to saved instance state. They are not new long-term preferences.

## 4. Migration and malformed-value rules

New helpers run migration on their first read or write. Migration uses one editor transaction per preference file. It adds missing managed defaults and writes the current schema version. It never calls `clear()`.

For every managed enum key, a missing key, blank token, unknown token, or type mismatch resolves to the table default and repairs only that key. For every managed text and numeric key, invalid data resolves to the table fallback and repairs only that key. A future schema version does not overwrite stored values solely because its version number is newer. The getter still returns a safe fallback for data it cannot interpret.

Existing preference files keep their current ownership:

| Existing file | Existing owner | Contract impact |
| --- | --- | --- |
| `theme_prefs` | `ThemeHelper` | This contract does not migrate theme values. Todo 6 owns the Light-only migration. |
| `language_prefs` | `LanguageHelper` | Locale changes recreate the activity through `AppCompatDelegate`; new state follows Section 6. |
| `quick_add_prefs` | `QuickAddHelper` | No key or default changes. |
| `category_hierarchy_settings_prefs` | `CategoryHierarchySettingsHelper` | No key or default changes. |
| `ai_assistant_settings_prefs` | `AiAssistantSettingsHelper` | Existing API configuration and active-session keys remain in this file. Home overview cache does not enter it. |

## 5. Custom amount keypad contract

The shared Add Record and Edit Record amount control has one canonical text buffer. Both the custom keypad and hardware numeric input pass through the same reducer, so they produce the same accepted text and rejection behavior.

| Rule | Exact behavior |
| --- | --- |
| Accepted characters | ASCII digits `0` through `9` and the decimal separator `.` only. |
| Intermediate grammar | Empty text, `0`, a nonzero digit followed by digits, either integer form followed by `.`, or either integer form followed by `.` and one or two digits. |
| Committed grammar | `0` or a nonzero digit followed by zero or more digits, optionally followed by `.` and one or two digits. |
| Decimal separator | `.` is the only stored and rendered separator, regardless of locale. Pressing it on an empty buffer creates `0.`. A second separator is ignored. |
| Fractional digits | At most two digits after `.`. A third fractional digit is ignored. |
| Leading zeros | Before the separator, collapse every leading zero sequence to one `0`. `0007` becomes `7`, `000.5` becomes `0.5`, and `00` becomes `0`. |
| Sign and exponent | `+`, `-`, `e`, `E`, whitespace, grouping separators, pasted punctuation, and non-ASCII digits are rejected. The buffer stays unchanged. |
| Numeric range | The parsed value must be finite and in the inclusive range `0.00` through `Double.MAX_VALUE`, the current SQLite `REAL` and Kotlin `Double` safe amount policy. An input that parses to infinity or exceeds this maximum is rejected. |
| Save validation | The buffer is parsed only after it satisfies the committed grammar. Add and Edit Record reject `0.00` at save time, preserving the current nonzero-record rule. |
| Backspace | Remove exactly the final character. If that leaves an empty buffer, keep it empty until the next input or long-press clear. |
| Long-press clear | Long-pressing Backspace replaces the buffer with `0`. It does not submit, save, dismiss, or change the record type. |
| Type switch | Switching expense and income preserves the canonical amount buffer exactly. It changes only record type and type-specific category selection. |
| Hardware parity | Hardware keyboard, accessibility keyboard actions, and paste input use the same reducer. Unsupported input is rejected without changing the buffer. |
| Soft keyboard | The amount field sets `showSoftInputOnFocus` to false. The system soft keyboard must not appear for the amount field. Hardware input remains enabled. |
| Touch target | Each digit, decimal, Backspace, long-press Backspace action, and expense or income switch has a visible or expanded touch target of at least 48dp by 48dp. |
| Accessibility | Each digit announces `Digit N`, decimal announces `Decimal point`, Backspace announces `Delete last digit`, and its long-click action announces `Clear amount`. The type switch exposes role, selected state, and the resulting record type. Rejected input and save validation errors are announced through the normal accessibility error channel. |

The initial rendered amount remains `0.00` for compatibility with the current form. Before the first custom keypad or hardware edit, it is treated as a replaceable default. The reducer's canonical cleared value is `0`, and formatting may render it as `0.00` only when the field is not actively being edited.

## 6. Recreation and process-death matrix

All listed Bundle values use explicit `state_*` keys. A Fragment restores these values before rendering dependent controls. Database entities are reloaded by stable ID after restoration. If an ID no longer resolves, the form remains open with its saved text and selection is cleared rather than selecting a different entity by name.

| Surface | `state_*` fields preserved for activity or locale recreation | Preserved for process death | Restore rule |
| --- | --- | --- | --- |
| Add Record | `amount_buffer`, `record_type`, `selected_date`, `selected_asset_id`, `selected_category_id`, `description`, `open_sheet`, `pending_category_id` | Yes | `record_type` accepts `expense` or `income`, else `expense`. `open_sheet` accepts `none`, `date`, `asset`, or `category`, else `none`. Category must still be a leaf of the restored type or is cleared. |
| Edit Record | `record_id`, `amount_buffer`, `record_type`, `selected_date`, `selected_asset_id`, `selected_category_id`, `description`, `open_sheet`, `pending_category_id` | Yes | `record_id` is required to load the persisted record. If it is missing or deleted, return to the caller. The remaining field rules match Add Record. |
| Add Asset | `asset_name`, `asset_amount_buffer`, `asset_type` | Yes | `asset_type` accepts `cash`, `bank`, `alipay`, or `wechat`, else `cash`. Text stays draft text and is not written to SQLite until Save. |
| Edit Asset | `asset_id`, `asset_name`, `asset_amount_buffer`, `asset_type` | Yes | `asset_id` is required to load the persisted asset. If it is missing or deleted, return to the caller. Draft values override loaded field values. |
| Ledger | `view`, `statistics_type`, `period_preset`, `custom_start_date`, `custom_end_date`, `chart_mode`, `details_scroll_position`, `details_scroll_offset`, `statistics_scroll_position`, `statistics_scroll_offset`, `open_filter_surface` | Yes | `view` accepts `details`, `statistics_expense`, or `statistics_income`, else the sanitized startup resolution. `period_preset` accepts `week`, `month`, `year`, `all`, or `custom`, else `month`. A custom range with either missing date or start after end falls back to `all`. `chart_mode` accepts `pie` or `line`, else `pie`. |
| Home Preferences | `summary_layout`, `overview_source`, `custom_sentence`, `ai_sentence`, `ai_fingerprint`, `ai_generated_at_epoch_ms`, `preview_layout`, `editing_custom_text` | Yes | Values are sanitized with Section 2 rules. The cached AI fields are also read from preferences, and the newer valid persistent tuple wins over an older Bundle tuple. |
| Agent | `active_session_id`, `composer_text`, `is_session_drawer_open`, `message_scroll_position`, `message_scroll_offset`, `session_scroll_position`, `session_scroll_offset` | Yes | The active session is restored only if it exists in SQLite. Otherwise use the existing sanitized active-session preference, then create or select the normal default session. Drawer state restores only when Agent remains visible and configured. |

`open_sheet` identifies only a currently presented date, asset, or category sheet. It never stores a `Dialog`, `View`, `Context`, adapter, or database helper. Pending category selection is saved separately so an open category sheet restores its unconfirmed selection. Date and asset sheets have no additional pending selection beyond the saved field values.

Home overview and Agent requests are in-flight work, not saved state. On view destruction, activity recreation, locale recreation, process death, navigation away, disabled Agent, missing Agent configuration, or user cancellation, cancel the request. Do not restore, replay, or automatically retry it. Clear the transient loading state after recreation. A completed valid cached Home sentence and persisted Agent messages may render after restoration, subject to their normal storage rules.

## 7. Privacy and secret handling

| Data | Sensitivity | Storage and handling rule |
| --- | --- | --- |
| MiniMax API key | Secret | Remains only in `ai_assistant_settings_prefs` under the existing helper. Never place it in a Bundle, ViewModel saved state, navigation argument, exception message, analytics payload, log, test fixture, screenshot evidence, or Home overview cache. |
| AI model and full request URL | Sensitive configuration | Remain in the existing AI settings helper. Never place them in a Bundle, log, Home overview cache, or evidence. |
| Home custom sentence | Sensitive financial context | Store only in `home_ux_prefs`. Do not log it or send it to MiniMax. |
| Cached AI overview sentence and fingerprint | Sensitive financial context and derived metadata | Store only in `home_ux_prefs`. Do not log them. Send only the approved aggregate payload when the user explicitly requests generation. |
| AI overview timestamp | Low sensitivity metadata | Store only with the cache tuple. Do not log it with financial content. |
| Add/Edit drafts and Agent composer text | Sensitive user-entered text | Keep only in saved instance state for restoration. Do not log, include in crash annotations, or copy into preferences. |
| Persisted Agent messages and sessions | Sensitive local conversation | Continue using existing SQLite persistence. Do not duplicate them into saved state or Home overview cache. |

The Home overview request may send only the fields in Section 2's canonical fingerprint input. It must never send individual records, descriptions, notes, asset names, asset balances, category paths, identifiers, chat history, API keys, model configuration, or any draft state. No AI overview path may create, update, or delete a financial record, asset, category, session, or message.

## 8. Required tests for the implementing todo

Todo 7 must add table-driven tests for every preference key, legal value, missing value, blank enum, unknown enum, wrong type, lower schema version, higher schema version, and unrelated sentinel preference. It must add Bundle restoration tests for every matrix row, interrupted locale recreation, process-style Bundle restoration, missing referenced entity, and cancellation of each in-flight AI request. Keypad tests must cover every grammar row and prove keypad and hardware input yield the same result.
