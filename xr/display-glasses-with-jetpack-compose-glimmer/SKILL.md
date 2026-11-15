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
  last-updated: '2026-11-13'
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
| **Intelligent eyewear** | Hands-free eyewear with speakers, camera, and microphone (audio-only or display). |
| **Display glasses** | Eyewear with a private in-lens display for glanceable visuals and audio. |
| **Jetpack Compose Glimmer** | Compose UI toolkit with components, theming, and focus for display glasses. |
| **Projected activity** | Host phone `Activity` whose UI and input are projected to connected eyewear. |
| **Projected device** | XR eyewear device connected to an Android host phone projecting content. |
| **GlimmerTheme** | Root styling provider for `GlimmerColors`, `GlimmerTypography`, `GlimmerShapes`. |
| **Additive display** | Display where black (`#000000`) renders 100% transparent by adding light. |
| **Augmented experiences** | Lightweight, additive Android XR experiences for users on the go. |

## Prerequisites and core constraints

- Target `compileSdk` 37 or higher (`compileSdk = 37` or `compileSdk { version
  = release(37) }`).
- When adding dependencies, follow [Create your first activity for intelligent
  eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/first-activity) and [Declaring Jetpack Compose Glimmer Dependencies](https://developer.android.com/jetpack/androidx/releases/xr-glimmer) (check `gradle/libs.versions.toml` first; never guess `1.0.0-alpha01`):
  - `androidx.xr.glimmer:glimmer` (`1.0.0-alpha21+` or `libs.androidx.glimmer`)
  - `androidx.xr.glimmer:glimmer-google-fonts` (`1.0.0-alpha21+` or `libs.androidx.glimmer.google.fonts`)
  - `androidx.xr.projected:projected` (`1.0.0-alpha13+` or `libs.androidx.xr.projected` or `libs.androidx.projected`)
- **Don't** : Use `MaterialTheme`, Material 3 (`androidx.compose.material3.*`), or Material elevation or shadow modifiers inside Glimmer UI.
- **Do** : Use `GlimmerTheme`, Glimmer components (`androidx.xr.glimmer.*`), `DepthEffectLevels`, and `createGoogleSansFlexTypography()` (`androidx.xr.glimmer.googlefonts`). Minimum text size is 18sp; avoid Thin or Hairline weights.

## 1. Set up dependencies

- **Set up projected activity** : Follow [Create your first activity for
  intelligent eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/first-activity) and [projectedcontext-source.md](references/projectedcontext-source.md). Set
  `android:exported="true"` and `android:requiredDisplayCategory` to
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

- **Mobile app integration** : Add a `Text("Launch on Glasses")` button on the
  host phone. Observe
  `ProjectedContext.isProjectedDeviceConnected(context, coroutineContext)`
  using `collectAsStateWithLifecycle(initialValue = false)` to set `enabled`
  (so it is disabled when disconnected) and launch `GlassesMainActivity` with
  `ProjectedContext.createProjectedActivityOptions(context).toBundle()`:


  ```kotlin
  val options = ProjectedContext.createProjectedActivityOptions(context)
  val intent = Intent(context, ProjectedMainActivity::class.java)
  context.startActivity(intent, options.toBundle())
  ```

  <br />

- **Theming, initial focus, and black background** : Follow
  [glimmersansflextypography-source.md](references/glimmersansflextypography-source.md). In `onCreate` (with
  `@OptIn(ExperimentalComposeUiApi::class)`), set
  `ComposeUiFlags.isInitialFocusOnFocusableAvailable = true`
  (`androidx.compose.ui.ComposeUiFlags`), wrap UI in
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

## 2. Minimize UI and map input controls

- Bottom-align UI (`Alignment.BottomCenter`), show one primary piece of info at a time (`VerticalStack`), and avoid multiple simultaneous cards.
- **Color contrast** : Follow [material-hct-source.md](references/material-hct-source.md) to keep at least a 70% HCT tone difference between foreground and background (`#000000`).
- **Focus and 1D input** : Avoid nesting scrolling controls; use System Back to
  dismiss views. For focus (`androidx.compose.foundation.focusGroup` and
  `androidx.compose.ui.focus.*`) and hardware input, follow [Focus in Jetpack
  Compose Glimmer](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/focus) and [Hardware Controls for display glasses](references/android/design/ui/ai-glasses/guides/interaction/inputs.md):


  ```kotlin
  @Composable
  fun FocusSample(initialFocus: FocusRequester) {
      Box(
          modifier =
              Modifier.focusProperties {
                      onEnter = {
                          initialFocus.requestFocus()
                          cancelFocusChange()
                      }
                  }
                  .focusGroup()
      ) {
          Button(onClick = {}) { Text("First Button") }
      }
  }
  ```

  <br />

## 3. Build with Jetpack Compose Glimmer

### Implementing Glimmer styles

Follow [glimmertheme-source.md](references/glimmertheme-source.md) when using `GlimmerTheme`.

| Category | Token | Value / Role |
|---|---|---|
| **Color** | primary / secondary | `#9BBFFF` / `#4C88E9` (Focal colors) |
| **Color** | positive / negative | `#7AD48F` (Confirmation) / `#F57082` (Alert) |
| **Color** | background / surface | `#000000` (Transparent) / `#262626` (`GlimmerTheme.colors.surface`, `SurfaceDefaults.border`) |
| **Shape** | `large` (Standard) / `small` | `RoundedCornerShape(36.dp)` / `RoundedCornerShape(12.dp)` |

#### Typography scale (Google Sans Flex) and depth levels

Pass `FontVariation.Settings(FontVariation.Setting("ROND", 100f),
FontVariation.width(100f), FontVariation.opticalSizing(9.sp),
FontVariation.weight(...))` to `createGoogleSansFlexTypography`
(`titleLargeVariationSettings`, `bodyMediumVariationSettings`,
`captionVariationSettings`, and other role settings):

| Style | Size / Line-Height | Weight |
|---|---|---|
| **Title Large / Medium / Small** | 30/36.sp, 24/32.sp, 20/28.sp | 725 |
| **Body Large / Medium / Small** | 30/36.sp, 24/32.sp, 20/28.sp | 520 |
| **Caption** | 18.sp / 28.sp | 650 |

Follow [deptheffect-source.md](references/deptheffect-source.md) and [deptheffectlevels-source.md](references/deptheffectlevels-source.md) for
`DepthEffect` and `DepthEffectLevels`: use
`GlimmerTheme.depthEffectLevels.level1` for resting state and `level2` for
focused state (using `SurfaceDepthEffect(depthEffect = level1,
focusedDepthEffect = level2)` on `Modifier.surface` or `Modifier.depthEffect`):


```kotlin
@Composable
fun DepthEffectLevelsSample() {
    val levels = GlimmerTheme.depthEffectLevels
    Box(contentAlignment = Alignment.Center) {
        DepthBox("Level 1", levels.level1)
        DepthBox("Level 2", levels.level2)
        DepthBox("Level 3", levels.level3)
        DepthBox("Level 4", levels.level4)
        DepthBox("Level 5", levels.level5)
    }
}

@Composable
private fun DepthBox(name: String, depthEffect: DepthEffect) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(100.dp)
            .surface(
                interactionSource = interactionSource,
                depthEffect = SurfaceDepthEffect(
                    depthEffect = depthEffect,
                    focusedDepthEffect = depthEffect
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(name)
    }
}
```

<br />

### Implementing Jetpack Compose Glimmer components

#### Cards, ActionCards, ImageCards, and ButtonGroups

Never embed `Card` (`androidx.xr.glimmer.Card`) in `ListItem`. Follow [Jetpack
Compose Glimmer: Cards](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/cards), [card-source.md](references/card-source.md), and
[card-samples-source.md](references/card-samples-source.md).

- **Clickable vs. non-clickable** : Pass `onClick = { ... }` if the whole card is a trigger; omit `onClick` if informational and control focus using `focusable: Boolean` (such as `Card(focusable = false)`).
- **Single primary action (`ActionCard`)** : Use
  `androidx.xr.glimmer.ActionCard` when anchoring a primary action button to
  the bottom of a card:


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

- **Multiple actions (`Card` and `ButtonGroup`)** : Use
  `androidx.xr.glimmer.ButtonGroup` (`rememberButtonGroupState`). **Never nest
  `ButtonGroup` inside `Card { ... }`.** Place `Card(focusable = false)`
  **above** `ButtonGroup` as a sibling in a `Column` to avoid focus
  contention:


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


  ```kotlin
  val scope = rememberCoroutineScope()
  val state = rememberButtonGroupState()
  ButtonGroup(modifier = Modifier.fillMaxWidth(), state = state) {
      Button(onClick = { scope.launch { state.animateScrollToItem(1) } }) {
          Text("Select last item")
      }
      Button(onClick = { scope.launch { state.animateScrollToItem(0) } }) {
          Text("Select first item")
      }
  }
  ```

  <br />

- **Image cards** : Use `ImageCard`, `LeadingImageCard`, or `TrailingImageCard`
  (`image = { ... }`, `title`, `subtitle`, `leadingIcon`, `trailingIcon`) with
  `ContentScale.Crop`, `Outline` (`CardDefaults.shape`), and `maxLines = 10`
  on long body `Text`:


  ```kotlin
  ImageCard(
      image = {
          Image(
              painter = myHeaderImage,
              contentDescription = "Header image",
              contentScale = ContentScale.FillWidth
          )
      },
      title = { Text("Card Title", style = GlimmerTheme.typography.titleMedium) },
      subtitle = { Text("Sub-heading text", style = GlimmerTheme.typography.titleSmall) },
      leadingIcon = { Icon(FavoriteIcon, contentDescription = "Favorite") },
  ) {
      Text(
          "This is the main body content of the card, utilizing theme-tokens for consistent styling.",
          style = GlimmerTheme.typography.bodyMedium
      )
  }
  ```

  <br />

#### Buttons and ToggleButtons

Follow [Jetpack Compose Glimmer: Buttons](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/buttons), [button-source.md](references/button-source.md), and
[button-samples-source.md](references/button-samples-source.md). Use `Button(onClick, buttonSize =
ButtonSize.Large, color = baseColor, focusedColor =
ButtonDefaults.focusedColor(baseColor))` (top-level parameters on `Button`,
never `ButtonDefaults.colors`), `IconButton` for stateless icon
triggers, and always use `ToggleButton(checked, onCheckedChange, leadingIcon)`
or `IconToggleButton(checked, onCheckedChange)` (never stateful `IconButton`)
for toggle or media playback controls:


```kotlin
Button(
    onClick = {},
    focusedColor = brandColor,
) {
    Text("Focus Color Button")
}
```

<br />


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


```kotlin
var checked by remember { mutableStateOf(false) }

val additionalColors =
    IconToggleButtonDefaults.colors(checkedBackgroundColor = Color(0xFF80B799))

IconToggleButton(
    checked = checked,
    onCheckedChange = { checked = it },
    colors = additionalColors
) {
    Icon(
        if (checked) FavoriteIcon else OutlinedFavoriteIcon,
        contentDescription = "Toggle favorite"
    )
}
```

<br />

#### Title chips

`TitleChip` (`androidx.xr.glimmer.TitleChip`) is a non-interactive pill header
(three words or fewer, single line). Follow [Jetpack Compose Glimmer: Title
Chips](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/title-chips), [titlechip-source.md](references/titlechip-source.md), and [titlechip-samples-source.md](references/titlechip-samples-source.md).
When placing a standalone `TitleChip` above a `Card` or `ActionCard`, center it
and space it with `TitleChipDefaults.associatedContentSpacing`:


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

Follow [Jetpack Compose Glimmer: Icons](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/icons), [icon-source.md](references/icon-source.md),
[iconbutton-source.md](references/iconbutton-source.md), and [iconsizes-source.md](references/iconsizes-source.md).

- **Sizing and contrast** : Pass `Modifier.size(GlimmerTheme.iconSizes.small)`,
  `GlimmerTheme.iconSizes.medium`, or `GlimmerTheme.iconSizes.large` to `Icon`
  whenever a small, medium, or large icon is needed (including inside card
  icon slots). **Never** use `Color.Black` or saturated error tints; use
  `Color.Unspecified` for multicolor vectors (`tint = null` on `Painter`). Use
  [Material Symbols](https://fonts.google.com/icons?icon.style=Rounded) (600 weight, Rounded, Unfilled), or build vectors
  with `ImageVector.Builder` or `painterResource` if `material-icons-*` isn't
  in `app/build.gradle.kts`:


  ```kotlin
  @Composable
  fun SizedIconSample() {
      Icon(
          FavoriteIcon,
          contentDescription = "Localized description",
          modifier = Modifier.size(GlimmerTheme.iconSizes.large),
      )
  }
  ```

  <br />

#### Lists

Follow [list-source.md](references/list-source.md), [listitem-source.md](references/listitem-source.md), and
[liststate-source.md](references/liststate-source.md).

- **Always use `GlimmerLazyColumn`** : Import `GlimmerLazyColumn`, `rememberGlimmerLazyListState`, and `items` from `androidx.xr.glimmer.list` (never foundation `LazyColumn`). Use `listState.scrollToItem(0)` or `animateScrollToItem(0)` to scroll to top.
- **Integrated title, spacing, and rows** : Pass `title = { TitleChip {
  Text("...") } }` directly on `GlimmerLazyColumn`, explicitly set
  `verticalArrangement = Arrangement.spacedBy(20.dp)`, and render rows with
  `androidx.xr.glimmer.ListItem(onClick = { ... }, supportingLabel = {
  Text(...) }) { Text(...) }` (never nest `Card` in `ListItem`):


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

Use `VerticalStack` (`androidx.xr.glimmer.stack.VerticalStack`) for
heterogeneous cards one at a time (**never** with `TitleChip`). Follow
[stack-source.md](references/stack-source.md), [stackstate-source.md](references/stackstate-source.md), and
[stackitemscope-source.md](references/stackitemscope-source.md). Align to `Alignment.BottomCenter`, set height to
**`66.dp` taller than the tallest card** (`Modifier.height(maxCardHeight +
66.dp)`), and apply `Modifier.itemDecoration(CardDefaults.shape)` to each item's
`Card`:


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
`rememberGlimmerPagerState(pageCount = { ... })`, `pageSpacing`, and
`pageIndicator = { GlimmerHorizontalPagerDefaults.PageIndicator(state,
selectedIndicatorColor = GlimmerTheme.colors.primary, unselectedIndicatorColor =
GlimmerTheme.colors.primary.copy(alpha =
GlimmerHorizontalPagerDefaults.UnselectedIndicatorAlpha)) }` (never foundation
`HorizontalPager`). Follow [Jetpack Compose Glimmer: Pager](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/pager). Use
`pagerState.animateScrollToPage` in a coroutine for programmatic scroll, and
configure Glimmer [`Text`](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/text) (`androidx.xr.glimmer.Text`, never Material
`Text`) with `style = LocalTextStyle.current.copy(textMotion =
TextMotion.Animated)`:


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


```kotlin
GlimmerHorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    // Use a page numbers instead of the default dot-indicator
    pageIndicator = {
        Text(
            text = "${pagerState.currentPage + 1} / ${pagerState.pageCount}",
            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
        )
    }
)
```

<br />

#### AlertDialogs and custom surfaces

- **`AlertDialog`** : Use `androidx.xr.glimmer.AlertDialog` (never
  `android.app` or Material `AlertDialog`) with `onDismissRequest`,
  `confirmButton`, `dismissButton`, `icon`, `title`, and `text` using Glimmer
  `Button`, `Icon`, and `Text`:


  ```kotlin
  var showDialog by remember { mutableStateOf(true) }

  if (showDialog) {
      AlertDialog(
          onDismissRequest = { showDialog = false },
          confirmButton = { Button(onClick = { /* confirm action */ }) { Text("Confirm") } },
          dismissButton = { Button(onClick = { showDialog = false }) { Text("Dismiss") } },
          icon = { Icon(imageVector = FavoriteIcon, contentDescription = "Alert icon") },
          title = { Text("Title") },
          text = { Text("This is an alert dialog with options to confirm or dismiss.") },
      )
  }
  ```

  <br />


  ```kotlin
  var showDialog by remember { mutableStateOf(true) }

  if (showDialog) {
      AlertDialog(
          onDismissRequest = { showDialog = false },
          confirmButton = { Button(onClick = { showDialog = false }) { Text("OK") } },
          icon = {
              Icon(
                  imageVector = OutlinedFavoriteIcon,
                  contentDescription = "Favorite icon",
              )
          },
          title = { Text("Title") },
          text = { Text("This is an alert dialog with only an option to dismiss.") },
      )
  }
  ```

  <br />

- **`Modifier.surface`** : Follow [surface-source.md](references/surface-source.md). Share one `remember
  { MutableInteractionSource() }` between
  `Modifier.surface(enabled = true, shape = GlimmerTheme.shapes.large, color
  = GlimmerTheme.colors.surface, depthEffect = ..., interactionSource =
  interactionSource)` and `.focusable(interactionSource = interactionSource)`
  or `.clickable(interactionSource = interactionSource, indication = null,
  ...)`, chaining `.surface(...)` **before** `.focusable(...)` or
  `.clickable(...)`:


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

Follow [Jetpack Compose Glimmer: Voice Input Indicator](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/voice-input-indicator) and [Jetpack Compose
Glimmer: Indirect Pointer](https://developer.android.com/develop/xr/jetpack-xr-sdk/jetpack-compose-glimmer/indirect-pointer).

- **`VoiceInputIndicator` and `ContainedVoiceInputIndicator`** : Normalize
  `SpeechRecognizer` `rmsdB` values into `0.0f..1.0f` using `.coerceIn(0f,
  1f)`:


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

- **`Modifier.onIndirectPointerGesture`** : Apply
  `androidx.xr.glimmer.onIndirectPointerGesture` (`onClick`, `onSwipeForward`,
  `onSwipeBackward`) to a container made **focusable** (`focusTarget()` or
  `focusable()`):


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

## 4. Integrate with system UI

- Follow [Understand notification behavior for intelligent eyewear](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/notifications/behavior) and [Start a glasses activity on display glasses from a notification](https://developer.android.com/develop/xr/jetpack-xr-sdk/ai-glasses/notifications/start-activity).
