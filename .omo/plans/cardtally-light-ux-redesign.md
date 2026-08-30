# cardtally-light-ux-redesign - Work Plan

## TL;DR (For humans)

**What you'll get:** CardTally will be redesigned and implemented as one light-only Android experience built around simple black, white, and gray outlined cards, with pale low-saturation color used only for meaning. Every active page and state—including the new Home Preferences and user-triggered AI overview—will share one coherent component system.

**Why this approach:** The work first locks accounting, privacy, deletion, and navigation behavior, then proves the reusable card system before generating and implementing whole screen families. This prevents attractive mockups from silently breaking local-first bookkeeping or producing inconsistent one-off pages.

**What it will NOT do:** It will not preserve dark or follow-system themes, the former “静奢理财日记” identity, or stale Stitch layouts. It will not introduce Compose, Room, cloud-backed bookkeeping, automatic AI bookkeeping, budgets, reminders, exports, or parent-category selection.

**Effort:** XL
**Risk:** High - this is a repository-wide visual redesign plus theme migration, new preferences, a custom keypad, reversible deletion, and a privacy-sensitive MiniMax summary flow.
**Decisions to sanity-check:** Light is the only runtime theme; card styling uses two density profiles; Agent appears only when enabled and fully configured; AI overview is explicitly user-triggered and sends aggregates only; asset-record identity remains name-based accepted debt; records use swipe-delete with exact undo while protected entities retain their rules.

Your next move: execute this reviewed plan in a worker session with `$start-work cardtally-light-ux-redesign`.

---

> TL;DR (machine): XL/high-risk end-to-end Stitch + Android light-only card UX redesign with theme migration, new preference/AI/undo behavior, full automated and visual QA.

## Scope

### Must have

- Create a dated superseding product/design decision, then a canonical root `DESIGN.md` for one light-only black/white/gray card system; pale red/cyan and other tints are semantic only.
- Preserve Kotlin, Fragment, XML, Material Components, hand-written SQLite, local-first financial storage, flat category statistics, leaf-only record binding, quick-add return behavior, persistent AI chat, and partial AI response retention.
- Inventory and redesign every active surface: activity shell; Home; Ledger Statistics/Details; shared Add/Edit Record; record date/asset/category sheets; Assets; shared Add/Edit Asset; asset records; archived assets; Agent/chat/session drawer/session dialogs; Me; Home Preferences; AI configuration; category management and active dialogs/menus.
- Support an overview density profile (Home, Assets summary, Me) and a dense workflow profile (Ledger details, category trees/pickers, Agent sessions) within the same tokens.
- Remove Dark and Follow System UI/runtime branches; normalize stored theme values to Light without clearing unrelated preferences; make system night mode visually inert.
- Add persisted Home summary-layout and overview-source preferences, persisted Ledger startup behavior, user-authored overview copy, and a user-triggered MiniMax overview.
- AI overview data contract: send only selected period label, aggregate income, aggregate expense, aggregate balance, and up to five flat leaf-category aggregate name/amount pairs; never send individual records, notes/descriptions, asset names/balances, category paths, API keys, chat history, or identifiers. Cache the returned sentence locally with its aggregate-input fingerprint and generation timestamp; do not add it to an Agent session. A changed fingerprint marks it stale; only explicit user action starts a network request.
- Agent destination predicate: visible only when the existing AI-entry preference is enabled **and** API key, model, and valid complete URL are configured. If it becomes unavailable while active, cancel any request and return to Home.
- Home “today” list means calendar-today records only. Fetch in bounded batches of at most 200 so all records remain reachable without violating the binding query limit.
- Deletion contract: records use swipe → immediate transactional delete and asset rollback → Snackbar Undo restoring record and balance exactly once; categories retain child/in-use protection and expose swipe only when deletion is valid; active assets prefer archive, while permanent deletion remains limited to the archived-assets surface and must preserve existing associated-record safeguards. AI-session deletion is not added.
- Keep current name-based asset-record association as explicitly accepted debt; rename behavior remains current behavior and the design must not claim immutable account linkage.
- Update current-facing Chinese/English strings, accessibility resources, README, AGENTS, collaboration docs, and design guides after implementation verification; mark historical plans/design exports as superseded rather than rewriting history.

### Must NOT have (guardrails, anti-slop, scope boundaries)

- No runtime dark or follow-system option, night-dependent visual change, high-saturation palette, decorative gradient, broad glassmorphism, or nested card around every row/label.
- No “静奢理财日记” language in current design contracts or current-facing product claims.
- No Room, Compose, Navigation Component, second architecture, cloud persistence, remote bookkeeping dependency, or new UI framework.
- No AI record/asset/category mutations, automatic AI overview requests, audit-log feature, financial advice claims, or inclusion of raw financial records in AI overview payloads.
- No new budget, reminder, export, dormant Search entry, dormant Categories screen, or stale ledger-mode bottom sheet.
- No parent-category selection, descendant roll-up in category statistics, unbounded record query, or balance mutation outside existing accounting rules.
- No deletion undo that bypasses category protections or asset association safeguards.
- No user-required device commands, APK installation, or `adb devices -l`; agent-controlled verification only, with an explicit environment blocker if no test target exists.

