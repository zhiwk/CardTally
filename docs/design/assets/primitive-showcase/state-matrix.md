# Primitive Showcase State Matrix

| Primitive | Variants and states shown | Density | Accessibility contract | Stress evidence |
| --- | --- | --- | --- | --- |
| Navigation shell | 3, 4, and 5 destinations; default and selected | Shell | Collection order, selected state, ≥48dp targets | Conditional Assets/Agent destinations |
| Headers | Top-level and secondary/back; trailing action | Both | Heading semantics and labeled icon actions | Long bilingual secondary title |
| Buttons | Primary, pressed, focused, selected, disabled, loading, destructive, error completion, text action | Both | Stable label during loading, visible focus, ≥48dp | Bilingual labels |
| FAB | Add action | Overview | Verb-object accessible label; 56dp target | Compact-width placement specimen |
| Summary card | Default, income, expense | Overview | Grouped summary, signs and labels beyond color | Tabular large amounts |
| Record group/rows | Default, selected, swipe reveal, disabled/pending, globally empty, filter empty | Dense | Edit/delete actions and swipe equivalent | 200+ RecyclerView, date grouping, stable 52dp rows |
| Asset card | Active/default | Overview | Open action and name-based association note | Long Chinese/English account name |
| Settings group | Default, selected/on, disabled/removed | Overview | One row role; state announced | Three rows share one outline |
| Category tree | Expanded parents, selected leaf, protected parent, levels 1–4 and L50 | Dense | Tree level, expanded/selected, parent vs leaf action | Bounded indentation, ancestry rail, breadcrumb, long bilingual label |
| Segments and filters | Selected/default period, details/statistics, deeper filter trigger | Dense | Tab/radio semantics and non-color selection | Four compact controls without horizontal overflow |
| Fields | Default, focused, error, disabled | Both | Programmatic label, live error, visible focus | Bilingual field values |
| Fixed keypad | Expense/income, pressed digit, decimal, digits, Backspace | Dense | ≥48dp keys; named digit/decimal/delete/clear actions | Fixed 3×4 grid and long-press clear note |
| Bottom sheet | Selected/default picker rows, cancel/confirm | Dense | Modal role, focus trap, Back dismiss, focus return | Compact-width full sheet specimen |
| Dialog | Protected action error, stacked enlarged-text action | Both | Heading relationship and single recovery action | Enlarged bilingual copy and stacked action |
| Chat bubbles | Assistant, user, partial/error retained | Dense | Sender plus message; throttled status announcements | Long bilingual response treatment |
| Session rows | Selected and disabled | Dense | Selected state and rename accessibility action | Rows share one outline |
| Skeleton | Async loading and reduced-motion static note | Both | Hidden shapes; owning region announces once | Geometry matches content; no fake SQLite loading |
| Inline error | Error plus retry | Both | Live region and local recovery action | Compact bilingual-safe region |
| Empty state | Concise empty with one action | Both | Heading and next action | Distinguishes globally empty from filter empty |
| Undo Snackbar | Shown with Undo action | Both | Polite announcement and ≥48dp Undo | Placement contract above fixed UI |

## State coverage summary

- **Default:** navigation, headers, buttons, cards, rows, fields, tree, overlays, bubbles.
- **Pressed:** button and keypad.
- **Focused:** button and field with visible cyan exterior ring.
- **Selected:** navigation, button, settings row, record row, category leaf, segment, sheet row, session row.
- **Disabled:** button, field, settings row, pending record, protected category, session row.
- **Loading:** button and async skeleton only.
- **Error:** button completion, field, protected dialog, partial chat, inline request error.
- **Empty:** global records, filtered records, and sessions.

## Visible semantics and reduced-motion ledger

Section 11 of `showcase.svg` visibly records the full static contract instead of relying on this matrix alone:

| Family | Role / name / state / value / action evidence | State applicability | Reduced-motion evidence |
| --- | --- | --- | --- |
| Navigation | Collection, localized destination, selected, Activate | Default, press, focus, selected, disabled | Selected marker fades without travel |
| Header actions and FAB | Heading; Button with verb-object name and action | Default, press, focus, disabled | Color/state change only |
| Summary, asset, settings, record | Group or row role, signed value, current state, Open/Edit/Delete | Press/focus only when actionable; selected/disabled/error/empty where shown | No list entrance; immediate status swap |
| Segments, filters, fields, keypad | Tab/radio/EditText/Button, selected/value/error, Change/Input | Default, press, focus, selected, disabled; every key ≥48dp | Indicator fade; key surface changes without scale |
| Sheet and dialog | Modal dialog, heading/value, selection/error, Confirm/Dismiss | Focused child, selected, disabled confirm, error | 120ms fade only; no translation |
| Chat and sessions | Sender plus message; session Button, selected, Switch/Rename | Default, selected, disabled, partial/error | Content/status crossfade only |
| Skeleton, empty, error, Snackbar | Status/live region, state/result, Retry/Add/Undo | Loading, empty, error, shown/restored | Skeleton static; Snackbar fade only |

The board shows shared action-state specimens once and references them from Section 11 for primitives that use the same press/focus/disabled treatment. It does not claim that a non-actionable card has pressed or focused states.

## Static-artifact limitations

The board specifies motion and TalkBack behavior but cannot execute them. Runtime proof remains assigned to later Android implementation and connected QA tasks. Connected ADB QA is user-waived/not-run for Todo 5 and is not claimed as passed.
