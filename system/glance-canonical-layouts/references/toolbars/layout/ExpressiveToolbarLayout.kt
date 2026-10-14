/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout.toolbars.layout

import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolBarLayoutDimens.minCenterButtonTapTarget
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolBarLayoutDimens.minCornerButtonTapTarget
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolBarLayoutDimens.scaledButtonBackground
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolBarLayoutDimens.scaledIconSize
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity

/**
 * Layout focused on presenting 5 most frequently used actions in your app.
 *
 * Uses a 4-sided cookie cutout as the background shape of the widget - the shape fits at-least two
 * vertical or two horizontal edges to meet the quality criteria; see
 * https://developer.android.com/design/ui/mobile/guides/widgets/widget_quality_guide
 *
 * Lays out the buttons using a Box layout by placing a center button with sufficient tap target
 * first; followed by 2x2 grid of corner buttons centered in each cell of the grid. The way Box
 * layout works, center button is placed behind the corner buttons. To avoid the overlap, at small
 * sizes, we show only center button.
 *
 * This serves as an implementation suggestion, but should be customized to fit your product's
 * needs. For instance, similar approach can be used to present the widget in other material shapes
 * such as a circle shape or a clover shape. At 4x1 size, you may decide to show a rectangular
 * toolbar instead of single center button shown in this layout.
 *
 * @param centerButton a button shown at the center of the shape; usually used for most frequently
 *                     used action among the 5
 * @param cornerButtons list of other 4 buttons displayed at the corners of the shape; arranged in
 *                      order (left-to-right, row-by-row) in a 2x2 grid
 * @see ExpressiveToolbarLayoutSize for supported breakpoints
 */
@Composable
fun ExpressiveToolbarLayout(
    centerButton: ExpressiveToolBarButton,
    // 4 buttons passed as list for convenience that you can inline in your implementation.
    cornerButtons: List<ExpressiveToolBarButton>,
) {
    checkCornerButtonsListSize(cornerButtons)

    val widgetSize = LocalSize.current
    // Since we use a non-rectangular background, we aren't able to fill entire widget space, however,
    // we try to at least fill the space either horizontally or vertically.
    val cookieBackgroundSize = widgetSize.height.coerceAtMost(widgetSize.width)

    val backgroundModifier = GlanceModifier
        .size(cookieBackgroundSize)
        .appWidgetBackground()
        .fourSidedCookieBackground()

    when (ExpressiveToolbarLayoutSize.fromLocalSize()) {
        ExpressiveToolbarLayoutSize.SMALL -> CenterButtonOnlyLayout(
            centerButton = centerButton,
            modifier = backgroundModifier
        )

        ExpressiveToolbarLayoutSize.MEDIUM -> AllButtonsScaledLayout(
            centerButton = centerButton,
            cornerButtons = cornerButtons,
            cookieBackgroundSize = cookieBackgroundSize,
            modifier = backgroundModifier
        )
    }
}

@Composable
private fun GlanceModifier.fourSidedCookieBackground(): GlanceModifier {
    return this.background(
        imageProvider = ImageProvider(R.drawable.four_side_cookie_background),
        colorFilter = ColorFilter.tint(GlanceTheme.colors.widgetBackground)
    )
}

/**
 * Lays out just the center button
 */
@Composable
private fun CenterButtonOnlyLayout(
    centerButton: ExpressiveToolBarButton,
    modifier: GlanceModifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        CenterButton(
            button = centerButton,
            buttonBackgroundSize = 48.dp,
            clickableSize = 48.dp,
            iconSize = 24.dp,
            filled = false,
            shape = RoundedCornerShape.MEDIUM
        )
    }
}

/**
 * Lays out corner and center buttons by scaling them based on background size and reference design.
 */
