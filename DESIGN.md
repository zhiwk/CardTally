# CardTally Design System

> Status: canonical implementation contract for the neutral card UX and light/dark appearance
> Authority: `docs/requirements/decisions/2026-08-12-light-ux-redesign.md`, with appearance superseded by `docs/requirements/decisions/2026-10-03-dark-appearance.md`
> Picture appearance extension: `docs/requirements/decisions/2026-10-05-wallpaper-appearance.md`
> Scope: Android Views, XML, Material Components, and Stitch generation
> Supersedes for current design work: historical Stitch exports, warm purple editorial styling, and the former three-theme visual direction

## 1. Atmosphere & Identity

The launcher display name is **小猫记帐** in both supported locales; Debug retains the **(Dev)** suffix to distinguish the two installations. Its approved icon is the white-background black-and-white round-faced kitten holding a ledger (`docs/design/assets/app-icon/cardtally-app-icon-v6-round-cat.png`), with no lettering. The packaged artwork remains unchanged; adaptive icons use a white background and a 13% inset on every foreground edge for launcher masks. Legacy launchers use the same artwork scaled to their icon bounds. Internal CardTally identifiers and application IDs remain stable.

### Current visual baseline (2026-09-06)

The owner's latest Ledger / Record-entry references supersede the older outlined-card and shadow defaults below. For light pages use `background_light` (`#EEEEEE`) as the canvas and opaque `surface_light` (`#FFFFFF`) for neutral working cards. Dark pages resolve the same resource names through `values-night`. Standard group radius is 12dp; the established prominent record-entry form may retain 20dp. Neutral cards and bottom action/navigation bars have no stroke and no elevation. `bg_card_surface` is the shared drawable for plain rounded cards; do not use alpha fills or background tints to simulate white.

Use system sans-serif, compact bold 22sp page/sheet titles, 16dp mobile gutters, and consistent grouped rows. Preserve input boundaries, internal separators, pressed/focus feedback, selected markers, and semantic income/expense/transfer colors. These are functional cues, not decorative card edges. Floating actions and modal scrims remain distinct from neutral cards. Existing navigation and data behavior are unchanged by this visual pass.

The user-selected Picture background appearance places the supplied image behind page canvases, centered and cropped to the available screen. Appearance is organized into two 12dp neutral cards, Solid background and Picture background, separated by 16dp. Each card offers Light, Dark and Follow system as 48dp minimum radio rows. Only one of the six rows is selected across both cards; choosing any picture row activates that background. Picture background defaults to Dark for existing users and Follow system resolves Android night mode. Light uses a `#C0FFFFFF` scrim and white `#FFFFFF` neutral cards; Dark uses a `#C0121416` scrim and `#1D2024` cards. Following the owner's transparency request, neutral page cards resolve `?attr/cardSurfaceColor` with user-selected opacity. The default is 80% (`#CCFFFFFF` / `#CC1D2024`); Settings → Appearance provides a 0–100% slider in 5% steps, applying and saving on release while retaining the appearance page scroll position through recreation. Text, amounts and icons retain full opacity. Card children remain transparent where the parent already supplies a fill, avoiding stacked surfaces. Ledger date groups paint the neutral fill once in the outer card; their record rows use transparent backgrounds, while standalone records paint their own fill and selected rows retain the opaque highlight. Inputs, modal surfaces, navigation, selection highlights and archived swipe rows retain their opaque backgrounds. Category management keeps translucent cards, but its edit/delete actions remain invisible when closed and are clipped to the uncovered area as the card moves; covered actions must never show through the card. Drag and settle animation share this clipping, and recycled rows reset to the closed state. No blur is introduced. Other appearances resolve the card token to the original opaque `surface_light`. Page canvases resolve `?attr/pageBackgroundColor`: the usual `background_light` token in other modes, transparent in Picture background. Window, modal, input and navigation backgrounds retain the opaque colors of the selected palette. Picture palette changes carry the currently displayed slider value, using one shared opacity across Light, Dark and Follow system. Plain surfaces remain opaque; switching back to pictures keeps the latest value. Import temporarily disables all six choices.

Appearance provides a local picture library below the rounded 180dp centerCrop preview. Three packaged defaults labelled Default picture 1, 2 and 3, existing custom and newly imported pictures appear in a horizontal RecyclerView with a fixed 16dp inset on both sides of its viewport, 104dp square rounded thumbnails, readable labels and a 2dp primary outline plus Selected text for the active image. Thumbnail decoding runs off the UI thread with bounded sampling and a small cache; recycled views cancel stale loads, and leaving the page closes workers. Recreation restores the gallery scroll offset after its asynchronous data load instead of scrolling back to the selected picture. Add picture and Restore default picture share a row below the library, each at least 48dp tall, and can grow vertically for large text. Each successful import adds a separately stored app-private image and activates Picture background; cancelling or failing preserves earlier images and selection. Choosing a thumbnail switches the background; restoring default keeps all imported pictures available. Only imported pictures, including the legacy custom picture, have a 16dp top-end × icon centered on a 24dp opaque neutral circle, inset 4dp from the top and end edges, within a 48dp touch target; all three packaged defaults never show this action and are also protected by the storage layer. Defaults are always listed directly from package resources, keeping their existing IDs and fixed order. Former suggestion copies and seed markers are ignored, so previously deleted packaged suggestions return as defaults without changing the active selection. Each delete button has an accessible name identifying the picture. Deletion responds independently from picture selection; deleting the active custom image selects Default picture 1. Restore default picture also selects Default picture 1. Deletion preserves the active background mode, palette and opacity, using the existing view state restoration after refresh. These actions preserve the current palette and opacity. Import or selection shows a stable busy label and temporarily disables appearance controls while processing off the UI thread. No broad photo-library permission is requested.

