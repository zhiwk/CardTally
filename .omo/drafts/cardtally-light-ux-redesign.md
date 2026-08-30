---
slug: cardtally-light-ux-redesign
status: plan-written-review-inconclusive
intent: clear
review_required: true
plan_path: .omo/plans/cardtally-light-ux-redesign.md
plan_sha256: null
review_round_id: review-binding-windows-inconclusive-20260812
pending-action: review blocked because this Windows runtime cannot guarantee descriptor-relative no-follow artifact binding
review:
  momus:
    status: inconclusive
    workspace_root: C:\Users\LiDru\Desktop\Data\Work_Space\CardTally
    runtime_home: null
    target: .omo/plans/cardtally-light-ux-redesign.md
    round_id: review-binding-windows-inconclusive-20260812
    plan_sha256: null
    launch_id: null
    session: null
    result: not launched; prerequisite artifact binding could not be established safely
  independent:
    status: inconclusive
    workspace_root: C:\Users\LiDru\Desktop\Data\Work_Space\CardTally
    runtime_home: null
    target: .omo/plans/cardtally-light-ux-redesign.md
    round_id: review-binding-windows-inconclusive-20260812
    plan_sha256: null
    launch_id: null
    session: ses_009c28351ffe5N8az9fA4JUcbT
    result: INCONCLUSIVE; Windows runtime could not guarantee descriptor-relative no-follow traversal, so no SHA-256 was asserted
approach: Redesign and implement CardTally as a single light-only, black-white-gray, card-led Android UX; permit layout, page, style, navigation-presentation, and interaction changes while preserving validated bookkeeping behavior and local-first data rules; remove dark and follow-system runtime capabilities, migrate existing theme preferences safely, and keep any auxiliary colors pale and low-saturation.
---

# Draft: cardtally-light-ux-redesign

## Components (topology ledger)
<!-- id | outcome (one line) | status: active|deferred | evidence path -->
1 | One light-only black-white-gray card design system, token contract, primitives, and state language | active | app/src/main/res/values/colors_light.xml; app/src/main/res/values/styles*.xml; user-approved direction supersedes the old “静奢理财日记” guideline
2 | Coherent app shell, conditional five-destination navigation, Home, and secondary-page navigation states | active | app/src/main/java/com/example/cardtally/MainActivity.kt; HomeFragment.kt; app/src/main/res/layout/activity_main.xml; fragment_home.xml
3 | Complete Ledger and shared Add/Edit Record UX, including period, chart, keyboard, and leaf-category picker states | active | StatisticsFragment.kt; AddRecordFragment.kt; EditRecordFragment.kt; fragment_statistics.xml; fragment_add_record.xml; bottom_sheet_record_*.xml
4 | Complete Assets UX, including add/edit shared baseline, asset records, archive, empty, and destructive states | active | AssetFragment.kt; AddAssetFragment.kt; EditAssetFragment.kt; AssetRecordsFragment.kt; ArchivedAssetsFragment.kt
5 | Complete optional persistent multi-session AI Assistant UX, including drawer, composer, streaming, partial, and failure states | active | AgentFragment.kt; AgentChatAdapter.kt; AgentSessionAdapter.kt; fragment_agent.xml
6 | Complete Me/settings UX and secondary configuration/category surfaces, plus accessibility/state matrices and Android handoff | active | SettingsFragment.kt; ThemeSettingsFragment.kt; AiAssistantSettingsFragment.kt; CategoryManageFragment.kt; values*/strings*.xml

