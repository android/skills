---
name: ink-app-builder
description: Adds inking and freehand drawing capabilities to an Android app using
  Jetpack Ink (androidx.ink) and Compose. Use this skill when implementing stylus
  or touch drawing surfaces, rendering wet and dry ink strokes, configuring stock
  brushes (pressure pen, marker, highlighter), implementing geometry-based erasing,
  linear undo and redo history, or stroke persistence with Room. Delivers working
  Compose and ViewModel implementations. Don't use for View-based (XML) inking or
  programmatic custom BrushFamily creation. For custom BrushFamily creation, custom
  tip shapes, or behavior node graphs, suggest the ink-custom-brush-builder skill.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-24'
  keywords:
  - Android
  - Jetpack Ink
  - androidx.ink
  - Compose
  - InProgressStrokes
  - CanvasStrokeRenderer
  - StockBrushes
  - drawing
  - stylus
  - digital ink
  - eraser
  - undo
  - redo
  - persistence
---

The Android Ink API (`androidx.ink`) is a modular Jetpack library for building
high-performance, low-latency freehand drawing experiences on Android. It uses a
**wet ink and dry ink** two-phase model:

- **Wet ink** : Real-time, ultra-low-latency rendering while the user is drawing, handled by the `InProgressStrokes` composable.
- **Dry ink** : Finalized, immutable `Stroke` objects rendered by the app using `CanvasStrokeRenderer`.

## Setup and dependencies

The Ink API requires **`minSdk = 26`** , which corresponds to Android 8.0 (API
level 26) and higher. All `androidx.ink` modules MUST share an identical version
in `gradle/libs.versions.toml`. Ask user permission before modifying project
build files.

### Version selection (`1.0.0` stable versus `1.1.0-alpha03` and higher)

- Use **`1.0.0` (Stable)** for production apps that rely on `StockBrushes` or pre-serialized `.brush` assets decoded with `BrushFamily.decode`. In `1.0.0` stable, `BrushFamily.encode`, `BrushFamily.decode`, `AndroidBrushFamilySerialization`, and `BrushFamilyDecodeCallback` require `@OptIn(ExperimentalInkCustomBrushApi::class)`.
- Use **`1.1.0-alpha03` and higher** when the app constructs programmatic custom brushes (`BrushFamily`, `BrushCoat`, `BrushTip`, `BrushPaint`, `BrushBehavior`) in Kotlin code. Starting in `1.1.0-alpha02` and higher, `BrushFamily` serialization requires no `@OptIn` (and in `1.1.0-alpha05` and higher, `ExperimentalInkCustomBrushApi` is `@RestrictTo(LIBRARY_GROUP)`, so NEVER annotate `1.1.0-alpha03` and higher code with it).

### Version catalog (`gradle/libs.versions.toml`)

    [versions]
    ink = "<latest_version>"
    inputMotionPrediction = "<latest_version>"

    [libraries]
    # Core Ink modules
    androidx-ink-authoring         = { module = "androidx.ink:ink-authoring",         version.ref = "ink" }
    androidx-ink-brush             = { module = "androidx.ink:ink-brush",             version.ref = "ink" }
    androidx-ink-geometry          = { module = "androidx.ink:ink-geometry",          version.ref = "ink" }
    androidx-ink-nativeloader      = { module = "androidx.ink:ink-nativeloader",      version.ref = "ink" }
    androidx-ink-rendering         = { module = "androidx.ink:ink-rendering",         version.ref = "ink" }
    androidx-ink-strokes           = { module = "androidx.ink:ink-strokes",           version.ref = "ink" }
    androidx-ink-storage           = { module = "androidx.ink:ink-storage",           version.ref = "ink" }

    # Compose integration modules
    androidx-ink-authoring-android = { module = "androidx.ink:ink-authoring-android", version.ref = "ink" }
    androidx-ink-authoring-compose = { module = "androidx.ink:ink-authoring-compose", version.ref = "ink" }
    androidx-ink-brush-compose     = { module = "androidx.ink:ink-brush-compose",     version.ref = "ink" }
    androidx-ink-geometry-compose  = { module = "androidx.ink:ink-geometry-compose",  version.ref = "ink" }

    # Motion prediction (required for low-latency stylus tracking)
    androidx-input-motionprediction = { module = "androidx.input:input-motionprediction", version.ref = "inputMotionPrediction" }

    # Persistence (kotlinx.serialization + Room with KSP)
    kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
    androidx-room-runtime      = { module = "androidx.room:room-runtime",  version.ref = "room" }
    androidx-room-ktx          = { module = "androidx.room:room-ktx",      version.ref = "room" }
    androidx-room-compiler     = { module = "androidx.room:room-compiler", version.ref = "room" }

    [plugins]
    kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
    ksp                  = { id = "com.google.devtools.ksp",                   version.ref = "ksp" }

