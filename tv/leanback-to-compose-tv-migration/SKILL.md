---
name: leanback-to-compose-tv-migration
description: Provides instructions and architectural patterns for migrating Android
  TV applications from legacy Leanback UI Toolkit, Android Views, or Support Fragments
  to Jetpack Compose for TV (androidx.tv). Use this skill for Leanback to Compose
  migrations, including browse screen, settings screen, authentication screen, login
  screen, or video playback screen migrations, or when replacing BrowseSupportFragment,
  LeanbackSettingsFragment, PreferenceFragment, BaseLeanbackPreferenceFragmentCompat,
  VideoSupportFragment, GuidedStepSupportFragment, SearchSupportFragment, VerticalGridSupportFragment,
  Presenter, ArrayObjectAdapter, or CursorMapper with modern Compose equivalents,
  implementing immersive carousels with focus memory, Media3 video playback with PlayerSurface,
  or custom 10-foot hero layouts.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-09-03'
  keywords:
  - Android TV
  - Jetpack Compose
  - Leanback
  - migration
  - androidx.tv
  - Carousel
  - 10-foot UI
  - PlayerSurface
  - Media3
  - focusRestorer
  - FocusRequester
  - TV development
  - Android Views
  - LeanbackSettingsFragment
  - PreferenceFragment
  - BrowseSupportFragment
---

## The 10-foot UI

A "10-foot UI" is a design paradigm for televisions that tailors an interface
for viewing from approximately 3 meters (10 feet) away. When designing for this
experience, account for these key characteristics:

- **Viewing distance**: Viewers are sitting far from the screen, "leaning back". Screen layouts are uncluttered, with text and UI elements that are large enough to be comfortably readable from a distance, without dense blocks of text.
- **Color contrast**: To avoid washed out colors on TV displays with low contrast ratios, the design uses high-contrast palettes and distinct visual indicators so focused states remain visible across different TV panels.
- **D-pad navigation** : Interaction relies on a directional remote control with limited 4-way navigation (`Up`, `Down`, `Left`, `Right`) with components organized into clear spatial grids and carousels without focus traps.

## Core architecture and library selection

When migrating an Android TV application to Jetpack Compose, you must use
`androidx.tv` libraries and follow these 10-foot UI patterns:

- **UI modernization**: Focus on custom, cinematic layouts over legacy direct 1:1 templates. You must use Jetpack Compose for TV features like dynamic gradient hero backdrops, custom focus animations, custom navigation drawers, and custom layouts.
- **Primary design system** : You must always use `androidx.tv.material3.*` (`androidx.tv:tv-material`) over mobile `androidx.compose.material3.*`. TV Material 3 provides built-in D-Pad focus handling, focus zoom scaling, and TV-optimized typography and shapes. To set up Compose for TV dependencies, follow [Compose for TV setup](references/android/training/tv/playback/compose/index.md).
- **Focus zoom animation** : For interactive cards, you must use `CompactCard`, `ClassicCard`, or `WideCardContainer` with `scale =
  CardDefaults.scale(focusedScale = 1.1f)` to provide standard TV focus animation.
- **Coil image loading** : To use declarative `AsyncImage(model,
  contentDescription, ...)` without passing an explicit `ImageLoader` parameter, you must include `io.coil-kt:coil-compose` in your Gradle dependencies.
- **Explicit imports** : You must always import TV Material 3 classes explicitly (for example, `import androidx.tv.material3.Surface`, `import
  androidx.tv.material3.ListItem`) instead of using wildcard imports (`import
  androidx.tv.material3.*`).
- **File naming conventions** : You must name Composable screen files after the screen (for example, name `BrowseScreen` as `BrowseScreen.kt`, `PlaybackScreen` as `PlaybackScreen.kt`, and `AuthenticationScreen` as `AuthenticationScreen.kt`). Don't use generic prefixes like `Main`.
- **Overscan and bezels** : You must apply horizontal padding (for example, `horizontal = 48.dp` or `32.dp`, `vertical = 24.dp`) to root containers, carousels, and top bars to prevent clipping.
- **Reading width constrainment** : You must constrain reading width using `Modifier.widthIn(max = 600.dp)` on text columns for long-form text.
- **Media3 Compose dependencies** : Include `androidx.media3:media3-ui-compose` in `app/build.gradle` when modernizing media playback screens.
- **Prohibition of legacy AndroidView wrappers** : Don't use legacy `AndroidView` wrappers to embed View-based components into Jetpack Compose screens. All migrated screens must use Compose components or Media3 Compose surfaces (`PlayerSurface`).