CardTally is simple, neutral, structured, calm, and card-led. It should feel like a clear personal ledger arranged on a bright desk: white working surfaces, black financial figures, quiet gray structure, and pale color only when meaning requires it. The signature is the **outlined financial card**: a white or whisper-gray surface with a fine cool-gray edge, a very light downward shadow, medium outer corners, and disciplined information grouping. Cards mark a meaningful summary, entity, or control group; rows inside a group remain rows rather than becoming nested cards. This is not the former “静奢理财日记” identity, not a dashboard template, and not a decorative lifestyle surface.

### Product principles

1. **Meaning before decoration.** A border, card, tint, icon, or animation must explain hierarchy, state, or action.
2. **Numbers are stable.** Financial values align, scan, and update without width jitter through tabular figures and consistent sign placement.
3. **Calm does not mean sparse everywhere.** Overview surfaces breathe; workflow surfaces compress responsibly while retaining 48dp actions and readable text.
4. **One system, two densities.** The same tokens and primitives serve overview and dense workflow profiles; density never creates a second visual language.
5. **Local-first trust is visible.** Copy stays direct and factual. AI offers optional conversation and native, user-confirmed record proposals under `2026-10-04-ai-record-tools.md`; never visualize it as magic, unconfirmed automation, or a financial authority.

### Anti-patterns

- No purple or blue AI gradients, decorative gradients, broad glass, neon, glow, or high-saturation color.
- No warm-purple editorial identity, serif display treatment, ornamental quotes, or paper-journal metaphors.
- No card around every row, label, chart legend, filter, or message fragment.
- No equal-card dashboard grid used by default; hierarchy determines card size and placement.
- No emojis. Use one coherent Android vector icon family with rounded 2dp strokes.
- No color-only meaning. Pair semantic color with a sign, icon, label, shape, or state text.
- No fake loading for synchronous SQLite content.

## 2. Color

The runtime offers Light, Dark, Follow system, and Picture background in Settings → Appearance, with Light as the default. Follow system resolves the palette from Android night mode; Picture background uses a separately saved Light, Dark or Follow system palette with the selected image as its canvas; its default remains Dark. Both palettes use the same semantic resource names; the historical `_light` suffix remains for compatibility. The neutral ramp is cool and nearly achromatic. Pale cyan is the interaction and informational accent; pale red is destructive and expense-related. Other status colors are pale containers with dark readable foregrounds, never brand colors.

### Light neutral palette and Android mappings

| Role | Design token | Value | Android resource mapping | Usage |
| --- | --- | --- | --- | --- |
| Canvas | `color.canvas` | `#F7F7F8` | `@color/background_light` | Window and page background |
| Surface | `color.surface` | `#FFFFFF` | `@color/surface_light`, `@color/surface_container_lowest` | Cards, sheets, dialogs |
| Surface subtle | `color.surface.subtle` | `#F2F3F4` | `@color/surface_container_low` | Segments, grouped row backgrounds, skeleton base |
| Surface muted | `color.surface.muted` | `#E9EAEC` | `@color/surface_container` | Disabled controls, stronger grouping |
| Surface pressed | `color.surface.pressed` | `#E1E3E6` | `@color/surface_container_high` | Pressed neutral control |
| Ink | `color.ink` | `#17181A` | `@color/onBackground_light`, `@color/onSurface_light` | Primary text, filled primary actions |
| Ink secondary | `color.ink.secondary` | `#565B61` | `@color/onSurfaceVariant_light` | Supporting text and metadata |
| Ink tertiary | `color.ink.tertiary` | `#747A81` | `@color/editorial_text_muted` | Captions; minimum contrast is still checked at use size |
| Ink disabled | `color.ink.disabled` | `#9AA0A6` | `@color/disabled_light`, `@color/placeholder_light` | Disabled labels and placeholders |
| Border | `color.border` | `#D5D8DC` | `@color/outline_light`, `@color/editorial_outline` | 1dp card and control outline |
| Border strong | `color.border.strong` | `#AEB3B9` | `@color/inputStroke_light` | Focus-neutral fallback and dividers needing emphasis |
| Border subtle | `color.border.subtle` | `#E5E7E9` | `@color/outlineVariant_light`, `@color/menuDivider_light` | Internal row separators |
| Scrim | `color.scrim` | `#5217181A` | `@color/dialogOverlay_light`, `@color/overlay_light` | Modal backdrop; no blur required |

