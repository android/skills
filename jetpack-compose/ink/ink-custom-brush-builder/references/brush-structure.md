Reference for defining `BrushFamily` and `BrushCoat` hierarchies, configuring
`BrushTip` geometry and `BrushPaint` blending, and tuning `InputModel` stroke
smoothing (`androidx.ink` `1.1.0-alpha03` and higher).

## `BrushFamily` and `BrushCoat` hierarchy

    graph TD
        BF["BrushFamily"]
        C0["BrushCoat[0]"]
        C1["BrushCoat[1] ..."]
        T0["BrushTip"]
        P0["BrushPaint[]"]
        BB["BrushBehavior[]"]
        TP["Tip Properties (scaleX, scaleY, cornerRounding, slantDegrees, pinch, rotationDegrees)"]
        SO["SelfOverlap"]
        CF["BrushPaint.ColorFunction[]"]
        TL["TextureLayer[] (TilingTexture / StampingTexture)"]

        BF --> C0
        BF --> C1
        C0 --> T0
        C0 --> P0
        T0 --> TP
        T0 --> BB
        P0 --> SO
        P0 --> CF
        P0 --> TL

| Level | Type | Role |
|---|---|---|
| **BrushFamily** | `BrushFamily` | Top-level immutable brush definition containing one or more `BrushCoat` layers and an `InputModel`. |
| **BrushCoat** | `BrushCoat` | Single rendering pass with one `BrushTip` and either `paint: BrushPaint` or a prioritized fallback list `paintPreferences: List<BrushPaint>`. Multiple `BrushCoat` entries in `BrushFamily.coats` composite from index `0` (bottom) upward. |
| **BrushTip** | `BrushTip` | Geometric mark shape at each point (`scaleX`, `scaleY`, `cornerRounding`, `slantDegrees`, `pinch`, `rotationDegrees`) plus dynamic `BrushBehavior` node trees. |
| **BrushPaint** | `BrushPaint` | Visual appearance (`SelfOverlap`, `BrushPaint.ColorFunction` list, and `BrushPaint.TilingTexture` or `StampingTexture` layers). |

### Construct and copy `BrushFamily` and `BrushCoat`

Use the multi-coat `BrushFamily(coats = listOf(...))` constructor or the
single-coat `BrushFamily(tip = ..., paint = ...)` shortcut, and derive variants
with `.copy(...)` (`BrushPaint` doesn't provide `copy`):


```kotlin
val singleCoatFamily = BrushFamily(
    tip = BrushTip(scaleX = 1f, scaleY = 1f, cornerRounding = 1f),
    paint = BrushPaint(selfOverlap = SelfOverlap.ANY),
)

val multiCoatFamily = BrushFamily(
    coats = listOf(
        // Coat 0: Wider semi-transparent shadow coat rendered underneath
        BrushCoat(
            tip = BrushTip(scaleX = 1.3f, scaleY = 1.3f, cornerRounding = 1f),
            paint = BrushPaint(
                selfOverlap = SelfOverlap.DISCARD,
                colorFunctions = listOf(BrushPaint.ColorFunction.OpacityMultiplier(0.15f)),
            ),
        ),
        // Coat 1: Main opaque stroke coat rendered on top
        BrushCoat(
            tip = BrushTip(scaleX = 1f, scaleY = 1f, cornerRounding = 1f),
            paint = BrushPaint(selfOverlap = SelfOverlap.DISCARD),
        ),
    ),
)

val roundedVariant = singleCoatFamily.copy(
    coats = listOf(
        singleCoatFamily.coats.first().let { coat ->
            coat.copy(tip = coat.tip.copy(cornerRounding = 0.5f))
        }
    ),
)
```

<br />

## `BrushTip` geometry configuration

| Property | Type and default | Range and description |
|---|---|---|
| `scaleX`, `scaleY` | `Float = 1f` | Horizontal and vertical tip scale multiplied by `Brush.size` (`>= 0f`; at least one must be `> 0f`). |
| `cornerRounding` | `Float = 1f` | `0f` = sharp rectangle or parallelogram; `1f` = fully rounded circle or pill. |
| `slantDegrees` | `Float = 0f` | Static shear angle in **degrees** (`[-90f, 90f]`) applied before `rotationDegrees`. |
| `pinch` | `Float = 0f` | Pinches top corners together (`0f` = rectangle, `1f` = triangle). |
| `rotationDegrees` | `Float = 0f` | Static rotation offset in **degrees** (note: `BrushTip` static angles use degrees, whereas `BrushBehavior` dynamic angles use **radians**). |
| `particleGapDistanceScale` | `Float = 0f` | Distance between discrete stamp particles as a multiple of brush size (`0f` = continuous stroke). |
| `particleGapDurationMillis` | `Long = 0L` | Time interval in ms between discrete stamp particles (`0L` = continuous stroke). |
| `behaviors` | `List<BrushBehavior> = emptyList()` | Dynamic `BrushBehavior` node graphs attached to this tip. |

### `BrushTip` shape examples (calligraphy nib and flat marker)


```kotlin
// Narrow angled calligraphy nib (static angle in degrees)
val calligraphyTip = BrushTip(
    scaleX = 0.3f,
    scaleY = 1.0f,
    cornerRounding = 0.2f,
    rotationDegrees = 45f,
)

// Wide flat marker tip
val markerTip = BrushTip(
    scaleX = 2.0f,
    scaleY = 0.5f,
    cornerRounding = 0.4f,
)
```

<br />

## `BrushPaint` blending and `ColorFunction`

### `SelfOverlap` modes

`SelfOverlap` controls how regions where a **single stroke** crosses itself
blend during rendering:

| Mode | Behavior and renderer compatibility |
|---|---|
| `SelfOverlap.ANY` **(default)** | Uses `ACCUMULATE` (`CanvasMeshRenderer`) on Android 14 (API level 34) and higher hardware `Canvas`, or `DISCARD` (`CanvasPathRenderer`) on Android 13 (API level 33) and lower or software `Canvas(bitmap)`. Required whenever a coat uses per-vertex opacity (`OPACITY_MULTIPLIER`) or color `BrushBehavior` instances. |
| `SelfOverlap.ACCUMULATE` | Accumulates opacity where a translucent stroke crosses itself. Requires `CanvasMeshRenderer` (Android 14 (API level 34) and higher hardware `Canvas`); rejected by `CanvasPathRenderer` unless a fallback `BrushPaint` is provided in `BrushCoat.paintPreferences`. |
| `SelfOverlap.DISCARD` | Renders a flat uniform-opacity outline (`CanvasPathRenderer`). **Incompatible** with `BrushPaint.StampingTexture` and per-vertex opacity or color `BrushBehavior` instances on the same `BrushCoat`. |

### `BrushPaint.ColorFunction` (`OpacityMultiplier` and `ReplaceColor`)

Apply static coat opacity or replace a coat's color (such as a fixed black
shadow coat) using `BrushPaint.ColorFunction`. Never call the internal
`ReplaceColor.withComposeColor`---use `withColorLong` or `withColorIntArgb`:


```kotlin
val translucentPaint = BrushPaint(
    selfOverlap = SelfOverlap.ANY,
    colorFunctions = listOf(
        BrushPaint.ColorFunction.OpacityMultiplier(0.7f),
    ),
)

val fixedBlackShadowPaint = BrushPaint(
    selfOverlap = SelfOverlap.DISCARD,
    colorFunctions = listOf(
        BrushPaint.ColorFunction.ReplaceColor.withColorLong(Color.Black.value.toLong()),
        BrushPaint.ColorFunction.OpacityMultiplier(0.2f),
    ),
)
```

<br />

## `InputModel` and stroke smoothing (`SlidingWindowModel`)

`BrushFamily.inputModel` smooths raw touch and stylus points before they reach
the behavior graph:

- **`BrushFamily.InputModel.SlidingWindowModel`** : Takes `(windowDurationMillis, upsamplingFrequencyHz)`, applying a moving-average window (`windowDurationMillis`, default `20L` ms; typical range `10L` to `50L` ms) and temporal upsampling (`upsamplingFrequencyHz`, default `180` Hz, or `0` to disable). `BrushFamily.InputModel.DEFAULT_INPUT_MODEL` uses `20L` ms and `180` Hz.
- **`BrushFamily.InputModel.PASSTHROUGH_MODEL`**: Bypasses smoothing and upsampling; reserve strictly for synthetic or pre-modeled input streams.

Combine `SlidingWindowModel` (for path smoothing) with a `DampingNode` in
`BrushBehavior` (for dynamic property smoothing such as pressure):


```kotlin
/**
 * Creates a custom [BrushFamily] configured for smooth, low-jitter strokes.
 *
 * @param windowDurationMillis Sliding-window duration in milliseconds (default 30ms)
 *   controlling input path smoothing versus latency.
 * @param upsamplingFrequencyHz Target upsampling frequency in Hz (default 180Hz)
 *   for smooth curve subdivision.
 */
fun createSmoothBrushFamily(
    windowDurationMillis: Long = 30L,
    upsamplingFrequencyHz: Int = 180,
): BrushFamily {
    return BrushFamily(
        coats = listOf(
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1f,
                    scaleY = 1f,
                    cornerRounding = 1f,
                    behaviors = listOf(
                        BrushBehavior(
                            terminalNode = TargetNode(
                                target = TargetNode.Target.SIZE_MULTIPLIER,
                                targetModifierRangeStart = 0.5f,
                                targetModifierRangeEnd = 1.5f,
                                input = DampingNode(
                                    dampOver = ProgressDomain.TIME_IN_SECONDS,
                                    strength = 0.15f,
                                    input = SourceNode(
                                        source = SourceNode.Source.NORMALIZED_PRESSURE,
                                        sourceValueRangeStart = 0f,
                                        sourceValueRangeEnd = 1f,
                                        sourceOutOfRangeBehavior = OutOfRange.CLAMP,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
                paint = BrushPaint(selfOverlap = SelfOverlap.ANY),
            ),
        ),
        inputModel = BrushFamily.InputModel.SlidingWindowModel(
            windowDurationMillis = windowDurationMillis,
            upsamplingFrequencyHz = upsamplingFrequencyHz,
        ),
    )
}
```

<br />

## Common pitfalls

- **Confusing `BrushFamily.coats` with `BrushCoat.paintPreferences`** : Multiple `BrushCoat` entries render on top of each other as layers; multiple `BrushPaint` entries in `paintPreferences` are fallback alternatives where the renderer draws only the *first* compatible `BrushPaint`.
- **Mixing degrees and radians** : `BrushTip` static angles (`rotationDegrees`, `slantDegrees`) use **degrees** (`45f`), while `BrushBehavior` dynamic targets (`ROTATION_OFFSET_IN_RADIANS`, `SLANT_OFFSET_IN_RADIANS`) and sources (`TILT_IN_RADIANS`) use **radians** (`(Math.PI / 4).toFloat()`).
- **Combining `SelfOverlap.DISCARD` with opacity or color `BrushBehavior`
  instances** : `DISCARD` forces `CanvasPathRenderer`, which ignores per-vertex opacity and color behaviors. Use `SelfOverlap.ANY` instead.
- **Setting `windowDurationMillis` over `50L` ms**: Excessive smoothing windows introduce noticeable stylus lag.