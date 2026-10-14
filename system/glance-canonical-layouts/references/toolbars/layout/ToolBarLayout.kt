/*
 * Copyright 2024 The Android Open Source Project
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
package com.example.platform.ui.appwidgets.glance.layout.toolbars.layout

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
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutDimens.iconSize
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutDimens.itemsSpacing
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutDimens.minButtonSize
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutDimens.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.Companion.canShowHeaderTitle
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.Companion.canUseFilledButtons
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.Companion.numberOfItemsThatFit
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.Companion.numberOfContentButtonsInTwoRowGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.HeaderTwoRowGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.HorizontalRow
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.TwoRowGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ToolBarLayoutSize.VerticalColumn
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting a brand icon, a most frequently used entrypoint along with 4
 * additional entry points that provide quick access to various functions in your app.
 *
 * This serves as an implementation suggestion, but should be customized to fit your product's
 * needs.
 *
 * @see appName a title for the toolbar e.g. app name that can be displayed at large sizes.
 * @see appIconRes a brand icon to be displayed in the toolbar at all sizes.
 * @param headerButton a button representing the most frequently used action of your app that is
 * displayed more prominently than than the other [buttons].
 * @param buttons list of additional 4 buttons for other frequently used functions in your app.
 */
@Composable
fun ToolBarLayout(
  appName: String,
  @DrawableRes appIconRes: Int,
  headerButton: ToolBarButton,
  // 4 items, as a list here for convenience, that you might inline in your implementation.
  buttons: List<ToolBarButton>,
) {
  // Deconstructed header items shown along side other buttons in smaller widget sizes where
  // a header isn't shown.
  val appIconItem: @Composable () -> Unit = {
    FluidHeaderAppIcon(iconRes = appIconRes)
  }
  val headerButtonItem: @Composable () -> Unit = {
    // Unlike in the combined header case, this header button is fluid & fills the available space
    // allowing us to display it along with other buttons.
    FluidHeaderIconButton(
      button = headerButton
    )
  }

  // Combined header item (which shows app icon and header button in the title bar); shown at larger
  // widget sizes.
  val header: @Composable () -> Unit = {
    Header(
      appIconRes = appIconRes,
      actionButton = headerButton,
      title = if (canShowHeaderTitle()) {
        appName
      } else {
        ""
      },
    )
  }

  // Other buttons
  val buttonItems: List<@Composable () -> Unit> =
    buttons.map { { FluidContentIconButton(it, filled = canUseFilledButtons()) } }

  when (val layoutSize = ToolBarLayoutSize.fromLocalSize()) {
    HorizontalRow, VerticalColumn -> {

      Scaffold(
        modifier = GlanceModifier.padding(vertical = widgetPadding),
        horizontalPadding = widgetPadding,
      ) {
        val horizontal = layoutSize == HorizontalRow
        val numberOfItems = numberOfItemsThatFit(
          horizontal = horizontal,
          minItemSize = minButtonSize,
          spacing = itemsSpacing
        )
        val allItems = listOf(appIconItem, headerButtonItem) + buttonItems
        val finalItems = allItems.take(numberOfItems)

        if (horizontal) {
          SpacedRow(
            items = finalItems,
            spacing = itemsSpacing,
            modifier = GlanceModifier.fillMaxSize()
          )
        } else {
          SpacedColumn(
            items = finalItems,
            spacing = itemsSpacing,
            modifier = GlanceModifier.fillMaxSize()
          )
        }
      }
    }

    TwoRowGrid -> {
      val contentButtonsToShow = buttonItems.take(
        numberOfContentButtonsInTwoRowGrid()
      )

      Scaffold(
        modifier = GlanceModifier.padding(vertical = widgetPadding),
        horizontalPadding = widgetPadding
      ) {
        TwoRowGrid(
          items = listOf(appIconItem, headerButtonItem) + contentButtonsToShow,
          spacing = itemsSpacing,
          modifier = GlanceModifier.fillMaxSize(),
        )
      }
    }

    HeaderTwoRowGrid -> {
      Scaffold(
        modifier = GlanceModifier.padding(bottom = widgetPadding),
        horizontalPadding = widgetPadding,
        titleBar = header,
      ) {
        TwoRowGrid(
          items = buttonItems,
          spacing = itemsSpacing,
          modifier = GlanceModifier.fillMaxSize()
        )
      }
    }
  }
}

/**
 * Title bar / header displayed at top at larger sizes.
 *
 * Displays brand icon, title, and a pill shaped filled action button.
 * @see [HeaderTwoRowGrid]
 */