@Composable
private fun AllButtonsScaledLayout(
    centerButton: ExpressiveToolBarButton,
    cornerButtons: List<ExpressiveToolBarButton>,
    cookieBackgroundSize: Dp,
    modifier: GlanceModifier,
) {
    val buttonBackgroundSize = scaledButtonBackground(cookieBackgroundSize)
    val iconSize = scaledIconSize(cookieBackgroundSize)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // Centered at the bottom layer (with larger tap target).
        CenterButton(
            button = centerButton,
            buttonBackgroundSize = buttonBackgroundSize,
            clickableSize = minCenterButtonTapTarget,
            iconSize = iconSize,
            // Older android versions didn't support rounded corners, we use unfilled buttons in those
            // versions.
            filled = Build.VERSION.SDK_INT > Build.VERSION_CODES.S,
            shape = RoundedCornerShape.MEDIUM
        )
        // 4 corner buttons on top layer
        CornerButtonsGrid(
            cornerButtons = cornerButtons,
            buttonBackgroundSize = buttonBackgroundSize,
            iconSize = iconSize,
            clickableSize = buttonBackgroundSize.coerceAtLeast(minCornerButtonTapTarget),
            modifier = GlanceModifier.fillMaxSize()
        )
    }
}

/**
 * Data class representing buttons displayed in an expressive toolbar widget.
 * @param iconRes Resource id of the icon button
 * @param contentDescription description about the button that can be used by the accessibility
 *                           services
 * @param onClick action to perform on click of the button
 * @param text optional text that will be displayed if space suffices.
 */
data class ExpressiveToolBarButton(
    @DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: Action,
    val text: String? = null,
)

@Composable
private fun CornerButtonsGrid(
    cornerButtons: List<ExpressiveToolBarButton>,
    buttonBackgroundSize: Dp,
    clickableSize: Dp,
    iconSize: Dp,
    modifier: GlanceModifier,
) {
    checkCornerButtonsListSize(cornerButtons)

    TwoRowGrid(
        spacing = 0.dp,
        modifier = modifier,
        items = cornerButtons.map {
            {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    CornerButton(
                        toolBarButton = it,
                        buttonBackgroundSize = buttonBackgroundSize,
                        iconSize = iconSize,
                        clickableSize = clickableSize
                    )
                }
            }
        },
    )
}

@Composable
private fun CornerButton(
    toolBarButton: ExpressiveToolBarButton,
    buttonBackgroundSize: Dp,
    clickableSize: Dp,
    iconSize: Dp,
) {
    IconButton(
        imageProvider = ImageProvider(toolBarButton.iconRes),
        contentDescription = toolBarButton.contentDescription,
        iconSize = iconSize,
        backgroundSize = buttonBackgroundSize,
        backgroundColor = androidx.glance.color.ColorProvider(Color.Transparent, Color.Transparent),
        contentColor = GlanceTheme.colors.primary,
        roundedCornerShape = RoundedCornerShape.FULL,
        onClick = toolBarButton.onClick,
        modifier = GlanceModifier.size(clickableSize)
    )
}

@Composable
private fun CenterButton(
    button: ExpressiveToolBarButton,
    clickableSize: Dp,
    buttonBackgroundSize: Dp,
    iconSize: Dp,
    shape: RoundedCornerShape,
    filled: Boolean,
) {
    val backgroundColor = if (filled) {
        GlanceTheme.colors.tertiary
    } else {
        androidx.glance.color.ColorProvider(Color.Transparent, Color.Transparent)
    }

    val contentColor = if (filled) {
        GlanceTheme.colors.onTertiary
    } else {
        GlanceTheme.colors.primary
    }

    IconButton(
        imageProvider = ImageProvider(button.iconRes),
        contentDescription = button.contentDescription,
        iconSize = iconSize,
        backgroundSize = buttonBackgroundSize,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        roundedCornerShape = shape,
        onClick = button.onClick,
        modifier = GlanceModifier.size(clickableSize)
    )
}

/**
 * Buttons that have separate clickable area and filled background container.
 */