### Interaction and semantic palette

| Role | Design token | Container / foreground | Android resource mapping | Rule |
| --- | --- | --- | --- | --- |
| Primary action | `color.action.primary` | `#17181A` / `#FFFFFF` | `@color/buttonPrimary_light`, `@color/onPrimary_light` | Save, confirm, active high-priority action |
| Cyan selected | `color.accent.cyan` | `#DDF3F4` / `#164E52` | `@color/primaryContainer_light`, `@color/onPrimaryContainer_light` | Selection, focus halo, informational emphasis |
| Cyan strong | `color.accent.cyan.strong` | `#1E6267` | `@color/primary_light`, `@color/inputStrokeFocused_light` | Text/icon/focus edge on light surfaces |
| Expense/destructive | `color.semantic.expense` | `#F9E4E3` / `#7A2926` | `@color/expense_background`, `@color/expense_primary` | Expense sign/icon and destructive treatment |
| Income/success | `color.semantic.income` | `#E2F1E8` / `#285C3D` | `@color/income_background`, `@color/income_primary`, `@color/success_container`, `@color/success_primary` | Income and confirmed success |
| Warning | `color.semantic.warning` | `#F6EDD7` / `#6A511C` | `@color/warning_container`, `@color/warning_primary` | Caution requiring review |
| Error | `color.semantic.error` | `#F9E4E3` / `#7A2926` | `@color/error_container`, `@color/error_primary`, `@color/error_light` | Validation, request failure, destructive action |
| Information | `color.semantic.info` | `#DDF3F4` / `#164E52` | `@color/info_container`, `@color/info_primary` | Privacy and neutral system information |
| Focus halo | `color.focus` | `#B8E4E6` | `@color/focus_ring_light` (new) | 2dp exterior focus ring plus 1dp surface gap |

### Dark palette (2026-10-03)

| Role | Android resource | Night value |
| --- | --- | --- |
| Canvas | `background_light` | `#121416` |
| Cards | `surface_light` | `#1D2024` |
| Primary text | `onSurface_light` | `#F1F3F5` |
| Supporting text | `onSurfaceVariant_light` | `#BEC3CA` |
| Primary action / foreground | `buttonPrimary_light` / `onPrimary_light` | `#E8EDF2` / `#17181A` |
| Selection / foreground | `primaryContainer_light` / `onPrimaryContainer_light` | `#183E43` / `#A8EEF5` |
| Expense / income | `expense_primary` / `income_primary` | `#FF8C83` / `#76D99A` |

Keep existing spacing, corner radii, typography, and account/category behavior across appearances. Bank and application brand artwork keeps its original colors and proportions. Use semantic resources for ordinary icons and surfaces; literal white/black is reserved for artwork, masks, and photo viewers.

### Color state rules

- Default cards use `color.surface` + `color.border`; selected cards add a 2dp `color.accent.cyan.strong` edge or selected marker and a `color.accent.cyan` surface, never tint alone.
- Pressed neutral surfaces use `color.surface.pressed`; pressed filled actions use `#303236`, mapped to new `@color/buttonPrimaryPressed_light`.
- Disabled controls use `color.surface.muted`, `color.border.subtle`, and `color.ink.disabled`; disabled status is exposed semantically.
- Error fields use a 1dp error edge, error icon, and inline error text. Focus and error can coexist: error remains the edge; focus adds the exterior halo.
- Body text contrast target is at least 4.5:1; large text and meaningful icons at least 3:1; disabled controls remain perceivable but are not treated as actionable.
- Extend this table before adding any color. Raw page-local color values are prohibited.

## 3. Typography

### Font stack

- **Android primary:** `sans-serif` using the platform Roboto/Noto Sans fallback chain. This is available on supported Android versions and robust across Simplified Chinese and English.
- **Android medium:** `sans-serif-medium` for emphasis without synthetic bold.
- **Financial numerals:** the same system sans with OpenType tabular figures (`fontFeatureSettings="tnum"`) where supported. Do not switch financial values to a narrow monospace face; alignment comes from tabular figures.
- **Stitch approximation:** Noto Sans for headline, body, and labels.
- Maximum one type family. No serif and no downloaded font dependency.

### Type scale and Android mappings

| Role | Size / line height | Weight | Tracking | Android resource mapping | Usage |
| --- | --- | --- | --- | --- | --- |
| Amount hero | 40sp / 48sp | 600 | `-0.01em` Latin, 0 CJK | `@style/TextAppearance.CardTally.AmountHero` (new) | Record entry and major balance only |
| Display metric | 32sp / 40sp | 600 | `-0.01em` Latin, 0 CJK | `@style/TextAppearance.CardTally.AmountLarge` | Total assets and major summaries |
| Page title | 24sp / 32sp | 600 | 0 | `@style/TextAppearance.CardTally.PageTitle` | Screen title |
| Section title | 20sp / 28sp | 600 | 0 | `@style/TextAppearance.CardTally.Title` | Major section heading |
| Card title | 17sp / 24sp | 600 | 0 | `@style/TextAppearance.CardTally.CardTitle` | Meaningful card heading |
| Body | 16sp / 24sp | 400 | 0 | `@style/TextAppearance.CardTally.Body1` | Default content and fields |
| Body medium | 15sp / 22sp | 500 | 0 | `@style/TextAppearance.CardTally.ListTitle` | Row title and button label |
| Supporting | 14sp / 20sp | 400 | 0 | `@style/TextAppearance.CardTally.Body2` | Secondary details and helper text |
| Caption | 12sp / 16sp | 500 | `0.01em` Latin, 0 CJK | `@style/TextAppearance.CardTally.Caption` | Metadata and concise labels |