## Verification strategy

> Zero human intervention - all verification is agent-executed.

- Test decision: tests-after, using existing JVM tests, Android instrumentation tests, deterministic Stitch/media inspection, resource/traceability audits, and actual visual QA on an agent-controlled emulator/device.
- Gradle is always serial in one workspace. Run, in order: `./gradlew.bat testDebugUnitTest`, `./gradlew.bat connectedDebugAndroidTest`, `./gradlew.bat assembleDebug`, then PowerShell `Test-Path -LiteralPath "app\build\outputs\apk\debug\CardTally-debug.apk"`.
- Each implementation todo records happy and failure evidence under `<attemptDir>/task-<N>-cardtally-light-ux-redesign/`; outside an ulw loop use `.omo/evidence/`.
- Visual evidence matrix: compact and regular Android widths; Chinese and English; normal and enlarged font; 3/4/5 visible navigation destinations; populated/empty/loading/error/disabled/selected/focus/destructive/keyboard/sheet/drawer states where actually applicable.
- Static design acceptance is by semantic inspection of `DESIGN.md`, screen/state inventory, and Android mapping—not grep counts. Generated Stitch screens are claims until reconciled against active Kotlin/XML paths.
- Runtime skeleton/retry surfaces are implemented only for genuine asynchronous/recoverable operations (AI overview/chat and any explicitly async screen load); synchronous SQLite screens use deterministic empty/error feedback without fake loading delays.

## Execution strategy

### Parallel execution waves

- **Wave 1 — contracts and inventory:** Todos 1–4 may run in parallel, then converge before design generation.
- **Wave 2 — design system and behavior foundations:** Todo 5 starts after contracts; Todos 6–10 can then run in parallel where their file sets do not overlap.
- **Wave 3 — screen-family implementation:** Todos 11–15 run in parallel after the primitive showcase and behavior helpers are stable; shared resource edits must be coordinated through Todo 11’s token contract.
- **Wave 4 — integration, documents, QA:** Todos 16–19 integrate all families, update current-facing documentation, run serial Android gates, and execute visual/review gates.
- **Final wave:** F1–F4 run in parallel after all implementation todos and all must approve.

### Dependency matrix

| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | — | 5, 16, 17 | 2, 3, 4 |
| 2 | — | 5, 11–15 | 1, 3, 4 |
| 3 | — | 5, 6, 11–15 | 1, 2, 4 |
| 4 | — | 5, 16 | 1, 2, 3 |
| 5 | 1–4 | 11–15 | — |
| 6 | 3 | 11, 16 | 7, 8, 9, 10 |
| 7 | 3 | 11, 12 | 6, 8, 9, 10 |
| 8 | 3 | 12, 16 | 6, 7, 9, 10 |
| 9 | 3 | 13, 16 | 6, 7, 8, 10 |
| 10 | 3 | 14, 16 | 6, 7, 8, 9 |
| 11 | 5–7 | 16, 18 | 12, 13, 14, 15 |
| 12 | 5, 7, 8 | 16, 18 | 11, 13, 14, 15 |
| 13 | 5, 9 | 16, 18 | 11, 12, 14, 15 |
| 14 | 5, 10 | 16, 18 | 11, 12, 13, 15 |
| 15 | 5 | 16, 18 | 11, 12, 13, 14 |
| 16 | 1, 4, 6, 8–15 | 17–19 | — |
| 17 | 1, 16 | 19 | 18 |
| 18 | 11–16 | 19 | 17 |
| 19 | 16–18 | F1–F4 | — |

## Todos

- [x] 1. Record superseding product, visual, AI, and theme decisions
  What to do / Must NOT do: Add a dated decision under `docs/requirements/decisions/` that explicitly supersedes the three-theme and “静奢理财日记” current direction, narrows the conflicting Agent business-operation decision to “not activated by this redesign,” defines the AI-overview aggregate payload/privacy contract, and records name-based asset association as accepted debt. Update the decisions index if one exists. Do not rewrite historical plans or claim the new design is implemented yet.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 5, 16, 17
  References (executor has NO interview context - be exhaustive): `AGENTS.md` sections 3–7; `docs/requirements/decisions/business_rules.md` record deletion/category/Agent/query-limit rules; `.omo/drafts/cardtally-light-ux-redesign.md` Decisions and Scope; `README.md` theme/AI claims.
  Acceptance criteria (agent-executable): Agent reads the new decision beside `business_rules.md` and verifies it unambiguously states precedence, effective date, light-only runtime, card-led black/white/gray direction, no Agent mutations/audit-log activation, exact AI aggregate allowlist/denylist, and accepted asset-name debt.
  QA scenarios (name the exact tool + invocation): happy—read decision and produce `.omo/evidence/task-1-cardtally-light-ux-redesign/decision-audit.md`; failure—compare against old theme/Agent clauses and prove every contradiction has an explicit supersession rather than silent omission.
  Commit: Y | `docs(decisions): supersede legacy visual and theme direction`

