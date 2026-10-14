package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.NUM_GRID_CELLS
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.fillItemItemPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.filledItemCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.imageCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.verticalSpacing
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.Dimensions.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayoutSize.Companion.shouldDisplayTrailingIconButton
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayoutSize.Companion.showTitleBar
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayoutSize.Large
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayoutSize.Medium
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayoutSize.Small
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.LargeWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting a list of text with an image, and an optional icon button. The
 * list is displayed in a [Scaffold] below an app-specific title bar.
 *
 * The layout drops not-so-important details as the size of the widget goes smaller. For instance,
 * in the smallest size it drops the image and the icon button to allow displaying more items at
 * at glance. In an medium size, displays image, text and optionally icon button in a single
 * column list. In the large size, shows items in 2 column grid.
 *
 * In this sample layout, text is the primary focus of the widget and image acts as a supporting
 * content. So, we prefer displaying horizontal [ListItem]s in most displays. However, for a
 * image-centric widget, you may prefer displaying a grid of
 * [com.example.platform.ui.appwidgets.glance.layout.components.list.VerticalListItem]s featuring the image across all
 * sizes.
 *
 * The layout serves as an implementation suggestion, but should be customized to fit your
 * product's needs. As you customize the layout, prefer supporting narrower as well as larger
 * widget sizes.
 *
 * Note: When using images as bitmap, you should limit the number of items displayed in widgets.
 *
 * @param title the text to be displayed as title of the widget, e.g. name of your widget or app.
 * @param titleIconRes a tintable icon that represents your app or brand, that can be displayed
 * with the provided [title]. In this sample, we use icon from a drawable resource, but you should
 * use an appropriate icon source for your use case.
 * @param titleBarActionIconRes resource id of a tintable icon that can be displayed as
 * an icon button within the title bar area of the widget. For example, a search icon.
 * @param titleBarActionIconContentDescription description of the [titleBarActionIconRes] button
 * to be used by the accessibility services.
 * @param titleBarAction action to be performed on click of the [titleBarActionIconRes] button.
 * @param items list of items to be displayed in the list; typically includes a short title for
 *              item, a supporting text and an image.
 *
 * @see [ImageTextListItemData] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.collections.ImageTextListAppWidget]
 */
@Composable
fun ImageTextListLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int,
  titleBarActionIconContentDescription: String,
  titleBarAction: (() -> Unit),
  items: List<ImageTextListItemData>,
) {
  val imageTextListLayoutSize = ImageTextListLayoutSize.fromLocalSize()

  fun titleBar(): @Composable (() -> Unit) = {
    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      title = title.takeIf { imageTextListLayoutSize != Small } ?: "",
      iconColor = GlanceTheme.colors.primary,
      textColor = GlanceTheme.colors.onSurface,
      actions = {
        CircleIconButton(
          imageProvider = ImageProvider(titleBarActionIconRes),
          contentDescription = titleBarActionIconContentDescription,
          contentColor = GlanceTheme.colors.secondary,
          backgroundColor = null, // transparent
          onClick = titleBarAction
        )
      }
    )
  }

  val scaffoldTopPadding = if (showTitleBar()) {
    0.dp
  } else {
    widgetPadding
  }

  Scaffold(
    backgroundColor = GlanceTheme.colors.widgetBackground,
    modifier = GlanceModifier.padding(
      top = scaffoldTopPadding,
      bottom = widgetPadding
    ),
    titleBar = if (showTitleBar()) {
      titleBar()
    } else {
      null
    }
  ) {
    Content(items)
  }
}

@Composable
private fun Content(items: List<ImageTextListItemData>) {
  val displayTrailingIconIfPresent = shouldDisplayTrailingIconButton()

  if (items.isEmpty()) {
    EmptyListContent()
  } else {
    when (ImageTextListLayoutSize.fromLocalSize()) {
      Small -> {
        ListView(
          items = items,
          displayImage = false,
          displayTrailingIconIfPresent = displayTrailingIconIfPresent,
        )
      }

      Medium -> {
        ListView(
          items = items,
          displayImage = true,
          displayTrailingIconIfPresent = displayTrailingIconIfPresent,
        )
      }

      Large -> {
        GridView(
          items = items,
          displayImage = true,
          displayTrailingIconIfPresent = displayTrailingIconIfPresent
        )
      }
    }
  }
}

/**
 * A vertical scrolling list displaying [FilledHorizontalListItem]s. Suitable for
 * [ImageTextListLayoutSize.Small] and [ImageTextListLayoutSize.Large] sizes.
 */