### Numeric treatment

- Amounts use tabular figures, locale-aware grouping for committed display, a consistent currency prefix, and a reserved sign column where lists align amounts.
- Input follows the canonical ASCII decimal grammar in the preference/state contract; formatting never changes the edit buffer.
- Positive and negative meaning uses `+`/`−`, spoken income/expense labels, and semantic icons in addition to color.
- Large financial values may wrap only at semantic boundaries; do not ellipsize an amount. Reduce from Display metric to Section title before allowing horizontal overflow.

### Bilingual and enlarged-text rules

- Chinese text uses natural phrase boundaries. Do not orphan a one-character particle, split short subject/predicate clauses, or compress CJK with negative tracking.
- English labels wrap to two lines where needed. Buttons grow vertically instead of truncating the action.
- At 1.3x and 2.0x font scale, cards and rows grow in height; primary content does not clip, overlap, or horizontally scroll.
- All-caps is limited to short Latin-only metadata when localization explicitly supplies it. Do not transform Chinese or mixed labels.
- Body text never drops below 14sp. Caption at 12sp is reserved for nonessential metadata and still meets contrast requirements.

## 4. Spacing & Layout

### Base unit and Android mappings

All intentional spacing derives from **4dp**. Existing `2dp` spacing is not part of the new system.

| Token | Value | Android resource mapping | Usage |
| --- | --- | --- | --- |
| `space.1` | 4dp | `@dimen/spacing_xs` | Icon optical gap, skeleton inset |
| `space.2` | 8dp | `@dimen/spacing_s` | Tight row content and control gap |
| `space.3` | 12dp | `@dimen/spacing_m` | Compact padding and related metadata |
| `space.4` | 16dp | `@dimen/spacing_l` | Standard mobile gutter and card padding |
| `space.5` | 20dp | `@dimen/spacing_5` (new) | Overview card padding |
| `space.6` | 24dp | `@dimen/spacing_xl` | Major group separation and regular-width gutter |
| `space.8` | 32dp | `@dimen/spacing_xxl` | Overview section rhythm |
| `space.10` | 40dp | `@dimen/spacing_10` (new) | Large section transition |
| `space.12` | 48dp | `@dimen/spacing_xxxl` | Sparse focal separation only |

### Shape, size, and icon tokens

| Token | Value | Android resource mapping | Rule |
| --- | --- | --- | --- |
| `radius.card` | 12dp | `@dimen/card_corner_radius_medium` | Standard outer card |
| `radius.card.prominent` | 16dp | `@dimen/card_corner_radius_large` | Summary, sheet, dialog, navigation shell |
| `radius.control` | 10dp | `@dimen/control_corner_radius` (new) | Buttons, segmented controls, fields |
| `radius.control.compact` | 8dp | `@dimen/input_corner_radius_medium` | Inner controls and dense filters |
| `radius.bubble` | 12dp with one 4dp sender corner | `@dimen/chat_bubble_radius` (new) | Chat bubbles |
| `size.target.min` | 48dp | `@dimen/touch_target_min` | Every actionable target |
| `size.row.overview` | min 64dp | `@dimen/row_height_overview` (new) | Settings and overview entity rows |
| `size.row.dense` | min 52dp | `@dimen/row_height_dense` (new) | Ledger, picker, tree, sessions; target remains 48dp |
| `size.icon.inline` | 20dp | `@dimen/icon_size_inline` (new) | Rows and fields |
| `size.icon.standard` | 24dp | `@dimen/icon_size_medium` | Navigation and actions |
| `size.fab` | 56dp | `@dimen/fab_size_normal` | Primary floating action |

### Density profiles

#### Overview profile

- Applies to Ledger summary, Assets summary, and Me.
- Mobile gutter 16dp; regular-width gutter 24dp. Section gap 24–32dp. Card padding 20dp. Related rows inside a group use 64dp minimum height.
- One dominant summary may use the 16dp prominent radius. Secondary summary cards use 12dp. No more than two adjacent summary cards on regular-width layouts.
- Favor one clear focal sentence or amount, then concise supporting content. Keep action count visible and low.

#### Dense workflow profile