- [x] 2. Freeze active surface, transition, state, and behavior-preservation ledgers
  What to do / Must NOT do: Create a current UX inventory under `docs/design/` mapping every reachable Fragment, layout, adapter, sheet/dialog/popup/drawer, entry condition, back behavior, conditional navigation rule, shared layout, empty/error/loading applicability, and current business behavior. Exclude dormant `SearchFragment`, dormant `CategoriesFragment`, and stale `bottom_sheet_ledger_mode.xml` unless a live caller is proven. Include quick-add cold start, AI/asset conditional tabs, Agent drawer nav hiding, and both shared Add/Edit families.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 5, 11–15
  References: `MainActivity.kt`; `bottom_nav_menu.xml`; all active `*Fragment.kt`; `activity_main.xml`; active `fragment_*.xml`, `bottom_sheet_record_*.xml`, `bottom_sheet_ledger_period.xml`, dialogs and adapters; `docs/collaboration/current-snapshot.md`.
  Acceptance criteria: Every active route from `MainActivity`, Home, Ledger, Assets, Agent, and Me appears exactly once with source paths; every designed state is labeled current behavior, new approved behavior, or design-only primitive; stale surfaces are explicitly excluded.
  QA scenarios: happy—codegraph/LSP call-path comparison produces `.omo/evidence/task-2-cardtally-light-ux-redesign/surface-traceability.md`; failure—inject checklist rows for dormant/stale screens and verify the audit rejects them as runtime authority.
  Commit: Y | `docs(design): inventory active UX surfaces and states`

- [x] 3. Specify new behavior contracts, preferences, saved state, and migrations
  What to do / Must NOT do: Define exact keys/defaults and implementation contracts for Home summary layout (`one_primary_two_secondary` default plus three alternatives), Home overview source (`local_rule` default, greeting, custom, AI), custom text, cached AI sentence/fingerprint/timestamp, Ledger startup (`details` default, `statistics`, `remember_last`), and current last view. Define custom keypad grammar: digits 0–9, one decimal separator `.`, max two fractional digits, normalize leading zeros, non-negative values only, finite maximum matching current DB-safe amount policy, backspace, long-press clear, type switch preserving magnitude, system/hardware numeric input parity, 48dp targets, and system soft keyboard disabled for amount. Define exact recreation/process-death saved-state fields for Add/Edit Record, Add/Edit Asset, Ledger, Home Preferences, Agent composer/session, open sheets/drawer, and in-flight requests (cancel and do not restore requests). Do not use “where possible.”
  Parallelization: Wave 1 | Blocked by: none | Blocks: 5–15
  References: `ThemeHelper.kt`; `AiAssistantSettingsHelper.kt`; `QuickAddHelper.kt`; `LanguageHelper.kt`; `AddRecordFragment.kt`; `EditRecordFragment.kt`; `StatisticsFragment.kt`; `AgentFragment.kt`; existing helper tests.
  Acceptance criteria: Contract names every key, default, legal value, invalid-value fallback, recreation/restart behavior, privacy sensitivity, and migration behavior; no new preference clears unrelated keys.
  QA scenarios: happy—table-driven contract audit saved to `.omo/evidence/task-3-cardtally-light-ux-redesign/preference-state-contract.md`; failure—unknown enum values and interrupted recreation are traced to deterministic fallbacks with no crash or silent data submission.
  Commit: Y | `docs(requirements): define redesign behavior contracts`

- [x] 4. Preflight Stitch and Android verification environments
  What to do / Must NOT do: List owned Stitch projects, create a new `CardTally` project only if no matching project exists, record literal project ID, then establish the design-system asset lifecycle. Inspect the Android serial verification guide and determine whether an agent-controlled emulator/device is available without running forbidden `adb devices -l` or installing an APK. Record blockers early; do not substitute a user/manual gate.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 5, 16
  References: `docs/design/assets/README.md`; `docs/collaboration/skills/android-gradle-serial-verification.md`; `skills/android-build-debug.md`; AGENTS operational safety section.
  Acceptance criteria: Handoff records one verified Stitch project ID and, once created, one design-system asset ID; Android verification capability is classified available/blocker with allowed commands and evidence.
  QA scenarios: happy—Stitch `list_projects`/`get_project` receipts and environment report saved under `.omo/evidence/task-4-cardtally-light-ux-redesign/`; failure—duplicate-project and unavailable-device paths stop dependent generation/visual acceptance instead of guessing.
  Commit: N | environment evidence only

