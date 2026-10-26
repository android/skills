Reference for building dynamic `BrushBehavior` node graphs that respond to
stylus pressure, tilt, speed, direction, and procedural noise (`androidx.ink`
`1.1.0-alpha03` and higher).

## Overview and package imports

A `BrushBehavior` is a tree-based node graph attached to `BrushTip.behaviors`
that evaluates per input point: **Source or ValueNode to \[Operators\] to
TerminalNode (`TargetNode` or `PolarTargetNode`)**.

- Import `BrushBehavior` and `InputToolType` from **`androidx.ink.brush.*`** (never `androidx.ink.brush.behavior.BrushBehavior`).
- Import all node classes and enums (`SourceNode`, `ConstantNode`, `NoiseNode`, `ToolTypeFilterNode`, `DampingNode`, `ResponseNode`, `IntegralNode`, `BinaryOpNode`, `InterpolationNode`, `TargetNode`, `PolarTargetNode`, `OutOfRange`, `ProgressDomain`, `EasingFunction`) from **`androidx.ink.brush.behavior.*`**.

## All 11 behavior node types

### 1. `SourceNode` (input signals)

Reads a raw stylus or touch property and normalizes it across
`[sourceValueRangeStart, sourceValueRangeEnd]` using `sourceOutOfRangeBehavior`
(`OutOfRange.CLAMP`, `OutOfRange.REPEAT` for cyclic angles, or
`OutOfRange.MIRROR`):


```kotlin
val pressureSource = SourceNode(
    source = SourceNode.Source.NORMALIZED_PRESSURE,
    sourceValueRangeStart = 0f,
    sourceValueRangeEnd = 1f,
    sourceOutOfRangeBehavior = OutOfRange.CLAMP,
)
```

<br />

#### Available `SourceNode.Source` constants

| Category | `SourceNode.Source` constants | Typical range |
|---|---|---|
| **Pressure and tilt** | `NORMALIZED_PRESSURE` (`[0, 1]`), `TILT_IN_RADIANS` (`[0, π/2]`), `TILT_X_IN_RADIANS` (`[-π/2, π/2]`), `TILT_Y_IN_RADIANS` (`[-π/2, π/2]`), `ORIENTATION_IN_RADIANS` (`[0, 2π)`), `ORIENTATION_ABOUT_ZERO_IN_RADIANS` (`(-π, π]`) | `[0, 1]` or radians |
| **Speed and velocity** | `SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND`, `VELOCITY_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND`, `VELOCITY_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND`, `SPEED_IN_CENTIMETERS_PER_SECOND`, `VELOCITY_X_IN_CENTIMETERS_PER_SECOND`, `VELOCITY_Y_IN_CENTIMETERS_PER_SECOND` | `[0, ∞)` or `(-∞, ∞)` |
| **Direction** | `DIRECTION_IN_RADIANS` (`[0, 2π)`), `DIRECTION_ABOUT_ZERO_IN_RADIANS` (`(-π, π]`), `NORMALIZED_DIRECTION_X` (`[-1, 1]`), `NORMALIZED_DIRECTION_Y` (`[-1, 1]`) | Use `OutOfRange.REPEAT` for angles |
| **Acceleration** | `ACCELERATION_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED`, `ACCELERATION_X_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED`, `ACCELERATION_Y_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED`, `ACCELERATION_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED`, `ACCELERATION_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND_SQUARED`, `ACCELERATION_IN_CENTIMETERS_PER_SECOND_SQUARED`, `ACCELERATION_X_IN_CENTIMETERS_PER_SECOND_SQUARED`, `ACCELERATION_Y_IN_CENTIMETERS_PER_SECOND_SQUARED`, `ACCELERATION_FORWARD_IN_CENTIMETERS_PER_SECOND_SQUARED`, `ACCELERATION_LATERAL_IN_CENTIMETERS_PER_SECOND_SQUARED` | `[0, ∞)` or `(-∞, ∞)` |
| **Distance and time** | `DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE`, `DISTANCE_TRAVELED_IN_CENTIMETERS`, `DISTANCE_REMAINING_IN_MULTIPLES_OF_BRUSH_SIZE`, `DISTANCE_REMAINING_AS_FRACTION_OF_STROKE_LENGTH` (`[0, 1]`), `PREDICTED_DISTANCE_TRAVELED_IN_MULTIPLES_OF_BRUSH_SIZE`, `PREDICTED_DISTANCE_TRAVELED_IN_CENTIMETERS`, `TIME_OF_INPUT_IN_SECONDS`, `TIME_FROM_INPUT_TO_STROKE_END_IN_SECONDS`, `PREDICTED_TIME_ELAPSED_IN_SECONDS`, `TIME_SINCE_INPUT_IN_SECONDS`, `TIME_SINCE_STROKE_END_IN_SECONDS` | `[0, ∞)` |