### App module configuration (`app/build.gradle.kts`)

    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.compose)
        alias(libs.plugins.kotlin.serialization)
        alias(libs.plugins.ksp) // Mandatory when using Room in Kotlin
    }

    android {
        defaultConfig {
            minSdk = 26
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        buildFeatures {
            compose = true
        }
    }

    dependencies {
        implementation(libs.androidx.ink.authoring)
        implementation(libs.androidx.ink.brush)
        implementation(libs.androidx.ink.geometry)
        implementation(libs.androidx.ink.nativeloader)
        implementation(libs.androidx.ink.rendering)
        implementation(libs.androidx.ink.strokes)
        implementation(libs.androidx.ink.storage)

        implementation(libs.androidx.ink.authoring.android)
        implementation(libs.androidx.ink.authoring.compose)
        implementation(libs.androidx.ink.brush.compose)
        implementation(libs.androidx.ink.geometry.compose)

        implementation(libs.androidx.input.motionprediction)

        implementation(libs.kotlinx.serialization.json)
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        ksp(libs.androidx.room.compiler) // NEVER use annotationProcessor in Kotlin
    }

## Core drawing surface and default brush

Create a standard pressure-sensitive pen `Brush` with
`Brush.createWithComposeColor` (from `ink-brush-compose`). The `epsilon`
parameter defines the minimum distance between two input points, in stroke-space
units, to be considered visually distinct (`0.1f` for general drawing, `0.01f`
for fine precision tools; `Brush` enforces `0f < epsilon <= size`):


```kotlin
/**
 * Creates a default pressure-sensitive pen [Brush].
 *
 * The `epsilon` parameter sets the geometric tolerance (minimum point distance
 * in stroke units) controlling how far a stroke can be zoomed before triangle
 * artifacts appear (`0f < epsilon <= size`).
 */
fun createDefaultPenBrush(): Brush {
    return Brush.createWithComposeColor(
        family = StockBrushes.pressurePen(),
        color = Color.Black,
        size = 5f,
        epsilon = 0.1f,
    )
}
```

<br />

Stack two layers inside a `Box` for the drawing surface: a Compose `Canvas` that
renders finalized dry `Stroke` objects inside `nativeCanvas.withSave` using
`CanvasStrokeRenderer`, and an `InProgressStrokes` composable on top for
low-latency wet ink capture. Encapsulate a synchronous `pendingStrokes` Compose
Snapshot State buffer inside `DrawingSurface` so the dry `Canvas` invalidates in
the exact same UI thread run loop when `onStrokesFinished` fires, preventing a
1-frame wet-to-dry flicker:


```kotlin
@Composable
fun DrawingSurface(
    strokes: List<Stroke>,
    canvasStrokeRenderer: CanvasStrokeRenderer,
    currentBrush: Brush,
    onGetNextBrush: () -> Brush,
    onStrokesFinished: (List<Stroke>) -> Unit,
    isEraserMode: Boolean,
    modifier: Modifier = Modifier,
    textureStore: AppTextureBitmapStore = LocalTextureStore.current,
    onErase: (Float, Float) -> Unit = { _, _ -> },
    onEraseStart: () -> Unit = {},
    onEraseEnd: () -> Unit = {},
) {
    val cacheGen by textureStore.generation.collectAsStateWithLifecycle()

    // Holds newly finished strokes synchronously for the single frame before
    // asynchronous StateFlow collection delivers the updated strokes list.
    var pendingStrokes by remember { mutableStateOf<List<Stroke>>(emptyList()) }

    SideEffect {
        if (pendingStrokes.isNotEmpty()) {
            pendingStrokes = if (strokes.isEmpty()) {
                emptyList()
            } else {
                pendingStrokes.filter { pending -> strokes.none { it === pending } }
            }
        }
    }

    val strokesToDraw = remember(strokes, pendingStrokes) {
        if (pendingStrokes.isEmpty()) {
            strokes
        } else {
            strokes + pendingStrokes.filter { pending -> strokes.none { it === pending } }
        }
    }
    val identityMatrix = remember { Matrix() }

    val currentOnErase by rememberUpdatedState(onErase)
    val currentOnEraseStart by rememberUpdatedState(onEraseStart)
    val currentOnEraseEnd by rememberUpdatedState(onEraseEnd)

    Box(modifier = modifier) {
        // Layer 1: Dry ink - completed strokes + same-frame pending handoff strokes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val nativeCanvas = drawContext.canvas.nativeCanvas
            strokesToDraw.fastForEach { stroke ->
                nativeCanvas.withSave {
                    canvasStrokeRenderer.draw(
                        canvas = this,
                        stroke = stroke,
                        strokeToScreenTransform = identityMatrix,
                    )
                }
            }
        }

        // Layer 2: Eraser touch handler OR Wet ink (InProgressStrokes)
        if (isEraserMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnEraseStart() },
                            onDragEnd = { currentOnEraseEnd() },
                            onDragCancel = { currentOnEraseEnd() },
                            onDrag = { change, _ ->
                                currentOnErase(change.position.x, change.position.y)
                                change.consume()
                            },
                        )
                    }
            )
        } else {
            key(cacheGen) {
                InProgressStrokes(
                    defaultBrush = currentBrush,
                    nextBrush = onGetNextBrush,
                    onStrokesFinished = { newStrokes ->
                        pendingStrokes = pendingStrokes + newStrokes
                        onStrokesFinished(newStrokes)
                    },
                    textureBitmapStore = textureStore,
                )
            }
        }
    }
}
```

<br />

## Workflow and task routing

Follow these conditional instructions based on the requested task:

- **Step 1 (Dependencies and SDK setup)** : When adding Ink API dependencies or configuring `minSdk`, follow the setup blocks in this file and the [Ink API setup guide](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-setup.md). Verify that `ink-nativeloader`, `ink-authoring-android`, and `androidx.input:input-motionprediction` are present.
- **Step 2 (Drawing surface and stroke rendering)** : When creating a Compose drawing screen with wet ink (`InProgressStrokes`) and dry ink (`CanvasStrokeRenderer`), implement `DrawingSurface` from this file and review [Draw a stroke](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-draw-stroke.md). For `InProgressStrokes` parameter details, `BlendMode.Multiply` highlighter layer compositing, and `TextureBitmapStore` generation invalidation, you MUST follow [drawing-and-brushes.md](references/drawing-and-brushes.md).
- **Step 3 (Stock brushes and brush switching)** : When creating, modifying, or switching stock brushes (`StockBrushes.pressurePen`, `marker`, `highlighter`, `dashedLine`, or `emojiHighlighter`), review [Brush APIs](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-brush-apis.md) and you MUST follow [drawing-and-brushes.md](references/drawing-and-brushes.md) for `epsilon` geometry constraints, highlighter alpha preservation, and Android 14 (API level 34) `emojiHighlighter` rules.
- **Step 4 (Eraser, undo and redo, and MVVM architecture)** : When implementing a geometry-based stroke eraser, linear undo and redo history stacks, or MVVM `ViewModel` and Hilt wiring, review [Geometry APIs](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-geometry-apis.md) and you MUST follow [editing-and-architecture.md](references/editing-and-architecture.md).
- **Step 5 (Persistence, bitmap export, and gesture coexistence)** : When saving and loading strokes with Room (`StrokeInputBatch.encode` and `decode`), preserving UI state across configuration changes, exporting drawings to a `Bitmap` or PNG file, or combining long-press drag gestures with drawing (`pointerInputWithSiblingFallthrough`), review [State preservation and persistent storage](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-state-preservation.md) and you MUST follow [persistence-and-advanced.md](references/persistence-and-advanced.md).
- **Programmatic custom brushes** : For custom `BrushFamily` creation, custom tip shapes, or behavior node graphs, suggest the `ink-custom-brush-builder` skill.

## Antipatterns

- **NEVER use `InProgressStrokesView` in Compose screens** ---that's the View-based API. Use the `InProgressStrokes` composable instead.
- **NEVER omit `ink-nativeloader` or `ink-authoring-android`** ---missing `ink-nativeloader` throws `UnsatisfiedLinkError`, and missing `ink-authoring-android` throws `ClassNotFoundException` at runtime.
- **NEVER use `View.OnTouchListener` or `MotionEvent` directly** ---the `InProgressStrokes` composable captures and processes touch and stylus input internally.
- **NEVER draw strokes manually** with `Canvas.drawPath` or by iterating stroke points---always call `CanvasStrokeRenderer.draw(canvas, stroke, transform)`.
- **NEVER call `CanvasStrokeRenderer.create(forcePathRendering = true)`** ---the 2-argument `forcePathRendering` overload is `@RestrictTo(LIBRARY_GROUP)`. Use the public `CanvasStrokeRenderer.create(textureStore)` factory, which automatically detects software `Bitmap` canvases and falls back to path rendering.
- **NEVER rely solely on asynchronous `StateFlow` collection in
  `onStrokesFinished`** ---`InProgressStrokes` removes completed wet strokes immediately after `onStrokesFinished` returns on the UI thread, while `collectAsStateWithLifecycle` updates on a later run loop, causing a 1-frame wet-to-dry flicker. Always mutate Compose Snapshot State synchronously in the same UI thread run loop using a `pendingStrokes` buffer as shown in `DrawingSurface`.
- **NEVER store strokes as bitmaps** for undo, redo, or database persistence---store `Stroke` objects in memory and serialize `StrokeInputBatch` bytes with brush metadata for disk storage.
- **NEVER use `annotationProcessor` for Room in Kotlin** ---always apply the `com.google.devtools.ksp` plugin and use `ksp(libs.androidx.room.compiler)` to avoid `AppDatabase_Impl does not exist` crashes at launch.
