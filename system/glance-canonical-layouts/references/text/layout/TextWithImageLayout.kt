package com.example.platform.ui.appwidgets.glance.layout.text.layout

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.NoDataContent
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutDimensions.contentSize
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutDimensions.contentSpacing
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutDimensions.pictureRadius
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutDimensions.verticalTextsSpacing
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutDimensions.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutSize.HorizontalLarge
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutSize.HorizontalSmall
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutSize.VerticalLarge
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutSize.VerticalSmall
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutTextStyles.primaryTextFontValues
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayoutTextStyles.secondaryTextFontValues
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.FontUtils
import com.example.platform.ui.appwidgets.glance.layout.utils.LargeWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting a long text (~65 characters), its one line caption, a supporting
 * image and a secondary text in a [Scaffold] with an app-specific title bar.
 *
 * In compact spaces, the image, title bar, secondary text may not be shown.
 *
 * The layout serves as an implementation suggestion, but should be customized to fit your product's
 * needs. For instance, a ratings element may be additionally displayed.
 *
 * @param title the text to be displayed as title of the widget, e.g. name of your widget or app.
 * @param titleIconRes a tintable icon that represents your app or brand, that can be displayed
 * with the provided [title]. In this sample, we use icon from a drawable resource, but you should
 * use an appropriate icon source for your use case.
 * @param titleBarActionIconRes optional resource id of a tintable icon that can be displayed as
 * an icon button within the title bar area of the widget. For example, a search icon to find other
 * articles in an "article of the day" widget.
 * @param titleBarActionIconContentDescription description of the [titleBarActionIconRes] button
 * to be used by the accessibility services.
 * @param titleBarAction action to be performed on click of the [titleBarActionIconRes] button.
 * @param data the primary text, caption, secondary text and image to be displayed.
 *
 * @see [TextWithImageData] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.text.TextWithImageAppWidget]
 *
 */
@Composable
fun TextWithImageLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int? = null,
  titleBarActionIconContentDescription: String? = null,
  titleBarAction: (() -> Unit)? = null,
  data: TextWithImageData? = null,
) {
  fun titleBar(): @Composable (() -> Unit) = {
    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      title = title.takeIf { LocalSize.current.width >= 230.dp } ?: "",
      iconColor = GlanceTheme.colors.primary,
      textColor = GlanceTheme.colors.onSurface,
      actions = {
        if (titleBarAction != null && titleBarActionIconRes != null) {
          CircleIconButton(
            imageProvider = ImageProvider(titleBarActionIconRes),
            contentDescription = titleBarActionIconContentDescription,
            contentColor = GlanceTheme.colors.secondary,
            backgroundColor = null, // transparent
            onClick = titleBarAction
          )
        }
      }
    )
  }

  val action = actionStartDemoActivity("TextWithImage: ${data?.textData?.key}")
  val layoutSize = TextWithImageLayoutSize.fromLocalSize()

  val titleBar = if (layoutSize.showTitleBar()) {
    titleBar()
  } else {
    null
  }
  val scaffoldTopPadding = if (layoutSize.showTitleBar()) {
    0.dp
  } else {
    widgetPadding
  }

  Scaffold(
    titleBar = titleBar,
    horizontalPadding = widgetPadding,
    backgroundColor = GlanceTheme.colors.widgetBackground,
    modifier = GlanceModifier
      .maybeClickable(action)
      .padding(
        bottom = widgetPadding,
        top = scaffoldTopPadding
      ),
  ) {
    when (data) {
      null -> NoDataContent()
      else -> {
        when (layoutSize) {
          VerticalSmall -> VerticalContent(
            data = data,
            showImage = true,
            showSecondaryText = false
          )

          VerticalLarge -> VerticalContent(
            data = data,
            showImage = true,
            showSecondaryText = true
          )

          HorizontalSmall -> HorizontalContent(
            data = data,
            showImage = false,
            showSecondaryText = false
          )

          HorizontalLarge -> HorizontalContent(
            data = data,
            showImage = true,
            showSecondaryText = true
          )
        }
      }
    }
  }
}

@Composable
private fun NoDataContent() {
  val context = LocalContext.current

  NoDataContent(
    noDataText = context.getString(R.string.sample_no_data_text),
    noDataIconRes = R.drawable.sample_no_data_icon,
    actionButtonText = context.getString(
      R.string.sample_learn_more_button_text
    ),
    actionButtonIcon = R.drawable.sample_info_icon,
    actionButtonOnClick = actionStartDemoActivity("on-click of info button in no data view")
  )
}

