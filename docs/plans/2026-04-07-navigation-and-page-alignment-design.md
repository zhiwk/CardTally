# Navigation And Page Alignment Design

**Goal:** Align `home`, `records`, `assets`, `agent`, and `me` with the updated Stitch layouts for bottom navigation placement and primary action placement, while preserving the current Fragment/XML/SQLite architecture and existing business behavior.

## Constraints

- Use current `MainActivity` bottom navigation shell as the single navigation host.
- Keep current data flows and Fragment entry points intact.
- Avoid upgrading `StatisticsFragment` into a new records architecture in the same change.
- Keep `SettingsFragment` as the current implementation target for the Stitch `me` page.

## Decisions

### Bottom Navigation

- Continue rendering the floating bottom navigation in `activity_main.xml`.
- Update labels and spacing to match the Stitch navigation language more closely.
- Ensure page content reserves enough bottom space so page-local controls do not overlap the navigation shell.

### Home

- Keep the current monthly summary, agent card, and recent entries structure.
- Reposition the floating add button to sit clearly above the floating bottom nav, following the Stitch composition.
- Refresh the record row and date header styling to match the new editorial list treatment.

### Records

- Keep `StatisticsFragment` as the current screen implementation target for the Stitch `records` page.
- Align the page shell, bottom spacing, and floating add button with the new design.
- Update the list item styling so the current statistics-driven content visually reads closer to the designed ledger presentation.
- Do not replace the fragment's data source with a full record browser in this task.

### Assets

- Keep the total assets header and asset list behavior.
- Remove reliance on the hidden floating action button and use the page-bottom primary CTA as the main add action.
- Update the asset row styling and page spacing to match the new grouped editorial presentation.

### Agent

- Keep the current static content structure.
- Move the chat input into a floating pill above the navigation shell, with enough bottom padding for the scrollable content.

### Me

- Keep current settings behavior and navigation targets.
- Align section spacing and visual hierarchy with the Stitch `me` page.
- Do not add new unsupported settings flows from the mock, such as reminder scheduling or vault security.

## Implementation Shape

- Modify `activity_main.xml` and `bottom_nav_menu.xml` for navigation shell alignment.
- Update `fragment_home.xml`, `fragment_asset.xml`, `fragment_agent.xml`, `fragment_settings.xml`, and `fragment_statistics.xml` for screen shell and CTA positioning.
- Update `item_date_header.xml`, `item_record.xml`, `item_asset.xml`, and `item_statistics.xml` for list presentation.
- Make the smallest Kotlin changes needed to keep click targets wired to existing behavior.

## Verification

- Build with `./gradlew.bat assembleDebug`.
- Manually verify that bottom navigation, floating buttons, and the agent input bar no longer overlap.
- Confirm `home`, `records`, `assets`, `agent`, and `me` still navigate and trigger their existing actions.
