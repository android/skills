/*
 * Copyright 2023 The Android Open Source Project
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
package com.example.platform.ui.appwidgets.glance.layout.text.layout

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
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.text.layout.WidgetTextDimensions.captionFontSizeAndMaxLines
import com.example.platform.ui.appwidgets.glance.layout.text.layout.WidgetTextDimensions.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.text.layout.WidgetTextDimensions.primaryTextFontSizeAndMaxLines
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.FontUtils.calculateFontSizeAndMaxLines
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting text only content.
 *
 * A longer text with a short caption are displayed in a [Scaffold] under an app-specific title bar.
 *
 * This serves as an implementation suggestion, but should be customized to fit your product's
 * needs.
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
 * @param data the text and caption to be displayed in the widget.
 * @param action action to be performed on click of the main content.
 */
@Composable
fun LongTextLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int? = null,
  titleBarActionIconContentDescription: String? = null,
  titleBarAction: (() -> Unit)? = null,
  data: LongTextLayoutData,
  action: Action? = null,
) {
  val showTitleBar = LongTextLayoutSize.fromLocalSize() != LongTextLayoutSize.XSmall
  val scaffoldTopPadding = if (showTitleBar) {
    0.dp
  } else {
    widgetPadding
  }

  Scaffold(
    backgroundColor = GlanceTheme.colors.widgetBackground,
    horizontalPadding = widgetPadding,
    modifier = GlanceModifier
      .maybeClickable(action)
      .padding(
        bottom = widgetPadding,
        top = scaffoldTopPadding
      ),
    titleBar = {
      if (showTitleBar) {
        TitleBarContent(
          titleIconRes,
          title,
          titleBarAction,
          titleBarActionIconRes,
          titleBarActionIconContentDescription
        )
      }
    },
  ) {
    TextStack(
      data = data,
      verticalAlignment = if (showTitleBar) {
        Alignment.Bottom
      } else Alignment.CenterVertically
    )
  }
}

@Composable
private fun TitleBarContent(
  titleIconRes: Int,
  title: String,
  titleBarAction: (() -> Unit)?,
  titleBarActionIconRes: Int?,
  titleBarActionIconContentDescription: String?,
) {
  TitleBar(
    startIcon = ImageProvider(titleIconRes),
    title = title.takeIf { showTitle() } ?: "",
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

@Composable
private fun TextStack(
  data: LongTextLayoutData,
  verticalAlignment: Alignment.Vertical,
) {
  Column(
    modifier = GlanceModifier.fillMaxSize(),
    verticalAlignment = verticalAlignment
  ) {
    val primaryTextFontSizeAndMaxLines = primaryTextFontSizeAndMaxLines(data.text)
    val captionFontSizeAndMaxLines =
      captionFontSizeAndMaxLines(primaryTextFontSizeAndMaxLines.first)
    // Caption
    Text(
      text = data.caption,
      style = TextStyle(
        fontSize = captionFontSizeAndMaxLines.first,
        color = GlanceTheme.colors.secondary
      ),
      maxLines = captionFontSizeAndMaxLines.second
    )
    // Primary text
    Text(
      text = data.text,
      style = TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontSize = primaryTextFontSizeAndMaxLines.first
      ),
      maxLines = primaryTextFontSizeAndMaxLines.second
    )
  }
}

private enum class LongTextLayoutSize {
  XSmall,
  Normal;

  companion object {
    @Composable
    fun fromLocalSize(): LongTextLayoutSize {
      return if (LocalSize.current.height <= 180.dp) {
        XSmall
      } else {
        Normal
      }
    }
  }
}

@Composable
private fun showTitle(): Boolean {
  return LocalSize.current.width >= 260.dp
}

private fun GlanceModifier.maybeClickable(action: Action?): GlanceModifier {
  return if (action != null) {
    this.clickable(action)
  } else {
    this
  }
}

/**
 * Information to be displayed in a [com.example.platform.ui.appwidgets.glance.layout.text.layout.LongTextLayout].
 *
 * @param key a unique identifier for the data being displayed as primary content e.g. article ID,
 * in an "article of the day" widget. This may not be necessary for your use case; in this sample,
 * we use this key to differentiate between placeholder and real data when demonstrating clicks.
 * @param text a required text representing primary information being conveyed to the user via the
 * widget; suitable for text of about 65 characters.
 * @param caption shorter text accompanying the primary [text] - that can fit in one line; e.g.
 * author's name in an "article of the day" widget.
 */
data class LongTextLayoutData(
  val key: String,
  val text: String,
  val caption: String,
)

internal object WidgetTextDimensions {
  val widgetPadding = 16.dp
  private val titleBarHeight: Dp
    @Composable get() = if (LongTextLayoutSize.fromLocalSize() == LongTextLayoutSize.XSmall) {
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
        height = size.height - widgetPadding - titleBarHeight
      )
    }

  // Upper and lower bounds for the caption.
  internal val minCaptionFontSize = 12.sp // low - GM3 Label Medium
  internal val maxCaptionFontSize = 14.sp // high - GM3 Label Large

  // Upper bound for primary text.
  internal val maxPrimaryTextFontSize = 28.sp // GM3 Headline Medium

  // For a font size 16 of primary text, we want caption to be of size 14.
  internal const val captionToPrimaryTextRatio = 0.875f

  @Composable
  fun primaryTextFontSizeAndMaxLines(text: String): Pair<TextUnit, Int> {
    val size = LocalSize.current
    // Primary text and caption share 70:30 height within the area available for texts.
    val availableHeightForPrimaryText = Dp(0.70f * contentSize.height.value)
    // In this layout, texts take up entire horizontal space except the paddings on the sides.
    val availableWidthForPrimaryText = size.width - (widgetPadding * 2)

    return calculateFontSizeAndMaxLines(
      context = LocalContext.current,
      text = text,
      availableWidth = availableWidthForPrimaryText,
      availableHeight = availableHeightForPrimaryText,
      minFontSize = (minCaptionFontSize.value / captionToPrimaryTextRatio).sp,
      maxFontSize = maxPrimaryTextFontSize
    )
  }

  fun captionFontSizeAndMaxLines(primaryFontSize: TextUnit): Pair<TextUnit, Int> {
    val estimatedFontSize = primaryFontSize.value * captionToPrimaryTextRatio
    val captionMaxLines = 1 // Caption is always 1 line.
    return estimatedFontSize.coerceAtMost(maxCaptionFontSize.value).sp to captionMaxLines
  }
}

