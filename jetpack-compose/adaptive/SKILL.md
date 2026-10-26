---
name: adaptive
description: Instructions to make or update an app's UI so that it adapts to different
  Android devices including phones, tablets, foldables, laptops, desktop, TV, Auto,
  and XR. It includes how to handle different window sizes, pointing devices (such
  as mouse) and text entry devices (such as keyboard) using the Compose MediaQuery
  API, and how to preserve UI, navigation, scroll, and media playback state across
  configuration changes (such as folding/unfolding a foldable, rotating a device,
  or resizing a window). It also covers multi-pane layouts using Navigation3 Scenes,
  adaptive UI components (such as buttons) with varying target sizes, and adaptive
  layouts (including navigation areas - nav rails and nav bars) using the Compose
  Grid and FlexBox APIs. It also covers desktop experiences with keyboard shortcuts,
  tooltips and custom cursors.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-24'
  keywords:
  - android
  - ui
  - adaptive
  - Grid
  - FlexBox
  - MediaQuery
  - navigation
  - foldables
  - state
  - resizability
  - configuration-changes
---

## Prerequisites

The app must:

- Use Compose for all screens. If it's still using Fragments or Views, suggest using the XML to Compose skill to migrate those screens.
- Use Jetpack Navigation 3. If it doesn't, suggest the Navigation 3 skill to migrate the app.

## Workflow to make an app adaptive

To make an app adaptive, follow these steps or a subset of them adapting to the
task.

- Step 1: Verify current UI
- Step 2: Make the navigation bar adaptive
- Step 3: Add multi-pane layouts
- Step 4: Make vertical lists adaptive by changing the number of columns
- Step 5: Hide app bars when scrolling
- Step 6: Preserve UI state across configuration changes
- Step 7: Add desktop experiences
- Step 8: Support foldable devices

## Step 1. Verify current UI

Ensure that screenshot tests exist to verify the current UI on different form
factors. If they don't exist, add the [Compose Preview Screenshot Testing
tool](references/android/studio/preview/compose-screenshot-testing-with-testsuites.md). Use the following annotation to create previews for all the major form
factors. For example:


```kotlin
@Preview(name = "Phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "Foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "Tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "Desktop", device = Devices.DESKTOP, showBackground = true)
annotation class FormFactorPreviews

@PreviewTest
@FormFactorPreviews
@Composable
fun FeedScreenPreview() {
    SnippetsTheme {
        Box {
            Text("My Screen")
        }
    }
}
```

<br />

## Step 2. Make the navigation bar adaptive

Bottom navigation bars are optimized for touch input when the user is holding a
phone in portrait mode. On larger screen hand-held devices, like tablets and
unfolded foldables, the navigation area must be accessible from the edge of the
screen (navigation rail).

If you need to provide more screen space for the content, hide the
navigation area. Examples of this include:

- Hiding the navigation bar when the user scrolls down and showing it again when the user scrolls up. The assumption is that when the user is scrolling down, they are consuming content but when scrolling up they are trying to navigate away from that content.
- Hiding the navigation area when its content is distracting. For example, in camera previews or when displaying a full-screen photo.

When the detail screen is displayed full-screen on mobile, full-screen mode must
be deactivated on larger screens.

Steps to migrate:

- Locate the existing navigation bar.
- Convert each item to a `NavigationSuiteItem`.
- Identify whether the navigation bar's visibility changes. For example, if it is wrapped with an `AnimatedContent` or `AnimatedVisibility` composable. If so, follow the guidance in the "Control navigation area visibility".
- Replace the container that held the navigation bar (often a `Scaffold`) with `NavigationSuiteScaffold` from the Material 3 adaptive layouts library.
- Supply the navigation items using the `navigationItems` parameter of `NavigationSuiteScaffold`.

### Step 2.1. Control navigation area visibility

If the navigation bar's visibility changes - it is hidden under certain
scenarios or on certain screens - this behavior must be maintained with the
adaptive navigation area. This is done using `NavigationSuiteScaffold`'s `state`
parameter.

