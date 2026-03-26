# Design System: High-End Financial Editorial

## 1. Overview & Creative North Star: "The Digital Private Bank"
This design system moves away from the cluttered, utility-first nature of traditional fintech. Our Creative North Star is **The Digital Private Bank**. Imagine a bespoke, leather-bound ledger or a high-end editorial magazine where information isn't just displayed—it is curated. 

We break the "template" look by utilizing the tall 1256x2808 aspect ratio to embrace **intentional asymmetry**. Data visualization should feel like art, using generous whitespace (`spacing-24`) and high-contrast typography scales to create a sense of "Quiet Luxury." We avoid the rigid "box-in-a-box" layout in favor of layered, organic surfaces that feel like physical sheets of fine stationery stacked on a marble desk.

---

## 2. Colors: Tonal Depth & The "No-Line" Rule
The palette is built on a foundation of `surface` (Off-White) and `primary` (Deep Slate), accented by the sophisticated `secondary` (Champagne Gold).

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders for sectioning. Structural boundaries must be defined solely through background color shifts.
*   **Implementation:** Use a `surface-container-low` section sitting directly on a `surface` background. The subtle shift in hex value provides enough contrast for the eye to perceive a container without the "cheap" look of a stroke.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers. Use the `surface-container` tiers to create depth:
*   **Base Layer:** `surface` (#f8f9fa).
*   **Secondary Content Areas:** `surface-container-low` (#f1f4f6).
*   **Interactive Cards:** `surface-container-lowest` (#ffffff) to create a subtle "lift" against the off-white background.
*   **Modals/Overlays:** `surface-bright` to draw the eye.

### Glass & Gradient Rule
To achieve a premium polish, use **Glassmorphism** for floating action buttons or sticky headers.
*   **Token Usage:** Use `surface` at 80% opacity with a `20px` backdrop-blur. 
*   **Signature Textures:** For main CTAs, do not use flat colors. Apply a subtle linear gradient from `primary` (#4e6073) to `primary_dim` (#425467) at a 135-degree angle. This provides a "brushed metal" or "silk" feel that flat UI cannot replicate.

---

## 3. Typography: Editorial Authority
We utilize a dual-font system to balance modern utility with classic sophistication.

*   **Display & Headlines (Manrope):** Chosen for its geometric yet elegant structure. Use `display-lg` with `letter-spacing: -0.02em` for large balance amounts. This creates an authoritative, "magazine-cover" aesthetic.
*   **Body & Labels (Inter):** Chosen for its extreme legibility at small sizes. Use `body-md` for transactional data.
*   **Hierarchy Tip:** Contrast `headline-sm` in `on_surface` (Deep Slate) with `label-md` in `outline` (Muted Grey) using all-caps and `letter-spacing: 0.1em` for secondary metadata. This creates a clear, premium distinction between data and labels.

---

## 4. Elevation & Depth: Tonal Layering
Traditional drop shadows are too aggressive for "Quiet Luxury." We convey hierarchy through ambient light.

*   **The Layering Principle:** Instead of shadows, "stack" your tokens. Place a `surface-container-lowest` card on a `surface-container-low` background. The delta in luminance creates a natural elevation.
*   **Ambient Shadows:** If a floating element (like a bottom sheet) requires a shadow, it must be:
    *   **Blur:** 40px - 60px.
    *   **Opacity:** 4% - 6% of `on_surface`.
    *   **Offset:** Y: 12px.
*   **The "Ghost Border" Fallback:** If a divider is essential for accessibility, use the `outline_variant` token at **15% opacity**. Never use a 100% opaque border.
*   **Glassmorphism:** Use for persistent navigation bars. The `surface_container` color with a 70% opacity allows the vibrant colors of financial charts to bleed through as the user scrolls, keeping the experience integrated.

---

## 5. Components

### Buttons
*   **Primary:** Gradient of `primary` to `primary_dim`. Roundedness: `md` (0.375rem). No shadow.
*   **Secondary (Champagne Gold):** Use `secondary_container` background with `on_secondary_container` text for high-end actions (e.g., "Invest").
*   **Tertiary:** `surface-container-low` background with `primary` text. No border.

### Input Fields
*   **Style:** Minimalist. No bounding box. Only a `surface-container-highest` bottom bar (2px).
*   **Focus State:** The bottom bar transitions to `secondary` (Champagne Gold).

### Cards & Financial Lists
*   **Strict Rule:** No divider lines between transactions. 
*   **Separation:** Use `spacing-3` (1rem) of vertical whitespace or alternating `surface` and `surface-container-low` backgrounds for "Zebra" striping that feels intentional rather than utilitarian.

### Bespoke Component: The "Wealth Carousel"
A wide-scrolling horizontal container using `surface-container-lowest` with `xl` (0.75rem) rounded corners. Each card features an asymmetrical layout: Large `display-sm` balance on the top-left, with a micro-sparkline chart in the bottom-right using the `tertiary` (Emerald) color.

---

## 6. Do's and Don'ts

### Do
*   **Do** use extreme whitespace (`spacing-16` or `spacing-20`) between major sections to let the data "breathe."
*   **Do** use `secondary` (Champagne Gold) sparingly—only for the most important financial "conversion" points.
*   **Do** align text-heavy sections with asymmetrical margins (e.g., more padding on the left than the right) to mimic high-end editorial layouts.

### Don't
*   **Don't** use pure black (#000000). Always use `on_background` (#2b3437) for text to maintain a soft, premium look.
*   **Don't** use standard "Success Green" (#00FF00). Use the `tertiary` (Emerald Green #2b6b3f) for a more mature, stable feel.
*   **Don't** use `DEFAULT` roundedness for everything. Mix `none` for top-level hero sections and `xl` for interactive cards to create visual interest.