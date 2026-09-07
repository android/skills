When using surfaces to build a custom component, refer to the following source
code in `Surface.kt`:


```kotlin
/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.glimmer

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.xr.glimmer.internal.color.withTone
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

/**
 * A surface is a fundamental building block in Glimmer. A surface represents a distinct visual area
 * or 'physical' boundary for components such as buttons and cards. A [surface] implements shared
 * visual decoration for Jetpack Compose Glimmer components:
 * 1) Clipping: a surface clips its children to the shape specified by [shape]
 * 2) Border: a surface draws an inner border to emphasize the boundary of the component. When
 *    focused, a surface draws a wider border with a focused highlight on top to indicate the focus
 *    state.
 * 3) Background: a surface has a background color of [color] (and [focusedColor] when focused).
 * 4) Depth effect: a surface can have different [DepthEffect] shadows for different states, as
 *    specified by [depthEffect].
 * 5) Content color: a surface provides a [contentColor] (and [focusedContentColor] when focused)
 *    for text and icons inside the surface. By default this is calculated from the provided
 *    background color.
 * 6) Interaction states: when focused, a surface displays draws a wider border with a focused
 *    highlight on top. When pressed, a surface draws a pressed overlay. This happens for
 *    interactions emitted from [interactionSource].
 *
 * Use surface on its own for decorative elements that cannot be interacted with by a user:
 *
 * @sample androidx.xr.glimmer.samples.SurfaceSample
 *
 * In most cases surfaces should be interactive, to allow users to consistently move focus and
 * navigate between components. You can use [androidx.compose.foundation.focusable] for focus-only
 * surfaces, or [androidx.compose.foundation.clickable] and other modifiers for surfaces with
 * actions. To ensure the surface correctly reflects the interaction states, provide the same
 * [InteractionSource] to all modifiers.
 *
 * For example, to create a clickable surface:
 *
 * @sample androidx.xr.glimmer.samples.ClickableSurfaceSample
 *
 * Similarly, to create a focusable surface:
 *
 * @sample androidx.xr.glimmer.samples.FocusableSurfaceSample
 *
 * To create a surface with custom colors:
 *
 * @sample androidx.xr.glimmer.samples.CustomFocusedColorSurfaceSample
 * @param enabled controls the enabled state of this surface. When `false`, a disabled overlay
 *   visual will be drawn on top of the surface. Note that this only affects the visual decoration;
 *   it does not intercept input or block interaction states (such as focus or press) from the
 *   [interactionSource].
 * @param shape the [Shape] used to clip this surface, and also used to draw the background and
 *   border
 * @param color the background [Color] for this surface. When providing a custom color, ensure it is
 *   suitable for a surface background or use [SurfaceDefaults.color] to adapt it.
 * @param focusedColor the background [Color] for this surface when focused. When providing a custom
 *   color, ensure it is suitable for a focused surface background or use
 *   [SurfaceDefaults.focusedColor] to adapt it.
 * @param contentColor the [Color] for content inside this surface
 * @param focusedContentColor the [Color] for content inside this surface when focused
 * @param depthEffect the [SurfaceDepthEffect] for this surface, representing the [DepthEffect]
 *   shadows rendered in different states.
 * @param interactionSource the [InteractionSource] that emits [Interaction]s for this surface. For
 *   interactive surfaces, the [InteractionSource] instance provided to this surface must be shared
 *   with the modifier responsible for emitting [Interaction]s, such as
 *   [androidx.compose.foundation.focusable] or [androidx.compose.foundation.clickable].
 */
@Composable
public fun Modifier.surface(
    enabled: Boolean = true,
    shape: Shape = GlimmerTheme.shapes.medium,
    color: Color = GlimmerTheme.colors.surface,
    focusedColor: Color = SurfaceDefaults.focusedColor(color),
    contentColor: Color = calculateContentColor(color),
    focusedContentColor: Color = calculateContentColor(focusedColor),
    depthEffect: SurfaceDepthEffect? = null,
    interactionSource: InteractionSource? = null,
): Modifier =
    this.surfaceDepthEffect(depthEffect, shape, interactionSource)
        .clip(shape)
        .then(
            SurfaceNodeElement(
                enabled = enabled,
                color = color,
                focusedColor = focusedColor,
                contentColor = contentColor,
                focusedContentColor = focusedContentColor,
                shape = shape,
                interactionSource = interactionSource,
            )
        )

/** Contains default values used by [surface]. */
public object SurfaceDefaults {

    /**
     * Returns the background [Color] for a surface derived from the provided [baseColor].
     *
     * Adjusts the provided [baseColor] so that it is suitable for use as a surface background.
     *
     * @param baseColor the base [Color] of the surface
     * @return the surface background [Color], adjusted to improve content contrast
     */
    @Composable
    public fun color(baseColor: Color = GlimmerTheme.colors.surface): Color =
        baseColor.withTone(newTone = SurfaceColorTone)

    /**
     * Returns the focused background [Color] for a surface derived from the provided [baseColor].
     *
     * Adjusts the provided [baseColor] so that it is suitable for use as a focused surface
     * background.
     *
     * @param baseColor the base [Color] of the surface
     * @return the focused surface background [Color], adjusted to improve content contrast
     */
    @Composable
    public fun focusedColor(baseColor: Color = GlimmerTheme.colors.surface): Color =
        baseColor.withTone(newTone = FocusedSurfaceColorTone)
}

/**
 * Represents the [DepthEffect] used by a [surface] in different states.
 *
 * Focused [surface]s with a [focusedDepthEffect] will have a higher zIndex set so they can draw
 * their focused depth effect over siblings.
 *
 * @property [depthEffect] the [DepthEffect] used when the [surface] is in its default state (no
 *   other interactions are ongoing)
 * @property [focusedDepthEffect] the [DepthEffect] used when the [surface] is focused
 */
@Immutable
public class SurfaceDepthEffect(
    public val depthEffect: DepthEffect?,
    public val focusedDepthEffect: DepthEffect?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceDepthEffect) return false

        if (depthEffect != other.depthEffect) return false
        if (focusedDepthEffect != other.focusedDepthEffect) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depthEffect?.hashCode() ?: 0
        result = 31 * result + (focusedDepthEffect?.hashCode() ?: 0)
        return result
    }
}

/**
 * Surface node responsible for drawing the border, focused border and highlight, and pressed
 * overlay.
 */
private class SurfaceNodeElement(
    private val enabled: Boolean,
    private val color: Color,
    private val focusedColor: Color,
    private val contentColor: Color,
    private val focusedContentColor: Color,
    private val shape: Shape,
    private val interactionSource: InteractionSource?,
) : ModifierNodeElement<SurfaceNode>() {
    override fun create(): SurfaceNode =
        SurfaceNode(
            enabled = enabled,
            color = color,
            focusedColor = focusedColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor,
            shape = shape,
            interactionSource = interactionSource,
        )

    override fun update(node: SurfaceNode) =
        node.update(
            enabled = enabled,
            color = color,
            focusedColor = focusedColor,
            contentColor = contentColor,
            focusedContentColor = focusedContentColor,
            shape = shape,
            interactionSource = interactionSource,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SurfaceNodeElement) return false

        if (enabled != other.enabled) return false
        if (color != other.color) return false
        if (focusedColor != other.focusedColor) return false
        if (contentColor != other.contentColor) return false
        if (focusedContentColor != other.focusedContentColor) return false
        if (shape != other.shape) return false
        if (interactionSource != other.interactionSource) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + color.hashCode()
        result = 31 * result + focusedColor.hashCode()
        result = 31 * result + contentColor.hashCode()
        result = 31 * result + focusedContentColor.hashCode()
        result = 31 * result + shape.hashCode()
        result = 31 * result + (interactionSource?.hashCode() ?: 0)
        return result
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "surface"
        properties["enabled"] = enabled
        properties["color"] = color
        properties["focusedColor"] = focusedColor
        properties["contentColor"] = contentColor
        properties["focusedContentColor"] = focusedContentColor
        properties["shape"] = shape
        properties["interactionSource"] = interactionSource
    }
}

private class SurfaceNode(
    private var enabled: Boolean,
    private var color: Color,
    private var focusedColor: Color,
    private var contentColor: Color,
    private var focusedContentColor: Color,
    private var shape: Shape,
    private var interactionSource: InteractionSource?,
) : DrawModifierNode, DelegatingNode() {

    override val shouldAutoInvalidate = false

    var isFocused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    contentColorNode.update(focusedContentColor)
                    startFocusAnimation()
                } else {
                    contentColorNode.update(contentColor)
                    stopFocusAnimation()
                }
                // No need to invalidate the border cache - we build it ahead of time to account for
                // focus changes. Just invalidate draw so we can switch to drawing the correct
                // border.
                invalidateDraw()
            }
        }

    private val contentColorNode = delegate(ContentColorProviderNode(contentColor))

    // Cache borders and highlight for unfocused and focused states. This means we
    // can avoid recreating these for a given surface, if the border and shape never
    // change. Changing between unfocused and focus states only requires a draw
    // invalidation, as the borders are already cached.

    // Focused border - this consists of two layers. A 'base' layer (which is the
    // unfocused border with a different size) and the highlight we draw on top of
    // this base layer. We need to increase the size of the underlying border to
    // make sure that the highlight area fully matches the underlying border, to
    // avoid inconsistent areas of coverage due to the transparency of the
    // highlight.
    private var borderLogic: BorderLogic? = null

    // Border shader / brush
    var borderShader: RuntimeShader? = null
    var borderShaderBrush: Brush? = null

    // Graphics Layer
    private var blurGraphicsLayer: GraphicsLayer? = null

    private var borderLayer: GraphicsLayer? = null
    private var borderLayerProvider: (() -> GraphicsLayer)? = null

    private var interactionCollectionJob: Job? = null
    private var ambientMotionJob: Job? = null

    private var _focusProgress: Animatable<Float, AnimationVector1D>? = null
    private val focusProgress
        get() = _focusProgress?.value ?: 0f

    private var _ambientProgress: Animatable<Float, AnimationVector1D>? = null
    private val ambientProgress
        get() = _ambientProgress?.value ?: 0f

    private var _pressedProgress: Animatable<Float, AnimationVector1D>? = null
    private val pressedProgress
        get() = _pressedProgress?.value ?: 0f

    // Job that runs for a minimum duration to make sure quick presses are still visible
    private var minimumPressDuration: Job? = null
    private var pressReleaseAnimation: Job? = null

    private var horizontalShader: RuntimeShader? = null
    private var verticalShader: RuntimeShader? = null

    private val cornerRadii = FloatArray(4)

    private val focusedBorderColor0: Color = Color.White

    private var focusedBorderColor1: Color = focusedColor.withTone(newTone = 85f)

    private var focusedBorderColor2: Color = focusedColor.withTone(newTone = 69f)

    private var focusedBorderColor3: Color = focusedColor.withTone(newTone = 77f)

    fun update(
        enabled: Boolean,
        color: Color,
        focusedColor: Color,
        contentColor: Color,
        focusedContentColor: Color,
        shape: Shape,
        interactionSource: InteractionSource?,
    ) {
        if (this.enabled != enabled) {
            this.enabled = enabled
            invalidateDraw()
        }
        if (this.color != color) {
            this.color = color
            invalidateDraw()
        }
        if (this.focusedColor != focusedColor) {
            this.focusedColor = focusedColor
            updateFocusedBorderColors(focusedColor)
            invalidateDraw()
        }
        if (this.contentColor != contentColor) {
            this.contentColor = contentColor
            if (!isFocused) contentColorNode.update(contentColor)
        }
        if (this.focusedContentColor != focusedContentColor) {
            this.focusedContentColor = focusedContentColor
            if (isFocused) contentColorNode.update(focusedContentColor)
        }
        if (this.shape != shape) {
            this.shape = shape
            invalidateDraw()
        }
        if (this.interactionSource != interactionSource) {
            this.interactionSource = interactionSource
            observeInteractions()
        }
    }

    override fun onAttach() {
        observeInteractions()
    }

    var isPressed = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    _pressedProgress = _pressedProgress ?: Animatable(0f)
                    pressReleaseAnimation?.cancel()
                    minimumPressDuration?.cancel()

                    minimumPressDuration =
                        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                            delay(PressedMinimumDurationMillis)
                        }
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        _pressedProgress?.animateTo(1f, PressedEnterAnimationSpec)
                    }
                } else {
                    _pressedProgress?.let { progress ->
                        pressReleaseAnimation =
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                minimumPressDuration?.join()
                                progress.animateTo(0f, PressedExitAnimationSpec)
                            }
                    }
                }
                invalidateDraw()
            }
        }

    private fun observeInteractions() {
        interactionCollectionJob?.cancel()
        interactionCollectionJob = null
        isFocused = false
        isPressed = false
        interactionSource?.let { source ->
            interactionCollectionJob =
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    var focusCount = 0
                    var pressCount = 0
                    source.interactions.collect { interaction ->
                        when (interaction) {
                            is FocusInteraction.Focus -> focusCount++
                            is FocusInteraction.Unfocus -> focusCount--
                            is PressInteraction.Press -> pressCount++
                            is PressInteraction.Release -> pressCount--
                            is PressInteraction.Cancel -> pressCount--
                        }
                        isFocused = focusCount > 0
                        isPressed = pressCount > 0
                    }
                }
        }
    }

    private fun startFocusAnimation() {
        _focusProgress = _focusProgress ?: Animatable(0f)
        _ambientProgress = _ambientProgress ?: Animatable(0f)

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusProgress?.animateTo(targetValue = 1f, animationSpec = FocusedEnterAnimationSpec)
        }

        ambientMotionJob?.cancel()
        ambientMotionJob =
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Delay after focus enter starts
                delay(AmbientInitialDelayMillis)
                while (isActive) {
                    _ambientProgress?.animateTo(
                        targetValue = 1f,
                        animationSpec = AmbientAnimationSpec,
                    )
                    _ambientProgress?.snapTo(0f)
                    delay(AmbientDelayMillis)
                }
            }
    }

    private fun stopFocusAnimation() {
        ambientMotionJob?.cancel()
        ambientMotionJob = null

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _ambientProgress?.animateTo(targetValue = 0f, animationSpec = FocusedExitAnimationSpec)
        }

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusProgress?.animateTo(targetValue = 0f, animationSpec = FocusedExitAnimationSpec)
        }
    }

    override fun ContentDrawScope.draw() {
        val outline = shape.createOutline(size, layoutDirection, this)
        val focusProgress = focusProgress
        val ambientProgress = ambientProgress
        val pressedProgress = pressedProgress

        // RuntimeShader used for progressive blur requires at least API 33.
        // Fall back to drawing a solid background and border on older API levels.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            drawBlurredBackgroundAndBorder(outline, focusProgress, ambientProgress, pressedProgress)
        } else {
            drawBackgroundAndBorder(outline, focusProgress, pressedProgress)
        }

        drawContent()

        if (!enabled) {
            drawOutline(outline, color = DisabledOverlayColor)
        }
    }

    /**
     * Draws background, pressed overlay, and solid border for devices below API 33
     * ([Build.VERSION_CODES.TIRAMISU]).
     */
    private fun DrawScope.drawBackgroundAndBorder(
        outline: Outline,
        focusProgress: Float,
        pressedProgress: Float,
    ) {
        val backgroundColor = lerp(color, focusedColor, focusProgress)

        // Pressed overlay transition: #FFFFFF with 16% opacity when pressed
        val compositeBackground =
            if (pressedProgress > 0f) {
                val pressedOverlayColor =
                    PressedOverlayColor.copy(alpha = PressedOverlayAlpha * pressedProgress)
                pressedOverlayColor.compositeOver(backgroundColor)
            } else {
                backgroundColor
            }

        drawOutline(outline, color = compositeBackground)

        val borderLogic = borderLogic ?: BorderLogic().also { borderLogic = it }
        val borderLayerProvider =
            borderLayerProvider
                ?: {
                        borderLayer
                            ?: requireGraphicsContext().createGraphicsLayer().also {
                                borderLayer = it
                            }
                    }
                    .also { borderLayerProvider = it }
        val borderColor = lerp(DefaultSolidBorderIdleColor, focusedBorderColor1, focusProgress)

        borderLogic.drawBorder(
            this,
            calculateSolidBorderWidth(),
            SolidColor(borderColor),
            borderLayerProvider,
            outline,
        )
    }

    /** Calculates solid border width for devices below API 33 ([Build.VERSION_CODES.TIRAMISU]). */
    private fun DrawScope.calculateSolidBorderWidth(): Dp =
        lerp(DefaultSurfaceBorderWidth, FocusedSurfaceBorderWidth, this@SurfaceNode.focusProgress)

    /**
     * Calculates AGSL shader border width for API 33 ([Build.VERSION_CODES.TIRAMISU]) and higher.
     */
    private fun DrawScope.calculateBorderWidth(): Dp =
        lerp(
            lerp(
                DefaultSurfaceBorderWidth,
                FocusedSurfaceBorderWidth,
                this@SurfaceNode.focusProgress,
            ),
            (size.minDimension / 8f).toDp(),
            this@SurfaceNode.pressedProgress,
        )

    /**
     * Records the background + border into a [GraphicsLayer] and applies two-pass (horizontal then
     * vertical) Gaussian blur.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun DrawScope.drawBlurredBackgroundAndBorder(
        outline: Outline,
        focusProgress: Float,
        ambientProgress: Float,
        pressedProgress: Float,
    ) {
        val width = size.width
        val height = size.height

        val idleBlurRadiusStartPx = IdleBlurRadiusStart.toPx()
        val idleBlurRadiusEndPx = IdleBlurRadiusEnd.toPx()
        val focusedBlurRadiusStartPx = FocusedBlurRadiusStart.toPx()
        val focusedBlurRadiusEndPx = FocusedBlurRadiusEnd.toPx()
        val ambientBlurRadiusMaxStartPx = AmbientBlurRadiusMaxStart.toPx()
        val ambientBlurRadiusMaxEndPx = AmbientBlurRadiusMaxEnd.toPx()

        if (outline is Outline.Rounded) {
            val roundRect = outline.roundRect
            cornerRadii[0] = roundRect.bottomRightCornerRadius.x
            cornerRadii[1] = roundRect.topRightCornerRadius.x
            cornerRadii[2] = roundRect.bottomLeftCornerRadius.x
            cornerRadii[3] = roundRect.topLeftCornerRadius.x
        } else {
            cornerRadii[0] = 0f
            cornerRadii[1] = 0f
            cornerRadii[2] = 0f
            cornerRadii[3] = 0f
        }

        val horizontalShader =
            BlurShaderHelper.configureShader(
                    shader = this@SurfaceNode.horizontalShader,
                    isVertical = false,
                    width = width,
                    height = height,
                    cornerRadii = cornerRadii,
                    focusProgress = focusProgress,
                    ambientProgress = ambientProgress,
                    pressedProgress = pressedProgress,
                    idleBlurRadiusStartPx = idleBlurRadiusStartPx,
                    idleBlurRadiusEndPx = idleBlurRadiusEndPx,
                    focusedBlurRadiusStartPx = focusedBlurRadiusStartPx,
                    focusedBlurRadiusEndPx = focusedBlurRadiusEndPx,
                    ambientBlurRadiusMaxStartPx = ambientBlurRadiusMaxStartPx,
                    ambientBlurRadiusMaxEndPx = ambientBlurRadiusMaxEndPx,
                )
                .also { this@SurfaceNode.horizontalShader = it }

        val verticalShader =
            BlurShaderHelper.configureShader(
                    shader = this@SurfaceNode.verticalShader,
                    isVertical = true,
                    width = width,
                    height = height,
                    cornerRadii = cornerRadii,
                    focusProgress = focusProgress,
                    ambientProgress = ambientProgress,
                    pressedProgress = pressedProgress,
                    idleBlurRadiusStartPx = idleBlurRadiusStartPx,
                    idleBlurRadiusEndPx = idleBlurRadiusEndPx,
                    focusedBlurRadiusStartPx = focusedBlurRadiusStartPx,
                    focusedBlurRadiusEndPx = focusedBlurRadiusEndPx,
                    ambientBlurRadiusMaxStartPx = ambientBlurRadiusMaxStartPx,
                    ambientBlurRadiusMaxEndPx = ambientBlurRadiusMaxEndPx,
                )
                .also { this@SurfaceNode.verticalShader = it }

        val horizontalEffect =
            RenderEffect.createRuntimeShaderEffect(horizontalShader, UniformContent)
        val verticalEffect = RenderEffect.createRuntimeShaderEffect(verticalShader, UniformContent)
        val renderEffect = RenderEffect.createChainEffect(verticalEffect, horizontalEffect)

        val blurLayer =
            blurGraphicsLayer
                ?: requireGraphicsContext().createGraphicsLayer().also { blurGraphicsLayer = it }

        blurLayer.renderEffect = renderEffect.asComposeRenderEffect()
        blurLayer.record(
            density = Density(density, fontScale),
            layoutDirection = layoutDirection,
            size = IntSize(ceil(size.width).toInt(), ceil(size.height).toInt()),
        ) {
            drawShaderBackgroundAndBorder(outline, focusProgress, ambientProgress)
        }

        drawLayer(blurLayer)
    }

    /** Draws the background and the AGSL shader border for API 33+ */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun DrawScope.drawShaderBackgroundAndBorder(
        outline: Outline,
        focusProgress: Float,
        ambientProgress: Float,
    ) {
        val backgroundColor = lerp(color, focusedColor, focusProgress)
        drawOutline(outline, backgroundColor)

        borderShader =
            BorderShaderHelper.configureShader(
                shader = borderShader,
                size = size,
                focusProgress = focusProgress,
                ambientProgress = ambientProgress,
                focusedBorderColor0 = focusedBorderColor0,
                focusedBorderColor1 = focusedBorderColor1,
                focusedBorderColor2 = focusedBorderColor2,
                focusedBorderColor3 = focusedBorderColor3,
            )
        val borderShaderBrush =
            borderShaderBrush ?: ShaderBrush(borderShader!!).also { borderShaderBrush = it }
        val borderLogic = borderLogic ?: BorderLogic().also { borderLogic = it }
        val borderLayerProvider =
            borderLayerProvider
                ?: {
                        borderLayer
                            ?: requireGraphicsContext().createGraphicsLayer().also {
                                borderLayer = it
                            }
                    }
                    .also { borderLayerProvider = it }
        borderLogic.drawBorder(
            this,
            calculateBorderWidth(),
            borderShaderBrush,
            borderLayerProvider,
            outline,
        )
    }

    override fun onDetach() {
        _focusProgress = null
        _ambientProgress = null
        _pressedProgress = null
        ambientMotionJob?.cancel()
        ambientMotionJob = null

        val gContext = requireGraphicsContext()

        borderLayerProvider = null
        borderLayer?.let {
            gContext.releaseGraphicsLayer(it)
            borderLayer = null
        }

        blurGraphicsLayer?.let {
            gContext.releaseGraphicsLayer(it)
            blurGraphicsLayer = null
        }

        horizontalShader = null
        verticalShader = null
    }

    private fun updateFocusedBorderColors(focusedColor: Color) {
        focusedBorderColor1 = focusedColor.withTone(newTone = 85f)
        focusedBorderColor2 = focusedColor.withTone(newTone = 69f)
        focusedBorderColor3 = focusedColor.withTone(newTone = 77f)
    }
}

/**
 * Renders and animates a [surface]'s [depthEffect] for a given [shape], by observing
 * [interactionSource].
 */
@Composable
private fun Modifier.surfaceDepthEffect(
    depthEffect: SurfaceDepthEffect?,
    shape: Shape,
    interactionSource: InteractionSource?,
): Modifier {
    if (depthEffect == null) return this
    val focusedProgress = remember { Animatable(0f) }
    // If focused and there is focused depth effect, we need to draw the surface on top of
    // other siblings to make sure the depth effect occludes siblings.
    val zIndex by remember {
        // Derived to avoid invalidating layout each frame of the animation
        derivedStateOf {
            if (depthEffect.focusedDepthEffect != null && focusedProgress.value >= 0.5f) 1f else 0f
        }
    }
    if (interactionSource != null) {
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is FocusInteraction.Focus ->
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            focusedProgress.animateTo(1f, FocusedEnterAnimationSpec)
                        }

                    is FocusInteraction.Unfocus ->
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            focusedProgress.animateTo(0f, FocusedExitAnimationSpec)
                        }
                }
            }
        }
    }

    return layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) { placeable.place(0, 0, zIndex = zIndex) }
        }
        .depthEffect(
            from = depthEffect.depthEffect,
            to = depthEffect.focusedDepthEffect,
            shape = shape,
            progress = { focusedProgress.value },
        )
}

/** String keys map directly to uniform parameter names inside the AGSL runtime shaders. */
private const val UniformResolution = "uResolution"
private const val UniformCornerRadii = "uCornerRadii"
private const val UniformFocusProgress = "uFocusProgress"
private const val UniformAmbientProgress = "uAmbientProgress"
private const val UniformPressedProgress = "uPressedProgress"
private const val UniformIdleStartRadius = "uIdleRadiusStart"
private const val UniformIdleEndRadius = "uIdleRadiusEnd"
private const val UniformFocusedStartRadius = "uFocusedRadiusStart"
private const val UniformFocusedEndRadius = "uFocusedRadiusEnd"
private const val UniformAmbientMaxStartRadius = "uAmbientMaxRadiusStart"
private const val UniformAmbientMaxEndRadius = "uAmbientMaxRadiusEnd"
private const val UniformContent = "uContent"

private const val UniformBorderFocusedColor0 = "uBorderFocusedColor0"
private const val UniformBorderFocusedColor1 = "uBorderFocusedColor1"
private const val UniformBorderFocusedColor2 = "uBorderFocusedColor2"
private const val UniformBorderFocusedColor3 = "uBorderFocusedColor3"

private const val SurfaceColorTone = 20f
private const val FocusedSurfaceColorTone = 34f

/** Default border width for a [surface]. */
private val DefaultSurfaceBorderWidth = 1.5.dp

/** Focused border width for a [surface]. */
private val FocusedSurfaceBorderWidth = 2.dp

/** Blur radii at the start / end of the blur gradient while the surface is idle (unfocused). */
private val IdleBlurRadiusStart = 2.dp
private val IdleBlurRadiusEnd = 8.dp

/** Blur radii at the start / end of the blur gradient while the surface is focused. */
private val FocusedBlurRadiusStart = 1.dp
private val FocusedBlurRadiusEnd = 3.dp

/** Maximum blur radii reached at the peak of the ambient motion */
private val AmbientBlurRadiusMaxStart = 5.3.dp
private val AmbientBlurRadiusMaxEnd = 15.9.dp

private val DefaultSolidBorderIdleColor = Color(0.25f, 0.25f, 0.25f, 0.5f)

/** Enter animation for focus highlight and depth effect */
private val FocusedEnterAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 800, easing = LinearOutSlowInEasing)

/** Exit animation for focus highlight and depth effect */
private val FocusedExitAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 500, easing = LinearOutSlowInEasing)

private val AmbientAnimationSpec: AnimationSpec<Float> =
    tween(durationMillis = 2000, easing = LinearEasing)

private const val AmbientInitialDelayMillis = 1800L
private const val AmbientDelayMillis = 4000L

internal val DisabledOverlayColor = Color(0x8F191919)

private val PressedOverlayColor = Color.White
private const val PressedOverlayAlpha = 0.16f

private val PressedEnterAnimationSpec: AnimationSpec<Float> =
    spring(dampingRatio = 0.84f, stiffness = 8000f)

private val PressedExitAnimationSpec: AnimationSpec<Float> =
    spring(dampingRatio = 0.85f, stiffness = 50f)

private const val PressedMinimumDurationMillis = 300L

@Language(value = "AGSL")
private const val BorderShader =
    """
uniform float2 uResolution;
uniform float uFocusProgress;
uniform float uAmbientProgress;

const half4 uBorderIdleColor0 = half4(0.81, 0.81, 0.81, 0.9); // Top-Left
const half4 uBorderIdleColor1 = half4(0.25, 0.25, 0.25, 0.5); // Top-Right
const half4 uBorderIdleColor2 = half4(0.16, 0.16, 0.16, 0.4); // Bottom-Right
const half4 uBorderIdleColor3 = half4(0.49, 0.49, 0.49, 0.7); // Bottom-Left

uniform half4 uBorderFocusedColor0;
uniform half4 uBorderFocusedColor1;
uniform half4 uBorderFocusedColor2;
uniform half4 uBorderFocusedColor3;

const half4 uAnimationPeakColor = half4(1.0, 1.0, 1.0, 1.0); // White

const float PI = 3.14159265;

half4 main(float2 fragCoord) {
    // Compute ambient pulse
    // Phase 1: Increase Ambient Pulse
    // Linearly scales 0.0 -> 0.2835 onto a 0.0 -> 1.0 range.
    float t1 = clamp(uAmbientProgress / 0.2835, 0.0, 1.0);

    // Phase 2: Steady and then Ramp-Down
    // Delays the fade-out until progress passes 0.375
    // Uses the remaining 0.625 of the duration to smoothly decline back to 0.0.
    float t2 = clamp(1.0 - (uAmbientProgress - 0.375) / 0.625, 0.0, 1.0);

    // Uses t1 (ramp-up/hold) for the first 37.5%, then switches to t2 (ramp-down).
    float ambientPulse = mix(t1, t2, step(0.375, uAmbientProgress));

    // Compute focus pulse peaking animation peak color at exactly 0.5
    float focusPulse = clamp(1.0 - 2.0 * abs(uFocusProgress - 0.5), 0.0, 1.0);

    // Use maximum of focus and ambient pulses
    float maxAnimationPeakPulse = max(ambientPulse, focusPulse);

    half4 colors[4];
    colors[0] = mix(mix(uBorderIdleColor0, uBorderFocusedColor0, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[1] = mix(mix(uBorderIdleColor1, uBorderFocusedColor1, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[2] = mix(mix(uBorderIdleColor2, uBorderFocusedColor2, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);
    colors[3] = mix(mix(uBorderIdleColor3, uBorderFocusedColor3, uFocusProgress), uAnimationPeakColor, maxAnimationPeakPulse);

    float w = uResolution.x;
    float h = uResolution.y;

    // Normalize coordinates to [-1, 1] relative to center
    float nx = 2.0 * fragCoord.x / max(w, 1.0) - 1.0;
    float ny = 2.0 * fragCoord.y / max(h, 1.0) - 1.0;

    // Compute the angle of the current pixel relative to the center in radians [-PI, PI].
    float theta = atan(ny, nx);

    // Map the angle from [-PI, PI] to a clean [0.0, 1.0] percentage circle.
    // The "+ 0.75 * PI" offsets the starting point (0.0) to the Top-Left corner.
    float d_cw = fract((theta + 0.75 * PI) / (2.0 * PI));

    // As focus progresses from 0 to 1, the gradient start position shifts from top left to top right
    float d_start = mix(0.0, 0.25, uFocusProgress);

    // Apply the rotation shift to the current pixel's position.
    float d_rotated = mod(d_cw - d_start + 1.0, 1.0);
    float dist = min(d_rotated, 1.0 - d_rotated);

    // Branchless color interpolation using chained mixes
    float factor0 = smoothstep(0.0, 0.1, dist);
    float factor1 = smoothstep(0.1, 0.25, dist);
    float factor2 = smoothstep(0.25, 0.4, dist);

    half4 color = mix(colors[0], colors[1], factor0);
    color = mix(color, colors[2], factor1);
    color = mix(color, colors[3], factor2);

    return color;
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object BlurShaderHelper {
    @JvmStatic
    fun configureShader(
        shader: RuntimeShader?,
        isVertical: Boolean,
        width: Float,
        height: Float,
        cornerRadii: FloatArray,
        focusProgress: Float,
        ambientProgress: Float,
        pressedProgress: Float,
        idleBlurRadiusStartPx: Float,
        idleBlurRadiusEndPx: Float,
        focusedBlurRadiusStartPx: Float,
        focusedBlurRadiusEndPx: Float,
        ambientBlurRadiusMaxStartPx: Float,
        ambientBlurRadiusMaxEndPx: Float,
    ): RuntimeShader {
        val runtimeShader = shader ?: RuntimeShader(getBlurShader(isVertical))
        runtimeShader.setFloatUniform(UniformResolution, width, height)
        runtimeShader.setFloatUniform(UniformCornerRadii, cornerRadii)
        runtimeShader.setFloatUniform(UniformFocusProgress, focusProgress)
        runtimeShader.setFloatUniform(UniformAmbientProgress, ambientProgress)
        runtimeShader.setFloatUniform(UniformPressedProgress, pressedProgress)
        runtimeShader.setFloatUniform(UniformIdleStartRadius, idleBlurRadiusStartPx)
        runtimeShader.setFloatUniform(UniformIdleEndRadius, idleBlurRadiusEndPx)
        runtimeShader.setFloatUniform(UniformFocusedStartRadius, focusedBlurRadiusStartPx)
        runtimeShader.setFloatUniform(UniformFocusedEndRadius, focusedBlurRadiusEndPx)
        runtimeShader.setFloatUniform(UniformAmbientMaxStartRadius, ambientBlurRadiusMaxStartPx)
        runtimeShader.setFloatUniform(UniformAmbientMaxEndRadius, ambientBlurRadiusMaxEndPx)
        return runtimeShader
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object BorderShaderHelper {
    @JvmStatic
    fun configureShader(
        shader: RuntimeShader?,
        size: Size,
        focusProgress: Float,
        ambientProgress: Float,
        focusedBorderColor0: Color,
        focusedBorderColor1: Color,
        focusedBorderColor2: Color,
        focusedBorderColor3: Color,
    ): RuntimeShader {
        val runtimeShader = shader ?: RuntimeShader(BorderShader)
        runtimeShader.setFloatUniform(UniformResolution, size.width, size.height)
        runtimeShader.setFloatUniform(UniformFocusProgress, focusProgress)
        runtimeShader.setFloatUniform(UniformAmbientProgress, ambientProgress)

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor0,
            focusedBorderColor0.red,
            focusedBorderColor0.green,
            focusedBorderColor0.blue,
            focusedBorderColor0.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor1,
            focusedBorderColor1.red,
            focusedBorderColor1.green,
            focusedBorderColor1.blue,
            focusedBorderColor1.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor2,
            focusedBorderColor2.red,
            focusedBorderColor2.green,
            focusedBorderColor2.blue,
            focusedBorderColor2.alpha,
        )

        runtimeShader.setFloatUniform(
            UniformBorderFocusedColor3,
            focusedBorderColor3.red,
            focusedBorderColor3.green,
            focusedBorderColor3.blue,
            focusedBorderColor3.alpha,
        )

        return runtimeShader
    }
}

@Language(value = "AGSL")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun getBlurShader(isVertical: Boolean): String {
    val loopOffset =
        if (isVertical) "float2(0.0, i + weightH / weight)" else "float2(i + weightH / weight, 0.0)"
    val oddOffset = if (isVertical) "float2(0.0, r)" else "float2(r, 0.0)"
    return """
        uniform shader uContent;

        uniform float2 uResolution;
        uniform float4 uCornerRadii;
        uniform float uFocusProgress;
        uniform float uAmbientProgress;
        uniform float uPressedProgress;

        uniform float uIdleRadiusStart;
        uniform float uIdleRadiusEnd;
        uniform float uFocusedRadiusStart;
        uniform float uFocusedRadiusEnd;
        uniform float uAmbientMaxRadiusStart;
        uniform float uAmbientMaxRadiusEnd;

        const float MAX_RADIUS = 120.0;
        const float PI = 3.14159265;

        float sdRoundedBox(float2 p, float2 b, float4 r) {
            float2 sign_p = step(0.0, p);
            float2 r2 = mix(r.zw, r.xy, sign_p.x);
            float rad = mix(r2.y, r2.x, sign_p.y);

            float2 q = abs(p) - b + rad;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
        }

        float gaussian(float x, float sigma) { return exp(-(x * x) / (2.0 * sigma * sigma)); }

        float radiusAt(float2 coord, float2 start, float2 end, float r0, float r1) {
            float2 axis = end - start;
            float t = clamp(dot(coord - start, axis) / max(dot(axis, axis), 1.0), 0.0, 1.0);
            return mix(r0, r1, t);
        }

        float4 blur(float2 coord, float radius) {
            float r = floor(radius);
            if (r < 1.0) return uContent.eval(coord);
            float sigma = max(radius / 2.0, 1.0);
            float2 center = uResolution / 2.0;
            float2 halfSize = uResolution / 2.0;
            float weightSum = 1.0;
            float limit = min(sigma * 3, MAX_RADIUS);
            float4 result = uContent.eval(coord);
            for (float i = 1.0; i < MAX_RADIUS; i += 2.0) {
                if (i >= limit) { break; }
                float weightL = gaussian(i, sigma);
                float weightH = gaussian(i + 1.0, sigma);
                float weight = weightL + weightH;
                float2 off = $loopOffset;

                float2 c1 = coord - off;
                float d1 = sdRoundedBox(c1 - center, halfSize, uCornerRadii);
                float w1 = mix(weight, 0.0, step(0.0, d1));
                if (w1 > 0.0) {
                    result += w1 * uContent.eval(c1);
                    weightSum += w1;
                }

                float2 c2 = coord + off;
                float d2 = sdRoundedBox(c2 - center, halfSize, uCornerRadii);
                float w2 = mix(weight, 0.0, step(0.0, d2));
                if (w2 > 0.0) {
                    result += w2 * uContent.eval(c2);
                    weightSum += w2;
                }
            }
            float oddMask = fract(r * 0.5) * 2.0 * (1.0 - step(MAX_RADIUS, r));
            float ow = gaussian(r, sigma) * oddMask;

            if (ow > 0.0) {
                float2 oo = $oddOffset;
                float2 o1 = coord - oo;
                float2 o2 = coord + oo;
                result += ow * uContent.eval(o1) + ow * uContent.eval(o2);
                weightSum += 2.0 * ow;
            }
            return result / weightSum;
        }

        half4 main(float2 coord) {
            // Compute ambient pulse
            // Phase 1: Increase Ambient Pulse
            // Linearly scales 0.0 -> 0.2835 onto a 0.0 -> 1.0 range.
            float t1 = clamp(uAmbientProgress / 0.2835, 0.0, 1.0);

            // Phase 2: Steady and then Ramp-Down
            // Delays the fade-out until progress passes 0.375
            // Uses the remaining 0.625 of the duration to smoothly decline back to 0.0.
            float t2 = clamp(1.0 - (uAmbientProgress - 0.375) / 0.625, 0.0, 1.0);

            // Uses t1 (ramp-up/hold) for the first 37.5%, then switches to t2 (ramp-down).
            float pulse = mix(t1, t2, step(0.375, uAmbientProgress));

            // Mix focused and ambient blurs based on the pulse
            float focusedBlurStart = mix(uFocusedRadiusStart, uAmbientMaxRadiusStart, pulse);
            float focusedBlurEnd = mix(uFocusedRadiusEnd, uAmbientMaxRadiusEnd, pulse);

            // Interpolate between idle and focused/ambient blur ranges
            float blur0 = mix(uIdleRadiusStart, focusedBlurStart, uFocusProgress);
            float blur1 = mix(uIdleRadiusEnd, focusedBlurEnd, uFocusProgress);

            // Enforce massive uniform during press interaction
            float minDimension = min(uResolution.x, uResolution.y);
            float pressedBlur = minDimension / 3.0;
            blur0 = mix(blur0, pressedBlur, uPressedProgress);
            blur1 = mix(blur1, pressedBlur, uPressedProgress);

            float centerX = uResolution.x / 2.0;
            float centerY = uResolution.y / 2.0;

            // Start position of blur shifts across the width based on focus progress
            float startX = uFocusProgress * uResolution.x;
            float startY = 0.0;
            float endX = uResolution.x / 2.0;
            float endY = uResolution.y;

            // Anchor coordinates for rotational animation phase
            float baseStartX = uResolution.x;
            float baseStartY = 0.0;
            float baseEndX = uResolution.x / 2.0;
            float baseEndY = uResolution.y;

            // Drive a full circle around the center for ambient motion
            float angle = uAmbientProgress * PI * 2;
            float cosA = cos(angle);
            float sinA = sin(angle);

            // Calculate start and end rotation X and Y
            float rotStartX = centerX + (baseStartX - centerX) * cosA - (baseStartY - centerY) * sinA;
            float rotStartY = centerY + (baseStartX - centerX) * sinA + (baseStartY - centerY) * cosA;
            float rotEndX = centerX + (baseEndX - centerX) * cosA - (baseEndY - centerY) * sinA;
            float rotEndY = centerY + (baseEndX - centerX) * sinA + (baseEndY - centerY) * cosA;

            float2 finalStart = mix(float2(startX, startY), float2(rotStartX, rotStartY), uFocusProgress);
            float2 finalEnd = mix(float2(endX, endY), float2(rotEndX, rotEndY), uFocusProgress);

            return blur(coord, radiusAt(coord, finalStart, finalEnd, blur0, blur1));
        }
    """
}
```

<br />