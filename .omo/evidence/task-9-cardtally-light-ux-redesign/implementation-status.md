# Todo 9 Implementation Status

Status: **code-edited / unverified**

No Gradle task, Android test, build, ADB command, installation, or connected QA was run. This evidence records source edits and static inspection only and must not be treated as runtime proof.

## Implemented source claims

- Database version 11 adds a persisted `record_deletion_undo` table. Each immutable token row stores its expiry, the applied asset-balance delta, and every record column, including ID, sort order, nullable category ID, category name/path snapshots, description, and asset name.
- `deleteRecord` captures the row, applies the expense refund or income reversal, inserts the token, and deletes the record inside one SQLite transaction.
- `undoRecordDeletion` reads and consumes the token inside one SQLite transaction. A valid token restores the exact row ID/columns and reverses the captured balance delta; a missing, repeated, or expired token returns `false` without restoring data.
- Expiration is checked when Undo is requested, with opportunistic cleanup during later record deletions. No scheduler or background job was added.
- Existing Home, Ledger (`StatisticsFragment`), Search, and Asset Records deletion handlers continue through `DatabaseHelper.deleteRecord`, so their current interactions use the transactional foundation without adding the later swipe/Snackbar UI.
- Active asset destructive actions now archive. Permanent deletion has a separate archived-only database API and rejects assets referenced by live records or unexpired record-deletion tokens under the accepted name-based association contract.
- Existing category child/in-use rejection remains in place. Unexpired deletion tokens also keep their category IDs protected until Undo is consumed or expires.
- No asset-ID migration, generic cross-entity Undo API, broad swipe UI, or record Snackbar was introduced.

## Regression source added before production edits

- `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecordDeletionTest.kt`

The instrumentation source covers exact expense deletion/Undo with repeated Undo, income balance semantics, expired Undo, category child/in-use protection, active asset archive, archived asset rejection for live records and pending Undo, and permanent deletion of an unreferenced archived asset. It was intentionally not executed under the user code-only constraint.

## ChangedClaim

Task-owned production files:

- `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`
- `app/src/main/java/com/example/cardtally/AssetFragment.kt`
- `app/src/main/java/com/example/cardtally/EditAssetFragment.kt`
- `app/src/main/java/com/example/cardtally/ArchivedAssetsFragment.kt`
- `app/src/main/res/values/strings.xml` (`asset_delete_cta` only)
- `app/src/main/res/values-en/strings.xml` (`asset_delete_cta` only)

Task-owned test and evidence files:

- `app/src/androidTest/java/com/example/cardtally/database/DatabaseHelperRecordDeletionTest.kt`
- `.omo/evidence/task-9-cardtally-light-ux-redesign/implementation-status.md`

Post-Todo-8 database paging and Ledger aggregation changes were preserved. Other dirty-worktree changes are not claimed. The Todo 9 plan checkbox remains unchecked and no commit was created.

## Outstanding verification

Run later, serially:

1. `.\gradlew.bat connectedDebugAndroidTest`
2. `.\gradlew.bat assembleDebug`

After runtime verification, manually confirm current record delete entry points refresh correctly, active assets disappear into Archived Assets, protected archived assets show the safeguard message, and a later screen-family implementation can present/consume the returned token exactly once.

## Static inspection limitations

- `git diff --check` completed without whitespace errors; Git emitted only existing line-ending conversion warnings.
- Kotlin LSP diagnostics are unavailable because `kotlin-ls` is not installed and installation was previously declined; no diagnostic-clean claim is made.
- No API key, personal financial data, device identifier, build output, or installation output is included here.