- Applies to Ledger rows, category trees and pickers, fixed keypad, Agent sessions, and long record lists.
- Gutter 12–16dp. Group gap 12–16dp. Dense row minimum 52dp with a 48dp interactive area. Row internal padding 12dp horizontal and 8dp vertical.
- Group multiple rows under one 12dp outlined container with subtle separators. Do not give each row its own card or shadow.
- Lists with 200+ records use RecyclerView virtualization, sticky or clearly repeated date headers, tabular trailing amounts, and stable row geometry. No staggered entrance for bulk data.

### Responsive and stress layout

- **Compact Android width:** one column, 16dp gutter, no horizontal scroll for primary content.
- **Regular/tablet width:** content max width 720dp for forms and lists; showcase boards may use two columns. Preserve one reading order.
- **Depth-50 categories:** show at most four visual indent steps at 12dp each. Beyond level four, hold the text column fixed and show a compact `L50` depth label plus a vertical ancestry rail and breadcrumb/path disclosure on demand. Parent/leaf affordances remain visible; parent nodes never masquerade as selectable leaves.
- **Long labels:** the title owns remaining width and may wrap to two lines. Trailing amount or action moves to a second aligned row before text is ellipsized.
- **Enlarged text:** columns collapse, segmented controls may wrap into a vertical radio-like group, dialog buttons stack, and keypad keys retain 48dp targets.
- Insets are consumed once by the activity shell. Fixed keypad, composer, Snackbar, FAB, and navigation shell never overlap system bars or each other.

## 5. Components

Every primitive uses design tokens above and Material Components or Android Views. “Applicable states” means the state is shown only when the component can genuinely enter it; synchronous SQLite lists do not receive fake loading.

### Navigation shell (3 / 4 / 5 destinations)
- **Structure:** one ordinary full-width bottom bar containing 3, 4, or 5 equal-width destinations; it is not a floating card and has no rounded bottom corners. Icons sit above short labels with a compact gap.
- **Variants:** the current shell has five destinations: Ledger, Statistics, Assets, Agent, and Me. Assets and Agent may be conditionally hidden by settings.
- **Density:** shared shell, flush to the bottom edge, 5dp bottom text inset, 0–4dp icon-to-label gap, and a minimum 48dp interactive height. No separate card per destination.
- **States:** default, pressed, focused, selected, disabled during guarded transitions. Selected uses cyan container plus icon/label weight and `selected=true`.
- **Accessibility:** `navigation` collection semantics; each item exposes destination name, selected state, and “activate” action; traversal order follows visual order. Target at least 48dp.
- **Motion:** selected marker fades and translates over 180ms; reduced motion swaps immediately with no travel.

### Top-level page standard and Ledger reference
- **Top-level page standard:** every primary destination is composed of two parts only: page content and the shared bottom navigation bar. Each destination Fragment owns its own page header and content structure; `MainActivity` owns only the page container and navigation shell.
- **Ledger reference status:** the Ledger page is the reference implementation for the top-level page shell, spacing tokens, header placement, scrolling behavior, summary surface, grouped records, and floating action placement. It is a standard-page example, not a template that requires Statistics, Assets, Agent, or Me to copy its content hierarchy.
- **Header behavior:** the Ledger account title, search, and calendar stay fixed above the month pager. Each month’s summary and grouped records scroll vertically together and move horizontally as one ViewPager2 page. Rightward swipes select the previous month; leftward swipes select the next month. Floating actions remain fixed. Other primary pages may use a different header arrangement when their information architecture requires it, while preserving the same header tokens and touch-target rules.

Statistics uses a two-page ViewPager2 for expense and income charts plus rankings. Period controls and the combined summary remain shared. The tab indicator follows continuous page progress, including drag cancellation; tab clicks use the same smooth paging path.

### Headers
- **Structure:** primary title, optional supporting line, leading Back/Menu action, and no more than two trailing actions.
- **Variants:** top-level, secondary/back, dense section header, date-group header.
- **States:** action default, pressed, focused, disabled; title itself is not interactive.
- **Accessibility:** heading semantics; icon buttons have localized action labels, never duplicate the visible title.

### Buttons
- **Structure:** text label with optional leading icon; progress replaces the icon without replacing the accessible label.
- **Variants:** primary black filled, secondary outlined, tertiary text, destructive pale-red outlined.
- **States:** default, pressed, focused, selected/toggle where applicable, disabled, loading, error completion where an async action owns the result.
- **Accessibility:** role Button; state and resulting action announced; 48dp minimum; loading announces “in progress” once and blocks duplicate activation.
- **Motion:** press scales to 0.98 for 100ms; loading/result content crossfades. Reduced motion uses color/state swap only.

### Floating action button
- **Structure:** 56dp black circular or 16dp rounded-square container with one 24dp vector icon.
- **Variants:** add record, add asset, add category.
- **States:** default, pressed, focused, disabled. No loading unless the FAB directly starts an async action.
- **Accessibility:** role Button and explicit verb-object label; never announce “plus.”

### Summary card
- **Structure:** concise label, primary tabular amount or sentence, up to three supporting metrics, optional one action.
- **Variants:** one-primary-two-secondary, two-by-two, primary-with-three-secondary, compact-list; status summary.
- **Profile:** overview.
- **States:** default, pressed/focused only if whole card opens a destination, selected in a settings preview, loading only for explicitly requested AI content, error, empty.
- **Accessibility:** one grouped summary description; individual values remain reachable when independently actionable. Income/expense announced in words and signs.

