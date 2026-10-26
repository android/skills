## Instructions

### Step 1. Isolate viewing content and interactive controls

Examine the target composable (for example, a media player or camera preview):

- Isolate the viewing composable: Extract the visual content displayed on the upright screen (such as video playback or camera preview).
- Isolate the controls composable: Extract the interactive touch elements (such as playback buttons or shutter controls).

### Step 2. Select layout strategy

Check whether the composables are identical across configurations:

- **Do not use `Grid` --- use conditional layout branching (Option A)** when the
  controls or content differ between layouts, requiring two different
  composables anyway (for example, a flat layout with simple overlay buttons
  versus a tabletop layout with an expanded control panel or different
  parameter controls).

- **Use `Grid` (Option B)** when the composables (viewing content and
  controls) are identical in both configurations and only their positions
  change. For example, a media player with unchanged player and controller
  composables. The composables overlay in flat posture and split into two rows
  in tabletop posture.

### Step 3. Implement the layout

#### Option A: Using conditional layout branching (non-Grid)

Use this approach when the controls or content differ between postures,
requiring different composables for each configuration.

1. Use `mediaQuery` from `androidx.compose.ui` to observe `windowPosture` (`import androidx.compose.ui.mediaQuery` and `import androidx.compose.ui.UiMediaScope`). Check for tabletop posture using `mediaQuery { windowPosture == UiMediaScope.Posture.Tabletop }` (or `Posture.Tabletop`). Do not use `windowPosture.isTabletop` directly. (Note: If not already enabled, set `ComposeUiFlags.isMediaQueryIntegrationEnabled = true` in the `Application` class).
2. Retrieve the physical horizontal hinge bounds from the window posture using `currentWindowAdaptiveInfoV2().windowPosture` (or `currentWindowAdaptiveInfo().windowPosture` if using an older library version).
3. In the parent container, translate the hinge bounds from window to local coordinates using `coordinates.windowToLocal` (for example, converting `Offset(0f, hinge.top.toFloat())` and `Offset(0f, hinge.bottom.toFloat())` in `onGloballyPositioned`). Never assume a 50/50 split when surrounding UI components are present.
4. Expose the local hinge top and bottom boundaries (for example, using `HorizontalRuler` or custom layout modifiers):
   - Constrain the viewing composable above the fold (`maxHeight` bounded by `hingeTop`).
   - Constrain and offset the controls below the fold (`maxHeight` bounded by `containerHeight - hingeBottom`, placed at `y = hingeBottom`).
5. If not in tabletop posture, return the standard flat layout (for example, the viewing composable filling the available space with overlay controls).

#### Option B: Using Grid

1. Define layout area identifiers using an `enum` class.
2. In `defaultConfig`, assign all areas to `row = 1, column = 1`. `Grid` uses implicit z-indexing, rendering composables declared later in the layout over earlier composables.
3. In `tabletopConfig`, define two rows of `0.5f` height and assign the primary content area to row 1 and controls to row 2.
4. Use `mediaQuery` (from `androidx.compose.ui.mediaQuery`) to select `tabletopConfig` when in tabletop posture (`windowPosture == UiMediaScope.Posture.Tabletop`), and `defaultConfig` otherwise.
5. Pass the config to `Grid` and bind content using `Modifier.gridItem(areaId)`.
6. Declare the viewing composable before the control composable in the `Grid` body. This verifies that controls render over the viewing content in flat posture.

For a reference implementation of Option B, see the
[Tabletop posture recipe](https://developer.android.com/develop/adaptive-apps/cookbook/tabletop-posture-media-query-grid).