## D-pad focus handling and navigation

Jetpack Compose for TV (`androidx.tv.material3`) requires explicit focus
management, as components don't receive initial focus automatically and
navigation uses 2D spatial coordinates. To configure TV D-pad navigation, follow
instructions in [TV Navigation guide](references/android/training/tv/get-started/navigation.md).

### Initial focus

You must assign initial focus to the primary interactive element on every screen
(such as the first action button, card, or `ListItem`) using `FocusRequester`
when entering a screen. Define `val focusRequester = remember { FocusRequester()
}`, attach `Modifier.focusRequester(focusRequester)` to the primary element, and
request focus inside `LaunchedEffect(Unit) { focusRequester.requestFocus() }`:


```kotlin
val focusRequester = remember { FocusRequester() }
val focusManager = LocalFocusManager.current

LaunchedEffect(Unit) {
    focusRequester.requestFocus()
}
```

<br />

*Note: For screens with dynamic state or pagers (like `OnboardingScreen` using
`HorizontalPager`), you must pass the state key to `LaunchedEffect` (for example
`LaunchedEffect(pagerState.currentPage)`) so that focus is re-applied when the
page changes.*

### Bidirectional focus routing and avoiding focus traps

When interactive elements sit on opposite sides of the display, standard 2D
spatial navigation fails to find targets across them. This creates focus traps
where users are unable to navigate out of an area using the D-pad.

For symmetrical, bidirectional D-pad navigation without focus traps, you must
rely on Compose's 2D spatial focus engine whenever possible. When connecting
adjacent UI elements across scrollable containers (like `LazyColumn` or
`LazyRow`), don't set directional overrides (`up = ...`, `down = ...`) targeting
individual items inside lazy lists. When an item scrolls off-screen during
vertical navigation, its `FocusRequester` becomes uninitialized, throwing
`IllegalStateException` during focus searches:


```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 48.dp, vertical = 16.dp),
    horizontalArrangement = Arrangement.End
) {
    Button(
        onClick = { /* Search */ },
        modifier = Modifier.focusRequester(topBarFocusRequester)
    ) { Text("Search") }
}
```

<br />

### Row focus recollection (`Modifier.focusRestorer`)

When navigating vertically between horizontal carousels (`LazyRow`), Compose's
default 2D spatial focus engine searches along the X coordinate of the focused
item. If a user scrolls right in Row 1 (for example to Item 4 at X=800dp) and
presses DOWN to navigate to Row 2, spatial routing focuses whatever item sits at
X=800dp in Row 2.

To make every row maintain its own recollection of card focus (restoring focus
to the previously visited item when revisited), you must attach
**`Modifier.focusRestorer`** (with no arguments) directly to the `LazyRow`.
Don't pass custom fallback `FocusRequester` lambdas in lazy containers, as
calling `requestFocus` on an unattached or off-screen item during rapid D-pad
scrolling throws `IllegalStateException`.


```kotlin
LazyRow(
    modifier = Modifier.focusRestorer(),
    contentPadding = PaddingValues(horizontal = 48.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    itemsIndexed(videos) { vidIndex, video ->
        CompactCard(
            onClick = { onVideoClick(video) },
            image = {
                AsyncImage(
                    model = video.cardImageUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            },
            title = { Text(video.title) },
            modifier = Modifier
                .then(
                    if (catIndex == 0 && vidIndex == 0) {
                        Modifier.focusRequester(firstCardFocusRequester)
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        focusedVideo = video
                        focusedCategoryIndex = catIndex
                    }
                }
        )
    }
}
```

<br />

### Text input, hardware keyboard enter interception, and IME focus chaining

When migrating search bars or login forms from Leanback
(`SearchSupportFragment`, `GuidedStepSupportFragment`), don't use bare
`BasicTextField` containers or empty `Surface(onClick = {})` wrappers, as they
prevent D-pad CENTER from attaching the virtual keyboard (IME).

