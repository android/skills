package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics

/**
 * Component to build an item with content arranged vertically. Lists are a continuous, vertical
 * indexes of text or images.
 *
 * Apply padding to the item using the [androidx.glance.layout.padding] modifier.
 *
 * @param titleContent the [Composable] title content of the list item; typically a 1-line
 *                        prominent text within the list item; displayed below the top content
 *                        (if present).
 * @param modifier [GlanceModifier] to be applied to the list item
 * @param topContent the content of the list item such as an image displayed on the top; even if
 *                   title content may be the primary information, displaying the content such as
 *                   images in the top allows to easy visual differentiation among the list items.
 * @param supportingContent 1-2 line text to be displayed below the headline text of the list item
 * @param trailingBottomContent the trailing meta text, icon, or a selection control such as switch,
 *                              checkbox, or a radio button displayed along side the bottom section
 *                              with the headline and the supporting content.
 * @param onClick an option action to be performed on click of the list item.
 * @param itemContentDescription an optional text used by accessibility services to describe what
 *                               this list item represents. If not provided, the non-clickable
 *                               content within the list item will be read out.
 */
@Composable
fun VerticalListItem(
  titleContent: @Composable (() -> Unit),
  modifier: GlanceModifier = GlanceModifier,
  topContent: @Composable (() -> Unit)? = null,
  supportingContent: @Composable (() -> Unit)? = null,
  trailingBottomContent: @Composable (() -> Unit)? = null,
  onClick: Action? = null,
  itemContentDescription: String? = null,
) {
  val listItemModifier = if (itemContentDescription != null) {
    modifier.semantics { contentDescription = itemContentDescription }
  } else {
    modifier
  }

  Column(
    modifier = listItemModifier.maybeClickable(onClick),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Top
    topContent?.let {
      it()
      // Smaller spacing to make the vertical spacing between individual list items more prominent.
      Spacer(modifier = GlanceModifier.height(4.dp))
    }
    // Bottom
    Row {
      Column(
        modifier = GlanceModifier.defaultWeight(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        titleContent()
        supportingContent?.let { it() }
      }
      // Trailing bottom
      trailingBottomContent?.let {
        Spacer(modifier = GlanceModifier.width(8.dp))
        it()
      }
    }
  }
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
  return if (action != null) {
    this.clickable(action)
  } else {
    this
  }
}
