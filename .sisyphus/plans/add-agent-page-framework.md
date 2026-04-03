# Add And Agent Page Framework Redesign

> **History note:** This is a historical execution plan. The current settled rule is: `记一笔` remains a Home/Records FAB entry, and Agent writes do **not** require pre-execution confirmation; they require auditability, result feedback, and safe correction paths.

## TL;DR
> **Summary**: Rebuild `记一笔` around the existing record form flow with a single primary save action, then introduce a greenfield `Agent` tab that uses a shared capability layer, idempotent execution, and auditable local persistence for full business writes.
> **Deliverables**:
> - Rebuilt `记一笔` page with single-save flow and retained asset linkage
> - Fixed bottom-nav `Agent` tab replacing deferred `统计`
> - Shared capability layer for UI and Agent across records/assets/categories
> - Agent conversation UI, execution/result flow, audit log, and local history persistence
> - Safety hardening for full-write Agent operations and regression coverage
> **Effort**: XL
> **Parallel**: YES - 2 waves
> **Critical Path**: 1 -> 2 -> 3 -> 5 -> 6 -> 7 -> 8

## Context
### Original Request
- Continue planning because other core pages were still not frozen after records/assets/my plans.

### Interview Summary
- This round covers `记一笔 + Agent` only.
- `记一笔` is a rebuild of the existing `AddRecordFragment`, not a new parallel entry.
- `记一笔` keeps the asset-source field visible and removes the dual primary action pattern in favor of one primary save action.
- `Agent` becomes a fixed bottom-navigation tab replacing deferred `统计`.
- `Agent` lands as a pure conversation screen.
- `Agent` v1 is allowed to perform business-wide writes, not just suggestions, and must use traceable source metadata plus full audit logging.

### Metis Review (gaps addressed)
- This plan does not assume prior test/bootstrap work is already landed; it includes the minimum extension or bootstrap required for add/agent coverage.
- LLM/provider access is treated as an adapter boundary, not a UI concern; the base app must remain usable when the provider is unavailable.
- Add-page validation/mutation logic is extracted into shared services so `记一笔`, existing edit flows, and `Agent` do not fork business rules.
- Agent history stays local-first in v1; no sync or multi-device memory is introduced.

### Oracle Review (architecture constraints addressed)
- Full-write Agent scope is unsafe with the current name-keyed `Record.category` and `Record.assetSource` model; this plan hardens references before enabling broad Agent writes.
- All Agent writes must execute through a thin shared capability layer above `DatabaseHelper`, never by calling fragments or UI handlers.
- Agent execution requires an append-only audit trail (`requested -> executed/failed`) plus idempotent operation IDs.
- Provider failure must degrade gracefully: the Agent tab remains reachable but non-destructive paths and manual app flows still work without AI availability.

## Work Objectives
### Core Objective
- Deliver one implementation plan that modernizes the high-frequency `记一笔` page and adds a safe, auditable Agent collaboration tab without making the rest of CardTally depend on Agent availability.

### Deliverables
- Shared record form/capability layer used by `AddRecordFragment`, `EditRecordFragment`, and Agent-triggered record writes.
- Data-reference hardening and execution safeguards needed for business-wide Agent writes.
- `AgentFragment` with chat history, result/audit feedback, provider adapter, and action execution pipeline.
- Bottom-navigation update from `首页 / 记录 / 资产 / 统计 / 我的`-in-transition to `首页 / 记录 / 资产 / Agent / 我的` as the v1 shell.
- Instrumentation/unit coverage for add-page flow, Agent confirmation/execution flow, and nav safety.

### Definition of Done (verifiable conditions with commands)
- `./gradlew.bat assembleDebug` succeeds after add/agent refactor.
- `./gradlew.bat testDebugUnitTest` succeeds for shared capability, audit, and idempotency tests.
- `./gradlew.bat connectedDebugAndroidTest` succeeds for add-page flow, Agent chat/execution flow, and bottom-nav routing.
- The app bottom nav shows `Agent` and no longer shows `统计`.
- `记一笔` exposes one primary save action and still supports asset-linked records.
- Agent write actions do not require pre-execution confirmation and always generate a persisted audit entry.