@Composable
private fun ListView(
  items: List<ImageTextListItemData>,
  displayImage: Boolean,
  displayTrailingIconIfPresent: Boolean,
) {
  RoundedScrollingLazyColumn(
    modifier = GlanceModifier.fillMaxSize(),
    items = items,
    verticalItemsSpacing = verticalSpacing,
    itemContentProvider = { item ->
      FilledHorizontalListItem(
        item = item,
        displayImage = displayImage,
        displayTrailingIcon = displayTrailingIconIfPresent,
        onClick = actionStartDemoActivity("Item ${item.key} click"),
        modifier = GlanceModifier.fillMaxSize(),
      )
    },
  )
}

/**
 * A grid of [FilledHorizontalListItem]s suitable for [ImageTextListLayoutSize.Large] sizes.
 *
 * Supporting the grid display allows large screen users view more information at once.
 */
@Composable
private fun GridView(
  items: List<ImageTextListItemData>,
  displayImage: Boolean,
  displayTrailingIconIfPresent: Boolean,
) {
  RoundedScrollingLazyVerticalGrid(
    gridCells = NUM_GRID_CELLS,
    items = items,
    cellSpacing = verticalSpacing,
    itemContentProvider = { item ->
      FilledHorizontalListItem(
        item = item,
        displayImage = displayImage,
        displayTrailingIcon = displayTrailingIconIfPresent,
        onClick = actionStartDemoActivity("Item ${item.key} click"),
        modifier = GlanceModifier.fillMaxSize()
      )
    },
    modifier = GlanceModifier.fillMaxSize()
  )
}

/**
 * Arranges the texts, the image and the icon button in a horizontal arrangement with a filled
 * container.
 */
@Composable
private fun FilledHorizontalListItem(
  item: ImageTextListItemData,
  displayImage: Boolean,
  displayTrailingIcon: Boolean,
  onClick: Action,
  modifier: GlanceModifier = GlanceModifier,
) {
  @Composable
  fun TitleText() {
    Text(
      text = item.title,
      maxLines = 2,
      style = TextStyles.titleText,
    )
  }

  @Composable
  fun SupportingText() {
    Text(
      text = item.supportingText,
      maxLines = 2,
      style = TextStyles.supportingText
    )
  }

  @Composable
  fun SupportingImage() {
    // Bitmaps are ensured to be within remote views limit in the repository class.
    val imageProvider = if (item.supportingImageBitmap != null) {
      ImageProvider(item.supportingImageBitmap)
    } else {
      ImageProvider(
        R.drawable.sample_placeholder_image
      )
    }

    Image(
      provider = imageProvider,
      // contentDescription is null because in this sample, it serves merely as a visual; but if
      // it gives additional info to user, you should set the appropriate content description.
      contentDescription = null,
      // Depending on your image content, you may want to select an appropriate ContentScale.
      contentScale = ContentScale.Crop,
      // Fixed size per UX spec
      modifier = modifier.cornerRadius(imageCornerRadius).size(Dimensions.imageSize)
    )
  }

  @Composable
  fun IconButton() {
    if (item.trailingIconButton != null) {
      // Using CircleIconButton allows us to keep the touch target 48x48
      CircleIconButton(
        imageProvider = ImageProvider(item.trailingIconButton),
        backgroundColor = null, // to show transparent background.
        contentDescription = item.trailingIconButtonContentDescription,
        onClick = actionStartDemoActivity("Item ${item.key} icon button click")
      )
    }
  }

  ListItem(
    modifier = modifier
      .padding(fillItemItemPadding)
      .cornerRadius(filledItemCornerRadius)
      .background(GlanceTheme.colors.secondaryContainer),
    headlineContent = { TitleText() },
    supportingContent = { SupportingText() },
    onClick = onClick,
    leadingContent = if (displayImage) {
      { SupportingImage() }
    } else {
      null
    },
    trailingContent = if (displayTrailingIcon) {
      { IconButton() }
    } else null,
  )
}

/**
 * Holds data fields for a [com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListLayout].
 *
 * @param key a unique identifier for a specific item
 * @param title a short text (1-3 words) representing the item
 * @param supportingText a compact text (~50-55 characters) supporting the [title]; this allows
 *                       keeping the title short and glanceable, as well as helps support smaller
 *                       widget sizes.
 * @param supportingImageBitmap an image to accompany the textual information displayed in [title]
 *                              and the [supportingText].
 * @param trailingIconButton a tintable icon representing an action that can be performed in context
 *                           of the item; e.g. bookmark icon, save icon, etc.
 * @param trailingIconButtonContentDescription description of the [trailingIconButton] to be used by
 *                                             the accessibility services.
 */
data class ImageTextListItemData(
  val key: String,
  val title: String,
  val supportingText: String,
  val supportingImageBitmap: Bitmap? = null,
  @DrawableRes val trailingIconButton: Int? = null,
  val trailingIconButtonContentDescription: String? = null,
)