- [ ] 5. Create canonical DESIGN.md and pass the primitive showcase gate
  What to do / Must NOT do: Create root `DESIGN.md` with atmosphere, black/white/gray ramps, pale semantic red/cyan and status roles, typography/numeric treatment, 4dp spacing scale, 12–16dp card radius, tighter inner controls, outlined-card depth recipe, overview/dense density profiles, icon rules, card-slide motion/reduced-motion path, accessibility constraints, accepted debt, and every reusable primitive/state. Build/generate a primitive showcase before product screens: navigation shell, headers, buttons, FAB, summary/record/asset/settings cards, dense rows, segmented controls/filters, fields/keypad, sheets, dialogs, chat bubbles/session rows, skeleton/error/empty/undo Snackbar. Prevent nested-card slop and prove depth-50/200-record density.
  Parallelization: Wave 2 | Blocked by: 1–4 | Blocks: 11–15
  References: root `DESIGN.md` contract from `/frontend`; new superseding decision; active resource files `colors_light.xml`, `styles.xml`, `styles_cards.xml`, `styles_typography.xml`; surface inventory; historical Stitch exports as inspiration only.
  Acceptance criteria: Every token has an Android resource mapping; every primitive lists default/pressed/focused/selected/disabled/loading/error/empty states where applicable, 48dp target behavior, TalkBack semantics, normal/enlarged text, Chinese/English stress, and density profile. Stitch showcase visually passes before family generation.
  QA scenarios: happy—Stitch showcase inspection plus media analysis stored in `.omo/evidence/task-5-cardtally-light-ux-redesign/primitive-showcase/`; failure—nested cards, clipped depth-50 tree, high-saturation accents, or dense-row inflation reject the showcase.
  Commit: Y | `docs(design): establish light card design system`

- [x] 6. Migrate runtime to one light theme and remove obsolete theme UI
  What to do / Must NOT do: First extend tests for stored Light/Dark/System/invalid values and unrelated preference sentinels. Make Light the only effective/persisted mode before `super.onCreate`; remove theme destination/UI/callers and three-choice strings/accessibility labels; neutralize then reference-safely remove dark/system styles, colors, `values-night` aliases, and ThemeColor branches only after compilation proves them dead. System night configuration must render identical representative resources. Do not clear the whole preference file.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 11, 16
  References: `ThemeHelper.kt`; `ThemeSettingsFragment.kt`; `SettingsFragment.kt`; `MainActivity.kt`; `fragment_theme_settings.xml`; `styles.xml`; `colors_dark.xml`; `values-night/*.xml`; `ThemeHelperTest.kt`; `MainActivityThemeApplicationTest.kt`.
  Acceptance criteria: Stored `0`, `1`, `2`, and `999` all return/persist Light; unrelated sentinel remains; no Theme Settings route/resource reference survives; `uiMode=night` yields the same background/text/card resources as day; project compiles after dead-resource removal.
  QA scenarios: happy—serial targeted tests and representative day/night screenshots under `.omo/evidence/task-6-cardtally-light-ux-redesign/`; failure—legacy System preference under night mode is proven unable to select any night override.
  Commit: Y | `feat(theme): enforce single light appearance`

- [ ] 7. Implement preference helpers and recreation-safe screen state
  What to do / Must NOT do: Add typed helpers for Home and Ledger preference enums with sanitize/migrate methods; persist custom overview text and AI cache metadata securely in local preferences; add saved-state handling for the exact fields from Todo 3. Preserve Add/Edit Record amount/type/date/asset/category/note, Add/Edit Asset values, Ledger mode/filters/period, Home Preferences selections/text, and Agent composer/active session across locale/activity recreation; cancel and visibly reset in-flight AI operations. Do not store API keys in saved-state bundles or logs.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 11, 12
  References: existing `*SettingsHelper.kt`; `LanguageHelper.kt`; `MainActivity.kt` locale overlay; active Fragments; existing helper tests.
  Acceptance criteria: Table-driven tests cover every legal/invalid preference, first-run default, upgrade migration, activity recreation, process-style Bundle restoration, and unrelated preference preservation.
  QA scenarios: happy—unit/instrumentation evidence under `.omo/evidence/task-7-cardtally-light-ux-redesign/`; failure—malformed preference and locale switch during partially filled record prove deterministic fallback/state preservation without secret leakage.
  Commit: Y | `feat(settings): add typed UX preferences and saved state`

