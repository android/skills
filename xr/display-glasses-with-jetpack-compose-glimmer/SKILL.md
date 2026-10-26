---
name: display-glasses-with-jetpack-compose-glimmer
description: Provides guidelines for developing projected Android XR apps for display
  glasses using the Jetpack Compose Glimmer UI toolkit. Covers Glimmer design principles,
  workflows, and interaction models for the glasses form factor. Use this skill to
  build an Android XR Augmented Experience app with Jetpack Compose Glimmer. Don't
  use for audio-only AI glasses without a display, non-glasses Android XR headsets
  (spatial panels or 3D volumes with Jetpack Compose for XR), or phone and tablet
  Material 3 UIs.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-24'
  keywords:
  - Jetpack Compose Glimmer
  - display glasses
  - Projected Activity
  - GlimmerTheme
  - Additive Display
  - Android XR - Augmented Experiences
---

## Glossary

| Term | Definition |
|---|---|
| **Intelligent eyewear** | Hands-free eyewear with speakers, camera, and microphone (audio-only or display glasses). |
| **Display glasses** | Eyewear with a private in-lens display for glanceable visuals that pair with audio. |
| **Jetpack Compose Glimmer** | Compose UI toolkit providing components, theming, and focus behavior for display glasses. |
| **Projected activity** | An `Activity` running on a phone host whose UI and input are projected to connected eyewear. |
| **Projected device** | An XR eyewear device connected to an Android host phone that projects app content to it. |
| **GlimmerTheme** | Root styling provider for `GlimmerColors`, `GlimmerTypography`, and `GlimmerShapes`. |
| **Additive display** | Display where black (`#000000`) renders as 100% transparent by adding light to the scene. |
| **Augmented experiences** | Lightweight, additive Android XR experiences that help users while they're on the go. |
| **Visual angle** | Perceived size in XR; minimum readable text is 0.6 degrees (about 18sp at 1 m). |

## Prerequisites

- Target `compileSdk` 37 or higher (`compileSdk = 37` or `compileSdk { version
  = release(37) }`). Update `compileSdk` to 37 if lower.
- When adding dependencies, follow the instructions in [Create your first
  activity for intelligent eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/first-activity):
  - `androidx.xr.glimmer:glimmer` (minimum `1.0.0-alpha21`, or catalog alias `libs.androidx.glimmer`)
  - `androidx.xr.glimmer:glimmer-google-fonts` (minimum `1.0.0-alpha21`, or catalog alias `libs.androidx.glimmer.google.fonts`)
  - `androidx.xr.projected:projected` (minimum `1.0.0-alpha13`, or catalog alias `libs.androidx.xr.projected` or `libs.androidx.projected`)
  - Check `gradle/libs.versions.toml` first; if catalog entries exist, use them in `app/build.gradle.kts` and verify their versions meet the minimums. Never guess `1.0.0-alpha01` for `glimmer-google-fonts`.

## Core constraints and limitations

- **Don't** : Use `MaterialTheme` or Material 3 components (`androidx.compose.material3.*`) inside Projected Activities or Glimmer UI.
- **Do** : Use `GlimmerTheme` and Glimmer components (`androidx.xr.glimmer.*`).
- **Do** : Always pass `createGoogleSansFlexTypography()` from `androidx.xr.glimmer.googlefonts` as `typography` in `GlimmerTheme`.
- Specific hardware device models or unique capabilities aren't detailed.
- **Elevation** : Material 3 shadow or elevation modifiers aren't supported. Use `ShadowScope` or `Depth` tokens from Jetpack Compose Glimmer.
- **Typography limits**: Minimum readable text size is 18sp (0.6°). Avoid "Thin" or "Hairline" weights, which shimmer on additive lenses.

## 1. Set up dependencies

- **Set up projected activity** : When creating a projected activity, follow
  [Create your first activity for intelligent eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/first-activity) and
  [projectedcontext-source.md](references/projectedcontext-source.md). Set `android:requiredDisplayCategory` to
  `android.hardware.display.category.XR_PROJECTED` in `AndroidManifest.xml`:


  ```xml
  <activity
      android:name="com.example.xr.projected.ProjectedMainActivity"
      android:exported="true"
      android:requiredDisplayCategory="android.hardware.display.category.XR_PROJECTED">
      <intent-filter>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.XR_PROJECTED_LAUNCHER"/>
      </intent-filter>
  </activity>
  ```

  <br />