## Open assumptions (announced defaults)
<!-- assumption | adopted default | rationale | reversible? -->
“Only one UX, light” applies to both design and runtime | Remove Dark and Follow System choices and normalize every existing theme preference to Light | The owner explicitly superseded the former three-theme product rule | no
Delivery target | Create a new Stitch project named CardTally, repository design/handoff package, and full Android implementation plan | No CardTally Stitch project exists, and the owner explicitly authorized page/layout/style/design changes plus runtime theme removal | yes
Information architecture | Preserve the current five structural primary destinations, with Assets and AI conditionally visible; keep Add contextual and secondary pages navigation-hidden | Verified by MainActivity.kt and bottom_nav_menu.xml; avoids inventing a new product structure | yes
Visual direction | Replace “静奢理财日记” with a simple black-white-gray, card-led light system; auxiliary colors may appear only as pale low-saturation tints such as light red or light cyan | Explicit owner decision supersedes the previous brand guideline | no
Surface strategy | Use cards for meaningful grouping and hierarchy, with a restrained radius/elevation scale; do not wrap every label or row in a card | Preserves the requested card-led character without producing nested-card clutter | yes
Design-system gate | Produce a canonical root DESIGN.md and a primitive/state showcase before product screens | Existing DESIGN.md files are historical Stitch exports, not a current project-wide contract | yes
State coverage | Design every state that can occur in current behavior; do not fabricate unsupported loading/retry behavior as implemented | Prevents static-happy-path UX while keeping implementation fact distinct from design specification | yes
Accessibility baseline | Android 48dp touch targets, text/non-color semantic cues, TalkBack names/roles/states/actions, large text and bilingual stress cases | Official Android guidance and current bilingual resources require it | yes
Testing strategy | Tests-after for theme migration and navigation/resource behavior; agent-executed Stitch inspection, Android unit/instrumentation tests, serial Gradle verification, and visual QA for the implemented light-only UI | The revised scope includes product-code implementation and removal of runtime modes | yes

## Findings (cited - path:lines)
- Product is native Android Kotlin + Fragment + XML + Material Components with local SQLite; architecture must not be replaced: README.md:17-25; AGENTS.md:37-47.
- Current top-level shell routes Home, Ledger (`StatisticsFragment`), Assets, AI Assistant, and Me, while secondary pages hide navigation: app/src/main/java/com/example/cardtally/MainActivity.kt:57-89 and current-snapshot.md:74-83.
- Assets and AI tabs are conditional, so the generated shell needs visible/hidden variants rather than assuming five tabs always appear: MainActivity.kt (`updateBottomNavigationVisibility`).
- Add/Edit Record share `fragment_add_record.xml`; Add/Edit Asset share `fragment_add_asset.xml`: current-snapshot.md:43-46,119-129.
- Records can select leaf categories only, and category hierarchy can be arbitrarily deep: current-snapshot.md:86-101,119-129; AGENTS.md:40-44.
- Agent is optional BYOK persistent multi-session text chat with streaming/non-streaming and partial-error retention, not an automation agent: README.md:15; current-snapshot.md:53-71; AGENTS.md:43-46.
- The only confirmed page-level historical design spec is Home; other old Stitch outputs are references, not current implementation truth: docs/design/guidelines/README.md:18-27; docs/design/assets/README.md.
- Existing “静奢理财日记” guidance is now superseded by the owner for this redesign and must be rewritten or archived so it cannot silently steer Stitch or implementation.
- Existing repository has no current root design system; found DESIGN.md files are under archived/reference Stitch exports.
- No existing owned Stitch project matches CardTally; a new project is required.
- Official Android guidance supports 3-5 compact-window primary destinations, requires at least 48dp targets, and prohibits relying on color alone; five tabs remain a repository-driven product fact rather than a universal standard.
- Current product supports Light/Dark/System and normalizes legacy values, but the owner has explicitly decided to remove Dark and Follow System. Runtime migration must normalize existing `THEME_DARK`, `THEME_SYSTEM`, and invalid values to Light without clearing unrelated preferences: ThemeHelper.kt; ThemeSettingsFragment.kt; MainActivity.kt; MainActivityThemeApplicationTest.kt; ThemeHelperTest.kt.
- Dormant `SearchFragment`, `CategoriesFragment`, and stale `bottom_sheet_ledger_mode.xml` are excluded unless an active caller is restored; active Ledger mode uses a PopupMenu.