@Composable
private fun Header(
  appIconRes: Int,
  title: String,
  actionButton: ToolBarButton,
) {
  TitleBar(
    startIcon = ImageProvider(appIconRes),
    title = title,
    iconColor = GlanceTheme.colors.primary,
    actions = {
      PillShapedButton(
        iconImageProvider = ImageProvider(actionButton.iconRes),
        contentDescription = actionButton.contentDescription,
        iconSize = iconSize,
        backgroundColor = if (canUseFilledButtons()) {
          GlanceTheme.colors.tertiary
        } else {
          ColorProvider(Color.Transparent, Color.Transparent)
        },
        contentColor = GlanceTheme.colors.onTertiary,
        onClick = actionButton.onClick,
        modifier = GlanceModifier.padding(end = widgetPadding)
      )
    }
  )
}

/**
 * Brand icon displayed in the compact sizes where a title bar (header) cannot be shown.
 *
 * The background of this icon fills the available space.
 *
 * Using a separate icon enables us to equally space and size it with other buttons.
 * @see [HorizontalRow], [VerticalColumn] & [TwoRowGrid]
 */
@Composable
private fun FluidHeaderAppIcon(@DrawableRes iconRes: Int) {
  Box(
    modifier = GlanceModifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Image(
      provider = ImageProvider(iconRes),
      contentDescription = null,
      modifier = GlanceModifier.size(iconSize),
      colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
    )
  }
}

/**
 * A rounded icon button for the prominent header action displayed alongside other buttons in
 * compact sizes where a title bar (header) cannot be shown.
 *
 * Using a separate icon button enables us to equally space and size it with other buttons.
 * @see [HorizontalRow], [VerticalColumn] & [TwoRowGrid]
 */
@Composable
private fun FluidHeaderIconButton(button: ToolBarButton) {
  RectangularIconButton(
    imageProvider = ImageProvider(button.iconRes),
    contentDescription = button.contentDescription,
    contentColor = if (canUseFilledButtons()) {
      GlanceTheme.colors.onTertiary
    } else {
      GlanceTheme.colors.onSecondaryContainer
    },
    backgroundColor = if (canUseFilledButtons()) {
      GlanceTheme.colors.tertiary
    } else {
      ColorProvider(Color.Transparent, Color.Transparent)
    },
    roundedCornerShape = RoundedCornerShape.FULL,
    iconSize = iconSize,
    modifier = GlanceModifier.fillMaxSize(),
    onClick = button.onClick
  )
}

/**
 * Button meant for regular action buttons that appear after the header / deconstructed header
 * items.
 *
 * Fills the given space, uses smaller radius, and supports filled / transparent backgrounds.
 */
@Composable
private fun FluidContentIconButton(button: ToolBarButton, filled: Boolean = true) {
  RectangularIconButton(
    imageProvider = ImageProvider(button.iconRes),
    contentDescription = button.contentDescription,
    iconSize = iconSize,
    roundedCornerShape = RoundedCornerShape.MEDIUM,
    backgroundColor = if (filled) {
      GlanceTheme.colors.secondaryContainer
    } else {
      ColorProvider(Color.Transparent, Color.Transparent)
    },
    contentColor = GlanceTheme.colors.onSecondaryContainer,
    onClick = button.onClick,
    modifier = GlanceModifier.fillMaxSize()
  )
}

/**
 * Data class representing buttons displayed in the toolbar widget.
 * @param iconRes Resource id of the icon button
 * @param contentDescription description about the button that can be used by the accessibility
 *                           services
 * @param onClick action to perform on click of the button
 * @param text optional text that will be displayed if space suffices.
 */
data class ToolBarButton(
  @DrawableRes val iconRes: Int,
  val contentDescription: String,
  val onClick: Action,
  val text: String? = null,
)

// Breakpoints from the UX design
private enum class ToolBarLayoutSize {
  // Row of app icon, featured button and regular action buttons that fit horizontally
  HorizontalRow,

  // Column of app icon, featured button and regular action buttons that fit vertically.
  VerticalColumn,

  // Two rows, 2-3 columns containing first column of app icon, featured action button and other
  // columns displaying regular action buttons that fit.
  TwoRowGrid,

  // Header row (containing app icon + featured button) followed by 2 row grid containing the 4
  // regular action buttons.
  HeaderTwoRowGrid;

