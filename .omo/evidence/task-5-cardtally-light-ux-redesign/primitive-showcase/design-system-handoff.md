# CardTally primitive showcase handoff

## Canonical artifact

- Root contract: `DESIGN.md`
- Contract structure: eight mandatory sections
- Authority: `docs/requirements/decisions/2026-08-12-light-ux-redesign.md`
- Historical Stitch `DESIGN.md` files: not used as source of truth
- Product code changed: none

## Stitch resources

- Project resource: `projects/8289831567392824734`
- Project title: `CardTally`
- Design-system resource: `assets/10432700544451438424`
- Design-system display name: `CardTally Light Outlined Card System`
- Asset version observed after creation: `1`
- Source association: Stitch associates the asset to the project by project/name metadata; the root `DESIGN.md` remains canonical.

## Visual direction

- Light-only black, white, and cool-gray system.
- Pale cyan marks selection, focus, and information.
- Pale red marks expense, destructive, and error semantics.
- Pale green and pale amber are status-only.
- Meaningful cards use a 1dp outline and very light downward shadow.
- Outer radii are 12dp, with 16dp reserved for prominent shells, sheets, and dialogs; inner controls use 8–10dp.
- Overview and dense workflow profiles share the same tokens.
- Dense rows share one outlined group and are not individually carded.

## Stress and accessibility contract

- Every action target is at least 48dp.
- TalkBack semantics name role, name, state, value, and action.
- Chinese and English support natural wrapping and enlarged text.
- Depth-50 categories cap visible indentation at four steps, then use an ancestry rail, `Lx` label, and breadcrumb disclosure.
- The 200+ record pattern requires RecyclerView virtualization, stable row geometry, date grouping, and no staggered entrance.
- Motion has a reduced-motion fade/static path.

## Accepted debt

- `DEBT-ASSET-IDENTITY-001`: asset-record association remains name-based and must not be described as immutable ID linkage.
- `DEBT-CONNECTED-QA-002`: connected ADB QA was user-waived and not run because no connected target is available; it is not passed evidence.