/**
 * Reference breakpoints for deciding on widget style to display e.g. list / grid etc.
 *
 * In this layout, only width breakpoints are used to scale the layout.
 */
private enum class ImageTextListLayoutSize(val maxWidth: Dp) {
  // Single column vertical list without images or trailing button in this size.
  Small(maxWidth = 260.dp),

  // Single column horizontal list with images and optional trailing button if exists.
  Medium(maxWidth = 479.dp),

  // 2 Column Grid of horizontal list items. Images are always shown; trailing button is shown if
  // it fits.
  Large(maxWidth = 644.dp);

  companion object {
    /**
     * Returns the corresponding [ImageTextListLayoutSize] to be considered for the current
     * widget size.
     */
    @Composable
    fun fromLocalSize(): ImageTextListLayoutSize {
      val width = LocalSize.current.width

      return if (width >= Medium.maxWidth) {
        Large
      } else if (width >= Small.maxWidth) {
        Medium
      } else {
        Small
      }
    }

    @Composable
    fun showTitleBar(): Boolean {
      return LocalSize.current.height >= 180.dp
    }

    /**
     * Returns if icon button should be displayed across medium and large sizes based on
     * predefined breakpoints.
     */
    @Composable
    fun shouldDisplayTrailingIconButton(): Boolean {
      val widgetWidth = LocalSize.current.width
      return (widgetWidth in 340.dp..479.dp || widgetWidth > 620.dp)
    }
  }
}

private object TextStyles {
  /**
   * Style for the text displayed as title within each item.
   */
  val titleText: TextStyle
    @Composable get() = TextStyle(
      fontWeight = FontWeight.Medium,
      fontSize = if (ImageTextListLayoutSize.fromLocalSize() == Small) {
        14.sp // M3 Title Small
      } else {
        16.sp // M3 Title Medium
      },
      color = GlanceTheme.colors.onSurface
    )

  /**
   * Style for the text displayed as supporting text within each item.
   */
  val supportingText: TextStyle
    @Composable get() =
      TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp, // M3 Label Medium
        color = GlanceTheme.colors.secondary
      )
}

private object Dimensions {
  /** Number of cells in the grid, when items are displayed as a grid. */
  const val NUM_GRID_CELLS = 2

  /** Padding around the the widget content */
  val widgetPadding = 12.dp

  /** Corner radius for each filled list item. */
  val filledItemCornerRadius = 16.dp

  /** Padding applied to each item in the list. */
  val fillItemItemPadding = 12.dp

  /** Vertical Space between each item in the list. */
  val verticalSpacing = 4.dp

  /** Size in which images should be displayed in the list. */
  val imageSize: Dp = 68.dp

  /** Corner radius for image in each item. */
  val imageCornerRadius = 12.dp
}

/**
 * Preview sizes for the widget covering the width based breakpoints of the image grid layout.
 *
 * This allows verifying updates across multiple breakpoints.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 259, heightDp = 200)
@Preview(widthDp = 261, heightDp = 200)
@Preview(widthDp = 480, heightDp = 200)
@Preview(widthDp = 644, heightDp = 200)
private annotation class ImageTextListBreakpointPreviews

/**
 * Previews for the image grid layout with both title and supporting text below the image
 *
 * First we look at the previews at defined breakpoints, tweaking them as necessary. In addition,
 * the previews at standard sizes allows us to quickly verify updates across min / max and common
 * widget sizes without needing to run the app or manually place the widget.
 */
@ImageTextListBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun ImageTextListLayoutPreview() {
  val context = LocalContext.current

  ImageTextListLayout(
    items = listOf(
      ImageTextListItemData(
        key = "0",
        title = "Blossom, petal, flower",
        supportingText = "23,815 views",
        supportingImageBitmap = null // placeholder
      ),
      ImageTextListItemData(
        key = "1",
        title = "Orchids at New York Botanical Garden",
        supportingText = "205,481 views",
        supportingImageBitmap = null // placeholder
      ),
      ImageTextListItemData(
        key = "2",
        title = "Tabletop composition with flower",
        supportingText = "85,060 views",
        supportingImageBitmap = null // placeholder
      ),
      ImageTextListItemData(
        key = "3",
        title = "Wild bee on flower",
        supportingText = "6,692 views ",
        supportingImageBitmap = null // placeholder
      )
    ),
    title = context.getString(
      R.string.sample_text_and_image_list_app_widget_name
    ),
    titleIconRes = R.drawable.sample_image_icon,
    titleBarActionIconRes = R.drawable.sample_refresh_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_refresh_icon_button_label
    ),
    titleBarAction = {},
  )
}