### Record group and record row
- **Structure:** one outlined group card per date/section; rows contain category icon, title/description, metadata, and aligned signed amount, separated by subtle lines.
- **Variants:** overview today row, dense Ledger row, asset-record row, swipe-revealed destructive action.
- **Profile:** overview rows on Ledger and Assets; dense elsewhere.
- **States:** default, pressed, focused, selected during multi-step action, swipe/reveal, disabled while deletion transaction resolves, empty at group owner. No per-row card and no fake loading.
- **Accessibility:** row role Button when editable; announces category, description, date/time, income/expense, amount, and available actions. Swipe action has an equivalent accessibility action.

### Asset card
- **Structure:** account icon, name, type, tabular balance, optional status/action row inside one meaningful entity card.
- **Variants:** active, archived, summary compact.
- **Profile:** overview for list, dense for archived management.
- **States:** default, pressed, focused, selected, archive pending, disabled/protected, empty at list owner, error inline when a safeguard rejects deletion.
- **Accessibility:** role Button for open; archive/restore/edit are separately named actions. Copy acknowledges name-based association limits where relevant.

### Settings group and settings row
- **Structure:** one outlined group card with section label outside or in the card header; rows use title, current value/help, optional icon, and trailing switch/chevron.
- **Variants:** navigation, switch, value/dialog, status.
- **Profile:** overview group; dense rows when the group is long.
- **States:** default, pressed, focused, selected/value active, disabled, inline error. No individual row cards.
- **Accessibility:** row exposes one unambiguous role. A switch row does not create a second duplicate click target; state and action are announced.

### Dense row and category tree row
- **Structure:** stable 52dp row, optional ancestry rail/depth label, icon, wrapping label, trailing expand/select/action affordance.
- **Variants:** parent expanded/collapsed, leaf unselected/selected, depth-overflow, protected, session row, picker row.
- **Profile:** dense workflow.
- **States:** default, pressed, focused, selected, disabled/protected, empty at tree owner, inline error for rejected operations.
- **Stress:** levels 0–4 indent visually; levels 5–50 keep text aligned and expose `Lx` plus ancestry. Long labels wrap; 200+ rows virtualize.
- **Accessibility:** tree/item role, level, expanded/collapsed, selected state, parent versus selectable leaf, and available action are announced. Parent tap expands; only leaf confirms record binding.

### Segmented control and filters

Category management expense/income tabs use the Statistics type-selector visual language: a single 12dp neutral `cardSurfaceColor` fill, transparent tab children, centered 16sp labels, bold `colorOnSurface` for the selected tab, `editorial_text_muted` for the other, and a centered 28dp × 2dp underline moving with native 180ms tab selection. Category tabs retain their 48dp touch height and native tab accessibility.

- **Structure:** one outlined 8–10dp container with two to five choices; compact filter buttons sit in one scroll-free wrapping cluster or open a deeper filter sheet.
- **Variants:** two-way record type, Ledger Statistics/Details, chart mode, period presets, filter trigger.
- **States:** default, pressed, focused, selected, disabled, error for invalid range.
- **Accessibility:** tablist/tabs for view switching; radio group for mutually exclusive values. Selection is text/shape/weight plus tint.
- **Motion:** selected marker translates 180ms using transform and fades; reduced motion swaps immediately.

### Fields
- **Structure:** label above or within a vertical field group, editable value, optional helper, inline icon/action, and inline error below.
- **Variants:** text, secret with reveal, amount display/input, read-only picker row, multiline composer.
- **States:** default, pressed for picker, focused, selected text, disabled, loading only for async validation if introduced, error, empty.
- **Accessibility:** role EditText/Button as behavior requires; label is programmatically associated; error is announced and remains visible; secret reveal announces current masked state and resets masked on recreation.

### Fixed keypad
- **Structure:** fixed in-page 3×4 grid for digits, decimal, zero, and Backspace; expense/income switch remains separate above it.
- **States:** key default, pressed, focused, disabled on rejected input boundary; amount error appears at amount field, not on every key.
- **Accessibility:** every key is at least 48dp; announces “Digit N,” “Decimal point,” “Delete last digit,” and long-click “Clear amount.” Hardware and accessibility input use the same reducer.
- **Motion:** key press uses surface change and 0.98 scale; reduced motion uses surface change only.

### Bottom sheet
- **Structure:** scrim, 16dp top-corner outlined surface, drag handle, title/description, scroll owner, optional fixed actions.
- **Variants:** date, asset, category, period/filter, generic picker.
- **States:** opening, default, focused child, selected row, empty, inline error; loading only for true asynchronous content.
- **Accessibility:** modal dialog semantics, initial focus on heading or selected value, focus trapped, Back closes, dismiss action named, focus returns to trigger.
- **Motion:** secondary surface slides upward 24dp while fading over 260ms; scrim fades over 180ms. Reduced motion uses a 120ms fade with no translation.