- **Mobile app integration** : In the host phone app, add a button with
  `Text("Launch on Glasses")`. Observe
  `ProjectedContext.isProjectedDeviceConnected(context, coroutineContext)`
  using
  `collectAsStateWithLifecycle(initialValue = false)` to set the button's
  `enabled` state so it's disabled when glasses are disconnected. When
  clicked, launch `GlassesMainActivity` on the glasses display using
  `ProjectedContext.createProjectedActivityOptions(context).toBundle()` as
  shown in the following snippet:


  ```kotlin
  val options = ProjectedContext.createProjectedActivityOptions(context)
  val intent = Intent(context, ProjectedMainActivity::class.java)
  context.startActivity(intent, options.toBundle())
  ```

  <br />

- **UI library and font** : When declaring the Glimmer, Google Fonts, and
  Projected artifacts in `app/build.gradle.kts`, follow [Declaring Jetpack
  Compose Glimmer Dependencies](https://developer.android.com/jetpack/androidx/releases/xr-glimmer), and follow
  [glimmersansflextypography-source.md](references/glimmersansflextypography-source.md) when configuring Google Sans Flex
  using `createGoogleSansFlexTypography`.

- **Theming, initial focus, and black background** : On additive displays, pure
  black (`Color.Black` or `GlimmerTheme.colors.background`) renders as 100%
  transparent. In `onCreate` (annotated with
  `@OptIn(ExperimentalComposeUiApi::class)`), set
  `ComposeUiFlags.isInitialFocusOnFocusableAvailable = true`
  (`androidx.compose.ui.ComposeUiFlags`), wrap content in
  `GlimmerTheme(typography = createGoogleSansFlexTypography())`, and apply
  `Modifier.background(Color.Black)` (or `GlimmerTheme.colors.background`) to
  the root container:


  ```kotlin
  @OptIn(ExperimentalProjectedApi::class)
  class ProjectedMainActivity : ComponentActivity() {

      private var displayController: ProjectedDisplayController? = null
      private var isVisualUiSupported by mutableStateOf(false)
      private var areVisualsOn by mutableStateOf(true)

      override fun onCreate(savedInstanceState: Bundle?) {
          super.onCreate(savedInstanceState)

          lifecycle.addObserver(object : DefaultLifecycleObserver {
              override fun onDestroy(owner: LifecycleOwner) {
                  displayController?.close()
                  displayController = null
              }
          })

          initializeGlassesFeatures()

          setContent {
              GlimmerTheme {
                  HomeScreen(
                      areVisualsOn = areVisualsOn,
                      isVisualUiSupported = isVisualUiSupported,
                      onClose = { finish() }
                  )
              }
          }
      }

      private fun initializeGlassesFeatures() {
          lifecycleScope.launch {
              // Check device capabilities
              val projectedDeviceController = ProjectedDeviceController.create(this@ProjectedMainActivity)
              isVisualUiSupported = projectedDeviceController.capabilities.contains(CAPABILITY_VISUAL_UI)

              val controller = ProjectedDisplayController.create(this@ProjectedMainActivity)
              displayController = controller
              val observer = GlassesLifecycleObserver(
                  context = this@ProjectedMainActivity,
                  controller = controller,
                  onVisualsChanged = { visualsOn -> areVisualsOn = visualsOn }
              )
              lifecycle.addObserver(observer)
          }
      }
  }
  ```

  <br />

- **Hardware capabilities, permissions, and access** : When checking hardware
  capabilities, requesting permissions, or accessing glasses hardware, follow
  [Check device capabilities at runtime](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/check-capabilities), [Request hardware
  permissions](references/android/develop/xr/jetpack-xr-sdk/request-hardware-permissions.md), and [Use a projected context to access hardware](references/android/develop/xr/jetpack-xr-sdk/access-hardware-projected-context.md).

## 2. Minimize and translate the UI

- Build UI with Jetpack Compose Glimmer components and use depth for priority.
- Design from the bottom up and always bottom-align UI (`Alignment.BottomCenter`).
- **One thing at a time** : Show one primary piece of information at a time (for example, using `VerticalStack`) and avoid multiple simultaneous cards.
- **Color contrast** : When choosing colors, follow [material-hct-source.md](references/material-hct-source.md) to maintain at least a 70% tone difference between foreground and background in HCT.

## 3. Map input controls

- Map tap and swipe interactions to the glasses touchpad. Inputs are one-dimensional; avoid nesting scrolling controls.
- Use System Back to dismiss temporary states or detailed views.
- When implementing focus navigation and hardware input handling, follow [Focus in Jetpack Compose Glimmer](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/focus.md) and [Hardware Controls for display
  glasses](references/android/design/ui/ai-glasses/guides/interaction/inputs.md). Focus movement is continuous for scrollable containers and discrete for elements like button rows.

## 4. Build with Jetpack Compose Glimmer

### Implementing Glimmer styles

When accessing theme tokens using `GlimmerTheme`, follow
[glimmertheme-source.md](references/glimmertheme-source.md).

| Category | Token | Value / Role |
|---|---|---|
| **Color** | primary / secondary | `#9BBFFF` / `#4C88E9` (Focal colors) |
| **Color** | positive / negative | `#7AD48F` (Confirmation) / `#F57082` (Alert) |
| **Color** | background | `#000000` (Pure black, renders 100% transparent) |
| **Color** | surface | `#262626` (Use `SurfaceDefaults.border` for borders) |
| **Shape** | Standard / Small | `RoundedCornerShape(36.dp)` / `RoundedCornerShape(12.dp)` |

#### Typography scale (Google Sans Flex) and depth levels

Use Google Sans Flex with variable axes `ROND` = `100f`, `wdth` = `100f`, `opsz`
= `9f`, and `wght` by role:

| Style | Size / Line-Height | Weight |
|---|---|---|
| **Title Large / Medium / Small** | 30/36.sp, 24/32.sp, 20/28.sp | 725 |
| **Body Large / Medium / Small** | 30/36.sp, 24/32.sp, 20/28.sp | 520 |
| **Caption** | 18.sp / 28.sp | 650 |

When simulating depth with `DepthEffect` and `DepthEffectLevels`, follow
[deptheffect-source.md](references/deptheffect-source.md) and [deptheffectlevels-source.md](references/deptheffectlevels-source.md): use `level1`
for resting cards and background UI, and `level2` for focused or pressed
interactive UI.

### Implementing Jetpack Compose Glimmer components

#### Cards, ActionCards, ImageCards, and ButtonGroups

Cards (`androidx.xr.glimmer.Card`) group related content into a single focal
point. Never embed a `Card` within a `ListItem`. When implementing cards, follow
the instructions in [Jetpack Compose Glimmer: Cards](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/cards.md), [card-source.md](references/card-source.md),
and [card-samples-source.md](references/card-samples-source.md).

- **Clickable versus non-clickable card** : Pass `onClick = { ... }` if the entire `Card` acts as a trigger; omit `onClick` if informational. Control focus on non-clickable cards with `focusable: Boolean = true` (such as `Card(focusable = false)`).
- **Single primary action (`ActionCard`)** : When a card has a single primary
  button action anchored to its bottom edge, use
  `androidx.xr.glimmer.ActionCard` instead of placing a `Button` inside
  `Card`. Its `action` button takes focus directly:


  ```kotlin
  ActionCard(
      action = {
          Button(onClick = {}, trailingIcon = { Icon(FavoriteIcon, "Localized description") }) {
              Text("Send")
          }
      }
  ) {
      Text("This is a card with a title, leading icon, header image, and action")
  }
  ```

  <br />

- **Multiple actions (`Card` and `ButtonGroup`)---avoiding focus contention** :
  When pairing a card with multiple action buttons, use
  `androidx.xr.glimmer.ButtonGroup` (with optional
  `rememberButtonGroupState`). **Never nest `ButtonGroup` inside `Card { ...
  }`.** Place a non-focusable `Card(focusable = false)` (without `onClick`)
  **above** `ButtonGroup` as a sibling in a `Column` so the card and buttons
  don't compete for focus:


  ```kotlin
  ButtonGroup(modifier = Modifier.fillMaxWidth()) {
      Button(onClick = {}) { Text("Button 1") }
      Button(onClick = {}) { Text("Button 2") }
      Button(onClick = {}) { Text("Button 3") }
      Button(onClick = {}) { Text("Button 4") }
      Button(onClick = {}) { Text("Button 5") }
  }
  ```

  <br />

- **Image cards** : Use `ImageCard`, `LeadingImageCard`, or `TrailingImageCard`
  to display prominent imagery alongside card text.

#### Buttons and ToggleButtons

When implementing buttons or toggle buttons, follow the instructions in [Jetpack
Compose Glimmer: Buttons](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/buttons.md), [button-source.md](references/button-source.md), and
[button-samples-source.md](references/button-samples-source.md).

- **Standard and icon buttons** : Use `Button` for text actions (with optional `leadingIcon`/`trailingIcon`) and `IconButton` for icon-only triggers (such as media next-track or previous-track buttons).
- **Toggle buttons (`ToggleButton` and `IconToggleButton`)** : Use
  `ToggleButton` or `IconToggleButton` for stateful on/off or play/pause
  controls that morph based on `checked`:


  ```kotlin
  @Composable
  fun ToggleButtonWithLeadingIconSample() {
      var checked by remember { mutableStateOf(false) }

      ToggleButton(
          checked = checked,
          leadingIcon = {
              Icon(
                  if (checked) FavoriteIcon else OutlinedFavoriteIcon,
                  contentDescription = "Toggle favorite"
              )
          },
          onCheckedChange = { checked = it }
      ) {
          Text(if (checked) "On" else "Off")
      }
  }
  ```

  <br />

#### Title chips

`TitleChip` (`androidx.xr.glimmer.TitleChip`) is a non-interactive pill header
(three words or fewer, single line). When implementing title chips, follow the
instructions in [Jetpack Compose Glimmer: Title Chips](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/title-chips.md),
[titlechip-source.md](references/titlechip-source.md), and [titlechip-samples-source.md](references/titlechip-samples-source.md).

- **Spatial spacing** : When placing a standalone `TitleChip` above a `Card` or
  `ActionCard`, center the `TitleChip` horizontally and separate it from the
  card using `TitleChipDefaults.associatedContentSpacing` (using `Spacer` or
  `Arrangement.spacedBy(TitleChipDefaults.associatedContentSpacing)`):


  ```kotlin
  @Composable
  fun TitleChipWithCardSample() {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
          TitleChip { Text("Title Chip") }

          Spacer(Modifier.height(TitleChipDefaults.associatedContentSpacing))

          Card(
              title = { Text("Title") },
              subtitle = { Text("Subtitle") },
              leadingIcon = { Icon(FavoriteIcon, "Localized description") },
          ) {
              Text("Card Content")
          }
      }
  }
  ```

  <br />

#### Icons

When adding icons or icon buttons, follow the instructions in [Jetpack Compose
Glimmer: Icons](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/icons.md), [icon-source.md](references/icon-source.md), [iconbutton-source.md](references/iconbutton-source.md), and
[iconsizes-source.md](references/iconsizes-source.md).

- **Sizing and interactivity** : Size icons with `GlimmerTheme.iconSizes` (`small`, `medium`, `large`). Use `IconButton` or `IconToggleButton` for triggers and `Icon` for status indicators.
- **Contrast and library** : **Never** use pure black (`Color.Black`) for icon tints; black renders as transparent. Use [Material Symbols](https://fonts.google.com/icons?icon.style=Rounded) (600 weight, Rounded, Unfilled). If `androidx.compose.material:material-icons-*` isn't in `app/build.gradle.kts`, build vectors with `ImageVector.Builder` or `painterResource` instead of importing `androidx.compose.material.icons.*`.

#### Lists

Use `GlimmerLazyColumn` (`androidx.xr.glimmer.list.GlimmerLazyColumn`) for
homogeneous scrollable items. When building scrollable lists and list items,
follow the API definitions in [list-source.md](references/list-source.md), [listitem-source.md](references/listitem-source.md),
and [liststate-source.md](references/liststate-source.md).

- **Always use `GlimmerLazyColumn`** : Never use foundation `LazyColumn`. Import `GlimmerLazyColumn`, `rememberGlimmerLazyListState`, and `items` from `androidx.xr.glimmer.list` (`item` is a scope member, not imported). For programmatic scrolling to the first item, hoist `val listState =
  rememberGlimmerLazyListState()` and call `listState.scrollToItem(0)` or `listState.animateScrollToItem(0)`.
- **Integrated title, spacing, and rows** : Pass headers using `title = {
  TitleChip { Text("...") } }` directly on `GlimmerLazyColumn` (not outside
  it), set `verticalArrangement = Arrangement.spacedBy(20.dp)`, and render
  rows with `androidx.xr.glimmer.ListItem(onClick = { ... }, supportingLabel =
  { Text(...) }) { Text(...) }` (pass the primary label as the trailing
  `content` lambda, not `headlineContent`, and never nest a `Card` inside
  `ListItem`):


  ```kotlin
  @Composable
  fun GlimmerLazyColumnWithTitleChipSample() {
      val ingredientItems =
          listOf("Milk", "Flour", "Egg", "Salt", "Apples", "Butter", "Vanilla", "Sugar", "Cinnamon")
      GlimmerLazyColumn(title = { TitleChip { Text("Ingredients") } }) {
          items(ingredientItems) { text -> ListItem { Text(text) } }
      }
  }
  ```

  <br />

#### Stacks

Use `VerticalStack` (`androidx.xr.glimmer.stack.VerticalStack`) to display
heterogeneous cards one at a time (such as calendar, transit, and weather
cards). **Never** use a `TitleChip` with `VerticalStack`. When implementing a
`VerticalStack`, follow the API definitions in [stack-source.md](references/stack-source.md),
[stackstate-source.md](references/stackstate-source.md), and [stackitemscope-source.md](references/stackitemscope-source.md).

- **Bottom alignment, height (+66.dp), and `itemDecoration`** : Align
  `VerticalStack` to `Alignment.BottomCenter`, set its height to **`66.dp`
  taller than the tallest card** (`maxCardHeight + 66.dp`), and apply
  `Modifier.itemDecoration(CardDefaults.shape)` to the `Card` inside every
  `item { ... }` block so background cards are clipped and masked:


  ```kotlin
  @Composable
  fun VerticalStackSample() {
      VerticalStack {
          item {
              Card { Text("Top Card") }
          }
          item {
              Card { Text("Second Card") }
          }
          item {
              Card { Text("Third Card") }
          }
      }
  }
  ```

  <br />

#### Horizontal pagers and animated text

Use `GlimmerHorizontalPager`
(`androidx.xr.glimmer.pager.GlimmerHorizontalPager`) with
`rememberGlimmerPagerState(pageCount = { ... })` for horizontal card
walkthroughs (never foundation `HorizontalPager`).

- **Smooth text motion** : Inside pager pages, configure Glimmer [`Text`](references/android/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/text.md)
  (`androidx.xr.glimmer.Text`, never `androidx.compose.material3.Text`) with
  `textMotion = TextMotion.Animated` (using `.copy(textMotion =
  TextMotion.Animated)`) for smooth rendering during snap transitions:


  ```kotlin
  // Hoist the pager state, specifying the total page count with a lambda.
  val pagerState = rememberGlimmerPagerState(pageCount = { 10 })

  GlimmerHorizontalPager(
      state = pagerState,
      modifier = Modifier.fillMaxSize(),
  ) { page ->
      // Use Glimmer components like Card and Text for optimized glasses styling.
      Card(modifier = Modifier.fillMaxWidth()) {
          Text(
              text = "Page: $page",
              // Recommended: use TextMotion.Animated for smooth transitions in a pager.
              style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
          )
      }
  }
  ```

  <br />

#### Surface

`Modifier.surface` (`androidx.xr.glimmer.surface`) builds custom interactive
components (such as status badges without `Card` or `Button`). When creating
custom surfaces, follow the API definitions in [surface-source.md](references/surface-source.md).

- **Shared `MutableInteractionSource` and order** : Create a single `remember {
  MutableInteractionSource() }` and pass it to both
  `Modifier.surface(interactionSource = interactionSource, ...)` and
  `.clickable(interactionSource = interactionSource, ...)` (or `.focusable`),
  chaining `.surface(...)` **before** `.clickable(...)`:


  ```kotlin
  @Composable
  fun FocusableSurfaceSample() {
      val interactionSource = remember { MutableInteractionSource() }
      Box(
          modifier = Modifier
              .size(100.dp)
              .surface(interactionSource = interactionSource)
              .focusable(interactionSource = interactionSource),
          contentAlignment = Alignment.Center
      ) {
          Text("Focusable")
      }
  }

  @Composable
  fun ClickableSurfaceSample() {
      val interactionSource = remember { MutableInteractionSource() }
      Box(
          modifier = Modifier
              .size(100.dp)
              .surface(interactionSource = interactionSource)
              .focusable(interactionSource = interactionSource)
              .clickable(
                  interactionSource = interactionSource,
                  indication = null
              ) { /* Handle click action */ },
          contentAlignment = Alignment.Center
      ) {
          Text("Clickable")
      }
  }
  ```

  <br />

#### Voice input indicator and indirect pointer gestures

- **`VoiceInputIndicator` and `ContainedVoiceInputIndicator`** : Pass a `level`
  lambda returning a `Float` normalized to `0.0f..1.0f`. When driven by
  `SpeechRecognizer` (`RecognitionListener.onRmsChanged(rmsdB: Float)`),
  normalize and clamp `rmsdB` with `.coerceIn(0f, 1f)`:


  ```kotlin
  // Example state variable to hold the normalized level (0.0 to 1.0)
  // that the VoiceInputIndicator component expects.
  val audioLevel = MutableStateFlow(0f)

  // Initialize the Android SpeechRecognizer
  val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

  // Listener to capture speech events and audio level changes
  val listener = object : RecognitionListener {
      override fun onRmsChanged(rmsdB: Float) {
          // Normalize raw dB level to a 0.0-1.0 range.
          // Android SpeechRecognizer's rmsdB typically ranges from 0 to ~10.
          audioLevel.value = ((rmsdB - 1f) / 9f).coerceIn(0f, 1f)
      }

      // ... Implement other required RecognitionListener methods ...
      override fun onReadyForSpeech(params: Bundle?) {}
      override fun onBeginningOfSpeech() {}
      override fun onEndOfSpeech() {}
      override fun onError(error: Int) {}
      override fun onResults(results: Bundle?) {}
      override fun onPartialResults(partialResults: Bundle?) {}
      override fun onEvent(eventType: Int, params: Bundle?) {}
      override fun onBufferReceived(buffer: ByteArray?) {}
  }

  // Attach the listener to the recognizer
  speechRecognizer.setRecognitionListener(listener)

  // Create an intent to specify the recognition model and behavior
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
      putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
  }
  // Begin listening for audio input
  speechRecognizer.startListening(intent)
  ```

  <br />


  ```kotlin
  @Composable
  fun VoiceInputExample(modifier: Modifier) {
      // Collect the flow as Compose State so the UI reacts to changes in real-time.
      val currentLevel by audioLevel.collectAsState()

      VoiceInputIndicator(
          // The VoiceInputIndicator component provides a visual "pulse" or indicator
          // that changes based on the 'level' lambda, which returns a Float between 0.0 and 1.0.
          level = { currentLevel },
          modifier = modifier
      )
  }
  ```

  <br />

- **`Modifier.onIndirectPointerGesture`** : Handle low-level touchpad tap
  (`onClick`), forward swipe (`onSwipeForward`), and backward swipe
  (`onSwipeBackward`) gestures using
  `androidx.xr.glimmer.onIndirectPointerGesture` on a container made
  **focusable** (using `focusTarget`, `focusable`, or `surface(focusable =
  true)`):


  ```kotlin
  @Composable
  fun OnIndirectPointerGestureSample() {
      var lastGesture by remember { mutableStateOf("None") }

      Box(
          modifier =
              Modifier.fillMaxSize()
                  .onIndirectPointerGesture(
                      enabled = true,
                      onSwipeForward = { lastGesture = "Forward" },
                      onSwipeBackward = { lastGesture = "Backward" },
                      onClick = { lastGesture = "Click" },
                  )
                  .focusTarget(),
          contentAlignment = Alignment.Center,
      ) {
          Text("Last Gesture: $lastGesture")
      }
  }
  ```

  <br />

## 5. Integrate with system UI

- When handling notifications or starting a projected activity from a notification, follow the instructions in [Understand notification behavior
  for intelligent eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/notifications/behavior) and [Start a glasses activity on display
  glasses from a notification](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/notifications/start-activity).
