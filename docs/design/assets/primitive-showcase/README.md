# CardTally Primitive Showcase

This directory contains the repository-local primitive showcase for the current design system. It is not a product screen and does not replace the canonical root `DESIGN.md`.

## Artifacts

- [`showcase.svg`](showcase.svg): self-contained 420px compact-width primitive/state board.
- [`state-matrix.md`](state-matrix.md): primitive, state, density, accessibility, and stress traceability.

Open `showcase.svg` directly in a browser or image viewer. The board is intentionally tall so every required primitive and state can be inspected at compact Android width without composing a product page.

## Authority and scope

- Canonical contract: root `DESIGN.md`.
- Historical design exports are not source of truth.
- This artifact is a static design/state harness, not Android implementation evidence and not a Home, Ledger, Assets, Agent, or Me screen.
- No Kotlin, XML, resource, build, test, Gradle, ADB, or install action belongs to this artifact.
- The root product `README.md` still describes pre-implementation runtime/theme reality and is intentionally not rewritten by this design-only task; plan Todo 17 owns current-facing product-document reconciliation after implementation verification.

## Visual rules represented

- Light-only black, white, and cool-gray outlined surfaces.
- Pale cyan, red, green, and amber only for semantic meaning.
- 4dp spacing, 12/16dp outer radius, 8/10dp inner controls.
- Fine gray border and one top-center lighting direction with very light downward shadow.
- Overview and dense workflow profiles share tokens.
- Dense rows share one group outline; individual rows are not cards.
- Noto Sans/system-safe bilingual typography and tabular financial figures.
- At least 48dp action targets and explicit TalkBack role/name/state/value/action annotations.
- Depth-50 and 200+ record stress strategies.
- SVG colors are centralized in shared classes. Literal colors remain only in the Section 01 token swatches and the two shared shadow-filter definitions, where the values are the specimen being documented.

## Non-goals

- No product-page composition or navigation flow.
- No runtime interaction proof.
- No connected-device, TalkBack, Android font-scale, or motion execution claim.
- No gradients, glassmorphism, high saturation, purple/blue AI styling, emojis, decorative illustration, or card-per-row layout.