Steps to migrate:

- Identify the scenarios under which the navigation bar is hidden. This is usually done with a boolean variable for the visibility. Use `isNavBarVisible` or `shouldShowNavBar` as the variable name.
- Create an instance of `NavigationSuiteScaffoldState` using `rememberNavigationSuiteScaffoldState()` and pass it to `NavigationSuiteScaffold`.
- When the navigation area visibility changes, use a `LaunchedEffect` to call `show` or `hide` on the `NavigationSuiteScaffoldState`.

For example:


```kotlin
// Pass this variable to any composable that needs to control the navigation area visibility
var isNavBarVisible by remember { mutableStateOf(true) }
val scaffoldVisibilityState = rememberNavigationSuiteScaffoldState()

NavigationSuiteScaffold(
    navigationSuiteItems = navItems,
    state = scaffoldVisibilityState
) {
    // Main content
}

LaunchedEffect(isNavBarVisible){
    if (isNavBarVisible) {
        scaffoldVisibilityState.show()
    } else {
        scaffoldVisibilityState.hide()
    }
}
```

<br />

## Step 3. Add multi-pane layouts using Navigation 3 Scenes

Analyze the codebase looking for related screens - tapping on something in one
screen opens another screen that shows information related to the first. There
are two canonical screen relationships: list-detail and supporting pane.

IMPORTANT: You must use the Navigation 3 `SceneStrategy` approach to implement
multi-pane layouts. Do not use `ListDetailPaneScaffold` or
`SupportingPaneScaffold`.

### Step 3.1. List-detail

#### Identify the list and detail screens

List-detail layouts display a list of items (this is the list screen) and
clicking on an item opens a new screen that shows more details about that item
(the detail screen).

Typical usage includes productivity apps like email, notes, and messaging.

Unless requested explicitly, avoid this pattern when the detail content requires
substantial screen space (e.g., images or media that benefits from a full-screen
presentation).

#### Add a Material list-detail SceneStrategy

- Add the `androidx.compose.material3.adaptive:adaptive-navigation3` library
- Create an `androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy` using `rememberListDetailSceneStrategy`
- Pass the `ListDetailSceneStrategy` to `NavDisplay` using its `sceneStrategies` parameter

#### Use metadata to identify the list and detail screens

- Add metadata using `entry(metadata = ...)` or `NavEntry(metadata = ...)` to the list entry using `ListDetailSceneStrategy.listPane(detailPlaceholder = {
  <placeholder composable> })`.
- Use the `detailPlaceholder` parameter to add a placeholder on the detail screen when no list items are selected.
- Add metadata to the detail entry using `ListDetailSceneStrategy.detailPane()`.

#### Important considerations

- When a detail screen displays its content full-screen on mobile (content fills the entire screen, bars or rails are hidden), full-screen mode must be deactivated if it's part of a list-detail layout.
- Detail screens must not show a back arrow when on a list-detail layout.

For a reference implementation, check the [Nav3 **Material** List Detail
recipe](references/android/guide/navigation/navigation-3/recipes/material-listdetail.md).

### Step 3.2. Supporting pane

Identify supporting pane screens where a main screen displays a single item, and
selecting it opens a "supporting screen" with more details. The supporting
screen complements the main screen and is shown in a supporting pane.

#### Add a Material supporting pane `SceneStrategy`

- If you haven't already, add the `androidx.compose.material3.adaptive:adaptive-navigation3` library
- Create an `androidx.compose.material3.adaptive.navigation3.SupportingPaneSceneStrategy` using `rememberSupportingPaneSceneStrategy`
- Pass the `SupportingPaneSceneStrategy` to `NavDisplay` using its `sceneStrategies` parameter

#### Use metadata to identify the main and supporting screens

- Add metadata using `entry(metadata = ...)` or `NavEntry(metadata = ...)` to the main entry using `SupportingPaneSceneStrategy.mainPane()`
- Add metadata to the supporting entry using `SupportingPaneSceneStrategy.supportingPane()`

### Step 3.3. Run screenshot tests