- [ ] 8. Add bounded today-record loading and correct Ledger aggregation contracts
  What to do / Must NOT do: Add a calendar-today query/paging seam that returns all today records in batches no larger than 200 and distinguishes no-today data from historical data. Correct Ledger ring summary so center total equals the complete filtered total; show at most top categories with an explicit Other slice or omit extra slices without altering total; retain flat leaf/category aggregation and period behavior. Do not roll child values into parents or use an unbounded query.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 12, 16
  References: `DatabaseHelper.kt`; `HomeFragment.kt`; `StatisticsFragment.kt`; `LedgerPeriodHelper.kt`; `DatabaseHelperLatestRecordDayTest.kt`; `business_rules.md` category/query-limit rules.
  Acceptance criteria: Tests with today/yesterday data show only all today records; 201+ today records are fully reachable through bounded pages; no-today is empty; ring center equals full filtered sum and flat child categories remain distinct across expense/income and ALL/CUSTOM ranges.
  QA scenarios: happy—database/JVM test reports under `.omo/evidence/task-8-cardtally-light-ux-redesign/`; failure—top-five categories summing below total and a parent/child dataset prove the corrected total without descendant roll-up.
  Commit: Y | `fix(ledger): bound today feed and preserve full totals`

- [ ] 9. Implement transactional record deletion and entity-specific destructive actions
  What to do / Must NOT do: Add a database-level reversible record deletion token/transaction that captures the exact record and balance delta, supports one successful Undo, and rejects duplicate/expired undo. Wire entity contracts: record swipe/delete+Snackbar Undo; active asset archive rather than generic permanent swipe; archived asset permanent deletion only with existing safeguards; categories expose swipe only when child/in-use checks permit and never bypass protection. Replace incompatible confirmation dialogs without applying one generic handler to all entities.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 13, 16
  References: `DatabaseHelper.deleteRecord`; Home/Ledger/AssetRecords record delete handlers; `AssetFragment.kt`; `ArchivedAssetsFragment.kt`; `CategoryManageFragment.kt`; `business_rules.md` balance rollback and category protection.
  Acceptance criteria: Deleting a ¥100 expense restores asset by exactly ¥100 once; Undo restores both row and original balance once; repeat/expired Undo is no-op; income behavior is correct; protected category deletion remains rejected; archive/permanent asset paths do not orphan associated records under current name-based debt.
  QA scenarios: happy—database and instrumentation tests under `.omo/evidence/task-9-cardtally-light-ux-redesign/`; failure—double Undo, category-with-child, in-use category, and asset-with-records cannot corrupt data.
  Commit: Y | `feat(records): add transactional swipe delete undo`

- [ ] 10. Implement privacy-bounded user-triggered AI overview service
  What to do / Must NOT do: Create a separate typed AI-overview request path using the existing MiniMax transport/parser patterns but not Agent sessions. Build payload only from the Todo 1 allowlist; require explicit user action; support configured/unconfigured, offline/network, timeout, cancellation, stale fingerprint, empty response, and success; cache only sentence/fingerprint/timestamp. Redact secrets from logs/evidence. Never send raw rows or mutate financial data.
  Parallelization: Wave 2 | Blocked by: 3 | Blocks: 14, 16
  References: `MiniMaxClient.kt`; `MiniMaxPayloadParser.kt`; `AiAssistantSettingsHelper.kt`; `AgentFragment.kt` error taxonomy/cancellation; AI tests; new decision.
  Acceptance criteria: Mocked tests prove exact JSON allowlist, denylisted fields/API key absent, no request before user action, no configuration handling, cancellation/stale response cannot overwrite current sentence, cached sentence invalidates on fingerprint change, and no chat/database mutation occurs.
  QA scenarios: happy—mock-server success/caching evidence under `.omo/evidence/task-10-cardtally-light-ux-redesign/`; failure—payload fixture containing notes/asset names/API key is rejected, plus timeout/cancel/stale-response tests.
  Commit: Y | `feat(ai): add explicit privacy-bounded home overview`

- [ ] 11. Implement app shell, Home, Home Preferences, and custom keypad record family
  What to do / Must NOT do: Implement the floating outlined-card navigation shell with 3/4/5-destination variants and deterministic fallback; Home sentence focal point, all-today natural list, no duplicate AI card, four summary layouts, Home Preferences preview-card selection and overview-source UI/privacy text; shared Add/Edit Record with prominent amount, fixed in-page keypad, top type switch, vertical field card, fixed save action, and card-slide sheets. Respect dense profile in today list/pickers. Agent visibility is `enabled && configured`; Assets keeps its existing preference predicate.
  Parallelization: Wave 3 | Blocked by: 5–7 | Blocks: 16, 18
  References: `MainActivity.kt`; `activity_main.xml`; `HomeFragment.kt`; `fragment_home.xml`; `HomeRecentRecordAdapter`; Add/Edit Record and shared layout; record sheet adapters/layouts; Todos 5, 7, 8, 10 contracts.
  Acceptance criteria: All four Home layouts persist/recreate; today list is complete; AI source requires explicit generate and inline disclosure; keypad grammar passes every edge case; Add/Edit share identical primitives and preserve fields; nav fallback returns Home when active AI/asset disappears; every control has ≥48dp target and bilingual accessibility semantics.
  QA scenarios: happy—instrumentation flows launch→Home→preferences→each layout and Home→Add→all sheets→save, with screenshots under `.omo/evidence/task-11-cardtally-light-ux-redesign/`; failure—201 records, max amount, invalid decimal, parent category tap, AI unconfigured, and mid-form locale switch remain usable.
  Commit: Y | `feat(home): deliver configurable light card entry experience`