### Must Have
- Keep `记一笔` as a page-internal task flow entered from Home/Records, not a bottom tab.
- Keep manual record/asset/category flows usable even if Agent provider setup is missing or broken.
- Keep `Agent` as an enhancement layer that shares business capabilities rather than driving UI handlers.
- Keep asset linkage visible in `记一笔`.
- Keep local persistence for Agent history and audit state in v1.

### Must NOT Have (guardrails, AI slop patterns, scope boundaries)
- No requirement that users must go through Agent to complete normal bookkeeping.
- No direct Agent calls into fragments, adapters, XML state, button handlers, or raw ad-hoc SQL.
- No cloud sync, voice input, deep personalization memory, or multi-step autonomous workflow engine in v1.
- No reintroduction of dual primary save buttons on `记一笔`.
- No unaudited Agent writes.

## Verification Strategy
> ZERO HUMAN INTERVENTION - all verification is agent-executed.
- Test decision: tests-after with unit + instrumentation coverage extended or bootstrapped as needed.
- QA policy: every task includes executable happy-path and failure/edge-path checks.
- Evidence: `.sisyphus/evidence/task-{N}-{slug}.{ext}`

## Execution Strategy
### Parallel Execution Waves
> Target: 5-8 tasks per wave. Shared foundations are extracted into Wave 1.

Wave 1: test/bootstrap extension, shared capability layer, stable-reference hardening, add-page rebuild.
Wave 2: Agent shell, provider/audit pipeline, bottom-nav integration, regression hardening.

### Dependency Matrix (full, all tasks)
| Task | Depends On | Enables |
|------|------------|---------|
| 1 | - | 2, 3, 4, 5, 6, 7, 8 |
| 2 | 1 | 3, 4, 6, 7, 8 |
| 3 | 1, 2 | 4, 6, 7, 8 |
| 4 | 1, 2, 3 | 7, 8 |
| 5 | 1 | 6, 7, 8 |
| 6 | 1, 2, 3, 5 | 7, 8 |
| 7 | 1, 2, 3, 4, 5, 6 | 8 |
| 8 | 1, 4, 6, 7 | Final Verification |

### Agent Dispatch Summary
| Wave | Task Count | Recommended Categories |
|------|------------|------------------------|
| 1 | 4 | unspecified-high, deep |
| 2 | 4 | deep, unspecified-high |
| Final | 4 | oracle, unspecified-high, deep |

## TODOs
> Implementation + Test = ONE task. Never separate.
> EVERY task MUST have: Agent Profile + Parallelization + QA Scenarios.

