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
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.headerItemHeight
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.iconSize
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.itemsSpacing
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.minButtonSize
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.sideBarLeadingItemWidth
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutDimens.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.Companion.canShowSearchText
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.Companion.canUseFilledButtons
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.Companion.numberOfItemsThatFit
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.HeaderTwoRowGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.HorizontalRow
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.SideBarTwoRowGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.TwoByTwoGrid
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayoutSize.VerticalColumn
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity

/**
 * Layout focused on presenting a search entrypoint along with additional handy shortcuts that
 * provide quick access to frequently used functions.
 *
 * This serves as an implementation suggestion, but should be customized to fit your product's
 * needs.
 *
 * @param searchButton a button representing the search action in the widget displayed more
 *                     prominently than the trailing buttons..
 * @param trailingButtons list of 4 buttons representing handy shortcuts to frequently used
 *                     functions in your app.
 * @see SearchToolBarLayoutSize for supported breakpoints
 */
@Composable
fun SearchToolBarLayout(
  searchButton: SearchToolBarButton,
  // 4 items, as a list here for convenience, that you might inline in your implementation.
  trailingButtons: List<SearchToolBarButton>,
) {
  val searchButtonItem: @Composable () -> Unit = {
    if (canShowSearchText()) {
      SearchBar(searchButton = searchButton)
    } else {
      SearchIconButton(
        searchButton = searchButton,
        filled = canUseFilledButtons()
      )
    }
  }

  val trailingButtonItems: List<@Composable () -> Unit> =
    trailingButtons.map {
      {
        TrailingButton(
          button = it,
          filled = canUseFilledButtons()
        )
      }
    }

  Scaffold(
    modifier = GlanceModifier.padding(vertical = widgetPadding),
    horizontalPadding = widgetPadding
  ) {
    when (val layoutSize = SearchToolBarLayoutSize.fromLocalSize()) {
      HorizontalRow, VerticalColumn -> {
        val horizontal = (layoutSize == HorizontalRow)
        val numberOfItems = numberOfItemsThatFit(
          horizontal = horizontal,
          minItemSize = minButtonSize,
          spacing = itemsSpacing
        )
        val allItems = listOf(searchButtonItem) + trailingButtonItems
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

      TwoByTwoGrid -> TwoRowGrid(
        // 4 items including search button
        items = listOf(searchButtonItem) + trailingButtonItems.take(3),
        spacing = itemsSpacing
      )

      SideBarTwoRowGrid -> SideBarTwoRowGrid(
        sideBarItem = searchButtonItem,
        items = trailingButtonItems.take(4),
        sideBarWidth = sideBarLeadingItemWidth,
        spacing = itemsSpacing
      )

      HeaderTwoRowGrid -> HeaderTwoRowGrid(
        headerItem = searchButtonItem,
        items = trailingButtonItems.take(4),
        headerHeight = headerItemHeight,
        spacing = itemsSpacing
      )
    }
  }
}

/**
 * Data class representing different buttons displayed in the search toolbar widget.
 * @param iconRes Resource id of the icon button.
 * @param contentDescription description about the button that can be used by the accessibility
 *                           services.
 * @param onClick action to perform on click of the button.
 * @param text optional text that can be displayed if space is available.
 */
data class SearchToolBarButton(
  @DrawableRes val iconRes: Int,
  val contentDescription: String,
  val onClick: Action,
  val text: String? = null,
)

/**
 * Search entrypoint in style of a rounded icon button.
 */
@Composable
private fun SearchIconButton(
  searchButton: SearchToolBarButton,
  filled: Boolean,
) {
  RectangularIconButton(
    imageProvider = ImageProvider(searchButton.iconRes),
    contentDescription = searchButton.contentDescription,
    backgroundColor = if (filled) {
      GlanceTheme.colors.tertiary
    } else {
      ColorProvider(Color.Transparent, Color.Transparent)
    },
    contentColor = if (filled) {
      GlanceTheme.colors.onTertiary
    } else {
      GlanceTheme.colors.onSecondaryContainer
    },
    iconSize = iconSize,
    roundedCornerShape = RoundedCornerShape.FULL,
    onClick = searchButton.onClick,
    modifier = GlanceModifier.fillMaxSize()
  )
}

/**
 * Search entrypoint in style of a search bar.
 */
@Composable
private fun SearchBar(searchButton: SearchToolBarButton) {
  Row(
    horizontalAlignment = Alignment.Start,
    verticalAlignment = Alignment.CenterVertically,
    modifier = GlanceModifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .background(GlanceTheme.colors.secondaryContainer)
      .cornerRadius(RoundedCornerShape.FULL.cornerRadius)
      .semantics { this.contentDescription = searchButton.contentDescription }
      .clickable(searchButton.onClick),
  ) {
    // Search or brand icon
    Image(
      provider = ImageProvider(searchButton.iconRes),
      contentDescription = null,
      colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
      modifier = GlanceModifier.size(iconSize)
    )
    // Followed by text
    searchButton.text?.let {
      Spacer(GlanceModifier.width(8.dp))
      Text(
        text = it,
        maxLines = 1,
        style = TextStyle(
          color = GlanceTheme.colors.onSecondaryContainer,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium
        ),
      )
    }
  }
}

/**
 * Rounded action buttons that support [filled] background when specified.
 */
@Composable
private fun TrailingButton(
  button: SearchToolBarButton,
  filled: Boolean = true,
) {
  RectangularIconButton(
    imageProvider = ImageProvider(button.iconRes),
    contentDescription = button.contentDescription,
    backgroundColor = if (filled) {
      GlanceTheme.colors.secondaryContainer
    } else {
      ColorProvider(Color.Transparent, Color.Transparent)
    },
    contentColor = GlanceTheme.colors.onSecondaryContainer,
    onClick = button.onClick,
    iconSize = iconSize,
    roundedCornerShape = RoundedCornerShape.MEDIUM,
    modifier = GlanceModifier.fillMaxSize()
  )
}

// Breakpoints based on the UX design.
private enum class SearchToolBarLayoutSize {
  // Row of search button followed by action buttons that fit horizontally.
  HorizontalRow,

  // Column of search button and action buttons that fit vertically.
  VerticalColumn,

  // A two row, two column grid containing search button and 3 trailing buttons.
  TwoByTwoGrid,

  // A side bar for search followed by a 2x2 grid of trailing buttons.
  SideBarTwoRowGrid,

  // A header containing header bar followed by a 2x2 grid of trailing buttons.
  HeaderTwoRowGrid;

  companion object {
    @Composable
    fun fromLocalSize(): SearchToolBarLayoutSize {
      val size = LocalSize.current
      val height = size.height
      val width = size.width

      return if (height < 128.dp) {
        HorizontalRow
      } else if (width < 128.dp) {
        VerticalColumn
      } else if (height < 188.dp && width < 188.dp) {
        TwoByTwoGrid
      } else if (height < 188.dp) {
        SideBarTwoRowGrid
      } else {
        HeaderTwoRowGrid
      }
    }

    /**
     * Helper to decide whether to show search text in current widget size.
     */
    @Composable
    fun canShowSearchText(): Boolean {
      val localSize = LocalSize.current

      // Per breakpoints in the UX design
      return localSize.width >= 184.dp && localSize.height >= 188.dp
    }

    /**
     * Helper to decide whether to show filled icons vs without containers in current widget size.
     */
    @Composable
    fun canUseFilledButtons(): Boolean {
      val localSize = LocalSize.current

      // Per breakpoints in the UX design
      return localSize.height >= 72.dp && localSize.width >= 72.dp
    }

    /**
     * Helper to decide how many items to show in the available space in given orientation.
     *
     * @param horizontal if its a horizontal orientation
     * @param minItemSize min size to maintain for each item when identify how many to fit
     * @param spacing spacing to between items
     */
    @Composable
    fun numberOfItemsThatFit(horizontal: Boolean, minItemSize: Dp, spacing: Dp): Int {
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
  }
}

// Various dimensions coming from the UX design
private object SearchToolBarLayoutDimens {
  /** Minimum size needed for buttons / clickable areas for accessibility. */
  val minButtonSize = 48.dp

  /** Padding around the content within the widget. */
  val widgetPadding = 12.dp

  /** Spacing between buttons in all layouts. */
  val itemsSpacing = 8.dp

  /** Size of icons in all buttons */
  val iconSize = 24.dp

  /** Height of side bar in the [SideBarTwoRowGrid] layout. */
  val sideBarLeadingItemWidth = 52.dp

  /** Height of header in the [HeaderTwoRowGrid] layout. */
  val headerItemHeight = 52.dp
}

/**
 * Previews for various breakpoints for this search toolbar layout.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 56, heightDp = 56) // only search icon
@Preview(widthDp = 128, heightDp = 48) // reveals one additional button - no background
@Preview(widthDp = 128, heightDp = 72) // w/ background colors
@Preview(widthDp = 296, heightDp = 48) // more buttons - no background
@Preview(widthDp = 296, heightDp = 72) // more buttons - w/ background colors
@Preview(widthDp = 72, heightDp = 228) // vertical
@Preview(widthDp = 128, heightDp = 128) // 2x2 grid
@Preview(widthDp = 296, heightDp = 128) // search sidebar + 2x2 grid
@Preview(widthDp = 128, heightDp = 228) // search on top (no text) + 2x2 grid
@Preview(widthDp = 240, heightDp = 228) // search on top w/ text + 2x2 grid
@Composable
private fun SearchToolbarPreview() {
    SearchToolBarLayout(
        searchButton = SearchToolBarButton(
            iconRes = R.drawable.sample_search_icon,
            contentDescription = "Search notes",
            text = "Search",
            onClick = actionStartDemoActivity("search notes button")
        ),
        trailingButtons = listOf(
            SearchToolBarButton(
                iconRes = R.drawable.sample_mic_icon,
                contentDescription = "audio",
                onClick = actionStartDemoActivity("audio button")
            ),
            SearchToolBarButton(
                iconRes = R.drawable.sample_videocam_icon,
                contentDescription = "video note",
                onClick = actionStartDemoActivity("video note button")
            ),
            SearchToolBarButton(
                iconRes = R.drawable.sample_camera_icon,
                contentDescription = "camera",
                onClick = actionStartDemoActivity("camera button")
            ),
            SearchToolBarButton(
                iconRes = R.drawable.sample_share_icon,
                contentDescription = "share",
                onClick = actionStartDemoActivity("share button")
            ),
        )
    )
}
