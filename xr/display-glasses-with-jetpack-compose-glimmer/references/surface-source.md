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
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.lerp
import androidx.xr.glimmer.internal.BlurShader
import androidx.xr.glimmer.internal.BorderShader
import androidx.xr.glimmer.internal.SurfaceAnimationAmbientDelayMillis
import androidx.xr.glimmer.internal.SurfaceAnimationAmbientEnterExit
import androidx.xr.glimmer.internal.SurfaceAnimationAmbientInitialDelayMillis
import androidx.xr.glimmer.internal.SurfaceAnimationFocusedEnter
import androidx.xr.glimmer.internal.SurfaceAnimationFocusedExit
import androidx.xr.glimmer.internal.SurfaceAnimationPressedEnter
import androidx.xr.glimmer.internal.SurfaceAnimationPressedExit
import androidx.xr.glimmer.internal.SurfaceAnimationPressedMinimumDurationMillis
import androidx.xr.glimmer.internal.SurfaceDisabledOverlayColor
import androidx.xr.glimmer.internal.SurfaceFocusedColorTone
import androidx.xr.glimmer.internal.color.withTone
import androidx.xr.glimmer.internal.surfacePressedOverlayColor
import kotlin.math.ceil
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
 *   suitable for a surface background.
 * @param focusedColor the background [Color] for this surface when focused. When providing a custom
 *   color, ensure it is suitable for a focused surface background or use
 *   [SurfaceDefaults.calculateFocusedColor] to adapt it.
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
    focusedColor: Color = SurfaceDefaults.calculateFocusedColor(color),
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
     * Derives a color suitable for use as a focused background color from the provided [seedColor].
     *
     * @param seedColor the base [Color] from which the focused surface color will be derived
     * @return a color to be used as the focused surface background [Color], adjusted to improve
     *   content contrast
     */
    public fun calculateFocusedColor(seedColor: Color): Color =
        seedColor.withTone(newTone = SurfaceFocusedColorTone)
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
) : Modifier.Node(), DrawModifierNode, ContentColorProviderNode {

    override val shouldAutoInvalidate = false

    var isFocused = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    currentContentColor = focusedContentColor
                    startFocusAnimation()
                } else {
                    currentContentColor = contentColor
                    stopFocusAnimation()
                }
                // No need to invalidate the border cache - we build it ahead of time to account for
                // focus changes. Just invalidate draw so we can switch to drawing the correct
                // border.
                invalidateDraw()
            }
        }

    override var currentContentColor: Color by mutableStateOf(contentColor)
        private set

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
            if (!isFocused) currentContentColor = contentColor
        }
        if (this.focusedContentColor != focusedContentColor) {
            this.focusedContentColor = focusedContentColor
            if (isFocused) currentContentColor = focusedContentColor
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
                            delay(SurfaceAnimationPressedMinimumDurationMillis)
                        }
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        _pressedProgress?.animateTo(1f, SurfaceAnimationPressedEnter)
                    }
                } else {
                    _pressedProgress?.let { progress ->
                        pressReleaseAnimation =
                            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                                minimumPressDuration?.join()
                                progress.animateTo(0f, SurfaceAnimationPressedExit)
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
            _focusProgress?.animateTo(
                targetValue = 1f,
                animationSpec = SurfaceAnimationFocusedEnter,
            )
        }

        ambientMotionJob?.cancel()
        ambientMotionJob =
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                // Delay after focus enter starts
                delay(SurfaceAnimationAmbientInitialDelayMillis)
                while (isActive) {
                    _ambientProgress?.animateTo(
                        targetValue = 1f,
                        animationSpec = SurfaceAnimationAmbientEnterExit,
                    )
                    _ambientProgress?.snapTo(0f)
                    delay(SurfaceAnimationAmbientDelayMillis)
                }
            }
    }

    private fun stopFocusAnimation() {
        ambientMotionJob?.cancel()
        ambientMotionJob = null

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _ambientProgress?.animateTo(
                targetValue = 0f,
                animationSpec = SurfaceAnimationFocusedExit,
            )
        }

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            _focusProgress?.animateTo(
                targetValue = 0f,
                animationSpec = SurfaceAnimationFocusedExit,
            )
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
            drawOutline(outline, color = SurfaceDisabledOverlayColor)
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
                surfacePressedOverlayColor(pressedProgress).compositeOver(backgroundColor)
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
        val borderColor =
            lerp(BorderShader.DefaultSolidBorderIdleColor, focusedBorderColor1, focusProgress)

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
        lerp(
            BorderShader.DefaultSurfaceBorderWidth,
            BorderShader.FocusedSurfaceBorderWidth,
            this@SurfaceNode.focusProgress,
        )

    /**
     * Calculates AGSL shader border width for API 33 ([Build.VERSION_CODES.TIRAMISU]) and higher.
     */
    private fun DrawScope.calculateBorderWidth(): Dp =
        lerp(
            lerp(
                BorderShader.DefaultSurfaceBorderWidth,
                BorderShader.FocusedSurfaceBorderWidth,
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

        val idleBlurRadiusStartPx = BlurShader.IdleBlurRadiusStart.toPx()
        val idleBlurRadiusEndPx = BlurShader.IdleBlurRadiusEnd.toPx()
        val focusedBlurRadiusStartPx = BlurShader.FocusedBlurRadiusStart.toPx()
        val focusedBlurRadiusEndPx = BlurShader.FocusedBlurRadiusEnd.toPx()
        val ambientBlurRadiusMaxStartPx = BlurShader.AmbientBlurRadiusMaxStart.toPx()
        val ambientBlurRadiusMaxEndPx = BlurShader.AmbientBlurRadiusMaxEnd.toPx()

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
            BlurShader.configureShader(
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
            BlurShader.configureShader(
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
            RenderEffect.createRuntimeShaderEffect(horizontalShader, BlurShader.UniformContent)
        val verticalEffect =
            RenderEffect.createRuntimeShaderEffect(verticalShader, BlurShader.UniformContent)
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
            BorderShader.configureShader(
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
                            focusedProgress.animateTo(1f, SurfaceAnimationFocusedEnter)
                        }

                    is FocusInteraction.Unfocus ->
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            focusedProgress.animateTo(0f, SurfaceAnimationFocusedExit)
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
```

<br />