### Dialog
- **Structure:** scrim, 16dp outlined surface, heading, concise body/form, error region, and actions.
- **Variants:** confirmation, text entry, single choice, protected-action explanation.
- **States:** default, focused, selected choice, disabled confirm, loading only for async confirm, error.
- **Accessibility:** modal dialog role, heading relationship, trapped focus, ordered actions, destructive action named specifically, focus restored on close. Actions stack at enlarged text.

### Chat bubble
- **Structure:** sender label or accessible sender metadata plus one bubble; assistant aligns left on neutral surface, user aligns right on pale cyan; errors append a compact status line rather than a nested card.
- **Variants:** assistant, user, partial assistant, generating placeholder, error-retained partial.
- **States:** default, selected text, streaming/loading, partial, error. No decorative AI gradient.
- **Accessibility:** announces sender then message. Streaming updates are throttled and announce meaningful completion/status changes rather than every chunk.

### Session row
- **Structure:** dense row with title, updated metadata, selected marker, and optional long-press rename action.
- **States:** default, pressed, focused, selected, disabled while sending, empty session list, inline error.
- **Accessibility:** role Button; selected session and rename action announced. Long press has an equivalent custom accessibility action.

### Skeleton
- **Structure:** neutral blocks matching final geometry inside the owning card/row, never a generic spinner.
- **Applicability:** explicit AI overview/chat network work or another proven asynchronous load only.
- **States:** loading and reduced-motion loading.
- **Accessibility:** owning region announces loading once; skeleton shapes are hidden from TalkBack.
- **Motion:** 800ms low-contrast opacity pulse; reduced motion uses a static neutral fill.

### Inline error
- **Structure:** pale-red icon, concise error text, and one local recovery action when recoverable.
- **States:** error default, focused action, retry loading, resolved.
- **Accessibility:** live region for new errors; does not steal focus unless the current action cannot continue.

### Concise empty state
- **Structure:** optional 24dp outline icon, one plain heading, one sentence, and at most one action. It lives in the content region, not another card nested in an empty card.
- **Variants:** no records, no filtered results, no assets, no categories, no sessions/configuration.
- **Accessibility:** heading plus next action; distinguishes globally empty from filter-empty.

### Undo Snackbar
- **Structure:** dark neutral bar above navigation/keypad/composer with concise result text and one “Undo” action.
- **States:** shown, focused action, undo in progress, restored, expired/dismissed.
- **Accessibility:** polite live announcement; Undo is a 48dp action and announces restored result exactly once.
- **Motion:** translates upward 16dp and fades over 180ms; reduced motion fades only. Placement never obscures navigation or fixed input.

## 6. Motion & Interaction

Motion communicates input, selection, spatial origin, and completion. It never decorates static content. Android interpolators approximate the referenced beui.dev Button, Tabs, and Bottom Sheet mechanisms without adding a motion library.

| Token | Duration | Easing / Android mapping | Usage |
| --- | --- | --- | --- |
| `motion.press` | 100ms | fast-out-linear-in | Button/key press scale and surface |
| `motion.micro` | 140ms | linear-out-slow-in | Icon/content fade and focus halo |
| `motion.selection` | 180ms | fast-out-slow-in | Segment/nav selected marker |
| `motion.surface` | 260ms | emphasized decelerate | Sheet/card secondary-surface slide |
| `motion.scrim` | 180ms | linear | Scrim fade |
| `motion.skeleton` | 800ms cycle | ease-in-out opacity | Async skeleton only |

### Interaction rules

- Spatial motion uses `translation`, `scale`, and alpha only. Do not animate layout width, height, margin, padding, top, or left.
- Secondary pages and sheets use the signature card slide: translate from the navigation edge or 24dp below while fading into the outlined surface. It clarifies origin; it is not a page-wide flourish.
- Bulk lists do not stagger. Selection changes may animate the indicator, not every row.
- Every interaction is interruptible. Repeated taps do not queue animations or duplicate transactions.
- Pressed feedback begins immediately. Async actions enter loading within 100ms and retain a stable accessible label.
- **Reduced motion:** when Android animator duration scale is off or accessibility reduce-motion behavior is available, remove translation/scale and use a 120–140ms alpha/state swap. Skeleton becomes static. No task or information is lost.

## 7. Depth & Surface

### Strategy: mixed outline plus very light shadow

One lighting direction applies everywhere: light falls from top center, so shadows fall subtly downward. The outline carries structure; shadow only separates meaningful floating/elevated surfaces.

| Level | Recipe | Android resource mapping | Usage |
| --- | --- | --- | --- |
| Flat group | White or subtle surface + 1dp `color.border`, 0dp elevation | `CardTally.Card.Group` (new) | Grouped settings, dense row groups, filters |
| Resting card | White + 1dp `color.border` + 1dp visual downward shadow at ~5% black | `CardTally.Card.Resting` (new), 1dp elevation | Summary, asset, record date group |
| Floating control | White/ink + 1dp border + 2dp downward shadow at ~7% black | `CardTally.Card.Floating` (new), 2dp elevation | Navigation shell, FAB, Snackbar |
| Modal | White + 1dp `color.border.strong` + 6dp downward shadow at ~10% black | `CardTally.Card.Modal` (new), 6dp elevation | Sheet, dialog, drawer |

