Reference providing copy-ready custom `BrushFamily` and `Brush` implementations
built with the Kotlin constructor API (`androidx.ink` `1.1.0-alpha03` and
higher).

## Shared behavior helpers

All following recipes use these `smoothedBehavior` and `jitterBehavior`
helpers:


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

fun jitterBehavior(
    target: TargetNode.Target,
    targetStart: Float,
    targetEnd: Float,
    basePeriod: Float,
    seed: Int = 42,
): BrushBehavior = BrushBehavior(
    terminalNode = TargetNode(
        target = target,
        targetModifierRangeStart = targetStart,
        targetModifierRangeEnd = targetEnd,
        input = NoiseNode(
            seed = seed,
            varyOver = ProgressDomain.DISTANCE_IN_CENTIMETERS,
            basePeriod = basePeriod,
        ),
    ),
)
```

<br />

## Recipe 1: Pressure-responsive pen

Maps stylus pressure (`NORMALIZED_PRESSURE`) to both stroke size (`50%` to
`150%`) and per-vertex opacity (`30%` to `100%`). Uses `SelfOverlap.ANY` so the
mesh renderer applies per-vertex `OPACITY_MULTIPLIER`:


```kotlin
fun createPressureResponsiveBrush(): Brush {
    val pressurePenFamily = BrushFamily(
        coats = listOf(
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1f,
                    scaleY = 1f,
                    cornerRounding = 1f,
                    behaviors = listOf(
                        smoothedBehavior(
                            source = SourceNode.Source.NORMALIZED_PRESSURE,
                            sourceStart = 0f,
                            sourceEnd = 1f,
                            target = TargetNode.Target.SIZE_MULTIPLIER,
                            targetStart = 0.5f,
                            targetEnd = 1.5f,
                            dampingSeconds = 0.15f,
                        ),
                        smoothedBehavior(
                            source = SourceNode.Source.NORMALIZED_PRESSURE,
                            sourceStart = 0f,
                            sourceEnd = 1f,
                            target = TargetNode.Target.OPACITY_MULTIPLIER,
                            targetStart = 0.3f,
                            targetEnd = 1.0f,
                            dampingSeconds = 0.1f,
                        ),
                    ),
                ),
                paint = BrushPaint(selfOverlap = SelfOverlap.ANY),
            ),
        ),
    )

    return Brush.createWithComposeColor(
        family = pressurePenFamily,
        color = Color.Black,
        size = 10f,
        epsilon = 0.1f,
    )
}
```

<br />

## Recipe 2: Tilt-responsive calligraphy nib

Combines an asymmetric, 45-degree-angled narrow `BrushTip` (`scaleX = 0.3f`,
`scaleY = 1.0f`, `rotationDegrees = 45f`) with stylus tilt (`TILT_IN_RADIANS` to
`WIDTH_MULTIPLIER`) and travel direction (`DIRECTION_IN_RADIANS` with
`OutOfRange.REPEAT` to `ROTATION_OFFSET_IN_RADIANS`):


```kotlin
fun createCalligraphyBrush(): BrushFamily {
    return BrushFamily(
        coats = listOf(
            BrushCoat(
                tip = BrushTip(
                    scaleX = 0.3f,
                    scaleY = 1.0f,
                    cornerRounding = 0.2f,
                    rotationDegrees = 45f,
                    behaviors = listOf(
                        smoothedBehavior(
                            source = SourceNode.Source.TILT_IN_RADIANS,
                            sourceStart = 0f,
                            sourceEnd = (Math.PI / 2).toFloat(),
                            target = TargetNode.Target.WIDTH_MULTIPLIER,
                            targetStart = 1.0f,
                            targetEnd = 2.5f,
                            dampingSeconds = 0.1f,
                        ),
                        smoothedBehavior(
                            source = SourceNode.Source.DIRECTION_IN_RADIANS,
                            sourceStart = 0f,
                            sourceEnd = (2 * Math.PI).toFloat(),
                            target = TargetNode.Target.ROTATION_OFFSET_IN_RADIANS,
                            targetStart = (-Math.PI).toFloat(),
                            targetEnd = Math.PI.toFloat(),
                            dampingSeconds = 0.05f,
                            outOfRange = OutOfRange.REPEAT,
                        ),
                    ),
                ),
                paint = BrushPaint(selfOverlap = SelfOverlap.DISCARD),
            ),
        ),
    )
}
```

<br />

## Recipe 3: Textured shading pencil with noise wobble

Combines tilt-based width, pressure-based opacity, procedural slant wobble
(`NoiseNode` to `SLANT_OFFSET_IN_RADIANS`), and a `BrushPaint.TilingTexture`
grain layer (`"pencil-grain"` registered in `TextureBitmapStore`):


```kotlin
fun createShadingPencilBrush(): BrushFamily {
    return BrushFamily(
        coats = listOf(
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1f,
                    scaleY = 1f,
                    cornerRounding = 1f,
                    behaviors = listOf(
                        smoothedBehavior(
                            source = SourceNode.Source.TILT_IN_RADIANS,
                            sourceStart = 0f,
                            sourceEnd = (Math.PI / 2).toFloat(),
                            target = TargetNode.Target.WIDTH_MULTIPLIER,
                            targetStart = 1.0f,
                            targetEnd = 2.5f,
                            dampingSeconds = 0.1f,
                        ),
                        smoothedBehavior(
                            source = SourceNode.Source.NORMALIZED_PRESSURE,
                            sourceStart = 0f,
                            sourceEnd = 1f,
                            target = TargetNode.Target.OPACITY_MULTIPLIER,
                            targetStart = 0.3f,
                            targetEnd = 1.0f,
                            dampingSeconds = 0.1f,
                        ),
                        jitterBehavior(
                            target = TargetNode.Target.SLANT_OFFSET_IN_RADIANS,
                            targetStart = -0.15f,
                            targetEnd = 0.15f,
                            basePeriod = 0.3f,
                        ),
                    ),
                ),
                paint = BrushPaint(
                    selfOverlap = SelfOverlap.ANY,
                    colorFunctions = listOf(BrushPaint.ColorFunction.OpacityMultiplier(0.8f)),
                    textureLayers = listOf(
                        BrushPaint.TilingTexture(
                            clientTextureId = "pencil-grain",
                            sizeX = 1.0f,
                            sizeY = 1.0f,
                            sizeUnit = BrushPaint.TextureLayer.SizeUnit.BRUSH_SIZE,
                            blendMode = BrushPaint.TextureLayer.BlendMode.MODULATE,
                        ),
                    ),
                ),
            ),
        ),
    )
}
```

<br />

## Recipe 4: Speed-sensitive watercolor wash

Fades stroke opacity (`OPACITY_MULTIPLIER`) as stylus speed
(`SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND`) increases, with a repeating
watercolor wash `TilingTexture`:


```kotlin
fun createWatercolorBrush(): BrushFamily {
    return BrushFamily(
        coats = listOf(
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1f,
                    scaleY = 1f,
                    cornerRounding = 1f,
                    behaviors = listOf(
                        smoothedBehavior(
                            source = SourceNode.Source.SPEED_IN_MULTIPLES_OF_BRUSH_SIZE_PER_SECOND,
                            sourceStart = 0f,
                            sourceEnd = 8f,
                            target = TargetNode.Target.OPACITY_MULTIPLIER,
                            targetStart = 1.0f,
                            targetEnd = 0.2f,
                            dampingSeconds = 0.3f,
                        ),
                    ),
                ),
                paint = BrushPaint(
                    selfOverlap = SelfOverlap.ANY,
                    colorFunctions = listOf(BrushPaint.ColorFunction.OpacityMultiplier(0.6f)),
                    textureLayers = listOf(
                        BrushPaint.TilingTexture(
                            clientTextureId = "watercolor-wash",
                            sizeX = 2.0f,
                            sizeY = 2.0f,
                            sizeUnit = BrushPaint.TextureLayer.SizeUnit.BRUSH_SIZE,
                            blendMode = BrushPaint.TextureLayer.BlendMode.MODULATE,
                        ),
                    ),
                ),
            ),
        ),
    )
}
```

<br />

## Recipe 5: Multi-coat layered shadow brush

Uses two `BrushCoat` layers in `BrushFamily.coats`: a wider (`1.3f`),
low-opacity (`0.15f`) bottom coat (`coats[0]`) rendered underneath a
pressure-responsive opaque top coat (`coats[1]`):


```kotlin
fun createMultiCoatBrush(): BrushFamily {
    return BrushFamily(
        coats = listOf(
            // Coat 0: Wider semi-transparent shadow layer underneath
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1.3f,
                    scaleY = 1.3f,
                    cornerRounding = 1f,
                ),
                paint = BrushPaint(
                    selfOverlap = SelfOverlap.DISCARD,
                    colorFunctions = listOf(
                        BrushPaint.ColorFunction.OpacityMultiplier(0.15f),
                    ),
                ),
            ),
            // Coat 1: Main opaque stroke layer on top
            BrushCoat(
                tip = BrushTip(
                    scaleX = 1f,
                    scaleY = 1f,
                    cornerRounding = 1f,
                    behaviors = listOf(
                        smoothedBehavior(
                            source = SourceNode.Source.NORMALIZED_PRESSURE,
                            sourceStart = 0f,
                            sourceEnd = 1f,
                            target = TargetNode.Target.SIZE_MULTIPLIER,
                            targetStart = 0.5f,
                            targetEnd = 1.5f,
                            dampingSeconds = 0.15f,
                        ),
                    ),
                ),
                paint = BrushPaint(
                    selfOverlap = SelfOverlap.DISCARD,
                    colorFunctions = listOf(
                        BrushPaint.ColorFunction.OpacityMultiplier(1f),
                    ),
                ),
            ),
        ),
    )
}
```

<br />

## Common pitfalls

- **Missing texture registration** : Textured recipes (`"pencil-grain"`, `"watercolor-wash"`) render as flat fills unless their bitmaps are supplied in `TextureBitmapStore`.
- **Stacking multiple behaviors on `SIZE_MULTIPLIER` unintentionally** : Multiplier targets combine multiplicatively---two `0.5f` to `1.5f` size behaviors produce a combined `0.25f` to `2.25f` range.
- **Using `SelfOverlap.DISCARD` on pressure-opacity or watercolor coats** : Always use `SelfOverlap.ANY` or `SelfOverlap.ACCUMULATE` when a coat relies on `OPACITY_MULTIPLIER` behaviors or overlap shading.