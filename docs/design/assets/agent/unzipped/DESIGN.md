# Design System Strategy: The Curated Chronicle

## 1. Overview & Creative North Star
The Creative North Star for this design system is **"The Curated Chronicle."** 

Most financial applications feel like spreadsheets—cold, rigid, and anxiety-inducing. This system rejects the "bank-utility" aesthetic in favor of a high-end editorial experience. It treats personal finance as a reflective diary rather than a data-entry chore. By utilizing intentional asymmetry, overlapping elements, and generous negative space, we create a sense of calm and intentionality. We move away from the "template" look by treating the mobile screen as a page in a bespoke lifestyle publication, where the rhythm of white space is as important as the data itself.

---

## 2. Colors & Surface Philosophy
The palette is grounded in "quiet luxury"—avoiding high-saturation "alert" colors in favor of muted, organic tones that suggest longevity and stability.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders to section content. Boundaries must be defined solely through:
1.  **Background Color Shifts:** Placing a `surface-container-low` element onto a `surface` background.
2.  **Tonal Transitions:** Using subtle shifts in the neutral scale to imply grouping.
3.  **Negative Space:** Using the spacing scale to create distinct visual clusters.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers, like stacked sheets of handmade paper. Use the `surface-container` tiers to create depth:
*   **Base:** `surface` (#fffcf7) for the primary canvas.
*   **Secondary Context:** `surface-container-low` (#fcf9f3) for subtle grouping.
*   **Interactive Focus:** `surface-container-high` (#f0eee5) or `highest` (#eae8de) for cards and modals that need to feel "closer" to the user.

### The "Glass & Gradient" Rule
To add a "signature" feel, floating elements (like Bottom Sheets or Navigation Bars) should utilize **Glassmorphism**. Apply `surface` or `surface-container` colors at 85% opacity with a high-density backdrop blur (20px-30px). 
*   **Signature Tones:** For high-impact areas (Hero sections), use a subtle radial gradient transitioning from `primary` (#615d66) to `primary-dim` (#55515a) at a 15-degree angle. This provides a "soul" to the color that flat hex codes cannot achieve.

---

## 3. Typography: Editorial Elegance
The typography system balances the classic authority of a serif with the modern legibility of a clean sans-serif.

*   **Display & Headlines (Newsreader):** Used for emotional storytelling, large balances, and date headers. The serif conveys a "diary" feel, slowing the user down to reflect.
    *   *Display-LG (3.5rem):* Reserved for milestone totals.
    *   *Headline-MD (1.75rem):* For entry titles and daily summaries.
*   **Body & Labels (Manrope):** A sophisticated sans-serif used for transactional data, secondary descriptions, and button text.
    *   *Body-LG (1rem):* Standard reading text.
    *   *Label-MD (0.75rem):* For metadata and overline tags.

**Typographic Intent:** Always prioritize "Ragged Right" alignment for long-form text to maintain a relaxed, humanistic feel. Avoid justified text or overly tight tracking.

---

## 4. Elevation & Depth: Tonal Layering
Traditional material design relies on heavy shadows. This system uses **Tonal Layering** to create a softer, more organic sense of lift.

*   **The Layering Principle:** Depth is achieved by "stacking." Place a `surface-container-lowest` card on a `surface-container-low` section. The change in tone provides a natural lift without visual noise.
*   **Ambient Shadows:** When an element must float (e.g., a FAB or a primary card), use an extra-diffused shadow.
    *   *Specs:* Blur: 32dp, Y-Offset: 8dp, Color: `on-surface` (#383831) at **4-6% opacity**. 
*   **The "Ghost Border" Fallback:** If a container lacks sufficient contrast, use a "Ghost Border": the `outline-variant` (#babab0) at 15% opacity. Never use 100% opaque borders.

---

## 5. Component Guidelines

### Buttons (The Tactile Signature)
*   **Primary:** Capsule-shaped (`full` radius). Use `primary` (#615d66) with `on-primary` text. No shadows; use a subtle `surface-tint` overlay on press.
*   **Tonal (Secondary):** Use `secondary-container` (#d3e8d5) with `on-secondary-container` (#435647). This provides a soft, "mossy" alternative to the primary action.

### Input Fields (The Quiet Entry)
*   Forgo the "Boxed" input. Use a "Minimalist Ledger" style: A simple bottom stroke using `outline-variant` at 30% opacity. 
*   Floating labels should use `newsreader` in `title-sm` to maintain the editorial vibe during interaction.

### Cards & Lists (The Narrative Flow)
*   **Cards:** Use `xl` (1.5rem / 24dp) corner radius. 
*   **Dividers:** **Strictly Prohibited.** Separate list items using 16dp of vertical white space or a subtle background toggle between `surface-container-low` and `surface-container-lowest`.
*   **Content Asymmetry:** In cards, experiment with placing the "Amount" in a large `display-sm` Newsreader font on the left, with the "Category" label tucked in a smaller `label-md` Manrope font to the upper right. Challenge the standard "Left-Label, Right-Value" banking grid.

### Chips (The Soft Filter)
*   Use `secondary-fixed-dim` (#c5dac7) for unselected states. These should feel like small, soft pebbles. 
*   Selected states should use `primary` (#615d66) with a subtle micro-shadow to indicate "depressed" tactile feedback.

---

## 6. Do’s and Don’ts

### Do:
*   **Embrace the "Empty Page":** If a user has no data, don't show an "Empty State" illustration. Show a beautifully typeset quote or a gentle prompt in `newsreader` headline-sm.
*   **Use Soft Linear Icons:** Use 1.5px stroke weights with rounded caps. Icons should feel "sketched" rather than "manufactured."
*   **Prioritize Warmth:** Ensure the `surface` (#fffcf7) remains the dominant color to keep the app feeling like oatmeal/paper rather than cold digital white.

### Don’t:
*   **Don't use "Red" for negative spending:** Use `error` (#a54453) sparingly. A diary should not "scold" the user. Consider using the purple-gray `primary` for negative numbers to maintain the "Quiet" vibe.
*   **Don't use standard Grids:** Avoid perfectly symmetrical 2x2 grids. Try 60/40 splits to give the UI an editorial, hand-crafted feel.
*   **Don't use hard shadows:** If it looks like a "drop shadow," it's too heavy. It should look like "ambient occlusion."