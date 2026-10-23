## Description

Use this document when you need to add or redesign tooltips in a Jetpack Compose
application targeting Android large screens and desktop environments. Do not use
Compose Multiplatform Desktop TooltipArea.

Expected output:

- Brief analysis of targets and input methods, followed by an implementation that preserves accessibility, focus, and design-system rules.

## Workflow

### Step 1: Identify tooltip targets and text

Search the code and write down every candidate before implementing. Decide
targets and wording yourself.

1. Icon-only controls: `IconButton`, `IconToggleButton`, `FloatingActionButton`, `TopAppBar` `navigationIcon` and `actions`, toolbar and toolbox icons, a `clickable` `Icon` with a `contentDescription`, and navigation items (`NavigationBarItem`, `NavigationRailItem`, `NavigationDrawerItem`, `navigationSuiteItems`, `Tab`) whose label is hidden, like `alwaysShowLabel = false` or a collapsed rail.
2. Unclear toolbar actions and controls with hidden keyboard shortcuts.
3. Skip controls that are already clear from visible text. Never use a tooltip as the only source of a label, instruction, validation error, or disabled-state requirement.
4. Check whether long-press already starts dragging, selection, or a context menu. Skip the tooltip if it conflicts.

For each tooltip, use a localized string and brief, sentence-case text:

- Start from the control's accessibility label: for an icon-only control the `Icon`'s `contentDescription` is normally the right tooltip text. Reuse the same string resource for both so they cannot drift, and keep the `contentDescription` on the icon --- a tooltip never replaces it.
- Prefer verbs for actions: `Add column`, `Export CSV`, `Mute track`.
- Keep plain tooltips short and clear.
- Do not repeat visible button text unless the tooltip adds a shortcut or consequence.
- Add shortcut hints only when accurate, e.g., `Save (Ctrl+S)`.
- For disabled controls, explain the requirement: `Select a row first`.

### Step 2: Implement the tooltips

Iterate through the identified targets and implement tooltips for each according
to the following patterns:

- **Reuse existing components:** Use a shared wrapper for repeated tooltips and inline `TooltipBox` for one-off tooltips.
- **Tooltip type:** Use `PlainTooltip` for a short label. Use `RichTooltip` for extra details or one action.
- **Tooltip trigger:** Use the default hover-effect, long-press, and
  keyboard-focus behavior unless the tooltip should only be shown from code.

- **API:** `TooltipBox(positionProvider, tooltip = { PlainTooltip { Text(...) }
  }, state = rememberTooltipState()) { control }`. Check the Material 3
  version in the Gradle files first: on 1.4.0 or newer use
  `TooltipDefaults.rememberTooltipPositionProvider(positioning =
  TooltipAnchorPosition.Above)`; on 1.3.x that function does not exist, use
  `TooltipDefaults.rememberPlainTooltipPositionProvider()`.

Follow these rules:

- Give each `TooltipBox` its own `TooltipState`.
- Wrap exactly one control. Keep its click, focus, enabled state, and accessibility on the control, and keep the wrapper bounds tight.
- For a rich tooltip with an action, use `rememberTooltipState(isPersistent =
  true)`, set `hasAction = true`, and dismiss it after the action.
- Before finishing, re-run the searches from Step 1: every match now has a tooltip or a stated reason to skip it. Do not stop at the first screen.

#### Show tooltips from code (optional)

Use this only for brief, nonessential help, such as feature education.

- Call `tooltipState.show()` from a coroutine.
- Call `tooltipState.dismiss()` directly when the tooltip is no longer needed.
- Set `enableUserInput = false` if hover-effect, long-press, and keyboard focus should not show it.
- Trigger `show()` from a user event or a one-time effect, not directly during composition.

## Reference links

Consult these references before proceeding with the implementation.

- AndroidX Material 3 tooltip [samples](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/TooltipSamples.kt)
- Material Design 3 tooltip [guidelines](https://m3.material.io/components/tooltips/guidelines)