- [ ] 12. Implement Ledger Statistics/Details family and filters
  What to do / Must NOT do: Implement same-page segmented Statistics/Details, default Details with Settings override/remember-last behavior, compact top filter strip and deeper filter surface, outlined-card grouped records, ring+numeric statistics, ALL/CUSTOM behavior, inline empty/error states, and swipe record Undo. Preserve edit/return refresh and no separate RecordsFragment. Do not revive stale ledger mode sheet.
  Parallelization: Wave 3 | Blocked by: 5, 7, 8 | Blocks: 16, 18
  References: `StatisticsFragment.kt`; `fragment_statistics.xml`; statistics/date-group/chart adapters/layouts; `LedgerPeriodHelper`; `LedgerDisplayHelper`; current ledger tests and Stitch references.
  Acceptance criteria: Details is default; Statistics/remember-last override persists; expense/income and period filters produce correct labels/totals; charts have text equivalents and non-color cues; list handles 200+ records with dense profile; return from Add/Edit rebinds; swipe Undo is exact.
  QA scenarios: happy—automated expense/income/week/month/year/all/custom/detail flows with screenshots under `.omo/evidence/task-12-cardtally-light-ux-redesign/`; failure—empty filtered result versus globally empty ledger, invalid custom range, 200+ records, and enlarged English labels do not clip or misstate totals.
  Commit: Y | `feat(ledger): redesign details and statistics cards`

- [ ] 13. Implement Assets, asset form, records, and archive family
  What to do / Must NOT do: Implement total-assets header plus concise name/type/balance account cards; tap opens asset records; Add/Edit use shared secondary page with prominent balance, vertical field card, and fixed save; asset records show summary plus records; archive is dedicated page. Apply active/archive/permanent-delete contracts and record Undo where records appear. Clearly document/display current name-based association limitations without claiming stable IDs.
  Parallelization: Wave 3 | Blocked by: 5, 9 | Blocks: 16, 18
  References: `AssetFragment.kt`; `AddAssetFragment.kt`; `EditAssetFragment.kt`; `AssetRecordsFragment.kt`; `ArchivedAssetsFragment.kt`; shared `fragment_add_asset.xml`; `item_asset.xml`; `DatabaseHelper` asset methods.
  Acceptance criteria: Populated/empty assets, add/edit validation, asset-record navigation, archive/restore/permanent-delete safeguards, large/negative/long balances, locale recreation, and conditional Assets tab all pass; shared Add/Edit visual baseline stays identical.
  QA scenarios: happy—Assets→add→edit→records→archive→restore flow under `.omo/evidence/task-13-cardtally-light-ux-redesign/`; failure—duplicate/blank name, invalid amount, long English name, asset with associated records, and hidden-tab fallback preserve data and navigation.
  Commit: Y | `feat(assets): deliver light card account workflows`

- [ ] 14. Implement Agent chat and AI configuration family
  What to do / Must NOT do: Implement concise empty welcome/composer, left/right pale bubbles, top-menu session drawer, new/rename dialogs, streaming/non-streaming/partial/error states, lightweight generating indicator, inline errors, and configuration card with status plus API key/model/full URL. API key masked by default with accessible reveal that resets masked on recreation. Hide Agent when disabled or incomplete; preserve sessions/messages and partial response behavior. Do not add voice, AI bookkeeping, or automatic overview requests.
  Parallelization: Wave 3 | Blocked by: 5, 10 | Blocks: 16, 18
  References: `AgentFragment.kt`; `AgentChatAdapter.kt`; `AgentSessionAdapter.kt`; `fragment_agent.xml`; message/session item layouts; `AiAssistantSettingsFragment.kt`; AI settings helper/client/parser tests.
  Acceptance criteria: All error taxonomy states, streaming and ordinary JSON, cancellation/partial retention, drawer nav hiding/restoration, session create/switch/rename, masked key, config predicate, and active-page fallback pass; TalkBack announces sender/state without repeated chunk spam.
  QA scenarios: happy—configured multi-session streaming flow with screenshots under `.omo/evidence/task-14-cardtally-light-ux-redesign/`; failure—unconfigured, invalid URL, auth, rate limit, timeout, cancel, interruption, partial message, long text, and locale recreation remain recoverable and secret-free.
  Commit: Y | `feat(agent): redesign optional multi-session assistant`

