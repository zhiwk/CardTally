# Local primitive showcase inspection

## Artifact

- Source: `docs/design/assets/primitive-showcase/showcase.svg`
- Index: `docs/design/assets/primitive-showcase/README.md`
- State matrix: `docs/design/assets/primitive-showcase/state-matrix.md`
- Rendered geometry: `420 × 7630` CSS pixels
- Surface: static SVG design/state harness, not a product screen
- Canonical contract: root `DESIGN.md`
- Stitch design-system asset retained: `10432700544451438424`

## Capture coverage

The final approval capture set is ten fresh Chromium PNGs, each `420 × 900`, with overlap and complete source coverage:

| Capture | Source Y range |
| --- | --- |
| `pass-01.png` | 0–900 |
| `pass-02.png` | 800–1700 |
| `pass-03.png` | 1600–2500 |
| `pass-04.png` | 2400–3300 |
| `pass-05.png` | 3200–4100 |
| `pass-06.png` | 4000–4900 |
| `pass-07.png` | 4800–5700 |
| `pass-08.png` | 5600–6500 |
| `pass-09.png` | 6400–7300 |
| `pass-10.png` | 6730–7630 |

The capture wrapper hid browser scrollbars only; it did not alter SVG layout, color, type, or content. Animations were disabled because the artifact is static.

## Repair loop

### Round 1 — REVISE

Direct `look_at` inspection found:

1. Top scope badge collided with adjacent metadata.
2. The depth-50 TalkBack sentence clipped at compact width.
3. The keypad long-press helper overlapped the final key row.
4. Browser scrollbars obscured capture seams and caused apparent horizontal overflow.
5. Endpoint coverage was incomplete.

Repairs widened and separated the badge, wrapped the TalkBack sentence, moved the keypad helper below the card, hid scrollbars in the capture wrapper, and changed capture stops to fully overlap the board.

### Round 2 — FAIL

All Round 1 defects were fixed, but the footer baseline sat only four pixels above the SVG endpoint and clipped descenders.

### Round 3 — direct PASS, independent REVISE/FAIL

The SVG height increased from 6840 to 6880 and the footer gained terminal breathing room. Direct `look_at` inspection passed, but independent reviewers found evidence gaps: several visible actions were below 48dp, per-primitive TalkBack and reduced-motion evidence was too implicit, state applicability relied on broad claims, repeated semantic fills were not fully class-driven, and the footer sentence still exceeded compact width.

### Round 4 — direct PASS, independent REVISE/PASS

The board now uses `420 × 7630` geometry and:

1. Raises segments, filters, sheet actions, and Retry to at least 48dp.
2. Adds a visible per-family accessibility/state/motion ledger covering role, name, state, value, action, applicability, focus, and reduced-motion alternatives.
3. Shows a 200% bilingual reflow specimen.
4. Replaces repeated raw semantic fills with shared SVG classes; foundation swatches remain literal by design because they display the actual tokens.
5. Splits the footer into two compact-safe lines.

`look_at` inspected all ten captures and returned `PASS`. The accessibility reviewer passed. The integrity reviewer found four remaining sub-48dp specimens, direct icon colors, and an out-of-scope root README concern. The actionable specimens and token classes were repaired; the artifact index records that product README reconciliation belongs to plan Todo 17.

### Round 5 — direct PASS, final independent verdict pending

The final source:

1. Uses ≥48dp for empty-state actions, record-type controls, the selected settings row, dialog recovery, Retry, and all three sheet picker rows.
2. Keeps the 44dp asset icon explicitly non-interactive inside a 130dp actionable card.
3. Uses shared classes for icon and component colors. Literal colors remain only in the foundation swatches and shared shadow definitions.
4. Preserves the complete semantics/state/motion ledger and compact-safe footer.

`look_at` inspected `pass-01.png` through `pass-10.png` and returned `PASS` with no blockers.

## Static accessibility evidence

- SVG root has `role="img"`, an `aria-labelledby` relationship, a meaningful `<title>`, and a comprehensive `<desc>`.
- Major specimen groups carry descriptive `aria-label` values in source.
- Visible annotations cover role, name, state, value, action, ≥48dp targets, focus return, modal focus, non-color cues, reduced motion, throttled streaming announcements, parent/leaf semantics, and swipe/long-press alternatives.
- The state matrix records each primitive’s TalkBack and stress contract.
- Runtime TalkBack, font-scale, and motion execution are intentionally not claimed by this static gate.

## Direct verdict

Direct verdict: `PASS`. Final gate requires fresh independent reviews of `pass-01.png` through `pass-10.png`.
