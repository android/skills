package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height

/**
 * A variant of [LazyColumn] that clips its scrolling content to a rounded rectangle.
 *
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param content a block which describes the content. Inside this block you can use methods like
 * [LazyListScope.item] to add a single item or [LazyListScope.items] to add a list of items. If the
 * item has more than one top-level child, they will be automatically wrapped in a Box.
 * @see LazyColumn
 */
@Composable
fun RoundedScrollingLazyColumn(
  modifier: GlanceModifier = GlanceModifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  content: LazyListScope.() -> Unit,
) {
  Box(
    modifier = modifier
      .fillMaxSize()
      .cornerRadius(16.dp) // to present a rounded scrolling experience
  ) {
    LazyColumn(
      horizontalAlignment = horizontalAlignment,
      content = content
    )
  }
}

/**
 * * A variant of [LazyColumn] that clips its scrolling content to a rounded rectangle and spaces
 * out each item in the list with a default 8.dp spacing
 *
 * @param items the list of data items to be displayed in the list
 * @param itemContentProvider a lambda function that provides item content without any spacing
 * @param modifier the modifier to apply to this layout
 * @param horizontalAlignment the horizontal alignment applied to the items.
 * @param verticalItemsSpacing vertical spacing between items
 * @see LazyColumn
 */
@Composable
fun <T> RoundedScrollingLazyColumn(
  items: List<T>,
  itemContentProvider: @Composable (item: T) -> Unit,
  modifier: GlanceModifier = GlanceModifier,
  horizontalAlignment: Alignment.Horizontal = Alignment.Start,
  verticalItemsSpacing: Dp = 4.dp
) {
  val lastIndex = items.size - 1

  RoundedScrollingLazyColumn(modifier, horizontalAlignment) {
    itemsIndexed(items) { index, item ->
      Column(modifier = GlanceModifier.fillMaxWidth()) {
        itemContentProvider(item)
        if (index != lastIndex) {
          Spacer(modifier = GlanceModifier.height(verticalItemsSpacing))
        }
      }
    }
  }
}