## Decisions (with rationale)
- Treat this as an end-to-end design-generation plus Android implementation project.
- Create exactly one coherent light visual language and remove runtime dark/system capabilities.
- Replace the previous brand direction with black, white, gray, and optional pale low-saturation semantic tints.
- Permit page, layout, style, component, and navigation-presentation redesign while preserving validated bookkeeping behavior, reachability, and local-first data rules.
- Generate families and states, not one screenshot per page: shell/Home, Ledger/record entry, Assets, Agent, and Me/settings.
- Make the design contract implementation-ready by mapping each screen and primitive to active Fragment/layout/adapter/resource paths.
- Use Stitch outputs as visual artifacts only; reconcile them against source before acceptance.
- Automatically run dual high-accuracy plan review after approval because the brief was open-ended and defaults were adopted.
- Global card treatment: gray-scale outlined cards — white/light-gray surfaces, fine gray borders, and only very light shadows.
- Global density: comfortable-balanced — preserve breathing room without reducing useful records/settings per screen.
- Global navigation: floating card navigation — rounded white/light-gray shell with a dark-gray capsule or pale selected surface; Assets and AI remain conditionally visible.
- Home focal point: a concise financial overview sentence appears before the numeric summary.
- Home summary customization: retain all four summary-card layouts discussed (one-primary/two-secondary, three-equal, split double-card, multiple small cards) and expose a user preference in Settings to switch among them; default still needs confirmation.
- Home recent records: show every record from today rather than a fixed latest-three subset; long-day behavior still needs confirmation.
- Home AI entry: remove the duplicate Home AI card/shortcut; AI remains reachable through its conditional bottom-navigation destination.
- Home default summary layout: first-run and migrated users start with one-primary/two-secondary.
- Home record overflow: today’s records continue in the page’s natural scroll, without an inner list scroll or arbitrary item cap.
- Home overview sentence: expose all discussed source modes in Settings (rule-generated, fixed greeting, user-authored, and AI-generated), pending explicit resolution of the AI-automation boundary.
- Ledger default: open on Details for first-run/unconfigured users; Settings may override with Statistics or Remember last view.
- Ledger structure: keep Statistics and Details as same-page segmented views.
- Ledger filters: use a compact top filter strip with deeper filters available from the secondary affordance.
- Ledger chart: use restrained ring chart plus numeric summary as the primary statistics visualization.
- Record entry: use an in-page numeric keypad with a prominent amount display.
- Record fields: use a vertical information card for date, asset, leaf category, and note.
- Record save: use a bottom-fixed primary action that respects keyboard and system insets.
- Record categories: use an expandable tree drawer; parents are not selectable and only leaves can be chosen.
- Assets: lead with total assets followed by account cards.
- Asset cards: show only name, type, and balance; tapping opens asset records.
- Asset creation/editing: use a dedicated secondary page with the shared card-form baseline and bottom save action.
- Asset account entry: tapping an active account opens its associated records by default.
- Agent empty state: concise welcome with composer, without turning the page into a feature dashboard.
- Agent messages: conventional left/right bubbles with pale low-saturation fills and clear sender distinction.
- Agent sessions: open the history drawer from a top menu button.
- Agent availability: hide the Agent bottom-navigation destination when MiniMax is not configured, rather than showing an unusable page.
- Me structure: top personal card followed by grouped settings cards.
- Theme settings: remove the theme settings entry entirely because the only runtime theme is Light.
- Home preferences: create one secondary Home Preferences page for summary-card layout and overview-sentence source.
- Settings cards: group Preferences, Management, AI, and About into separate outlined cards rather than one card per row.
- Category management: render each top-level category as a grouped card with indented descendants.
- Category actions: place add-child/manage actions at the bottom of each category card; retain row-level edit/delete where needed.
- Category icons: preserve existing icon selection but reduce visual weight using grayscale or pale fills.
- Home overview sources: support local rules, fixed greeting, user-authored sentence, and user-triggered MiniMax AI overview; the AI option requires privacy disclosure, configuration gating, request/loading, failure, cancellation, and no-network states.
- Home overview default source: local rules; all four sources remain selectable in Home Preferences.
- Home Preferences layout choice: four live thumbnail preview cards with single selection.
- Asset form: lead with balance and use a vertical field card for name/type and related inputs.
- Asset records: show an asset summary header followed by its associated record list.
- Archived assets: use a dedicated secondary list page with restore and delete actions where supported by current behavior.
- AI configuration: use one configuration card with status plus API Key, model, and complete request URL fields.
- API Key: masked by default with an eye-toggle reveal action.
- AI overview privacy: show an inline explanation beneath the AI source option in Home Preferences, naming the local data sent to MiniMax.
- Global empty states: concise explanation plus one primary action, with restrained pale icon/illustration.
- Global failures: inline error card with explanation and retry, with Snackbar only as supplemental feedback.
- Global loading: shape-matched card skeletons; AI uses a lightweight generating state.
- Destructive actions: use swipe-to-delete with a short-lived undo affordance instead of pre-delete confirmation dialogs.
- Motion: use card-like slide-in transitions for secondary pages and drawers.
- Record keypad: keep the numeric keypad fixed at the bottom, with income/expense switching in the amount area above it.
- Language switch: smoothly rebuild the current page with a short fade/transition while preserving current input/state where possible.
- Card radius: use a medium 12–16dp system-wide range, with tighter inner controls.

