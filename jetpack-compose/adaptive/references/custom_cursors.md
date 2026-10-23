## Description

Use this document when adding or modifying pointer cursor behavior in a Jetpack
Compose application targeting Android large screens and desktop environments.

## Workflow

For each following step, search the whole codebase for the listed APIs, write
down every match, and only then implement with
`Modifier.pointerHoverIcon(icon)` on the element itself. Pick the cursor type
from these steps yourself. Before finishing, re-run the searches:
every match is either handled or skipped for a stated reason.

### Step 1: Find and process draggable elements (Grab / Grabbing)

Go through the app, find all the draggable elements that would benefit from
adding a `TYPE_GRAB` / `TYPE_GRABBING` cursor.

- **Search for:** `dragAndDropSource`, `detectDragGesturesAfterLongPress`, reorderable list items, `draggable(` on movable content.
- **Examples:** Reorderable list items, movable floating windows, map surfaces, draggable sliders, and custom drag-and-drop targets.
- **Implementation:** For each one of them, implement the cursor using `PointerIcon(android.view.PointerIcon.TYPE_GRAB)` for movable surfaces and `PointerIcon(android.view.PointerIcon.TYPE_GRABBING)` only during an active drag.

### Step 2: Find and process handwriting and drawing elements (Crosshair)

Go through the app, find all handwriting or precision drawing elements.

- **Search for:** `Canvas(`, `InProgressStrokesView`, drawing surfaces in `AndroidView`, `pointerInput` blocks that draw strokes or shapes.
- **Examples:** Canvas drawing areas, signature capture pads, image cropping tools and spatial selection bounding boxes.
- **Implementation:** For each one of them, implement the cursor using `PointerIcon.Crosshair`.

### Step 3: Find and process resizable elements (Resize)

Go through the app, find all splitters, resizable edges, and resize handles.

- **Search for:** `VerticalDragHandle`, `paneExpansionDragHandle`, `paneExpansionDraggable`, `dragHandle =` of sheets, `anchoredDraggable`, `draggable(` on a divider.
- **Examples:** Split pane dividers (horizontal or vertical), resizable dialog edges, and bottom sheet drag handles.
- **Implementation:** For each one, implement the correct system cursor (e.g., `PointerIcon(android.view.PointerIcon.TYPE_HORIZONTAL_DOUBLE_ARROW)` for vertical splitters). Match the cursor direction to the handle.

### Step 4: Find and process clickable elements (Hand)

Go through the app, find all links, clickable text, and icon-only controls.

- **Search for:** `IconButton`, `IconToggleButton`, `FloatingActionButton`, `NavigationBarItem`, `NavigationRailItem`, `NavigationDrawerItem`, `navigationSuiteItems`, `Tab(`, `clickable` on an `Icon` or `Text`.
- **Examples:** Hyperlinks in text, standalone icon buttons, navigation bar, rail and drawer items, tabs, and active toggle switches.
- **Implementation:** For each one, implement `PointerIcon.Hand` when the element is active. When inactive or disabled, you must default back to `PointerIcon.Default`. Do not use `Hand` for every single clickable surface.

### Step 5: Find and process text selection elements (Text)

Go through the app, find all custom editable or selectable text regions.

- **Examples:** Custom code editors, selectable log outputs, and custom text selection overlays.
- **Implementation:** For each one, implement `PointerIcon.Text`. Ensure you do not override the correct default cursor of standard Compose text fields.

### Step 6: Find and process temporarily blocked elements (Wait)

Go through the app, find short, temporarily blocked regions.

- **Examples:** Form submission overlays, local processing states, and refreshing panels.
- **Implementation:** For each one, implement the cursor using `PointerIcon(android.view.PointerIcon.TYPE_WAIT)`.

## Reference links

Consult these references before proceeding with the implementation.

- [Compose `pointerHoverIcon` API reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/package-summary#pointerHoverIcon(androidx.compose.ui.Modifier,androidx.compose.ui.input.pointer.PointerIcon,kotlin.Boolean))
- [Compose `PointerIcon` API reference](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/PointerIcon)
- [Android pointer input overview](https://developer.android.com/develop/ui/compose/touch-input/pointer-input)