- [ ] 1. Bootstrap or extend minimal test support for add-page and Agent flows

  **What to do**: Make this plan self-sufficient for testing. If Android test/unit test support already exists from prior work, extend it; otherwise add the minimum Gradle dependencies, source-set directories, and smoke helpers required to run add-page, bottom-nav, and Agent execution tests. Create smoke classes for one manual add-page launch and one Agent-tab launch so later tasks have a stable verification baseline.
  **Must NOT do**: Do not assume prior records/assets/my test plans were already executed; do not add cloud CI or broad coverage automation in this task; do not introduce JUnit 5.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines Gradle, Android test setup, and host activity instrumentation prerequisites.
  - Skills: [`systematic-debugging`] - Reason: Android test bootstrapping is failure-prone and foundational.
  - Omitted: [`brainstorming`] - Reason: product decisions are already fixed.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 2, 3, 4, 5, 6, 7, 8 | Blocked By: none

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/build.gradle` - Existing Android app module config to extend.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt` - Activity host for add/agent tab instrumentation.
  - Pattern: `.sisyphus/plans/records-page-framework.md` - Earlier plan expected richer shared test support but did not land it yet.
  - Pattern: `.sisyphus/plans/assets-my-page-framework.md` - Earlier plan also assumes page-scope Android test support.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `app/src/androidTest/java/com/example/cardtally/` exists with smoke tests for add-page launch and Agent-tab launch.
  - [ ] `app/src/test/java/com/example/cardtally/` exists with at least one compiling local unit smoke test for the forthcoming shared capability layer.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddAgentSmokeTest` passes.
  - [ ] `./gradlew.bat testDebugUnitTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Add/Agent instrumentation smoke suite runs
    Tool: Bash
    Steps: ./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddAgentSmokeTest
    Expected: BUILD SUCCESSFUL and XML results are generated for the smoke class
    Evidence: .sisyphus/evidence/task-1-add-agent-smoke.txt

  Scenario: Local unit-test task remains healthy after bootstrap
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest
    Expected: BUILD SUCCESSFUL with no new test-task failures
    Evidence: .sisyphus/evidence/task-1-add-agent-unit.txt
  ```

  **Commit**: YES | Message: `test(agent): bootstrap add-agent coverage foundation` | Files: `app/build.gradle`, `app/src/androidTest/java/com/example/cardtally/...`, `app/src/test/java/com/example/cardtally/...`

- [ ] 2. Extract a shared ledger capability layer above `DatabaseHelper`

  **What to do**: Introduce a thin application-service layer that becomes the only write/read execution boundary for records, assets, and categories used by both UI flows and Agent. Move validation, normalization, and mutation orchestration currently duplicated in `AddRecordFragment`, `EditRecordFragment`, and legacy asset/category flows into services such as `RecordService`, `AssetService`, and `CategoryService` plus shared request/result models. Preserve `DatabaseHelper` as the persistence implementation under the service layer.
  **Must NOT do**: Do not let Agent or rebuilt fragments continue to own business rules independently; do not rewrite the app into a large repository/DI architecture; do not make fragments call each other.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this is the architecture spine for both manual and Agent-driven mutations.
  - Skills: [`systematic-debugging`] - Reason: logic extraction from fragments can silently change behavior.
  - Omitted: [`executing-plans`] - Reason: the plan already defines the execution sequence.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 3, 4, 6, 7, 8 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt:182` - Current record-save validation/mutation flow to extract.
  - Pattern: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt:217` - Current record-update validation/mutation flow to extract.
  - Pattern: `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt` - Existing persistence and side-effect surface for records/assets/categories.
  - External: `docs/plans/2026-03-26-cardtally-agent-integration-principles.md:16` - UI and Agent must share business capabilities rather than page actions.

  **Acceptance Criteria** (agent-executable only):
  - [ ] New shared capability classes exist for records, assets, and categories under a dedicated package (for example `ledger/` or `domain/`).
  - [ ] `AddRecordFragment` and `EditRecordFragment` no longer contain primary validation/mutation rules inline; they delegate to the shared capability layer.
  - [ ] Unit tests verify record create/update validation, asset create/update validation, and category create/update validation through the shared capability layer.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.*ServiceTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Shared record service validates and creates records deterministically
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.RecordServiceTest"
    Expected: BUILD SUCCESSFUL and tests prove manual and agent record writes share the same rules
    Evidence: .sisyphus/evidence/task-2-record-service.txt

  Scenario: Shared asset/category services enforce stable validation rules
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.AssetServiceTest" --tests "com.example.cardtally.CategoryServiceTest"
    Expected: BUILD SUCCESSFUL and tests prove assets/categories no longer rely on fragment-local validation
    Evidence: .sisyphus/evidence/task-2-asset-category-services.txt
  ```

  **Commit**: YES | Message: `refactor(core): extract shared ledger capability layer` | Files: `app/src/main/java/com/example/cardtally/...service...`, `app/src/test/java/com/example/cardtally/...ServiceTest.kt`, `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`, `app/src/main/java/com/example/cardtally/EditRecordFragment.kt`