## Scope IN
- New CardTally Stitch project and one light-only black-white-gray card design system.
- Canonical DESIGN.md with light tokens, typography, spacing, shape, depth, motion, accessibility constraints, primitives, and accepted debt.
- Primitive/state showcase before final screens.
- All verified active primary, secondary, modal, sheet, popup, drawer, keyboard, empty, populated, disabled, validation, destructive, AI loading/error, conditional-visibility, and bilingual stress states.
- Stitch screen families for Shell/Home, Ledger/Add/Edit Record, Assets, Agent, Me/settings.
- UX surface inventory, transition map, state matrix, accessibility matrix, screen-to-source traceability, token-to-Android-resource mapping, and design QA evidence.
- Android implementation across active layouts, styles, drawables, adapters, Fragments, strings, accessibility resources, and navigation presentation as required by the approved design.
- Theme migration: Light becomes the only effective mode; Dark/System settings UI and runtime branches are removed; stored legacy values safely normalize to Light.
- Removal or neutralization of `values-night` overrides and dark/system resources after reference-safe cleanup, with tests proving system night mode cannot alter the light UI.
- Rewrite current README, AGENTS, collaboration snapshot/entrypoints, and design guidelines so they no longer claim three theme modes or “静奢理财日记.”

## Scope OUT (Must NOT have)
- No dark design variant and no runtime dark/follow-system option.
- No Room, Compose, Navigation Component, cloud dependency, or architecture migration.
- No new budgets, reminders, exports, search entry, AI bookkeeping actions, automation, audit logs, or other mock-only features.
- No parent-category selection for records and no change to local-first data behavior.
- No revival of dormant/stale screens unless active routing is separately restored.
- No continuation of the old “静奢理财日记” visual identity.
- No high-saturation accent colors, colorful dashboard treatment, decorative gradients, or nested cards around every element.
- No pixel-copying historical Stitch screens as product truth.

## Open questions
The owner explicitly requested page-by-page design discussion. Interview order: global shell/design language → Home → Ledger → Add/Edit Record and sheets → Assets and asset secondary pages → AI Assistant → Me/settings/category/configuration → cross-page states and verification.

## Approval gate
status: approved-plan-written-review-inconclusive
The user approved the complete page-by-page scheme. The plan was written. Mandatory dual high-accuracy review could not start because the Windows runtime cannot satisfy the required descriptor-relative no-follow artifact-binding prerequisite; this is recorded as INCONCLUSIVE, not approval. Execution remains separate via `$start-work cardtally-light-ux-redesign`.
