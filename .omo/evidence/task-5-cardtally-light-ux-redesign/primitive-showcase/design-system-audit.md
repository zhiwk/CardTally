# Canonical design-system audit

## Contract coverage

| Requirement | Evidence in `DESIGN.md` | Result |
| --- | --- | --- |
| Eight-section contract | Sections 1 through 8 are present | PASS |
| Neutral outlined-card atmosphere | Sections 1, 2, and 7 | PASS |
| Pale semantic accents only | Section 2 palette and state rules | PASS |
| Android-safe bilingual type and tabular figures | Section 3 | PASS |
| 4dp spacing and 12–16dp outer radii | Section 4 | PASS |
| Overview and dense profiles | Section 4 density profiles | PASS |
| Required primitives and applicable states | Section 5 | PASS |
| Card/sheet motion and reduced motion | Section 6 | PASS |
| One lighting direction; no broad glass | Section 7 | PASS |
| 48dp, TalkBack, stress, and debt | Section 8 | PASS |

## Existing-system extraction

The audit read the current color, theme, card, typography, and dimension resources plus these active component samples: activity navigation shell, Home, Add/Edit Record, Ledger, Assets, Agent, Me, recent record row, category tree row, Agent message, and settings row. The reusable Material/XML anatomy was retained as the implementation seam. Conflicting warm-purple colors, serif financial figures, excessive radii/elevation, per-row cards, sub-48dp actions, and arbitrary spacing were not carried into the canonical system.

## Android mapping rule

Every design token names an existing or explicitly new Android resource mapping. “New” mappings are contracts for later implementation tasks, not claims that resources already exist. This task intentionally does not edit Kotlin, XML, drawables, colors, dimensions, or styles.

## Anti-slop audit

- Purple/blue AI gradients: prohibited.
- Decorative gradients and broad glass: prohibited.
- High saturation: prohibited.
- Emojis as icons: prohibited.
- Nested card around each dense row: prohibited.
- Fake loading on synchronous SQLite content: prohibited.
- Product-screen composition before primitive gate: prohibited.