/**
 * Previews of the long text layout with shorter caption and longer main text
 *
 * Previewing them at standard & min-max sizes allows us to adjust font sizes if needed. Use the
 * Preview annotation to view the widget at specific width / height.
 */
@SmallWidgetPreview
@MediumWidgetPreview
@Composable
private fun ShortCaptionSuperLongTextPreview() {
  LongTextLayoutPreview(
    text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Cursus mattis molestie a iaculis at erat pellentesque adipiscing commodo elit at imperdiet dui accumsan sit amet.",
    caption = "Ut mollis",
  )
}


/**
 * Previews of the long text layout with longer caption and shorter main text
 *
 * Previewing them at standard & min-max sizes allows us to adjust font sizes if needed. Use the
 * Preview annotation to view the widget at specific width / height.
 */
@SmallWidgetPreview
@MediumWidgetPreview
@Composable
private fun LongCaptionShortTextPreview() {
  LongTextLayoutPreview(
    text = "Comparatively small text",
    caption = "Ipsum faucibus ut mollis amet cursus",
  )
}

/**
 * Previews of the long text layout with medium sized caption and main text
 *
 * Previewing them at standard & min-max sizes allows us to adjust font sizes if needed. Use the
 * Preview annotation to view the widget at specific width / height.
 */
@SmallWidgetPreview
@MediumWidgetPreview
@Composable
private fun MediumTextPreview() {
  LongTextLayoutPreview(
    text = "This allows for a longer text string. Specifically because the focus in this, layout is on the primary text.",
    caption = "Ut mollis amet cursus",
  )
}

@Composable
private fun LongTextLayoutPreview(text: String, caption: String) {
  val context = LocalContext.current

  LongTextLayout(
    title = context.getString(R.string.sample_long_text_app_widget_name),
    titleIconRes = R.drawable.sample_text_icon,
    titleBarActionIconRes = R.drawable.sample_refresh_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_refresh_icon_button_label
    ),
    titleBarAction = {},
    data = LongTextLayoutData(
      key = "1",
      text = text,
      caption = caption,
    ),
    action = actionStartDemoActivity("1"),
  )
}