- [ ] 15. Implement Me, grouped settings, category management, and secondary preference surfaces
  What to do / Must NOT do: Implement top personal card plus Preferences/Management/AI/About outlined groups; remove Theme entry; add Home Preferences and Ledger default controls; retain language, quick add, AI enablement, category depth, and current implemented settings only. Implement top-level category grouped cards with indented descendants, subdued icons, bottom card actions, add/edit/icon picker and protected deletion. Use dense profile for deep trees; do not add mock-only reminder/security/settings features.
  Parallelization: Wave 3 | Blocked by: 5 | Blocks: 16, 18
  References: `SettingsFragment.kt`; `fragment_settings.xml`; setting item layouts; `CategoryManageFragment.kt`; category layouts/adapters/dialogs; `CategoryHierarchySettingsHelper`; Chinese/English strings/accessibility resources.
  Acceptance criteria: Every setting displays current value and persists; Theme route is absent; groups match approved structure; depth-50 category tree remains navigable with parent non-selection, leaf rules, and protection messages; icons are grayscale/pale; focus/order/text expansion pass in both locales.
  QA scenarios: happy—Me→Home Preferences/Ledger preference/AI config/category management/language flows under `.omo/evidence/task-15-cardtally-light-ux-redesign/`; failure—depth 50, in-use/has-child deletion, invalid depth, long English labels, enlarged text, and locale switch preserve state without clipping.
  Commit: Y | `feat(settings): group preferences and category cards`

- [ ] 16. Integrate generated Stitch families with Android resources and reconcile every state
  What to do / Must NOT do: Generate/edit Stitch families only after the design-system asset/showcase passes, then reconcile each output against the surface inventory and implemented Android family. Consolidate colors/dimens/styles/drawables/icons/strings instead of page-local hardcoding; enforce one lighting direction, two density profiles, 12–16dp outer radius, non-color semantics, and card-slide/reduced-motion behavior. Implement skeleton/error/empty only where the state matrix permits. Do not treat Stitch HTML/code or historical exports as repository truth.
  Parallelization: Wave 4 | Blocked by: 1, 4, 6, 8–15 | Blocks: 17–19
  References: verified Stitch project/design-system IDs; root `DESIGN.md`; surface/state matrix; all generated screen IDs; active Android families; `colors_light.xml`, `styles*.xml`, `dimens*.xml`, drawables and string resources.
  Acceptance criteria: Traceability matrix maps every active state to Stitch screen, Fragment/layout/adapter/resource; no orphan design or active screen omission; resource audit finds no ad-hoc palette/radius drift; generated and implemented screens match structure, state, copy, and safe areas.
  QA scenarios: happy—fresh screenshot-to-reference comparisons and traceability audit under `.omo/evidence/task-16-cardtally-light-ux-redesign/`; failure—stale screen, unsupported feature, nested-card slop, wrong density, or unmapped token rejects integration.
  Commit: Y | `refactor(ui): consolidate light card system across screens`

- [ ] 17. Update current-facing product, collaboration, and design documentation after verification
  What to do / Must NOT do: After implementation facts pass, update README, AGENTS long-term theme/identity boundaries, collaboration task entrypoints/current snapshot/project overview/engineering constraints, and design guidelines to the new light-only card system and new Home/AI behavior. Mark historical plans and old Stitch assets as superseded/reference-only; do not rewrite their original content or claim unverified capabilities.
  Parallelization: Wave 4 | Blocked by: 1, 16 | Blocks: 19
  References: `README.md`; `AGENTS.md`; `docs/collaboration/*.md`; `docs/design/guidelines/*.md`; `docs/design/assets/README.md`; new decision and verified source.
  Acceptance criteria: Current docs consistently state one light theme, black/white/gray card direction, exact AI overview privacy/trigger, current primary/secondary routes, and no old identity/current three-theme claim; history remains clearly labeled.
  QA scenarios: happy—source-to-doc fact audit under `.omo/evidence/task-17-cardtally-light-ux-redesign/`; failure—compare all current-facing claims against source/decision and reject wishful, stale, or secret-bearing text.
  Commit: Y | `docs(product): align guidance with light card redesign`

