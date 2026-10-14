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
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.wrapContentHeight
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.contentPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.gridCells
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.imageCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.itemCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.textStartMargin
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutDimensions.titleTextBreakpoint
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutSize.Large
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutSize.Medium
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridLayoutSize.Small
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils
import com.example.platform.ui.appwidgets.glance.layout.utils.LargeWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting a grid of images (with optional title and supporting text). The
 * list is displayed in a [Scaffold] below an app-specific title bar.
 *
 * In this sample layout, image is the primary focus of the widget and title & supporting text act
 * as secondary information.
 *
 * The layout scales by adjusting the number of grid cells, and the font size of the texts (if
 * present).
 *
 * [com.example.platform.ui.appwidgets.glance.layout.collections.ImageGridAppWidget] loads the images as bitmaps, and
 * scales them to fit into widget's limits. Number of items are capped. When using bitmaps, this
 * approach is suitable for use cases that recommend / feature limited items.
 *
 * The layout serves as an implementation suggestion, but should be customized to fit your
 * product's needs.
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
 * @param items list of items to be displayed in the grid; typically, image with optional title
 *              and supporting text.
 *
 * @see [ImageGridItemData] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.collections.ImageGridAppWidget]
 */
@Composable
fun ImageGridLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int,
  titleBarActionIconContentDescription: String,
  titleBarAction: () -> Unit,
  items: List<ImageGridItemData>,
) {

  @Composable
  fun TitleBar() {
    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      title = title.takeIf { LocalSize.current.width >= titleTextBreakpoint } ?: "",
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

  Scaffold(
    titleBar = { TitleBar() },
    backgroundColor = GlanceTheme.colors.widgetBackground,
    horizontalPadding = contentPadding,
    modifier = GlanceModifier.padding(bottom = contentPadding)
  ) {
    if (items.isEmpty()) {
      EmptyListContent()
    } else {
      Grid(items)
    }
  }
}

@Composable
private fun Grid(items: List<ImageGridItemData>) {
  RoundedScrollingLazyVerticalGrid(
    modifier = GlanceModifier.fillMaxSize(),
    gridCells = gridCells,
    items = items,
    itemContentProvider = { item ->
      GridItem(
        item = item,
        modifier = GlanceModifier.fillMaxSize()
      )
    })
}

@Composable
private fun GridItem(
  item: ImageGridItemData,
  modifier: GlanceModifier,
) {
  @Composable
  fun Image() {
    val imageProvider = if (item.image != null) {
      ImageProvider(item.image)
    } else {
      ImageProvider(R.drawable.sample_placeholder_image)
    }

    Image(
      provider = imageProvider,
      contentDescription = item.imageContentDescription,
      contentScale = ContentScale.Fit,
      modifier = GlanceModifier
        .cornerRadius(imageCornerRadius)
        .fillMaxWidth()
        .wrapContentHeight()
    )
  }

  @Composable
  fun Title(text: String) {
    Text(
      text = text,
      maxLines = 1,
      style = ImageGridLayoutTextStyles.titleText,
      modifier = GlanceModifier.padding(start = textStartMargin)
    )
  }

  @Composable
  fun SupportingText(text: String) {
    Text(
      text = text,
      maxLines = 1,
      style = ImageGridLayoutTextStyles.supportingText,
      modifier = GlanceModifier.padding(start = textStartMargin)
    )
  }

  if (item.title != null) {
    VerticalListItem(
      modifier = modifier
        .cornerRadius(itemCornerRadius)
        .clickable(ActionUtils.actionStartDemoActivity("Item click ${item.title}")),
      topContent = { Image() },
      titleContent = { Title(text = item.title) },
      supportingContent = takeComposableIf(item.supportingText != null) {
        SupportingText(text = checkNotNull(item.supportingText))
      }
    )
  } else {
    Box(modifier) {
      Image()
    }
  }
}

/** Returns the provided [block] composable if [predicate] is true, else returns null */
@Composable
private inline fun takeComposableIf(
  predicate: Boolean,
  crossinline block: @Composable () -> Unit,
): (@Composable () -> Unit)? {
  return if (predicate) {
    { block() }
  } else null
}

data class ImageGridItemData(
  val key: String,
  val image: Bitmap?,
  val imageContentDescription: String?,
  val title: String? = null,
  val supportingText: String? = null,
)

/**
 * Size of the widget per the reference breakpoints. Each size has its own display
 * characteristics such as - number of grid cells, font sizes, etc.
 *
 * In this layout, only width breakpoints are used to scale the layout.
 */
private enum class ImageGridLayoutSize(val maxWidth: Dp) {
  // Smaller fonts, single column
  Small(maxWidth = 320.dp),

  // Larger fonts, 2 column
  Medium(maxWidth = 519.dp),

  // 3 column
  Large(maxWidth = Dp.Infinity);

  companion object {
    /**
     * Returns the corresponding [ImageGridLayoutSize] to be considered for the current widget
     * size.
     */
    @Composable
    fun fromLocalSize(): ImageGridLayoutSize {
      val size = LocalSize.current

      ImageGridLayoutSize.values().forEach {
        if (size.width < it.maxWidth) {
          return it
        }
      }
      throw IllegalStateException("No mapped size ")
    }
  }
}

private object ImageGridLayoutTextStyles {
  /**
   * Style for the text displayed as title within each item.
   */
  val titleText: TextStyle
    @Composable get() = TextStyle(
      fontWeight = FontWeight.Medium,
      fontSize = if (ImageGridLayoutSize.fromLocalSize() == Small) {
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

private object ImageGridLayoutDimensions {
  val contentPadding = 12.dp

  val titleTextBreakpoint = 200.dp
  /**
   * Amount of space before the text in each item
   */
  val textStartMargin = 4.dp

  /** Corner radius for image in each item. */
  val imageCornerRadius = 16.dp

  /** Corner radius applied to each item **/
  val itemCornerRadius = 16.dp

  /**
   * Number of columns the grid layout should use to display items in available space.
   */
  val gridCells: Int
    @Composable get() {
      return when (ImageGridLayoutSize.fromLocalSize()) {
        Medium -> 2
        Large -> 3
        else -> 1
      }
    }
}

/**
 * Preview sizes for the widget covering the width based breakpoints of the image grid layout.
 *
 * This allows verifying updates across multiple breakpoints.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 319, heightDp = 200)
@Preview(widthDp = 321, heightDp = 200)
@Preview(widthDp = 518, heightDp = 200)
@Preview(widthDp = 520, heightDp = 200)
private annotation class ImageGridBreakpointPreviews

/**
 * Previews for the image grid layout with both title and supporting text below the image
 *
 * First we look at the previews at defined breakpoints, tweaking them as necessary. In addition,
 * the previews at standard sizes allows us to quickly verify updates across min / max and common
 * widget sizes without needing to run the app or manually place the widget.
 */
@ImageGridBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun ImageTextGridLayoutPreview() {
  val context = LocalContext.current

  ImageGridLayout(
    title = context.getString(R.string.sample_image_grid_app_widget_name),
    titleIconRes = R.drawable.sample_grid_icon,
    titleBarActionIconRes = R.drawable.sample_refresh_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_refresh_icon_button_label
    ),
    titleBarAction = {},
    items = listOf(
      ImageGridItemData(
        key = "0",
        image = null, // placeholder
        imageContentDescription = "A single water droplet rests in a budding red pansy.",
        title = "A single water droplet rests in a budding red pansy.",
        supportingText = "193 views"
      ),
      ImageGridItemData(
        key = "1",
        image = null, // placeholder
        imageContentDescription = "A single water droplet rests in a budding red pansy.",
        title = "A single water droplet rests in a budding red pansy.",
        supportingText = "193 views"
      ),
      ImageGridItemData(
        key = "2",
        image = null, // placeholder
        imageContentDescription = "Green plant, sky and flowers",
        title = "Green plant, sky and flowers",
        supportingText = "99,467 views"
      ),
      ImageGridItemData(
        key = "3",
        image = null, // placeholder
        imageContentDescription = "A snow-shoer walking up Strelapass",
        title = "A snow-shoer walking up Strelapass",
        supportingText = "3,033 views"
      )
    )
  )
}

/**
 * Previews for the image grid layout with only images
 *
 * First we look at the previews at defined breakpoints, tweaking them as necessary. In addition,
 * the previews at standard sizes allows us to quickly verify updates across min / max and common
 * widget sizes without needing to run the app or manually place the widget.
 */
@ImageGridBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun ImageOnlyGridLayoutPreview() {
  val context = LocalContext.current

  ImageGridLayout(
    title = context.getString(R.string.sample_image_grid_app_widget_name),
    titleIconRes = R.drawable.sample_grid_icon,
    titleBarActionIconRes = R.drawable.sample_refresh_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_refresh_icon_button_label
    ),
    titleBarAction = {},
    items = (0..5).map { index ->
      ImageGridItemData(
        key = "$index",
        image = null, // placeholder
        imageContentDescription = null,
      )
    }
  )
}