- [ ] 3. Harden record references and persistence safety for full-write Agent scope

  **What to do**: Normalize the data model enough to make business-wide Agent writes safe. Replace fragile name-only relationships for category/asset references with stable identifiers in records and in the shared capability layer, add transactional mutation paths where record writes also affect asset balances, and introduce local persistence for Agent-side audit state and operation idempotency. If schema migration is required, define and implement it here together with backward-compatible reads for existing rows.
  **Must NOT do**: Do not keep Agent write execution keyed only by display names; do not enable destructive Agent writes before stable reference resolution exists; do not leave record/asset partial-update risk unaddressed.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this is the core safety hardening that makes full-write Agent support viable.
  - Skills: [`systematic-debugging`] - Reason: schema and side-effect changes are high-risk and need strong regression discipline.
  - Omitted: [`brainstorming`] - Reason: architecture direction is already fixed by product choice and Oracle review.

  **Parallelization**: Can Parallel: NO | Wave 1 | Blocks: 4, 6, 7, 8 | Blocked By: 1, 2

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/model/Record.kt` - Current record model storing category/asset display names.
  - Pattern: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt:185` - Current save path uses category name and `assetSource` string.
  - Pattern: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt:220` - Current update path also uses display-name references.
  - External: `docs/plans/2026-03-26-cardtally-agent-integration-principles.md:142` - Idempotency and safe failure are mandatory for Agent writes.
  - External: `docs/plans/2026-03-26-cardtally-agent-integration-principles.md:120` - Auditability requirements for Agent-triggered mutations.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Record persistence no longer depends solely on category/asset display names for write integrity.
  - [ ] Shared mutation paths use DB transactions or equivalent atomic behavior for record + asset-balance updates.
  - [ ] Local persistence exists for Agent operation log and idempotent `operationId` tracking.
  - [ ] Migration or backward-compatible read logic preserves existing user data.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.LedgerMigrationTest" --tests "com.example.cardtally.AgentOperationStoreTest"` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Existing rows migrate or resolve safely to stable references
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.LedgerMigrationTest"
    Expected: BUILD SUCCESSFUL and tests prove legacy records remain editable/queryable after reference hardening
    Evidence: .sisyphus/evidence/task-3-migration.txt

  Scenario: Retrying the same operation does not duplicate writes
    Tool: Bash
    Steps: ./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.AgentOperationStoreTest"
    Expected: BUILD SUCCESSFUL and tests prove duplicate confirmation/execution attempts are deduped by operationId
    Evidence: .sisyphus/evidence/task-3-idempotency.txt
  ```

  **Commit**: YES | Message: `feat(core): harden stable references and audit persistence` | Files: `app/src/main/java/com/example/cardtally/model/Record.kt`, `app/src/main/java/com/example/cardtally/database/DatabaseHelper.kt`, `app/src/main/java/com/example/cardtally/...operation store...`, `app/src/test/java/com/example/cardtally/LedgerMigrationTest.kt`, `app/src/test/java/com/example/cardtally/AgentOperationStoreTest.kt`

- [ ] 4. Rebuild `记一笔` around the simplified single-save manual flow

  **What to do**: Refactor `AddRecordFragment` and its layout into the dedicated `记一笔` task page that matches the confirmed product direction: one primary save button, retained asset-source field, amount as the visual focus, expense/income switch, category grid, date, and optional description. Remove the `继续记一笔` primary action and any UI elements only supporting that mode. Keep return behavior aligned with the existing navigation spec: entering from Home/Records returns to the trigger source when the task completes or cancels.
  **Must NOT do**: Do not add new fields, do not hide asset source, do not force Agent into the manual add flow, and do not add dialog-based confirmation after save.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: combines page-shell refactor, state simplification, and navigation behavior.
  - Skills: [`systematic-debugging`] - Reason: add-flow regressions will affect the core manual task path.
  - Omitted: [`brainstorming`] - Reason: the add-page UX tradeoffs are already fixed.

  **Parallelization**: Can Parallel: YES | Wave 1 | Blocks: 7, 8 | Blocked By: 1, 2, 3

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt:27` - Current add-page fragment to rebuild.
  - Pattern: `app/src/main/res/layout/fragment_add_record.xml:118` - Amount field is already the primary visual focal point.
  - Pattern: `app/src/main/res/layout/fragment_add_record.xml:247` - Dual-button footer to collapse into one primary action.
  - Pattern: `app/src/main/java/com/example/cardtally/adapter/CategorySelectorAdapter.kt:14` - Existing category grid selector to preserve.
  - External: `docs/stitch-guidance/page-design-guide.md:114` - Add-page guidance emphasizing low friction and one clear save path.
  - External: `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md:94` - Add-flow task-page return behavior.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `fragment_add_record.xml` contains one primary save control and no `btn_save_and_continue` active path.
  - [ ] `AddRecordFragment` still shows amount, type toggle, category selector, date, description, and asset source.
  - [ ] Saving a valid record returns to the triggering screen and refreshes manual records state.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddRecordFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Single save action creates a record and returns
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddRecordFragmentTest#singleSaveCreatesRecordAndReturns`; inside the test enter amount, category, and asset source, tap the only save button, and assert the previous screen is restored with the new record visible.
    Expected: BUILD SUCCESSFUL and Espresso proves the add flow is single-action and still linked to assets
    Evidence: .sisyphus/evidence/task-4-add-save.txt

  Scenario: Empty amount blocks save without dialog confirmation
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddRecordFragmentTest#emptyAmountShowsInlineFailure`; inside the test leave amount empty, tap save, and assert the fragment remains visible with failure feedback and no record created.
    Expected: BUILD SUCCESSFUL and Espresso proves low-friction validation happens before persistence without a second confirmation dialog
    Evidence: .sisyphus/evidence/task-4-add-validation.txt
  ```

  **Commit**: YES | Message: `feat(add): rebuild add record flow` | Files: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt`, `app/src/main/res/layout/fragment_add_record.xml`, `app/src/androidTest/java/com/example/cardtally/AddRecordFragmentTest.kt`