@Composable
private fun IconButton(
    imageProvider: ImageProvider,
    contentDescription: String,
    backgroundSize: Dp,
    iconSize: Dp,
    roundedCornerShape: RoundedCornerShape,
    onClick: Action,
    modifier: GlanceModifier,
    backgroundColor: ColorProvider,
    contentColor: ColorProvider,
) {
    Box( // clickable area
        contentAlignment = Alignment.Center,
        modifier = modifier
            .cornerRadius(roundedCornerShape.cornerRadius)
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick)
    ) {
        Box( // colored background
            contentAlignment = Alignment.Center,
            modifier = GlanceModifier
                .size(backgroundSize)
                .background(backgroundColor)
                .cornerRadius(roundedCornerShape.cornerRadius)
        ) {
            Image(
                provider = imageProvider,
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = GlanceModifier.size(iconSize)
            )
        }
    }
}

private enum class ExpressiveToolbarLayoutSize {
    // Size at which only center button is shown
    SMALL,

    // Size at which center button has filled background and corner buttons are shown as well.
    MEDIUM;

    companion object {
        @Composable
        fun fromLocalSize(): ExpressiveToolbarLayoutSize {
            val size = LocalSize.current
            val boxSize = size.width.coerceAtMost(size.height)

            return when {
                // Size below which buttons would get too close to be able to keep their tap targets
                // accessible. We would like to drop widget at 2x2 size. In most 5 column grids, 2x2 size
                // has ~140-141dp width. So, we try to show all buttons at that breakpoint and show only
                // center button at smaller sizes.
                boxSize < 140.dp -> SMALL

                // At these sizes, all buttons can be shown and filled background for center button.
                else -> MEDIUM
            }
        }
    }
}

// Various dimensions coming from the UX design (with a 4-sided cookie cut shape)
private object ExpressiveToolBarLayoutDimens {
    val minCornerButtonTapTarget = 48.dp
    val minCenterButtonTapTarget = 60.dp

    // Reference UX design uses an icon of 24.dp when widget size is 182.dp
    // So, we scale the icon by 24/182 = 13%
    fun scaledIconSize(backgroundSize: Dp) =
        Dp((13 * backgroundSize.value) / 100)

    // Reference UX design uses background of 48.dp when widget size is 182.dp
    // So, we scale the icon by 48/182 = 26%
    // This isn't size of tap target, so it can be smaller.
    fun scaledButtonBackground(backgroundSize: Dp) =
        Dp((26 * backgroundSize.value) / 100)
}

private fun checkCornerButtonsListSize(cornerButtons: List<ExpressiveToolBarButton>) {
    if (cornerButtons.size != 4) {
        Log.w("ExpressiveToolbarLayout", "Expected 4 corner buttons, but passed ${cornerButtons.size}")
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Composable
@Preview(widthDp = 56, heightDp = 56) // Min resize size
@Preview(widthDp = 120, heightDp = 115) // Min drop size
@Preview(widthDp = 140, heightDp = 140) // all buttons; center button transparent bg
@Preview(widthDp = 624, heightDp = 200) // Max size
fun ExpressiveToolbarLayoutPreview() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ExpressiveToolbarLayout(
            centerButton = ExpressiveToolBarButton(
                iconRes = R.drawable.sample_add_icon,
                contentDescription = "Add notes",
                onClick = actionStartDemoActivity("add notes button")
            ),
            cornerButtons = listOf(
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_mic_icon,
                    contentDescription = "mic",
                    onClick = actionStartDemoActivity("mic button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_camera_icon,
                    contentDescription = "camera",
                    onClick = actionStartDemoActivity("camera button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_share_icon,
                    contentDescription = "share",
                    onClick = actionStartDemoActivity("share button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_file_upload_icon,
                    contentDescription = "file upload",
                    onClick = actionStartDemoActivity("file upload button")
                ),
            )
        )
    }
}
