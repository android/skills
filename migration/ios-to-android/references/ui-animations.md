> [!IMPORTANT]
> **Important:** **ORCHESTRATION GATEWAY PREREQUISITE** : This document is a supplemental reference guide for [`../SKILL.md`](https://developer.android.com/agents/skills/migration/ios-to-android/skill). You **MUST** read and follow the primary orchestration protocol in `../SKILL.md` before applying any patterns from this guide. `SKILL.md` enforces mandatory incremental compilation gates (`./gradlew compileDebugKotlin`), vertical feature slicing, and strict anti-premature-exit execution discipline.

This skill maps iOS `SwiftUI` animation constructs and workflows to their
Android equivalents using Jetpack Compose.

**Official Reference Links:**

- [Animation in Jetpack Compose \| Android Developers](https://developer.android.com/develop/ui/compose/animation/introduction)
- [Graphics in Jetpack Compose \| Android Developers](https://developer.android.com/develop/ui/compose/graphics)

## Prerequisites

Before using this skill, you **Must** ensure the following prerequisites are
met:

- **Source Context:** Access to the existing iOS implementation using `SwiftUI` animations is **Mandatory**.
- **Software Requirements:** Android Gradle Plugin (AGP) 8.0+, Kotlin 1.8+, Jetpack Compose BOM 2023.08.00+, and Jetpack Compose Animation (`androidx.compose.animation:animation:1.6+`) are **Mandatory**.
- **Skill Dependencies:** Familiarity with basic Jetpack Compose state management is **Mandatory**.

## Limitations

This skill has the following limitations:

- **Compose Focus:** Examples are tailored exclusively for Jetpack Compose. It does not cover traditional Android View animations or XML-based transitions.
- **SwiftUI Focus:** Focuses on modern SwiftUI animation APIs (iOS 13+), not legacy UIKit animations.

## Glossary

| iOS Term (`SwiftUI`) | Android Equivalent (`Compose`) | Definition |
|---|---|---|
| `withAnimation` | `animate*AsState` / `LaunchedEffect` | Explicitly triggering an animation on state change. |
| `.animation(..., value:)` | `animate*AsState` | Implicitly animating a value when state changes. |
| `.transition()` | `AnimatedVisibility` | Defining enter/exit transitions for views appearing/disappearing. |
| `AnimatedContent` | `AnimatedContent` | Animating transitions between different content based on state. |
| `spring()` | `spring()` | Physics-based animation with damping and stiffness. |
| `keyframes()` | `keyframes()` | Defining multi-step animations with specific durations and interpolations. |
| `matchedGeometryEffect` | `SharedTransitionLayout` | Shared element transitions between different screens or layouts. |

## Setup \& Dependencies

You **Must** add the following dependencies to your `build.gradle.kts` file to
enable full animation support. Using the Compose Bill of Materials (BOM) is
**Mandatory** to ensure library compatibility.

```kotlin
dependencies {
   // Import the Compose BOM
   val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
   implementation(composeBom)

   // Import animation libraries without versions (handled by BOM)
   implementation("androidx.compose.animation:animation")
   implementation("androidx.compose.animation:animation-graphics")
}
```

## Instructions: Mapping Concepts

### 1. State-Driven Property Animations

Map iOS implicit or explicit state animations to Compose `animate*AsState`.

**Logic:**

- **IF** animating a single numeric or color property (e.g., height, opacity, color) based on a state change, **THEN** you **Must** use the corresponding `animate*AsState` function (e.g., `animateDpAsState`, `animateFloatAsState`, `animateColorAsState`).
- **IF** you need fine-grained control over the animation spec (duration, easing), **THEN** you **Must** provide an `animationSpec` parameter.

**iOS**
\`\`\`swift
@State private var isExpanded = false

// ...
Text("Content")
.frame(height: isExpanded ? 200 : 100)
.animation(.easeInOut(duration: 0.3), value: isExpanded)
\`\`\`

**Android**
\`\`\`kotlin
var isExpanded by remember { mutableStateOf(false) }

val height by animateDpAsState(
targetValue = if (isExpanded) 200.dp else 100.dp,
animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
)

Box(modifier = Modifier.height(height)) {
Text("Content")
}
\`\`\`

### 2. Visibility Transitions

Map iOS `.transition()` modifiers to Compose `AnimatedVisibility`.

**Logic:**

- **IF** a view appears or disappears conditionally (e.g., `if (isVisible)`), **THEN** you **Must** wrap it in `AnimatedVisibility`.
- You **Must** specify `enter` and `exit` transitions using the following factory methods: `fadeIn()`, `fadeOut()`, `slideInVertically()`, `slideOutVertically()`, `scaleIn()`, and `scaleOut()`. These can be combined using the `+` operator.

**iOS**
`swift
if isVisible {
Image(systemName: "star")
.transition(.scale.combined(with: .opacity))
}`

**Android**
`kotlin
AnimatedVisibility(
visible = isVisible,
enter = scaleIn() + fadeIn(),
exit = scaleOut() + fadeOut()
) {
Icon(Icons.Default.Star, contentDescription = null)
}`

### 3. Content Transitions (Layout Switches)

Map iOS view replacement animations to Compose `AnimatedContent`.

**Logic:**

- **IF** switching between different layouts or views based on a state (e.g., loading -\> loaded), **THEN** you **Must** use `AnimatedContent`.
- You **Must** define custom `SizeTransform` if the content sizes differ significantly.

**iOS**
`swift
Group {
if isLoading {
ProgressView()
} else {
ContentView()
}
}
.animation(.default, value: isLoading)`

**Android**
`kotlin
AnimatedContent(
targetState = isLoading,
transitionSpec = {
(fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith
fadeOut(animationSpec = tween(90))).using(
SizeTransform { initialSize, targetSize ->
keyframes {
durationMillis = 300
}
}
)
}
) { targetIsLoading ->
if (targetIsLoading) {
CircularProgressIndicator()
} else {
ContentView()
}
}`

### 4. Physics-Based (Spring) Animations

Map iOS `spring()` to Compose `spring()`.

**Logic:**

- **IF** targeting natural, interactive motion, **THEN** you **Must** use `spring()` spec instead of `tween()`.
- Map iOS `damping` to Compose `dampingRatio` and iOS `stiffness` to Compose `stiffness`.

**iOS**
`swift
.animation(.spring(response: 0.5, dampingFraction: 0.6), value: state)`

**Android**
`kotlin
val offset by animateDpAsState(
targetValue = if (active) 50.dp else 0.dp,
animationSpec = spring(
dampingRatio = Spring.DampingRatioMediumBouncy,
stiffness = Spring.StiffnessLow
)
)`

### 5. Low-Level Coroutine Animations (Advanced)

When property animations are insufficient, use `Animatable` inside a coroutine
scope.

**Logic:**

- **IF** you need to chain animations sequentially, or animate values outside of the composition (e.g., in a touch handler), **THEN** you **Must** use `Animatable`.
- You **Must** launch the animation inside a `LaunchedEffect` or a coroutine scope.

**iOS**
`swift
// Sequencing in SwiftUI often requires Combine or custom delays
withAnimation(.easeInOut(duration: 0.3)) {
self.offset = 100
}`

**Android**
\`\`\`kotlin
val color = remember { Animatable(Color.Gray) }

LaunchedEffect(ok) {
color.animateTo(Color.Green, animationSpec = tween(1000))
color.animateTo(Color.Red, animationSpec = tween(500))
}
\`\`\`

### 6. Performance Optimization with Graphics Layer

You **Must** optimize animations to avoid excessive recomposition.

**Logic:**

- **IF** animating properties like translation (offset), scale, or rotation, **THEN** you **Must** use `Modifier.graphicsLayer` rather than changing layout modifiers like `Modifier.offset` directly, whenever possible. This ensures the animation runs on the RenderThread.

**Android Code Example:**
\`\`\`kotlin
val alpha by animateFloatAsState(targetValue = if (visible) 1f else 0f)

// Antipattern: Forces recomposition on every frame
// Box(Modifier.alpha(alpha))

// Best Practice: Runs on GPU
Box(Modifier.graphicsLayer(alpha = alpha))
\`\`\`

### 7. Keyframe Animations

Map iOS `KeyframeAnimator` or complex timing curves to Compose `keyframes`.

**Logic:**

- **IF** an animation has multi-step requirements with specific values at specific timestamps, **THEN** you **Must** use `keyframes` animation spec.

**iOS**
`swift
// SwiftUI KeyframeAnimator (iOS 17+)
KeyframeAnimator(initialValue: 0.0) { value in
Text("Hello")
.scaleEffect(value)
} keyframes: { _ in
KeyframeTrack(\.self) {
LinearKeyframe(1.0, duration: 0.2)
SpringKeyframe(1.5, duration: 0.4)
LinearKeyframe(1.0, duration: 0.2)
}
}`

**Android**
`kotlin
val scale by animateFloatAsState(
targetValue = if (active) 1f else 0f,
animationSpec = keyframes {
durationMillis = 800
0f at 0 // ms
1f at 200 with LinearEasing
1.5f at 600 with FastOutSlowInEasing
1f at 800
}
)`

### 8. Infinite Animations (Loops)

Map iOS infinite animation loops to Compose `rememberInfiniteTransition`.

**Logic:**

- **IF** an animation should run continuously without user interaction (e.g., a pulse effect or loading spinner), **THEN** you **Must** use `rememberInfiniteTransition`.

**iOS**
`swift
Text("Loading...")
.opacity(pulse ? 1.0 : 0.5)
.animation(.easeInOut(duration: 1.0).repeatForever(autoreverses: true), value: pulse)`

**Android**
\`\`\`kotlin
val infiniteTransition = rememberInfiniteTransition()
val alpha by infiniteTransition.animateFloat(
initialValue = 0.5f,
targetValue = 1f,
animationSpec = infiniteRepeatable(
animation = tween(1000, easing = LinearEasing),
repeatMode = RepeatMode.Reverse
)
)

Box(Modifier.graphicsLayer(alpha = alpha)) {
Text("Loading...")
}
\`\`\`

### 9. Gesture-Driven Animations

Map iOS gesture-linked animations to Compose pointer input and `Animatable`.

**Logic:**

- **IF** an animation must track a user's touch or drag gesture in real-time, **THEN** you **Must** use `Animatable` and update its value in the `pointerInput` modifier.

**iOS**
\`\`\`swift
@State private var offset = CGSize.zero

var dragGesture: some Gesture {
DragGesture()
.onChanged { value in
self.offset = value.translation
}
.onEnded { _ in
withAnimation(.spring()) {
self.offset = .zero
}
}
}

Text("Drag me")
.offset(offset)
.gesture(dragGesture)
\`\`\`

**Android**
\`\`\`kotlin
val offsetX = remember { Animatable(0f) }
val offsetY = remember { Animatable(0f) }
val coroutineScope = rememberCoroutineScope()

Box(
Modifier
.offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
.pointerInput(Unit) {
detectDragGestures(
onDrag = { change, dragAmount -\>
change.consume()
coroutineScope.launch {
offsetX.snapTo(offsetX.value + dragAmount.x)
offsetY.snapTo(offsetY.value + dragAmount.y)
}
},
onDragEnd = {
coroutineScope.launch {
offsetX.animateTo(0f, spring())
offsetY.animateTo(0f, spring())
}
}
)
}
) {
Text("Drag me")
}
\`\`\`

### 10. Shared Element Transitions

Map iOS `matchedGeometryEffect` to Compose `SharedTransitionLayout`.

**Logic:**

- **IF** creating a "Hero" animation where a view transitions smoothly between two different layouts or screens, **THEN** you **Must** use `SharedTransitionLayout`.
- You **Must** wrap the transitioning views in `sharedElement` modifier with a matching `rememberSharedContentState` key.

**iOS**
`swift
.matchedGeometryEffect(id: "item", in: namespace)`

**Android**
`kotlin
SharedTransitionLayout {
AnimatedContent(targetState = isFull) { targetIsFull ->
if (targetIsFull) {
DetailView(
Modifier.sharedElement(
rememberSharedContentState(key = "item"),
animatedVisibilityScope = this@AnimatedContent
)
)
} else {
ThumbnailView(
Modifier.sharedElement(
rememberSharedContentState(key = "item"),
animatedVisibilityScope = this@AnimatedContent
)
)
}
}
}`

### 11. Animating Multiple Properties (`updateTransition`)

Map iOS complex animations with multiple properties to Compose
`updateTransition`.

**Logic:**

- **IF** you need to animate multiple properties (e.g., color, size, rotation) simultaneously based on a single state change, **THEN** you **Must** use `updateTransition`.
- This is more efficient than chaining multiple `animate*AsState` calls.

**Android Code Example:**
\`\`\`kotlin
var isSelected by remember { mutableStateOf(false) }
val transition = updateTransition(targetState = isSelected, label = "selectionTransition")

val color by transition.animateColor(label = "color") { state -\>
if (state) Color.Red else Color.Gray
}

val size by transition.animateDp(label = "size") { state -\>
if (state) 200.dp else 100.dp
}

Box(Modifier.size(size).background(color))
\`\`\`

## Antipatterns

### DO

- **DO** use `animate*AsState` for simple property animations.
- **DO** use `Modifier.graphicsLayer` for properties that don't affect layout (alpha, scale, rotation) to improve performance.
- **DO** use `AnimatedVisibility` for elements entering/exiting the screen.

### DON'T

- **DON'T** use heavy animations inside a `LazyColumn` without optimization (use fixed size or graphics layer).
- **DON'T** chain multiple `animate*AsState` for a single complex animation; use `updateTransition` or `Animatable` instead.

### NEVER

- **NEVER** run infinite animations without a way to stop them or bounding them to the lifecycle (use `rememberInfiniteTransition`).
- **NEVER** block the main thread with complex calculation during animation frames.

## Testing Animations

You **Must** verify animation behavior using Compose testing APIs.

- **Control Time:** Use `mainClock` to advance time and verify intermediate states.

**Android Code Example:**
\`\`\`kotlin
@Test
fun testAnimation() {
composeTestRule.mainClock.autoAdvance = false

// Trigger animation
composeTestRule.onNodeWithTag("toggle").performClick()

// Advance time halfway
composeTestRule.mainClock.advanceTimeBy(150)

// Assert intermediate state
composeTestRule.onNodeWithTag("animated_box").assertWidthIsEqualTo(150.dp)
}
\`\`\`