1. **Clickable TV surface wrapper with Back-key interception
   (`onPreviewKeyEvent`)** : Wrap standard M3 `TextField` inside a focusable TV `Surface(onClick = { focusRequester.requestFocus() }, scale =
   ClickableSurfaceDefaults.scale(focusedScale = 1.01f), border =
   ClickableSurfaceDefaults.border(focusedBorder = Border(BorderStroke(2.dp,
   Color.White))))` to provide a focused border outline and D-pad focus scaling. You must attach `Modifier.onPreviewKeyEvent` on the text field or wrapper to intercept `Key.Back` and `Key.Escape` so the user's able to remove focus from the input field without exiting the screen.
2. **Why Back-key interception is mandatory** : When editing a text field on Android TV, pressing the D-pad **Back** button normally navigates back and exits the screen. By intercepting `Key.Back` and `Key.Escape` on `KeyUp` in `onPreviewKeyEvent` to stop editing (clearing focus), the user's able to return to D-pad form navigation without being trapped in the text field or accidentally exiting the screen.
3. **IME focus chaining** : For multi-field forms (such as Username and Password in authentication screens), attach `KeyboardActions(onNext = {
   focusManager.moveFocus(FocusDirection.Down) })` with `ImeAction.Next` on top fields to route focus down to the next input box, and `ImeAction.Done` on the bottom field to route focus directly to the submit button:


```kotlin
Surface(
    onClick = { focusRequester.requestFocus() },
    border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = BorderStroke(2.dp, Color.White))),
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
) {
    OutlinedTextField(
        value = username,
        onValueChange = { username = it },
        label = { Text("Username") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    focusManager.moveFocus(FocusDirection.Down)
                    true
                } else false
            }
    )
}

Surface(
    onClick = {},
    border = ClickableSurfaceDefaults.border(focusedBorder = Border(border = BorderStroke(2.dp, Color.White))),
    modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onLoginSuccess() }),
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                    onLoginSuccess()
                    true
                } else false
            }
    )
}
```

<br />

## Lazy containers

When implementing scrollable lists or grids in Jetpack Compose for TV, you must
use standard `LazyColumn`, `LazyRow`, and `LazyVerticalGrid` from
`androidx.compose.foundation.lazy` and `androidx.compose.foundation.lazy.grid`.
For catalog browsing layouts, follow instructions in [Catalog Browser guide](references/android/training/tv/playback/compose/browse.md).

1. **Pivot scrolling with `BringIntoViewSpec`** : When defining a custom pivot scroll line for catalog rows using `BringIntoViewSpec` and `CompositionLocalProvider(LocalBringIntoViewSpec provides ...)`, you must ensure your project compiles against Compose Foundation 1.7.0+ by adding `implementation platform('androidx.compose:compose-bom:2024.06.00')` (or newer) or `implementation 'androidx.compose.foundation:foundation:1.7.0'` in `app/build.gradle`. Without Compose Foundation 1.7.0+, `import
   androidx.compose.foundation.gestures.LocalBringIntoViewSpec` will fail with `Unresolved reference: LocalBringIntoViewSpec`.
2. **Row focus recollection (`Modifier.focusRestorer`)** : Annotate your composable with `@OptIn(ExperimentalFocusRestorerApi::class,
   ExperimentalComposeUiApi::class)` and attach `Modifier.focusRestorer` on every category `LazyRow` to remember and restore the last focused card when navigating vertically across catalog rows.

When populated with focusable TV Material 3 components (`CompactCard`,
`ListItem`, `Button`), standard Compose lazy containers handle 2D D-Pad focus
routing, focus memory, and edge scrolling automatically:


```kotlin
@Composable
fun CatalogBrowser(
    featuredContentList: List<Movie>,
    sectionList: List<Section>,
    modifier: Modifier = Modifier,
    onItemSelected: (Movie) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(sectionList.size) { index ->
            val section = sectionList[index]
            SectionRow(section, onItemSelected = onItemSelected)
        }
    }
}
```

<br />

## Media3 video playback and transport controls

When migrating legacy Leanback video playback (`VideoSupportFragment` /
`PlaybackGlue`), use Compose Media3 `PlayerSurface`
(`androidx.media3.ui.compose.PlayerSurface`) combined with a translucent
transport controls overlay:

1. **Mandatory Media3 transport control buttons (`PlayPauseButton`)** : Over the `PlayerSurface`, you must layer a translucent bottom controls bar (`Box(modifier = Modifier.align(Alignment.BottomCenter))`) containing explicit Media3 UI Compose buttons: at minimum `PlayPauseButton`, `SeekBackButton`, and `SeekForwardButton`. Never leave the transport controls overlay empty. To use these composables, you must add `implementation 'androidx.media3:media3-ui-compose-material3:1.6.0'` alongside `androidx.media3:media3-ui-compose` in your `app/build.gradle` dependencies.
2. **D-pad directional seeking (`onPreviewKeyEvent`)** : To support seeking backward and forward with the remote control D-pad, attach `Modifier.onPreviewKeyEvent` on the container or controls overlay and intercept Compose `Key.DirectionLeft` and `Key.DirectionRight` (for example, `keyEvent.key == Key.DirectionLeft`) to seek backward and forward by 10 seconds (`exoPlayer.seekTo(exoPlayer.currentPosition - 10000)`). Never use legacy Android View keycodes (`KeyEvent.KEYCODE_DPAD_LEFT` or `nativeKeyEvent.keyCode`).
3. **Prohibition of legacy AndroidView wrappers** : Don't wrap legacy `PlayerView` or `StyledPlayerView` in `AndroidView { ... }`. You must use `PlayerSurface` (`androidx.media3.ui.compose.PlayerSurface`) with `ExoPlayer` for video rendering in Compose for TV.

## Phased migration strategy

To migrate an app cleanly without breaking compilation or introducing circular
dependencies, execute in five distinct phases:

- **Phase 1**: Foundation and design system
- **Phase 2**: Leaf and standalone screens
- **Phase 3**: Core browsing and discovery screens
- **Phase 4**: Details and media playback
- **Phase 5**: Final unification and cleanup

### Phase 1: Foundation and design system

- Create `TvTheme.kt` wrapping `TvMaterialTheme` with custom `ColorScheme`, `Typography`, and `Shapes`.
- Build atomic reusable components: `MovieCard`, `SectionHeader`, `LoadingIndicator`, `ErrorState`.

### Phase 2: Leaf and standalone screens

- Migrate screens with no outbound navigation first, such as error messages, onboarding screens, or settings screens, by replacing them with `ErrorDialog`, `OnboardingScreen`, and `SettingsScreen`.
- Replace anything using `BaseLeanbackPreferenceFragmentCompat`, `BaseLeanbackPreferenceFragment`, or `LeanbackSettingsFragment` to use Compose, for example by using `ListItem` + `Switch` bound directly to `SharedPreferences` and replacing the `findPreference` implementation.
- Ensure screens receive initial D-pad focus on the first or main component of that screen using `FocusRequester`, for example on the first `ListItem`.
- Replace legacy `Fragment` classes with activities of type `ComponentActivity` that declaratively use components in Compose.
- Clean up legacy style and theme references in `res/values/styles.xml` and `res/values/themes.xml` (such as removing `preferenceTheme` that points to `@style/PreferenceThemeOverlay.v14.Leanback`) that aren't supported once leanback dependencies are removed:


```kotlin
@Composable
fun TvSettingsScreen(
    modifier: Modifier = Modifier
) {
    var autoPlayNext by remember { mutableStateOf(true) }
    var highQualityAudio by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(48.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                ListItem(
                    selected = false,
                    onClick = { autoPlayNext = !autoPlayNext },
                    headlineContent = { Text("Autoplay Next Video") },
                    supportingContent = { Text("Automatically start playing next item in queue") },
                    trailingContent = {
                        Switch(
                            checked = autoPlayNext,
                            onCheckedChange = null
                        )
                    }
                )
            }

            item {
                ListItem(
                    selected = false,
                    onClick = { highQualityAudio = !highQualityAudio },
                    headlineContent = { Text("High Quality Audio") },
                    supportingContent = { Text("Use spatial audio and multi-channel output when available") },
                    trailingContent = {
                        Switch(
                            checked = highQualityAudio,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    }
}
```

<br />

### Phase 3: Core browsing and discovery screens

- Migrate `VerticalGridScreen` (`LazyVerticalGrid`), `SearchScreen` (`BasicTextField` with live list filtering), and `BrowseScreen` (`LazyColumn` of `LazyRow`s).
- Eradicate legacy `ArrayObjectAdapter`, `ListRowPresenter`, `CardPresenter`, and `HeaderItem` classes.