If you have made changes, record new reference files. Ask the user to visually
verify that the new layouts are correct.

## Step 4. Make vertical lists adaptive by changing the number of columns

### Step 4.1. Make lazy lists adaptive

Look for the following vertical list composables: `LazyColumn`,
`LazyVerticalGrid`, `LazyVerticalStaggeredGrid`.

Steps to migrate:

- Choose a suitable minimum width in dp for the column. The item must be clearly visible to the user at this width.
- For `LazyColumn`: change to a `LazyVerticalGrid` and follow the instruction later
- For `LazyVerticalGrid`: change the `columns` parameter to use `GridCells.Adaptive(<width>.dp)`
- For `LazyVerticalStaggeredGrid`: change the `columns` parameter to use `StaggeredGridCells.Adaptive(<width>.dp)`

### Step 4.2. Migrate non-lazy lists to Grid

WARNING: Grid is an experimental API available from Compose 1.11.0-beta01.
Confirm with the user that they are happy to use an experimental API in their
codebase.

Look for any `Column` that contains multiple items of the same type and replace
it with `Grid`. Do not replace it with `LazyVerticalGrid` or any other lazy
layout. Do not place `Grid` inside the existing `Column`. Completely replace it.

`Grid` is configured by supplying a lambda (an extension function on
`GridConfigurationScope`) to its `config` parameter. Inside the lambda,
`constraints` provides the minimum and maximum dimensions of the grid container
and can be used to change the number of rows and columns based on the available
size. For example, the following code configures `Grid` such that when the
available width is:

- less than 800dp, a 2x4 grid is used
- 800dp or more, a 4x2 grid is used


```kotlin
Grid(
    config = {
        val maxWidthDp = constraints.maxWidth.toDp()
        val (cols, rows) = if (maxWidthDp < 800.dp){
            2 to 4
        } else{
            4 to 2
        }

        val gapSizeDp = 8.dp
        val cellSize = ((maxWidthDp - (gapSizeDp * (cols - 1))) / cols).coerceAtLeast(0.dp)
        repeat(cols) { column(cellSize) }
        repeat(rows) { row(cellSize) }
        gap(gapSizeDp)
    }
) { /** items **/ }
```

<br />

`Grid` is an experimental API so add the `@OptIn(ExperimentalGridApi::class)`
annotation to any function that uses it.

## Step 5: Hide App Bars when scrolling

In an app with multiple top-level destinations, each screen must manage its own
app bar state independently. There are two main scroll behaviors:

- `exitUntilCollapsedScrollBehavior`: Hides on scroll down, stays hidden while you scroll up until you reach the very top (0 offset).
- `enterAlwaysScrollBehavior`: Hides on scroll down, shows immediately on scroll up.

## Step 6: Preserve UI state across configuration changes

IMPORTANT: **Never** add `android:configChanges` or `android:screenOrientation`
to `AndroidManifest.xml` to suppress Activity recreation or lock orientation.
Opting out of recreation hides state-loss bugs instead of fixing them, breaks
adaptive resource reloading, and prevents an app's process from restarting.

When fixing state loss across configuration changes, systematically audit
**every** usage of `remember` across all screens, components, and state holders
(in other words, do not only search for `remember { mutableStateOf(...) }`):

### Step 6.1. Preserve primitive and custom UI state in Composables

- Replace `remember { mutableStateOf(...) }` with `rememberSaveable { mutableStateOf(...) }` for user input and interactive UI state, such as `TextField` draft text, search queries, scroll position, and toggle states.
- Key state on relevant identifiers when appropriate (e.g., `rememberSaveable(conversationId) { mutableStateOf("") }`).
- For custom data classes or collections (for example, a `Set`) that are not natively `Bundle`-saveable:
  - When delegating a `MutableState<T>` via `var state by rememberSaveable(...) { mutableStateOf(...) }`, pass your custom `Saver` using **`stateSaver`** (do **not** pass `saver = ...` when the block returns `MutableState<T>`):


