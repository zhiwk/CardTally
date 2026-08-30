# Task 3 Preference and State Contract Audit

## Scope

Audited the approved contract at `docs/requirements/decisions/2026-08-12-light-ux-preference-state-contract.md` against Todo 3 in `.omo/plans/cardtally-light-ux-redesign.md`, the current preference helpers, Fragment behavior, and helper tests.

## Table-driven audit

| Contract requirement | Contract location | Result |
| --- | --- | --- |
| New requirements decision exists in the current decision location | Document title and Sections 1 through 8 | Pass |
| Home summary layout enum has four values and defaults to `one_primary_two_secondary` | Section 2, `home_summary_layout` | Pass |
| Overview source supports `local_rule`, greeting, custom, and AI, with `local_rule` default | Section 2, `home_overview_source` and behavior list | Pass |
| Custom sentence and AI sentence, fingerprint, and timestamp have named keys and validation | Section 2 | Pass |
| Ledger startup supports details, statistics, and remember-last, with details default | Section 3 | Pass |
| Current last Ledger view has named legal values and deterministic startup resolution | Section 3 | Pass |
| Each key defines file, type, legal values, default, malformed fallback, and migration behavior | Sections 2 through 4 | Pass |
| Malformed values repair only their managed key and preserve unrelated preferences | Sections 1 and 4 | Pass |
| Keypad defines digits, decimal, two fractional digits, leading-zero normalization, finite maximum, backspace, clear, type switch, and hardware parity | Section 5 | Pass |
| Keypad targets are at least 48dp and amount soft keyboard is suppressed | Section 5 | Pass |
| Add/Edit Record, Add/Edit Asset, Ledger, Home Preferences, and Agent name their recreation fields | Section 6 matrix | Pass |
| Open sheets, Agent drawer, scroll positions, and pending category selection have restore rules | Section 6 matrix and following paragraph | Pass |
| In-flight Home overview and Agent requests cancel and do not restore | Section 6 | Pass |
| API keys are excluded from saved state and logs | Section 7 | Pass |
| Privacy sensitivity and handling are stated for configuration, drafts, overview cache, and Agent data | Section 7 table | Pass |
| Contract language has no unresolved owner decision or conditional implementation wording | Sections 1 through 8 | Pass |
| Contract avoids vague conditional implementation language | Full-document literal audit | Pass |

## Source alignment notes

| Existing source fact | Contract treatment |
| --- | --- |
| Current helpers use separate named `SharedPreferences` files and read-time sanitization patterns | Sections 1 and 4 extend that pattern without changing existing helper ownership. |
| `LanguageHelper` changes locale through `AppCompatDelegate` | Section 6 explicitly treats locale recreation as state-preserving recreation. |
| Existing Add/Edit Record forms keep amount, type, date, asset, category, and description in Fragment memory | Section 6 names each field for Bundle preservation. |
| Existing Agent stores sessions and messages in SQLite and its active session ID in AI preferences | Section 6 restores session identity without duplicating messages, and Section 7 excludes secrets from state. |
| Existing record save validation rejects zero amounts and currently uses Kotlin `Double` with SQLite `REAL` storage | Section 5 retains the nonzero save rule and defines finite `Double.MAX_VALUE` input bounds. |

## Verification performed

Read the task plan, current decision document, current helper source returned by codegraph, current Fragment state patterns returned by codegraph, and current helper tests. No Gradle task ran, as required for this Wave 1 documentation task.