- [ ] 5. Add the greenfield Agent shell, provider boundary, and local conversation persistence

  **What to do**: Introduce the Agent feature as a self-contained shell with no dependency on provider availability. Create `AgentFragment`, its layout, a provider interface/adapter boundary (for example `AgentProvider`), a local conversation/thread/message store, and a lightweight unavailable/error state when no provider is configured. Keep the landing as a pure conversation page with message history and input/send actions; do not add dashboard cards or mandatory proactive widgets.
  **Must NOT do**: Do not wire a concrete provider SDK directly into the fragment, do not hide the rest of the app when the provider is unavailable, and do not add suggestion-card landing UI for v1.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this introduces a new first-class page plus provider and persistence boundaries.
  - Skills: [`systematic-debugging`] - Reason: provider error states and local persistence behavior need deliberate handling.
  - Omitted: [`brainstorming`] - Reason: the Agent landing pattern is already chosen.

  **Parallelization**: Can Parallel: YES | Wave 2 | Blocks: 6, 7, 8 | Blocked By: 1

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Existing bottom-nav fragment routing style the Agent shell must fit.
  - Pattern: `app/src/main/res/menu/bottom_nav_menu.xml:3` - Current nav menu to extend with Agent later in task 7.
  - External: `docs/plans/2026-03-26-cardtally-agent-capabilities.md:187` - V1 Agent should at least provide independent entry and basic conversation space.
  - External: `docs/stitch-guidance/page-design-guide.md:222` - Agent-page visual/interaction direction.
  - External: `docs/plans/2026-03-26-cardtally-v1-scope-freeze.md` - Base product must remain usable without Agent.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `AgentFragment` and its layout exist with a message list, input field, and send action.
  - [ ] A provider interface exists outside the fragment, with a local fake/mock implementation usable in tests.
  - [ ] Local conversation/thread/message persistence exists and restores prior chat history on relaunch.
  - [ ] Provider-unavailable state renders safely without breaking the app shell.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentFragmentTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Agent chat history persists across relaunch
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentFragmentTest#chatHistoryPersistsAcrossRelaunch`; inside the test send a prompt through the fake provider, recreate the activity, and assert the message history still appears.
    Expected: BUILD SUCCESSFUL and Espresso proves the conversation shell is local-persistent
    Evidence: .sisyphus/evidence/task-5-agent-history.txt

  Scenario: Agent remains reachable when provider is unavailable
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentFragmentTest#providerUnavailableShowsSafeState`; configure the fake provider to fail and assert the Agent screen still opens with a non-destructive unavailable state.
    Expected: BUILD SUCCESSFUL and Espresso proves Agent failure does not block manual product usage
    Evidence: .sisyphus/evidence/task-5-agent-unavailable.txt
  ```

  **Commit**: YES | Message: `feat(agent): add agent shell and conversation persistence` | Files: `app/src/main/java/com/example/cardtally/AgentFragment.kt`, `app/src/main/java/com/example/cardtally/agent/...`, `app/src/main/res/layout/fragment_agent.xml`, `app/src/androidTest/java/com/example/cardtally/AgentFragmentTest.kt`

- [ ] 6. Implement Agent audit and execution pipeline for full writes

  **What to do**: Build the core Agent action pipeline on top of the shared capability layer. Convert provider outputs into structured action requests, execute through the shared services with an `operationId`, surface clear result feedback to the user, and persist an append-only audit trail (`requested -> executed/failed`) including source = `Agent`. Support records/assets/categories actions in scope, including destructive operations, but only through the normalized stable-reference layer from task 3.
  **Must NOT do**: Do not let freeform model text execute directly, do not write without audit persistence, and do not bypass the shared service boundary.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this is the highest-risk architecture task and the heart of full-write Agent v1.
  - Skills: [`systematic-debugging`] - Reason: execution, retries, and audit safety are easy to get subtly wrong.
  - Omitted: [`brainstorming`] - Reason: the write scope and guardrails are already fixed.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 7, 8 | Blocked By: 1, 2, 3, 5

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/...service...` - Shared capability layer from task 2 is the only execution boundary.
  - Pattern: `app/src/main/java/com/example/cardtally/...operation store...` - Idempotent audit persistence from task 3.
  - External: `docs/plans/2026-03-26-cardtally-agent-integration-principles.md:100` - Write operations must be explicit, auditable, traceable, and correctable.
  - External: `docs/plans/2026-03-26-cardtally-agent-integration-principles.md:132` - High-risk writes do not require pre-execution confirmation, but do require audit and correction safety.
  - External: `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md:131` - Agent writes must be marked as Agent-originated for audit.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Agent-generated write requests are structured and executed only through the service-layer action boundary.
  - [ ] A single request executes exactly one service-layer action with a persisted `operationId`.
  - [ ] Duplicate execution attempts do not create duplicate domain rows.
  - [ ] Failed destructive actions leave domain data unchanged and still record the failure/audit state.
  - [ ] `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.AgentExecutionPipelineTest"` and `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentExecutionTest` both pass.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Agent record creation writes once and logs audit state
    Tool: Bash
    Steps: Run `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.AgentExecutionPipelineTest.confirmedRecordCreateWritesOnce"`
    Expected: BUILD SUCCESSFUL and tests prove the shared service executes once with persisted requested/executed states
    Evidence: .sisyphus/evidence/task-6-agent-write.txt

  Scenario: Failed Agent destructive action performs no mutation
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentExecutionTest#failedDeleteDoesNotMutateData`; inside the test send a delete prompt via fake provider, force failure in the execution boundary, and assert the target row still exists.
    Expected: BUILD SUCCESSFUL and Espresso proves destructive actions are auditable and fail safely
    Evidence: .sisyphus/evidence/task-6-agent-failed-delete.txt
  ```

  **Commit**: YES | Message: `feat(agent): add audit and execution pipeline` | Files: `app/src/main/java/com/example/cardtally/agent/...`, `app/src/test/java/com/example/cardtally/AgentExecutionPipelineTest.kt`, `app/src/androidTest/java/com/example/cardtally/AgentExecutionTest.kt`

- [ ] 7. Replace Statistics with Agent in the app shell and wire cross-entry navigation

  **What to do**: Update the app shell to match the confirmed IA for this stage: replace the deferred statistics slot with Agent in `bottom_nav_menu.xml`, update `MainActivity` routing, add any required icon resources, and preserve the page-internal `记一笔` entry rules from Home/Records only. Ensure Agent does not become the default startup target even when quick-add is enabled, and keep bottom-nav/secondary-page visibility rules consistent.
  **Must NOT do**: Do not make Agent the only path into add-record, do not keep `统计` as a hidden placeholder, and do not expose a `记一笔` default entry from Agent.

  **Recommended Agent Profile**:
  - Category: `unspecified-high` - Reason: touches app-shell navigation, tab labels, and startup behavior.
  - Skills: [`systematic-debugging`] - Reason: nav-shell regressions are cross-cutting and user-visible.
  - Omitted: [`brainstorming`] - Reason: navigation structure is already fixed by the IA and user decisions.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: 8 | Blocked By: 1, 2, 3, 4, 5, 6

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:36` - Current bottom-nav item dispatch.
  - Pattern: `app/src/main/java/com/example/cardtally/MainActivity.kt:53` - Quick-add startup logic that must remain independent of Agent.
  - Pattern: `app/src/main/res/menu/bottom_nav_menu.xml:3` - Current nav structure still containing `统计`.
  - External: `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md:33` - Target v1 navigation pattern.
  - External: `docs/plans/2026-03-26-cardtally-ia-navigation-spec.md:111` - Agent must not be the default `记一笔` entry.

  **Acceptance Criteria** (agent-executable only):
  - [ ] Bottom navigation contains `首页 / 记录 / 资产 / Agent / 我的` and no `统计` item.
  - [ ] Selecting `Agent` opens `AgentFragment`.
  - [ ] Quick-add startup behavior still goes to `AddRecordFragment`, not Agent.
  - [ ] No page outside Home/Records exposes `记一笔` as a mandatory top-level action because of Agent integration.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentBottomNavTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Bottom navigation routes to Agent and no longer shows Statistics
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentBottomNavTest#agentReplacesStatisticsTab`; inside the test assert no `统计` item exists, tap `Agent`, and assert the chat shell is visible.
    Expected: BUILD SUCCESSFUL and Espresso proves shell navigation matches the new IA
    Evidence: .sisyphus/evidence/task-7-agent-bottom-nav.txt

  Scenario: Quick-add startup remains manual and bypasses Agent
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentBottomNavTest#quickAddStartupStillOpensAddRecord`; enable the quick-add preference in setup, launch the activity, and assert `AddRecordFragment` is shown first.
    Expected: BUILD SUCCESSFUL and Espresso proves Agent does not swallow the manual bookkeeping shortcut
    Evidence: .sisyphus/evidence/task-7-quick-add-startup.txt
  ```

  **Commit**: YES | Message: `feat(shell): replace statistics with agent tab` | Files: `app/src/main/java/com/example/cardtally/MainActivity.kt`, `app/src/main/res/menu/bottom_nav_menu.xml`, `app/src/main/res/drawable/...agent icon...`, `app/src/androidTest/java/com/example/cardtally/AgentBottomNavTest.kt`

- [ ] 8. Harden add/agent regressions, failure states, and manual fallback guarantees

  **What to do**: Finish the combined redesign with regression hardening across the full manual+Agent chain: no-category state on `记一笔`, no-asset state, provider failure on Agent, repeated execution attempts, and destructive write rollback/failure paths. Add any compatibility fixes needed so manual pages, existing edit flows, and Agent coexist safely, but do not expand the feature set.
  **Must NOT do**: Do not add provider-specific production polish, do not broaden Agent into analytics/reminders beyond the chosen conversation scope, and do not re-open product decisions.

  **Recommended Agent Profile**:
  - Category: `deep` - Reason: this is the final cross-surface hardening pass tying the architecture together.
  - Skills: [`requesting-code-review`] - Reason: the goal is confidence, safety, and scope fidelity.
  - Omitted: [`brainstorming`] - Reason: no new behavior should be invented at this stage.

  **Parallelization**: Can Parallel: NO | Wave 2 | Blocks: Final Verification | Blocked By: 1, 4, 6, 7

  **References** (executor has NO interview context - be exhaustive):
  - Pattern: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt:126` - Category loading and empty-state risk surface.
  - Pattern: `app/src/main/java/com/example/cardtally/AddRecordFragment.kt:147` - Asset loading and empty-state risk surface.
  - Pattern: `app/src/main/java/com/example/cardtally/EditRecordFragment.kt:148` - Existing edit flow must remain compatible with add/agent hardening.
  - Pattern: `app/src/main/java/com/example/cardtally/agent/...` - Provider unavailable, execution failure, and retry/idempotency paths from tasks 5-6.
  - External: `docs/plans/2026-03-26-cardtally-agent-capabilities.md:35` - Agent is enhancement-layer only; manual product must still stand on its own.

  **Acceptance Criteria** (agent-executable only):
  - [ ] `记一笔` handles empty category and empty asset datasets without crashing and with clear user feedback.
  - [ ] Agent provider failure leaves manual pages fully usable and does not corrupt local history/audit state.
  - [ ] Re-confirming the same Agent proposal does not duplicate mutations.
  - [ ] Failed destructive Agent writes leave domain data unchanged and record a failed audit state.
  - [ ] `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AddRecordFragmentTest,com.example.cardtally.AgentFragmentTest,com.example.cardtally.AgentExecutionTest,com.example.cardtally.AgentBottomNavTest` passes.

  **QA Scenarios** (MANDATORY - task incomplete without these):
  ```
  Scenario: Agent provider failure preserves manual app usability
    Tool: Bash
    Steps: Run `./gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.cardtally.AgentFragmentTest#providerFailureDoesNotBreakManualFlows`; force provider failure, exit Agent, open `记一笔`, save a manual record, and assert success.
    Expected: BUILD SUCCESSFUL and Espresso proves the enhancement layer cannot break the base product
    Evidence: .sisyphus/evidence/task-8-agent-failure-manual-fallback.txt

  Scenario: Repeated execution attempt does not create duplicate mutations
    Tool: Bash
    Steps: Run `./gradlew.bat testDebugUnitTest --tests "com.example.cardtally.AgentExecutionPipelineTest.reconfirmDoesNotDuplicateMutation"`
    Expected: BUILD SUCCESSFUL and tests prove idempotency holds even on retry or duplicate confirm actions
    Evidence: .sisyphus/evidence/task-8-agent-reconfirm.txt
  ```

  **Commit**: YES | Message: `test(add-agent): harden regressions and failure handling` | Files: `app/src/androidTest/java/com/example/cardtally/AddRecordFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/AgentFragmentTest.kt`, `app/src/androidTest/java/com/example/cardtally/AgentExecutionTest.kt`, `app/src/androidTest/java/com/example/cardtally/AgentBottomNavTest.kt`, `app/src/test/java/com/example/cardtally/AgentExecutionPipelineTest.kt`, related implementation files as needed

## Final Verification Wave (MANDATORY - after ALL implementation tasks)
> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.
> **Do NOT auto-proceed after verification. Wait for user's explicit approval before marking work complete.**
> **Never mark F1-F4 as checked before getting user's okay.** Rejection or user feedback -> fix -> re-run -> present again -> wait for okay.
- [ ] F1. Plan Compliance Audit - oracle
- [ ] F2. Code Quality Review - unspecified-high
- [ ] F3. Real Manual QA - unspecified-high (+ playwright if UI)
- [ ] F4. Scope Fidelity Check - deep

## Commit Strategy
- Commit 1: `test(agent): bootstrap add-agent coverage foundation`
- Commit 2: `refactor(core): extract shared ledger capability layer`
- Commit 3: `feat(core): harden stable references and audit persistence`
- Commit 4: `feat(add): rebuild add record flow`
- Commit 5: `feat(agent): add agent shell and conversation persistence`
- Commit 6: `feat(agent): add confirmation and execution pipeline`
- Commit 7: `feat(shell): replace statistics with agent tab`
- Commit 8: `test(add-agent): harden regressions and failure handling`

## Success Criteria
- `记一笔` remains a fast manual-entry flow with one clear primary action.
- Agent exists as an independent tab without swallowing the manual product chain.
- Full-write Agent actions are auditable, idempotent, and do not leave partial ledger state.
- Manual and Agent-triggered mutations share the same business capability layer.
- The app remains functional when the Agent provider is unavailable.