```kotlin
// this Saver implementation converts CustomState object which we don't know how to save
// to List<String> which we can save
private val CustomStateSaver = Saver<CustomState, List<String>>(
    save = { customState -> customState.values.map { it.value } },
    restore = { values -> CustomState(values.map { CustomStateValue.valueOf(it) }.toSet()) }
)

@Composable
fun MyComposable() {
    var customState by rememberSaveable(stateSaver = CustomStateSaver) {
        mutableStateOf(CustomState())
    }
}
```

<br />

- Alternatively, annotate the class with `@Serializable` and use `rememberSerializable`.

### Step 6.2. Preserve scroll state in lazy and scrollable layouts

Plain `remember { LazyListState() }`, `remember { LazyGridState() }`, and
`remember { ScrollState(...) }` do **not** save scroll position across
configuration changes because they bypass the state class's `Saver`.

Inspect all scrollable containers (`LazyColumn`, `LazyRow`, `LazyVerticalGrid`,
`LazyHorizontalGrid`, `LazyVerticalStaggeredGrid`, `Modifier.verticalScroll`,
and `Modifier.horizontalScroll`) across all screens and reusable components, and
replace plain `remember` instantiations with the built-in saveable factory
functions:

- `remember { LazyListState() }` -\> `rememberLazyListState()`
- `remember { LazyGridState() }` -\> `rememberLazyGridState()`
- `remember { LazyStaggeredGridState() }` -\> `rememberLazyStaggeredGridState()`
- `remember { ScrollState(initial = 0) }` -\> `rememberScrollState()`

### Step 6.3. Preserve Navigation 3 state and multiple back stacks

Check out the Navigation 3 guide to [Save and manage navigation state](references/android/guide/navigation/navigation-3/save-state.md)
for details.

### Step 6.4. Preserve media playback state (`ExoPlayer` / Media3)

When an MediaPlayer (such as `ExoPlayer`) is created inside a composable with `remember`, it is
released on disposal and recreated from scratch on configuration change,
resetting playback to `0:00` and auto-playing even if the user had paused the
video.

To preserve `ExoPlayer` playback state (`currentPosition` and `playWhenReady`):

1. Store `savedPositionMs` (`mutableLongStateOf(0L)`) and `savedPlayWhenReady` (`mutableStateOf(true)`) using `rememberSaveable(videoResId)`.
2. **Keep saved state updated during playback --- do not only save in `onDispose`** : update `savedPlayWhenReady` and `savedPositionMs` in a `Player.Listener` (`onPlayWhenReadyChanged`, `onPositionDiscontinuity`) **and** periodically inside a `LaunchedEffect(player)` loop while active.
3. **Initialize the player with the restored state** : Set `playWhenReady = savedPlayWhenReady` and call `seekTo(savedPositionMs)` before `prepare()`.
4. **Guard `LifecycleStartEffect` so it does not auto-play a paused video or overwrite `savedPlayWhenReady`** :
   - In `LifecycleStartEffect(player)`, only call `player.play()` when `savedPlayWhenReady` is `true` (otherwise a paused video will start playing again after restoration).
   - In `onStopOrDispose`, capture `val wasPlaying = player.playWhenReady` and `savedPositionMs = player.currentPosition` before calling `player.pause()`, then restore `savedPlayWhenReady = wasPlaying` so that `player.pause()` on stop/dispose does not overwrite `savedPlayWhenReady` to `false`:


```kotlin
@UnstableApi
@Composable
private fun rememberExoPlayer(@RawRes videoResId: Int, fromUri: Unit.(Uri) -> MediaItem): ExoPlayer {
    val context = LocalContext.current
    var savedPositionMs by rememberSaveable(videoResId) { mutableLongStateOf(0L) }
    var savedPlayWhenReady by rememberSaveable(videoResId) { mutableStateOf(true) }

    val player = remember(videoResId) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(videoResId)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = savedPlayWhenReady
            seekTo(savedPositionMs)
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                savedPlayWhenReady = playWhenReady
                savedPositionMs = player.currentPosition
            }
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                savedPositionMs = newPosition.positionMs
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            savedPositionMs = player.currentPosition
            delay(200)
        }
    }

    LifecycleStartEffect(player) {
        if (savedPlayWhenReady) {
            player.play()
        }
        onStopOrDispose {
            val wasPlaying = player.playWhenReady
            savedPositionMs = player.currentPosition
            player.pause()
            savedPlayWhenReady = wasPlaying
        }
    }

    return player
}
```

