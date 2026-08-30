# Todo 6 Theme Migration Evidence

## Scope and dirty-worktree probe

- Pre-existing unrelated changes observed before implementation: `AGENTS.md`, `.omo/`, `DESIGN.md`, `docs/design/current-ux-inventory.md`, and the two `2026-08-12` decision/contract documents.
- Those files were not edited by this task except this task-owned evidence directory.
- No ADB, connected test, APK installation, commit, or preference-file `clear()` was used in production code.

## TDD chronology

1. Baseline characterization added for the old stored `0/1/2` to Light/Dark/System style mapping.
2. Command: `.\gradlew.bat testDebugUnitTest --tests "com.example.cardtally.util.ThemeHelperCharacterizationTest" --console=plain`
3. Result: `BUILD SUCCESSFUL in 38s`; baseline passed before production edits.
4. New normalization test added for stored `0/1/2/999`.
5. Same targeted command result under old code: `BUILD FAILED in 2s`, with `Unresolved reference 'normalizeTheme'`. This was the expected red failure.
6. After implementation, command: `.\gradlew.bat testDebugUnitTest --tests "com.example.cardtally.util.ThemeHelperCharacterizationTest" --tests "com.example.cardtally.util.ThemeResourceParityTest" --console=plain`
7. Result: `BUILD SUCCESSFUL in 32s`; normalization and representative day/night parity tests passed.
8. A follow-up JVM regression was added for String and Boolean `theme_mode` values. Before production hardening, its targeted run failed as intended with `Unresolved reference 'normalizeStoredTheme'`.
9. The subsequent production fix and gradient cleanup are code-edit-only changes and have not been executed. Fresh targeted JVM tests and `assembleDebug` remain required later.

## Behavior and preservation proof

- `ThemeHelper.normalizeTheme()` maps `0`, stale `1`, stale `2`, and malformed `999` to Light.
- `ThemeHelper.getTheme()` repairs only `theme_mode` when needed.
- `ThemeHelper.getTheme()` now reads the raw SharedPreferences boundary value, so stale String and Boolean values normalize without `ClassCastException`; no broad exception handler was added.
- `ThemeHelper.saveTheme()` persists only normalized Light.
- Android characterization covers integer, String, and Boolean stored values and asserts an unrelated sentinel in `theme_prefs` remains unchanged. The newest Android regression has not been run.
- `MainActivity` repairs the stored value and applies `Theme.CardTally.Light` before `super.onCreate`.

## Dead-route and resource audits

Repository reference searches after removal returned no matches for:

- `ThemeSettingsFragment`, `fragment_theme_settings`, `card_theme`, or `text_current_theme`.
- Theme-choice strings, arrays, toasts, and accessibility labels.
- `THEME_DARK`, `THEME_SYSTEM`, `Theme.CardTally.Dark`, or `Theme.CardTally.System`.
- App resource references ending in `_dark` or `_system`.
- The ten `gradient_*_dark_{start,end}` definitions removed from `colors_gradients.xml`; repository-wide static search found no references outside their definitions.

`app/src/main/res/values-night/*.xml` returns no files. The JVM resource-parity test verifies representative `background_light`, `onBackground_light`, `surface_light`, and `surface_container_low` resources exist in base values and have no app-owned night override.

## Build and resource dump

- Command: `.\gradlew.bat assembleDebug --console=plain`
- Earlier result, before the String/Boolean hardening and gradient cleanup: `BUILD SUCCESSFUL in 4s`; APK generated at `app/build/outputs/apk/debug/CardTally-debug.apk`.
- `aapt dump resources` shows each representative color with `flags=0x00000000` and one app value, including `background_light=#fffcf7`, `onBackground_light=#383831`, `surface_light=#fffcf7`, and `surface_container_low=#fcf9f3`. No app `values-night` configuration remains to select an alternate value.
- This prior build is not passing evidence for the newest code-only edits. A fresh `assembleDebug` remains required.

## AndroidTest compilation and execution status

- Command: `.\gradlew.bat compileDebugAndroidTestKotlin --console=plain`
- Current independent verification reports this compilation command succeeds; the earlier TLS-blocker claim is obsolete.
- Successful AndroidTest compilation is not instrumentation execution evidence. No instrumentation or connected-device tests were run or claimed passing.
- The String/Boolean Android regression added in the latest code-only follow-up is unexecuted and requires later verification.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` is not installed; no installation was attempted. Previous JVM/build gates predate the newest code-only edits.

## Cleanup and review

- No Theme Settings route or UI survives.
- Obsolete dark/system style definitions, dark/system colors, app-owned night aliases, and proven-dead dark gradient definitions were removed only after reference checks.
- Static re-read confirms the latest changes are localized to the ThemeHelper preference boundary, its JVM/Android regressions, dead gradient definitions, and this evidence file. `SettingsFragment` remains in the 200-250 warning band but this task only removed theme responsibility and lines from it.
- No secrets, personal financial data, device identifiers, or generated build artifacts are included in evidence.
- Latest verification status: changed claim only. The wrong-type production fix, Android regression, and gradient cleanup have not passed a post-edit test or build under the code-edit-only constraint.