### Surface rules

- Outer cards use 12dp radius; prominent summary, navigation, sheet, and dialog surfaces use 16dp. Inner controls use 8–10dp.
- Never nest two fully outlined/elevated cards. A card’s internal hierarchy uses spacing, typography, pale surface blocks, or subtle separators.
- Dense rows share one group outline. Individual rows have no radius or shadow.
- Cyan/red status containers are flat inside the parent and do not gain independent shadow.
- No broad blur or glass. Scrims are translucent neutral color only.
- Shadows use the same neutral hue and downward direction. No colored shadow, halo, or glow.

## 8. Accessibility Constraints & Accepted Debt

### Inclusive personas and constraints

| Persona / context | Design requirement | Primitive-showcase proof |
| --- | --- | --- |
| Screen-reader user managing finances | Clear role, name, state, value, and action; logical traversal; no color-only meaning | Navigation, field error, tree parent/leaf, chat sender, Undo semantics are annotated |
| Low-vision user with enlarged text | 1.3x and 2.0x text reflow without clipping, overlap, or lost actions | Long bilingual labels, stacked actions, wrapping rows, amount fallback scale |
| Motor-impaired or situational one-handed user | Every action at least 48×48dp; swipe and long-press have equivalent actions | Keypad, row delete, session rename, FAB, sheet close |
| Motion-sensitive user | No essential transform motion; reduced-motion fade/static alternatives | Sheet/card slide, selected marker, Snackbar, skeleton alternatives |
| User reviewing 200+ records | Stable geometry, virtualization, date grouping, no decorative list motion | Dense record group pattern and explicit virtualization note |
| User navigating a depth-50 category tree | Bounded indentation, level/path disclosure, parent/leaf distinction | Levels 1–4 plus L50 rail/breadcrumb stress row |
| Bilingual Chinese/English user | Natural CJK breaks, two-line English labels, system font glyph coverage | Long Chinese and English labels in overview and dense profiles |

### Accessibility constraints

- Target WCAG 2.2 AA-equivalent Android outcomes: 4.5:1 body text, 3:1 large text and meaningful icons, visible focus, non-color state cues, and no flashing.
- All interactive targets are at least 48dp even when the visible icon is 20–24dp.
- TalkBack exposes role, name, state, value, and action. Collection position/level is exposed for navigation, lists, and trees.
- Focus enters modal surfaces predictably, remains inside them, and returns to the invoking control after close.
- Focus order follows reading order. Conditional navigation changes do not strand focus; unavailable active destinations return to Ledger.
- Charts require text equivalents and labeled values; color is secondary.
- Async status announcements are concise and throttled. Streaming text does not announce every chunk.
- Error, empty, loading, and disabled states use plain language and preserve a recovery path where recovery exists.
- Secrets, API keys, personal finance values, user text, and device identifiers must not appear in screenshots, logs, evidence, or content descriptions.

### Primitive showcase gate

Before any product screen is generated or implemented, one primitive/state board must demonstrate:

1. Neutral and semantic tokens, typography, spacing, radii, border, and shadow direction.
2. Navigation in 3/4/5 variants; headers; buttons/FAB; summary, record, asset, and settings structures.
3. Dense rows, segmented controls/filters, fields, fixed keypad, sheets, dialogs, chat bubbles, and session rows.
4. Default, pressed, focused, selected, disabled, loading, error, and empty states where applicable.
5. Skeleton, inline error, concise empty state, and Undo Snackbar.
6. Depth-50 category strategy, 200+ record pattern, long Chinese/English labels, and enlarged-text reflow.
7. No product-page composition: the artifact is a labeled state harness, not a dashboard mockup.

### Accepted debt

| ID | Item | Location / affected users | Why accepted | Owner / exit condition |
| --- | --- | --- | --- | --- |
| `DEBT-ASSET-IDENTITY-001` | Resolved for newly created records: records now store immutable asset IDs; legacy name fields remain display-only compatibility data. | Asset cards, asset records, archive/delete messaging | The financial store is reset at the current development boundary, so new records no longer depend on asset names for balance mutation. | Keep ID-based binding in all future record and transfer changes; remove compatibility name queries after the next cleanup pass. |
| `DEBT-CONNECTED-QA-002` | Connected ADB/device QA is user-waived and was not run for this task because no connected target is available. | Runtime interaction, TalkBack, actual Android font scaling, and motion claims | This task creates the design contract and Stitch showcase only; static inspection cannot prove connected-device behavior. It must never be reported as passed. | Todo 18/19 on an agent-controlled Android target, or explicit future owner acceptance of remaining runtime evidence gaps. |

No accessibility defect is accepted by this document. Any newly discovered accessibility debt requires explicit user acknowledgement, affected-user detail, severity, exact location, and a remediation owner before it can be added.