### 2-9. Value and operator nodes

Use `ConstantNode`, `NoiseNode`, `ToolTypeFilterNode`, `DampingNode`,
`ResponseNode`, `IntegralNode`, `BinaryOpNode`, and `InterpolationNode` to
generate, filter, smooth, or combine signals across a `ProgressDomain`
(`TIME_IN_SECONDS`, `DISTANCE_IN_CENTIMETERS`, or
`DISTANCE_IN_MULTIPLES_OF_BRUSH_SIZE`):


```kotlin
// 2. ConstantNode: fixed scalar output
val halfConstant = ConstantNode(0.5f)

// 3. NoiseNode: procedural noise over stroke distance or time
val distanceNoise = NoiseNode(
    seed = 42,
    varyOver = ProgressDomain.DISTANCE_IN_CENTIMETERS,
    basePeriod = 0.3f,
)

// 4. ToolTypeFilterNode: pass signal only for selected InputToolType set
val stylusOnlyPressure = ToolTypeFilterNode(
    enabledToolTypes = setOf(InputToolType.STYLUS),
    input = pressureSource,
)

// 5. DampingNode: low-pass smoothing over time or distance
val dampedPressure = DampingNode(
    dampOver = ProgressDomain.TIME_IN_SECONDS,
    strength = 0.15f,
    input = pressureSource,
)

// 6. ResponseNode: non-linear easing (Predefined, CubicBezier, Linear, Steps)
val easedPressure = ResponseNode(
    responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
    input = dampedPressure,
)

// 7. IntegralNode: accumulates input over time or distance
val accumulatedPressure = IntegralNode(
    integrateOver = ProgressDomain.TIME_IN_SECONDS,
    integralValueRangeStart = 0f,
    integralValueRangeEnd = 1f,
    integralOutOfRangeBehavior = OutOfRange.CLAMP,
    input = pressureSource,
)

// 8. BinaryOpNode: PRODUCT, SUM, MIN, MAX, AND_THEN, OR_ELSE, XOR_ELSE
val scaledPressure = BinaryOpNode(
    operation = BinaryOpNode.BinaryOp.PRODUCT,
    firstInput = easedPressure,
    secondInput = halfConstant,
)

// 9. InterpolationNode: LERP or INVERSE_LERP between startInput and endInput
val lerpedNode = InterpolationNode(
    interpolation = InterpolationNode.Interpolation.LERP,
    paramInput = easedPressure,
    startInput = ConstantNode(0.2f),
    endInput = ConstantNode(1.0f),
)
```

<br />

### 10. `TargetNode` (scalar terminal node)

Maps a normalized `[0, 1]` `ValueNode` input onto
`[targetModifierRangeStart, targetModifierRangeEnd]` (endpoints must be finite
and distinct: `rangeStart != rangeEnd`):

| `TargetNode.Target` constant | Effect on stroke |
|---|---|
| `SIZE_MULTIPLIER`, `WIDTH_MULTIPLIER`, `HEIGHT_MULTIPLIER` | Scales overall tip size, horizontal width, or vertical height |
| `SLANT_OFFSET_IN_RADIANS`, `ROTATION_OFFSET_IN_RADIANS` | Adds dynamic shear or rotation offset in **radians** |
| `CORNER_ROUNDING_OFFSET`, `PINCH_OFFSET` | Adds offset to tip `cornerRounding` or `pinch` |
| `OPACITY_MULTIPLIER`, `PAINT_ANIMATION_PROGRESS_OFFSET` | Multiplies per-vertex opacity (requires `SelfOverlap.ANY` or `ACCUMULATE`) or offsets animated texture progress |
| `HUE_OFFSET_IN_RADIANS`, `CHROMA_MULTIPLIER`, `LIGHTNESS_OFFSET` | Shifts color hue, chroma, or lightness |
| `POSITION_OFFSET_X_IN_MULTIPLES_OF_BRUSH_SIZE`, `POSITION_OFFSET_Y_IN_MULTIPLES_OF_BRUSH_SIZE`, `POSITION_OFFSET_FORWARD_IN_MULTIPLES_OF_BRUSH_SIZE`, `POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE` | Offsets tip position in multiples of brush size |

### 11. `PolarTargetNode` (2D vector terminal node)

Applies a polar vector (`angleInput` + `magnitudeInput`) to 2D position offset
targets (`POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE` or
`POSITION_OFFSET_RELATIVE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE`):


