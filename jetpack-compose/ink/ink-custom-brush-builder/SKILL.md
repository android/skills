---
name: ink-custom-brush-builder
description: Creates custom BrushFamily definitions for an Android app using Jetpack
  Ink (androidx.ink 1.1.0-alpha03 and higher). Use this skill when configuring custom
  BrushTip shapes, adding pressure, tilt, speed, or noise sensitivity using BrushBehavior
  node graphs, creating multi-coat layered brushes, applying tiling or particle stamping
  textures, configuring SlidingWindowModel input smoothing, or serializing brushes.
  Delivers complete Kotlin BrushFamily, BrushCoat, BrushTip, BrushPaint, and BrushBehavior
  implementations. Don't use for View-based (XML) inking, Compose drawing surface
  setup, or stock brush selection. For Compose drawing surface setup, InProgressStrokes
  wet and dry rendering, eraser, or persistence, suggest the ink-app-builder skill.
license: Complete terms in LICENSE.txt
metadata:
  author: Google LLC
  last-updated: '2026-10-28'
  keywords:
  - Android
  - Jetpack Ink
  - androidx.ink
  - BrushFamily
  - BrushCoat
  - BrushTip
  - BrushPaint
  - BrushBehavior
  - TilingTexture
  - StampingTexture
  - ColorFunction
  - SlidingWindowModel
  - custom brush
---

Custom brushes in the Android Ink API are built around the **`BrushFamily`**
hierarchy:

    BrushFamily
    ├── InputModel (BrushFamily.InputModel.SlidingWindowModel or PASSTHROUGH_MODEL)
    └── BrushCoat[] (one or more rendering layers)
        ├── BrushTip (shape: scaleX, scaleY, cornerRounding, rotationDegrees, slantDegrees, pinch)
        │   └── BrushBehavior[] (dynamic response to stylus or touch input)
        │       └── Tree: TargetNode(input = [Operators](input = SourceNode))
        └── BrushPaint (visual appearance)
            ├── SelfOverlap (ANY, ACCUMULATE, or DISCARD)
            ├── TextureLayer[] (BrushPaint.TilingTexture or BrushPaint.StampingTexture)
            └── BrushPaint.ColorFunction[] (OpacityMultiplier or ReplaceColor)

Create a `Brush` by combining a custom `BrushFamily` with a Compose `Color`,
`size`, and `epsilon`:


```kotlin
val brush = Brush.createWithComposeColor(
    family = myCustomFamily,
    color = Color.Black,
    size = 10f,
    epsilon = 0.1f,
)
```

<br />

> **Version and experimental API requirements** :
> - **Requires `androidx.ink` `1.1.0-alpha03` and higher** : In `1.0.0` (Stable)
> and `1.1.0-alpha01`-`alpha02`, `BrushFamily(...)`, `BrushCoat`, `BrushTip`,
> `BrushPaint`, `BrushBehavior`, and `androidx.ink.brush.behavior.*` nodes
> were `@RestrictTo(LIBRARY_GROUP)` (in `1.0.0` stable, custom brushes can
> only be loaded from binary proto streams using `BrushFamily.decode`).
> Starting in **`1.1.0-alpha03` and higher** , programmatic custom brush
> constructors graduated to public API (`BrushPaint.TilingTexture`,
> `BrushPaint.StampingTexture`, `BrushPaint.ColorFunction`,
> `BrushFamily.InputModel.SlidingWindowModel`, and all behavior nodes).
> - **No `@OptIn` required in `1.1.0-alpha03` and higher** : All public custom
> brush classes and `ink-storage` `BrushFamily` serialization APIs
> (`BrushFamily.encode` and `decode`, `AndroidBrushFamilySerialization`,
> `BrushFamilyDecodeCallback`) are unannotated in `1.1.0-alpha03` and higher
> (`ExperimentalInkCustomBrushApi` is `@RestrictTo(LIBRARY_GROUP)` in
> `1.1.0-alpha05` and higher, and is only required on `BrushFamily`
> serialization in `1.0.0` stable).

## Workflow and task routing

Follow these conditional instructions based on the requested custom brush task:

- **Step 1 (Dependencies)** : When setting up an Android project for programmatic custom brushes, ask user permission before editing build files and verify that `androidx.ink:ink-brush`, `androidx.ink:ink-brush-compose`, `androidx.ink:ink-storage`, and `androidx.ink:ink-nativeloader` use version `1.1.0-alpha03` or higher in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- **Step 2 (Hierarchy, tip shape, paint, and input smoothing)** : When defining `BrushFamily` and multi-coat `BrushCoat` layers, configuring `BrushTip` geometry (`scaleX`, `scaleY`, `cornerRounding`, `rotationDegrees`, `slantDegrees`, `pinch`), setting `BrushPaint` properties (`SelfOverlap`, `ColorFunction`), or smoothing stroke input with `BrushFamily.InputModel.SlidingWindowModel`, you MUST follow [brush-structure.md](references/brush-structure.md). For baseline `Brush` creation or `1.0.0` resource loading, review [Brush APIs](references/android/develop/ui/compose/touch-input/stylus-input/ink-api-brush-apis.md).
- **Step 3 (Dynamic stylus and touch behaviors)** : When making a brush respond dynamically to stylus pressure, tilt, speed, direction, or procedural noise using `BrushBehavior` node trees (`SourceNode`, `DampingNode`, `NoiseNode`, `ResponseNode`, `TargetNode`, `PolarTargetNode`), you MUST follow [brush-behaviors.md](references/brush-behaviors.md).
- **Step 4 (Textures and serialization)** : When adding `BrushPaint.TilingTexture` or `BrushPaint.StampingTexture` layers, implementing `TextureBitmapStore`, or encoding and decoding custom `BrushFamily` definitions (`BrushFamily.encode` and `decode`, `AndroidBrushFamilySerialization`, `BrushFamilyDecodeCallback`), you MUST follow [textures-and-serialization.md](references/textures-and-serialization.md). Inform the user which texture image files (such as `app/src/main/res/drawable-nodpi/pencil_grain.png`) to add to their project and register in `TextureBitmapStore`.
- **Step 5 (Complete brush recipes)** : When building a complete pressure pen, calligraphy nib, shading pencil, watercolor wash, or multi-coat shadow brush from scratch, start from the copy-ready implementations in [examples.md](references/examples.md).
- **Compose drawing surface, eraser, or persistence** : For Compose drawing surface setup, `InProgressStrokes` wet and dry rendering, geometry-based erasing, or Room stroke persistence, suggest the `ink-app-builder` skill.

## Antipatterns

- **NEVER use raw `ink.proto.*` builders** to construct brushes---always use the public Kotlin constructors (`BrushFamily`, `BrushCoat`, `BrushTip`, `BrushPaint`, `BrushBehavior` from `androidx.ink.brush.*`).
- **NEVER manually scale stroke width in a touch listener** for pressure or tilt sensitivity---use `BrushBehavior` mapping `SourceNode.Source.NORMALIZED_PRESSURE` or `TILT_IN_RADIANS` to `TargetNode.Target.SIZE_MULTIPLIER` or `WIDTH_MULTIPLIER`.
- **NEVER use manual `Canvas` bitmap stamping or `BitmapShader`** for textured brushes---use `BrushPaint.TilingTexture` or `BrushPaint.StampingTexture` with a `TextureBitmapStore`.
- **NEVER use `BlendMode.SRC_OVER` on the final (or only) `TextureLayer`** ---use `BlendMode.MODULATE`, `DST_IN`, `DST_OUT`, `SRC_ATOP`, or `SRC_IN` so edge anti-aliasing and `OPACITY_MULTIPLIER` behaviors aren't overridden.
- **NEVER serialize `BrushFamily` as JSON, Gson, Moshi, or `Parcelable`** ---always use `BrushFamily.encode` and `BrushFamily.decode` or `AndroidBrushFamilySerialization` from `androidx.ink.storage`.
- **NEVER implement manual Bézier (`quadTo`, `cubicTo`) smoothing** ---configure `BrushFamily.InputModel.SlidingWindowModel` for input smoothing and `DampingNode` for behavior smoothing.
- **NEVER use `Random.nextFloat` in draw loops** for organic stroke wobble---attach a `NoiseNode` varied over stroke distance in `BrushBehavior`.