### Phase 4: Details and media playback

- Migrate `VideoDetailsScreen` and `GuidedStepScreen`.
- Migrate `PlaybackScreen` using Compose Media3 `PlayerSurface` (`androidx.media3.ui.compose.PlayerSurface`) paired with a translucent bottom overlay containing Media3 Compose transport controls (such as `PlayPauseButton`, `SeekBackButton`, `SeekForwardButton`).

### Phase 5: Final unification and cleanup

- Remove all remaining legacy `.java` activities, fragments, presenters, and XML layout files.
- Ensure all activities extend `ComponentActivity` or `FragmentActivity` calling `setContent { ... }`.
- Completely remove legacy Leanback themes and style declarations from `res/values/styles.xml` and `res/values/themes.xml` (for example, any styles inheriting from `Theme.Leanback` or referencing `lb_` styles).

## Component and class mapping guide

| Legacy Leanback / View Class | Modern Jetpack Compose Equivalent |
|---|---|
| `BrowseSupportFragment` / `MainFragment` | `BrowseScreen` (`LazyColumn` containing categorized `LazyRow`s + Hero Banner) |
| `DetailsSupportFragment` | `VideoDetailsScreen` (Poster image, text column, action buttons, related `LazyRow`) |
| `VideoSupportFragment` / `PlaybackGlue` | `PlaybackScreen` (Media3 `ExoPlayer` + Compose `PlayerSurface`) |
| `GuidedStepSupportFragment` | `GuidedStepScreen` (Split-screen layout: 40% left guidance pane, 60% right actions pane) |
| `SearchSupportFragment` | `SearchScreen` (`BasicTextField` + live filtering + `LazyVerticalGrid`) |
| `VerticalGridSupportFragment` | `VerticalGridScreen` (`LazyVerticalGrid(columns = GridCells.Fixed(5))`) |
| `ArrayObjectAdapter` / `Presenter` | Declarative `@Composable` functions observing immutable `State<List<T>>` |
| `CursorMapper` / `LoaderManager` / `CursorLoader` | Kotlin Coroutines / `withContext(Dispatchers.IO)` in a Repository object |
| `OnboardingSupportFragment` | `OnboardingScreen` (`HorizontalPager` + D-Pad navigation buttons) |
| `LeanbackSettingsFragment` / `PreferenceFragment` | `SettingsScreen` (`FocusRequester` on first `ListItem` + trailing `Switch` bound to `SharedPreferences`) |

## Reference implementations and battle-tested patterns

### Modern TV immersive list architecture (`BrowseScreen`)

When building a 10-foot TV browse screen or Immersive List, don't place a hero
banner before or outside a scrolling list, and don't fight Compose's automatic
`BringIntoView` system with programmatic `animateScrollToItem` calls.

Instead, use `BringIntoViewSpec` with `LocalBringIntoViewSpec` from Compose
Foundation to define exact TV pivot scrolling (for example, pivoting active rows
at 35% from the top edge of the display). Combine this with a reshaping
immersive row: when lower rows are focused (`focusedCategoryIndex > 0`), hide
the hero text and display a normal section header on Row 0:


```kotlin
@Composable
fun ImmersiveBrowseScreen(
    categories: Map<String, List<Video>>,
    onVideoClick: (Video) -> Unit
) {
    var focusedVideo by remember { mutableStateOf<Video?>(null) }
    var focusedCategoryIndex by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    val topBarFocusRequester = remember { FocusRequester() }
    val firstCardFocusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = focusedVideo?.bgImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        PositionFocusedItemInLazyLayout(parentFraction = 0.35f, childFraction = 0.5f) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 36.dp, bottom = 64.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 48.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { /* Search */ },
                            modifier = Modifier.focusRequester(topBarFocusRequester)
                        ) { Text("Search") }
                    }
                }

                itemsIndexed(categories.entries.toList()) { catIndex, (categoryName, videos) ->
                    Column {
                        if (catIndex == 0 && focusedCategoryIndex == 0) {
                            Column(
                                modifier = Modifier
                                    .heightIn(min = 200.dp)
                                    .padding(horizontal = 48.dp)
                            ) {
                                Text(
                                    text = focusedVideo?.title ?: "",
                                    style = MaterialTheme.typography.displayMedium
                                )
                                Text(
                                    text = focusedVideo?.description ?: "",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 48.dp, vertical = 8.dp)
                            )
                        }

                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            itemsIndexed(videos) { vidIndex, video ->
                                CompactCard(
                                    onClick = { onVideoClick(video) },
                                    image = {
                                        AsyncImage(
                                            model = video.cardImageUrl,
                                            contentDescription = video.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    },
                                    title = { Text(video.title) },
                                    modifier = Modifier
                                        .then(
                                            if (catIndex == 0 && vidIndex == 0) {
                                                Modifier.focusRequester(firstCardFocusRequester)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                focusedVideo = video
                                                focusedCategoryIndex = catIndex
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PositionFocusedItemInLazyLayout(
    parentFraction: Float = 0.35f,
    childFraction: Float = 0.5f,
    content: @Composable () -> Unit,
) {
    val bringIntoViewSpec = remember(parentFraction, childFraction) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float {
                if (offset >= 0f && offset <= containerSize * 0.45f) {
                    return 0f
                }
                val initialTargetForLeadingEdge = parentFraction * containerSize - (childFraction * size)
                val targetForLeadingEdge = if (size <= containerSize && (containerSize - initialTargetForLeadingEdge) < size) {
                    containerSize - size
                } else {
                    initialTargetForLeadingEdge
                }
                return offset - targetForLeadingEdge
            }
        }
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec, content = content)
}
```

<br />

### Media3 playback in Compose TV (`PlaybackScreen`)

Implement a custom playback screen using
`androidx.media3.ui.compose.PlayerSurface` as the video rendering canvas. Layer
Material3 transport controls (`SeekBackButton`, `PlayPauseButton`,
`SeekForwardButton` from `androidx.media3:media3-ui-compose-material3`) over the
surface in a translucent bottom overlay, and handle D-pad remote key events with
an auto-hide timeout:


```kotlin
@OptIn(UnstableApi::class)
@Composable
fun Media3PlaybackScreen(
    video: Video,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(context) { ExoPlayer.Builder(context).build() }
    var showControls by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var autoHideJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleAutoHide() {
        autoHideJob?.cancel()
        autoHideJob = coroutineScope.launch {
            delay(5000)
            showControls = false
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onFinish()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            autoHideJob?.cancel()
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(video) {
        focusRequester.requestFocus()
        scheduleAutoHide()

        val mediaItem = MediaItem.fromUri(video.videoUrl)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause, Key.Spacebar -> {
                            if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionLeft, Key.MediaRewind -> {
                            val newPos = (exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionRight, Key.MediaFastForward -> {
                            val duration = exoPlayer.duration.coerceAtLeast(0L)
                            val newPos = (exoPlayer.currentPosition + 10_000L).coerceAtMost(duration)
                            exoPlayer.seekTo(newPos)
                            showControls = true
                            scheduleAutoHide()
                            true
                        }
                        Key.DirectionUp, Key.DirectionDown -> {
                            showControls = !showControls
                            if (showControls) scheduleAutoHide() else autoHideJob?.cancel()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        contentAlignment = Alignment.Center
    ) {
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier.fillMaxSize()
        )

        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
                    .padding(48.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SeekBackButton(player = exoPlayer)
                    PlayPauseButton(player = exoPlayer)
                    SeekForwardButton(player = exoPlayer)
                }
            }
        }
    }
}
```

<br />

### Replacing CursorLoader with reactive coroutine flow

Replace legacy `LoaderManager.LoaderCallbacks<Cursor>` and `CursorObjectAdapter`
with a repository returning a `Flow` that observes database changes and triggers
asynchronous fetching when empty:


```kotlin
object VideoFlowRepository {
    fun getVideosFlow(context: Context, contentUri: Uri): Flow<List<Video>> = callbackFlow {
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryVideos(context, contentUri))
            }
        }

        context.contentResolver.registerContentObserver(
            contentUri,
            true,
            contentObserver
        )

        val initialVideos = queryVideos(context, contentUri)
        trySend(initialVideos)

        awaitClose {
            context.contentResolver.unregisterContentObserver(contentObserver)
        }
    }.flowOn(Dispatchers.IO)

    private fun queryVideos(context: Context, uri: Uri): List<Video> {
        // Query database or ContentProvider
        return emptyList()
    }
}
```

<br />