@Composable
private fun HorizontalContent(
  data: TextWithImageData,
  showImage: Boolean,
  showSecondaryText: Boolean,
) {
  val contentWidth = contentSize.width - contentSpacing
  val contentHeight = contentSize.height - (2 * verticalTextsSpacing)
  Row(
    verticalAlignment = Alignment.Vertical.Bottom,
    horizontalAlignment = Alignment.Horizontal.Start,
    modifier = GlanceModifier.fillMaxSize()
  ) {
    TextStack(
      data = data.textData,
      showSecondaryText = showSecondaryText,
      modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
      availableSize = DpSize(
        // Use 40% of available width for text area if showing image on side.
        width = (0.4 * contentWidth).takeIf { showImage } ?: contentWidth,
        // Use 80% of vertical space for text area.
        height = (0.80 * contentHeight).takeIf { showImage } ?: contentHeight
      )
    )
    if (showImage) {
      Spacer(modifier = GlanceModifier.width(contentSpacing))
      Image(
        data = data.imageData,
        modifier = GlanceModifier.fillMaxHeight().width(contentWidth / 2)
      )
    }
  }
}

@Composable
private fun VerticalContent(
  data: TextWithImageData,
  showImage: Boolean,
  showSecondaryText: Boolean,
) {
  Column(
    verticalAlignment = Alignment.Vertical.Bottom,
    modifier = GlanceModifier.fillMaxHeight()
  ) {
    val contentWidth = contentSize.width
    val contentHeight = contentSize.height - (2 * verticalTextsSpacing)

    if (showImage) {
      Image(
        data = data.imageData,
        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
      )
      Spacer(modifier = GlanceModifier.height(contentSpacing))
    }
    TextStack(
      data = data.textData,
      showSecondaryText = showSecondaryText,
      modifier = GlanceModifier.fillMaxWidth(),
      availableSize = DpSize(
        // Use 80% of available width to leave some offset for word break differences.
        width = 0.8 * contentWidth,
        // Use 40% vertical space for text area if showing image
        height = (0.4 * contentHeight).takeIf { showImage } ?: contentHeight
      )
    )
  }
}

@Composable
private fun TextStack(
  data: TextData,
  modifier: GlanceModifier,
  availableSize: DpSize,
  showSecondaryText: Boolean,
) {
  val (primaryTextFontSize, primaryTextMaxLines) = primaryTextFontValues(
    text = data.primary,
    availableSize = availableSize,
    showSecondaryText = showSecondaryText
  )

  Column(
    verticalAlignment = Alignment.Bottom,
    modifier = modifier
  ) {
    Text(
      text = data.caption,
      maxLines = 1,
      style = TextWithImageLayoutTextStyles.caption,
      modifier = GlanceModifier.fillMaxWidth()
    )
    Spacer(modifier = GlanceModifier.height(verticalTextsSpacing))
    Text(
      text = data.primary,
      maxLines = primaryTextMaxLines,
      style = TextWithImageLayoutTextStyles.primary.copy(fontSize = primaryTextFontSize),
    )
    if (showSecondaryText) {
      val (secondaryTextFontSize, secondaryTextMaxLines) = secondaryTextFontValues(
        text = data.secondary,
        availableSize = availableSize
      )

      Spacer(modifier = GlanceModifier.height(verticalTextsSpacing))
      Text(
        text = data.secondary,
        maxLines = secondaryTextMaxLines,
        style = TextWithImageLayoutTextStyles.secondary.copy(fontSize = secondaryTextFontSize),
      )
    }
  }
}

@Composable
private fun Image(
  data: ImageData,
  modifier: GlanceModifier,
) {
  val imageProvider = if (data.bitmap != null) {
    ImageProvider(data.bitmap)
  } else {
    ImageProvider(R.drawable.sample_placeholder_image)
  }

  Image(
    provider = imageProvider,
    contentDescription = data.contentDescription,
    contentScale = ContentScale.Crop,
    modifier = modifier.cornerRadius(pictureRadius),
  )
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
  return if (action != null) {
    this.clickable(action)
  } else {
    this
  }
}

/**
 * Data object holding information displayed in
 * [com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayout].
 */
data class TextWithImageData(
  val textData: TextData,
  val imageData: ImageData,
)

/**
 * Data object holding texts displayed in
 * [com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayout].
 *
 * @param key a unique identifier for the data being displayed as primary content e.g. article ID,
 * in an "article of the day" widget. This may not be necessary for your use case; in this sample,
 * we use this key to differentiate between placeholder and real data when demonstrating clicks.
 * @param primary text representing primary information being conveyed to the user via the
 * widget
 * @param secondary text supporting the primary text; for example, in an "article of the day"
 * widget, this could be a brief description of the article.
 * @param caption shorter text (couple of words) that can fit in one line; e.g. author's name in an
 * "article of the day" widget.
 */