```kotlin
val polarScatterBehavior = BrushBehavior(
    terminalNode = PolarTargetNode(
        target = PolarTargetNode.PolarTarget.POSITION_OFFSET_ABSOLUTE_IN_RADIANS_AND_MULTIPLES_OF_BRUSH_SIZE,
        angleRangeStart = 0f,
        angleRangeEnd = (2 * Math.PI).toFloat(),
        angleInput = NoiseNode(
            seed = 101,
            varyOver = ProgressDomain.DISTANCE_IN_CENTIMETERS,
            basePeriod = 0.2f,
        ),
        magnitudeRangeStart = 0f,
        magnitudeRangeEnd = 0.5f,
        magnitudeInput = SourceNode(
            source = SourceNode.Source.NORMALIZED_PRESSURE,
            sourceValueRangeStart = 0f,
            sourceValueRangeEnd = 1f,
        ),
    ),
)
```

<br />

## Reusable behavior patterns and examples

Build reusable helper functions for direct (`Source` to `Target`), smoothed
(`Source` to `Damping` to `Target`), and procedural noise (`Noise` to `Target`)
behaviors and attach them to `BrushTip(behaviors = listOf(...))`:


```kotlin
fun smoothedBehavior(
    source: SourceNode.Source,
    sourceStart: Float,
    sourceEnd: Float,
    target: TargetNode.Target,
    targetStart: Float,
    targetEnd: Float,
    dampingSeconds: Float,
    outOfRange: OutOfRange = OutOfRange.CLAMP,
): BrushBehavior = BrushBehavior(
    terminalNode = TargetNode(
        target = target,
        targetModifierRangeStart = targetStart,
        targetModifierRangeEnd = targetEnd,
        input = DampingNode(
            dampOver = ProgressDomain.TIME_IN_SECONDS,
            strength = dampingSeconds,
            input = SourceNode(
                source = source,
                sourceValueRangeStart = sourceStart,
                sourceValueRangeEnd = sourceEnd,
                sourceOutOfRangeBehavior = outOfRange,
            ),
        ),
    ),
)

// 1. Pressure -> Size (50% to 150% with 150ms damping)
val pressureToSize = smoothedBehavior(
    source = SourceNode.Source.NORMALIZED_PRESSURE,
    sourceStart = 0f,
    sourceEnd = 1f,
    target = TargetNode.Target.SIZE_MULTIPLIER,
    targetStart = 0.5f,
    targetEnd = 1.5f,
    dampingSeconds = 0.15f,
)

// 2. Stylus Tilt -> Width (1x to 2.5x as tilt increases to pi/2)
val tiltToWidth = smoothedBehavior(
    source = SourceNode.Source.TILT_IN_RADIANS,
    sourceStart = 0f,
    sourceEnd = (Math.PI / 2).toFloat(),
    target = TargetNode.Target.WIDTH_MULTIPLIER,
    targetStart = 1.0f,
    targetEnd = 2.5f,
    dampingSeconds = 0.1f,
)

// 3. Organic distance-based wobble using NoiseNode -> lateral position offset
fun createWobblyBrush(): BrushFamily {
    val wobbleBehavior = BrushBehavior(
        terminalNode = TargetNode(
            target = TargetNode.Target.POSITION_OFFSET_LATERAL_IN_MULTIPLES_OF_BRUSH_SIZE,
            targetModifierRangeStart = -0.25f,
            targetModifierRangeEnd = 0.25f,
            input = NoiseNode(
                seed = 12345,
                varyOver = ProgressDomain.DISTANCE_IN_CENTIMETERS,
                basePeriod = 0.4f,
            ),
        ),
    )
    return BrushFamily(
        tip = BrushTip(
            scaleX = 1f,
            scaleY = 1f,
            cornerRounding = 1f,
            behaviors = listOf(wobbleBehavior),
        ),
        paint = BrushPaint(selfOverlap = SelfOverlap.ANY),
    )
}
```

<br />

## Common pitfalls

- **Reversing node nesting order** : `TargetNode` or `PolarTargetNode` must be the outermost `terminalNode`, wrapping operator `ValueNode` instances that wrap `SourceNode` or `NoiseNode`.
- **Setting `rangeStart == rangeEnd`** : Range endpoints on `SourceNode`, `TargetNode`, and `PolarTargetNode` must be distinct (`start != end`); equal endpoints throw `IllegalArgumentException`. Use `ConstantNode` for fixed values.
- **Using `OutOfRange.CLAMP` on cyclic direction or orientation** : `DIRECTION_IN_RADIANS` and `ORIENTATION_IN_RADIANS` wrap around 2π---always pass `OutOfRange.REPEAT`.
- **Fading `OPACITY_MULTIPLIER` to `0f` without reducing `SIZE_MULTIPLIER` to
  `0f`** : Also taper `SIZE_MULTIPLIER` to `0f` so invisible stroke mesh geometry is pruned.