  companion object {
    @Composable
    fun fromLocalSize(): ToolBarLayoutSize {
      val size = LocalSize.current
      val height = size.height
      val width = size.width

      return if (height < 128.dp) {
        HorizontalRow
      } else if (width < 128.dp) {
        VerticalColumn
      } else if (height < 172.dp) {
        TwoRowGrid
      } else {
        HeaderTwoRowGrid
      }
    }

    /**
     * Indicates if buttons with background color can be displayed for the current widget size.
     *
     * Background is hidden when we are limited by height / width.
     */
    @Composable
    fun canUseFilledButtons(): Boolean {
      val localSize = LocalSize.current

      return localSize.height >= 72.dp && localSize.width >= 72.dp
    }

    /**
     * Returns how many items to show that would potentially fit in the given orientation
     * (horizontal / vertical) if we were filling entire space.
     * @see [HorizontalRow] & [VerticalColumn]
     */
    @Composable
    fun numberOfItemsThatFit(
      horizontal: Boolean,
      minItemSize: Dp,
      spacing: Dp,
    ): Int {
      val size = if (horizontal) {
        LocalSize.current.width
      } else {
        LocalSize.current.height
      }

      // n buttons have n-1 content spacers, so, we add one to total width to make the width division
      // simpler.
      val normalizedWidth: Dp = size + spacing
      val normalizedButtonWidth: Dp = minItemSize + spacing
      // Number of equally wide buttons that fit in a row
      return ((normalizedWidth / normalizedButtonWidth)).toInt()
    }

    /**
     * Returns number of regular buttons that can fit in a 2-row grid where brand icon and a
     * featured action button would also be shown.
     *
     * @see [TwoRowGrid]
     */
    @Composable
    fun numberOfContentButtonsInTwoRowGrid() =
      if (LocalSize.current.width >= 240.dp) { // from UX design
        4 // 1st column (app icon, featured button) and 2nd & 3rd column (4 regular buttons)
      } else {
        2 // 1st column (app icon, featured button) and 2nd column (2 regular buttons)
      }

    /**
     * Identifies if we should show or hide the title in the header at the current widget size.
     */
    @Composable
    fun canShowHeaderTitle() =
      LocalSize.current.width >= 240.dp && LocalSize.current.height >= 172.dp // from UX design
  }
}

// Dimensions from UX design.
private object ToolBarLayoutDimens {
  /** Minimum size needed for buttons / clickable areas for accessibility. */
  val minButtonSize = 48.dp

  /** Padding around the content within the widget. */
  val widgetPadding = 12.dp

  /** Spacing between buttons in all layouts. */
  val itemsSpacing = 8.dp

  /** Size of icons in all buttons */
  val iconSize = 24.dp
}

/**
 * Previews for various breakpoints for this toolbar layout.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 56, heightDp = 56) // only app icon
@Preview(widthDp = 128, heightDp = 48) // reveals header button - no background
@Preview(widthDp = 128, heightDp = 72) // w/ background colors
@Preview(widthDp = 296, heightDp = 48) // more buttons - no background
@Preview(widthDp = 296, heightDp = 72) // more buttons - w/ background colors
@Preview(widthDp = 72, heightDp = 228) // vertical
@Preview(widthDp = 128, heightDp = 128) // 2x2 grid
@Preview(widthDp = 296, heightDp = 128) // 3x2 grid
@Preview(widthDp = 128, heightDp = 228) // w/ title bar - no title
@Preview(widthDp = 240, heightDp = 228) // w/ title bar and title
@Composable
private fun ToolbarPreview() {
    ToolBarLayout(
        appName = "App name",
        appIconRes = R.drawable.sample_app_logo,
        headerButton = ToolBarButton(
            iconRes = R.drawable.sample_add_icon,
            contentDescription = "add",
            onClick = actionStartDemoActivity("add button")
        ),
        buttons = listOf(
            ToolBarButton(
                iconRes = R.drawable.sample_mic_icon,
                contentDescription = "mic",
                onClick = actionStartDemoActivity("mic button")
            ),
            ToolBarButton(
                iconRes = R.drawable.sample_share_icon,
                contentDescription = "share",
                onClick = actionStartDemoActivity("share button")
            ),
            ToolBarButton(
                iconRes = R.drawable.sample_videocam_icon,
                contentDescription = "video",
                onClick = actionStartDemoActivity("video button")
            ),
            ToolBarButton(
                iconRes = R.drawable.sample_camera_icon,
                contentDescription = "camera",
                onClick = actionStartDemoActivity("camera button")
            )
        )
    )
}