data class TextData(
  val key: String,
  val primary: String,
  val secondary: String,
  val caption: String,
)

/**
 * Data object holding raw information about the image accompanying the [TextData] in
 * [com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayout].
 *
 * @param resId an image accompanying the [TextData] content; for example, a preview image of
 * an article in an "article of the day" widget.
 * @param contentDescription an optional description about the [resId] that can be
 * used by the accessibility services
 */
data class ImageData(
  val bitmap: Bitmap? = null,
  val contentDescription: String? = null,
)

private enum class TextWithImageLayoutSize {
  // No title bar or secondary text
  HorizontalSmall,

  // No title bar or secondary text
  VerticalSmall,

  // Text with Image on the side
  HorizontalLarge,

  // Image with text below it
  VerticalLarge;

  companion object {
    @Composable
    fun fromLocalSize(): TextWithImageLayoutSize {
      val size = LocalSize.current
      val isTall = size.height >= size.width

      return if (isTall && size.height <= 300.dp) {
        VerticalSmall
      } else if (isTall) {
        VerticalLarge
      } else if (size.width <= 165.dp) {
        HorizontalSmall
      } else {
        HorizontalLarge
      }
    }
  }

    @Composable
    fun showTitleBar() = LocalSize.current.height >= 180.dp
}


private object TextWithImageLayoutTextStyles {
  val primary: TextStyle
    @Composable get() = TextStyle(
      fontWeight = FontWeight.Medium,
      color = GlanceTheme.colors.onSurface
    )

  val secondary: TextStyle
    @Composable get() = TextStyle(
      color = GlanceTheme.colors.onSurfaceVariant
    )

  val caption: TextStyle
    @Composable get() =
      TextStyle(color = GlanceTheme.colors.secondary)

  @Composable
  fun primaryTextFontValues(
    text: String,
    availableSize: DpSize,
    showSecondaryText: Boolean,
  ): Pair<TextUnit, Int> {
    val availableHeight = if (showSecondaryText) {
      // Within the text area, 30% space is used by primary text.
      0.30 * availableSize.height
    } else {
      0.60 * availableSize.height
    }

    return FontUtils.calculateFontSizeAndMaxLines(
      context = LocalContext.current,
      text = text,
      availableWidth = availableSize.width,
      availableHeight = availableHeight,
      maxFontSize = 24.sp,
      minFontSize = 16.sp
    )
  }

  @Composable
  fun secondaryTextFontValues(text: String, availableSize: DpSize): Pair<TextUnit, Int> {
    return FontUtils.calculateFontSizeAndMaxLines(
      context = LocalContext.current,
      text = text,
      availableWidth = availableSize.width,
      // Within the text area, 25% space is used by secondary text.
      availableHeight = 0.25 * availableSize.height,
      maxFontSize = 24.sp,
      minFontSize = 12.sp
    )
  }
}

private object TextWithImageLayoutDimensions {
  /** Padding that visually appears between the widget outline and anything inside. */
  val widgetPadding = 16.dp

  /** Corner radius to be applied to an image. */
  val pictureRadius = 16.dp

    /**
     * Space between larger building blocks within the layout. e.g space between text stack and
     * the image.
     */
    val contentSpacing = 12.dp

    /**
     * Vertical spacing between text items
     */
    val verticalTextsSpacing = 4.dp

  /** Height of the title bar. */
  private val titleBarHeight: Dp
    @Composable get() = if (TextWithImageLayoutSize.fromLocalSize().showTitleBar()) {
      0.dp
    } else {
      56.dp
    }

  /** Height and width in dp available to main content (excluding title bar, padding, spacing). */
  val contentSize: DpSize
    @Composable get() {
      val size = LocalSize.current

      return DpSize(
        width = size.width - (2 * widgetPadding),
        height = size.height - titleBarHeight - widgetPadding
      )
    }
}

@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun TextWithImagePreview() {
  val context = LocalContext.current

  TextWithImageLayout(
    title = context.getString(R.string.sample_text_and_image_app_widget_name),
    titleIconRes = R.drawable.sample_text_icon,
    titleBarActionIconRes = R.drawable.sample_refresh_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_refresh_icon_button_label
    ),
    titleBarAction = {},
    data = TextWithImageData(
      textData = TextData(
        key = "1",
        primary = "This allows for a longer text string.",
        secondary = "Specifically because the focus in this, layout is on the primary text.",
        caption = "Ut mollis amet cursus",
      ),
      imageData = ImageData()
    )
  )
}