<br />

## Step 7: Add desktop experiences

### Step 7.1: Identify the required desktop feature

Look at the user's prompt, requirements, or uploaded files to determine which
desktop feature needs to be implemented.

### Step 7.2: Run feature-specific processing

Based on the feature identified, you must read the corresponding reference file
and follow its instructions:

- **Keyboard Shortcuts** -\> read [`references/keyboard_shortcuts.md`](references/keyboard_shortcuts.md)
- **Tooltips** -\> read [`references/tooltips.md`](references/tooltips.md)
- **Custom Cursors** -\> read [`references/custom_cursors.md`](references/custom_cursors.md)

Each reference starts by searching the whole app for the targets of that
feature. Write that list down first, implement all of it (not only the first
screen you open), and re-check the list before you finish.

## Step 8. Support foldable devices

Tabletop mode allows users to view and interact with content while their
foldable device is placed on a flat surface. The top screen is angled at a
convenient viewing angle and provides visual content. The bottom screen is flat
on the surface and provides controls for that content.

To support foldable devices, identify any screens that provide the following:

- Media and audio playback or streaming: The video or audio content renders on the upright top display. Comments, details, and playback scrubber controls render on the flat bottom surface.
- Video calls: Remote participants display on the upright screen. Microphone and camera toggles, participant lists, and chat controls sit on the flat bottom surface.
- Camera capture: The camera viewfinder displays on the upright screen. The shutter button, zoom controls, and exposure sliders sit on the flat bottom surface for hands-free photography.
- Photo editing: The image canvas displays unobstructed on the upright screen. Sliders, preset palettes, and curves remain accessible on the flat bottom surface.
- Audio mixing: Waveforms and stem controls display on the upright screen. Turntable decks and pads sit on the flat bottom surface.
- Cooking: Viewing recipe instructions hands-free on the upright screen while keeping timers or step navigation on the bottom screen.
- Slide shows: Viewing presentation slides on the top screen with presenter notes or navigation on the bottom screen.

To create layouts for foldables on these screens, you must read
[tabletop posture](references/foldable-tabletop.md).

## Final step: Build and test

Build the app and run the local tests. If the project has screenshot tests, run
them but DO NOT update the reference images. Prompt the user to do this after
they have viewed the screenshot diffs.

## Additional documentation for experimental adaptive APIs

The following APIs are available from Compose 1.11.0.

### FlexBox

Check the FlexBox documentation:

- [Overview](references/android/develop/ui/compose/layouts/adaptive/flexbox/index.md)
- [Get started - setup](references/android/develop/ui/compose/layouts/adaptive/flexbox/get-started.md)
- [Set container behavior](references/android/develop/ui/compose/layouts/adaptive/flexbox/container-behavior.md)
- [Set item behavior](references/android/develop/ui/compose/layouts/adaptive/flexbox/item-behavior.md)

## MediaQuery

Check the [MediaQuery documentation](references/android/develop/ui/compose/layouts/adaptive/mediaquery/index.md) when you need to query the device's
screen size, pointer precision, keyboard type, whether it has cameras or
microphones, and other device capabilities.

## Grid

Check the Grid documentation when you need to display a fixed number of items in
a grid layout:

- [Overview](references/android/develop/ui/compose/layouts/adaptive/grid/index.md)
- [Get started - setup](references/android/develop/ui/compose/layouts/adaptive/grid/get-started.md)
- [Set container properties](references/android/develop/ui/compose/layouts/adaptive/grid/container-properties.md)
- [Set item properties](references/android/develop/ui/compose/layouts/adaptive/grid/item-properties.md)