- [ ] 18. Run accessibility, localization, content-stress, and visual QA repair loops
  What to do / Must NOT do: Use actual built surfaces and `/visual-qa` with fresh screenshots. Verify 48dp targets, TalkBack name/role/state/action, focus restoration/trapping, chart text equivalents, live AI announcements, non-color semantics, Chinese/English, enlarged fonts, long/unbroken strings, compact/regular widths, safe areas, 3/4/5 tab shells, keyboard/keypad, drawers/sheets, empty/loading/error/undo states, and reduced motion. Repair all Critical/Major issues; record only user-accepted Minor debt in `DESIGN.md`.
  Parallelization: Wave 4 | Blocked by: 11–16 | Blocks: 19
  References: root `DESIGN.md` accessibility section; `strings_accessibility.xml` and English parity; visual QA skill; implemented screen/state matrix; approved Stitch references.
  Acceptance criteria: Visual QA and design/accessibility/heuristic/persona reviews pass on fresh evidence; no clipping, overlap, inaccessible color-only cue, flat generic surface, secret exposure, or unresolved Critical/Major issue.
  QA scenarios: happy—full evidence packet under `.omo/evidence/task-18-cardtally-light-ux-redesign/`; failure—depth-50 tree, 200+ records, long AI message, large amount, enlarged English text, and drawer/nav overlap are actively driven and must fail before repair.
  Commit: Y | `fix(ui): resolve accessibility and visual QA findings`

- [ ] 19. Execute serial quality gates and produce final evidence ledger
  What to do / Must NOT do: Re-read changed files, run targeted tests then full JVM, connected instrumentation, and assemble serially; verify APK path; inspect logs/screenshots for API keys, user data, or device identifiers; reconcile every todo acceptance criterion and evidence path. If connected verification lacks an agent-controlled target, report a blocker—do not ask the user to install or run device commands as substitute proof.
  Parallelization: Wave 4 | Blocked by: 16–18 | Blocks: F1–F4
  References: `docs/collaboration/skills/android-gradle-serial-verification.md`; root skills for build/screenshot constraints; all changed tests; evidence ledger.
  Acceptance criteria: `testDebugUnitTest`, `connectedDebugAndroidTest`, and `assembleDebug` each report `BUILD SUCCESSFUL` in serial order; APK `Test-Path` returns `True`; evidence ledger has no missing task, secret, or contradictory claim.
  QA scenarios: happy—command logs and artifact manifest under `.omo/evidence/task-19-cardtally-light-ux-redesign/`; failure—misleading stale output, skipped tests, absent target, secret leakage, or APK from an unverified build blocks completion.
  Commit: Y | `test(app): verify complete light UX redesign`

## Final verification wave

> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.

- [ ] F1. Plan compliance audit
  Verify every Must Have/Must NOT Have, todo acceptance criterion, decision precedence, evidence artifact, and active surface; reject self-report without exact artifact/command assertions.
- [ ] F2. Code quality review
  Review Kotlin/XML/SQLite/resources for regressions, duplicated style logic, unsafe preference/data migration, secret leakage, accessibility defects, unbounded queries, non-transactional undo, and architecture drift.
- [ ] F3. Real manual QA
  On an agent-controlled Android target, execute the nine critical flows: launch/Home/preferences/AI overview; Add/Edit Record and sheets/keypad; Ledger modes/filters; Assets/archive; Agent sessions/stream/failures; Me/settings; category depth/protection; locale recreation; secondary navigation/fallback. Capture fresh evidence.
- [ ] F4. Scope fidelity
  Compare implementation to the confirmed page-by-page brief: one light black/white/gray card UX, approved pale accents, every active page/state, no old identity/theme modes, no unsupported features, and no reduced scope.

## Commit strategy

- Use one atomic commit per todo where marked `Y`; do not combine design decisions, data behavior, visual families, documentation, and final test evidence into one commit.
- Keep generated design assets and their traceability metadata together; keep product-code implementation separate from historical/reference assets.
- Never commit `local.properties`, API keys, financial fixtures containing personal data, device identifiers, build outputs, or raw screenshots with secrets.
- Recommended order follows the todo sequence; if parallel workers finish out of order, integrate dependency commits before dependent UI commits and rerun affected gates.

## Success criteria

- Exactly one runtime Light theme remains, legacy theme values safely normalize, system night mode produces no visual variation, and no Theme Settings route survives.
- Root `DESIGN.md`, Stitch design-system asset, primitive showcase, screen families, and Android resources express one coherent black/white/gray outlined-card system with approved pale semantic accents and two density profiles.
- Every active primary, secondary, sheet/dialog/popup/drawer, conditional navigation state, and approved new preference/AI state is implemented, traceable, localized, accessible, and visually verified.
- Existing accounting invariants pass: local-first data, expense balance rollback, leaf-only category binding, flat category statistics, bounded queries, shared Add/Edit flows, and AI partial response persistence.
- New behavior passes deterministic tests: Home preferences, all-today bounded feed, Ledger startup preference, keypad grammar, saved state, transactional record Undo, Agent/config predicate, and privacy-bounded user-triggered AI overview.
- No current-facing docs retain the old identity or three-theme claims; historical materials are preserved and clearly superseded.
- All serial Gradle gates pass, the verified debug APK exists, visual/accessibility review has no unresolved Critical/Major issue, and F1–F4 approve.
