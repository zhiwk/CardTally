# Todo 7 Implementation Status

Status: **code-edited / unverified**

No Gradle task, Android test, build, ADB command, installation, or connected QA was run. This evidence records source edits only and must not be treated as runtime proof.

## Implemented source claims

- Added typed Home summary-layout and overview-source enums plus isolated `home_ux_prefs` storage for custom text and AI cache sentence/fingerprint/timestamp.
- Added typed Ledger startup and last-view enums plus isolated `ledger_ux_prefs` storage and deterministic startup resolution.
- Both helpers migrate to schema 1 in one editor transaction, repair only malformed managed keys, preserve unrelated preferences, and leave future-schema storage untouched while returning read-time fallbacks.
- Added explicit `state_*` Bundle codecs for Add/Edit Record, Add/Edit Asset, Ledger, Home Preferences state ownership, and Agent composer/session/drawer/scroll state. No API key, model, request URL, message history, database helper, adapter, view, context, or request object is serialized.
- Add/Edit Record restoration resolves asset/category selections by stable ID. Missing IDs clear the selection; restored categories must still be leaves of the restored record type. Open date/asset/category sheets and pending category ID restore explicitly.
- Edit Record and Edit Asset return to the caller when the required persisted entity no longer exists. Draft values override loaded entity fields after a valid ID reload.
- Ledger now defaults through the typed startup preference, saves last view only after a user view change, and restores view/type/period/custom range/chart/list positions/filter surface.
- Agent restores only an existing SQLite session ID, then falls back to the existing active-session preference/default-session path. Drawer restoration requires enabled, complete configuration. Composer text and both list positions restore without duplicating messages into the Bundle.
- Agent in-flight work is not serialized. User cancellation, forced stop, configuration loss, navigation/view destruction, and recreation cancel the transport and reset transient sending/loading state.
- No Home Preferences UI or Home AI request exists in the current source, so no UI/request path was invented. Typed state/cache foundations are present for the later owning todos.

## Regression sources added before production edits

- `app/src/androidTest/java/com/example/cardtally/util/HomeUxPreferencesTest.kt`
- `app/src/androidTest/java/com/example/cardtally/util/LedgerUxPreferencesTest.kt`
- `app/src/androidTest/java/com/example/cardtally/state/ScreenStateBundleTest.kt`

The sources cover legal enum values, missing defaults, blank/unknown/wrong-typed values, lower/wrong/higher schema markers, unrelated sentinels, text/cache boundaries, every saved-state matrix row, malformed ranges/enums, missing stable IDs, process-style Bundle copying, secret-key absence, and AI cancellation/reset. They were intentionally not executed under the user constraint.

## ChangedClaim

Task-owned changed production files:

- `app/src/main/java/com/example/cardtally/util/HomeUxPreferences.kt`
- `app/src/main/java/com/example/cardtally/util/LedgerUxPreferences.kt`
- `app/src/main/java/com/example/cardtally/state/StateSupport.kt`
- `app/src/main/java/com/example/cardtally/state/RecordFormState.kt`
- `app/src/main/java/com/example/cardtally/state/AssetFormState.kt`
- `app/src/main/java/com/example/cardtally/state/LedgerScreenState.kt`
- `app/src/main/java/com/example/cardtally/state/HomePreferencesScreenState.kt`
- `app/src/main/java/com/example/cardtally/state/AgentScreenState.kt`
- `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`
- `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`
- `app/src/main/java/com/example/cardtally/AddAssetFragment.kt`
- `app/src/main/java/com/example/cardtally/EditAssetFragment.kt`
- `app/src/main/java/com/example/cardtally/StatisticsFragment.kt`
- `app/src/main/java/com/example/cardtally/AgentFragment.kt`

Task-owned changed test/evidence files are the three regression sources listed above and this file. Existing Todo 6/design/plan changes in the dirty worktree were not modified or claimed by this task.

Outstanding commands, not run:

1. `./gradlew.bat testDebugUnitTest`
2. `./gradlew.bat connectedDebugAndroidTest`
3. `./gradlew.bat assembleDebug`

These must run serially in the repository-prescribed order chosen for the eventual verification pass. Connected QA remains user-waived/not-run for this task. The plan checkbox was not changed and no commit was created.

## Static inspection

- Re-read all task-owned changed files.
- `git diff --check` completed without whitespace errors; Git emitted only existing line-ending conversion warnings.
- Kotlin LSP was configured but not installed, so no LSP diagnostic claim is made and no server was installed.
- No secret/configuration values appear in this evidence.
