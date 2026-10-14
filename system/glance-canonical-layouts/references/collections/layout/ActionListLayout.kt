package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.ExperimentalGlanceApi
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
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeActionListDataRepository.Companion.demoData
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.circularCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.filledItemCornerRadius
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.filledItemPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.gridCells
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.itemContentSpacing
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.stateIconBackgroundSize
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.stateIconSize
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.verticalSpacing
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutDimensions.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutSize.Companion.showTitleBar
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutSize.Large
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayoutSize.Small
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.LargeWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting list of two-state actions represented by a title (1-2 words),
 * supporting text (~20 characters) and a leading state icon button to indicate the current state
 * and an optional trailing icon button to perform additional operations.
 *
 * Tapping each item changes the state (e.g. turn ON / OFF). The additional actions can be used to
 * perform tuned state changes e.g. editing the temperature The layout is suitable for use cases
 * such as home control, Quick Settings, etc.
 *
 * The content is displayed in a [Scaffold] under an app-specific title bar.
 *
 * In larger sizes, the layout shows a multi-column grid with each item taking minimum size to
 * fit the item content. The state icon is dropped in smaller sizes to allow space for additional
 * action and the text.
 *
 * The layout is an implementation suggestion, you can customize it to your needs. For example,
 * if you don't have a supporting text, you can drop the filled containers and use spacing to
 * individually differentiate your content. You can use separate icons for on and off states if it
 * suits your use case. And, if you need support for more states, you can use appropriate text and
 * icons.
 *
 * @param title the text to be displayed as title of the widget, e.g. name of your widget or app.
 * @param titleIconRes a tintable icon that represents your app or brand, that can be displayed
 *                     with the provided [title]. In this sample, we use icon from a drawable
 *                     resource, but you should use an appropriate icon source for your use case.
 * @param titleBarActionIconRes resource ID of a tintable icon that can be displayed as
 *                              an icon button within the title bar area of the widget. For
 *                              example, a search icon to launch search for finding specific
 *                              items.
 * @param titleBarActionIconContentDescription description of the [titleBarActionIconRes] button
 *                                             to be used by the accessibility services.
 * @param titleBarAction action to be performed on click of the [titleBarActionIconRes] button.
 * @param items list of items to be included in the list; typically includes a short title, a
 *              supporting text and the action icon information.
 * @param checkedItems list of keys of items that are in "ON" / checked state.
 * @param actionButtonClick handler to toggle the state.
 *
 * @see [FilledActionListItem] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.collections.CheckListAppWidget]
 *
 */
@Composable
fun ActionListLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int,
  titleBarActionIconContentDescription: String,
  titleBarAction: Action,
  items: List<ActionListItem>,
  checkedItems: List<String>,
  actionButtonClick: (String) -> Unit,
) {
  fun titleBar(): @Composable (() -> Unit) = {
    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      title = title.takeIf { ActionListLayoutSize.fromLocalSize() != Small } ?: "",
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
    modifier = GlanceModifier.padding(top = scaffoldTopPadding),
    titleBar = if (showTitleBar()) {
      titleBar()
    } else {
      null
    },
  ) {
    Content(
      items = items,
      checkedItems = checkedItems,
      actionButtonOnClick = actionButtonClick,
    )
  }
}

@Composable
private fun Content(
  items: List<ActionListItem>,
  checkedItems: List<String>,
  actionButtonOnClick: (String) -> Unit,
) {
  val actionListLayoutSize = ActionListLayoutSize.fromLocalSize()

  Box(modifier = GlanceModifier.padding(bottom = widgetPadding)) {
    if (items.isEmpty()) {
      EmptyListContent()
    } else {
      when (actionListLayoutSize) {
        Large -> GridView(
          items = items,
          checkedItems = checkedItems,
          actionButtonOnClick = actionButtonOnClick
        )

        else -> ListView(
          items = items,
          checkedItems = checkedItems,
          actionButtonOnClick = actionButtonOnClick
        )
      }
    }
  }
}

@Composable
private fun ListView(
  items: List<ActionListItem>,
  checkedItems: List<String>,
  actionButtonOnClick: (String) -> Unit,
) {
  RoundedScrollingLazyColumn(
    modifier = GlanceModifier.fillMaxSize(),
    items = items,
    verticalItemsSpacing = verticalSpacing,
    itemContentProvider = { item ->
      FilledActionListItem(
        item = item,
        isChecked = checkedItems.contains(item.key),
        actionButtonClick = actionButtonOnClick,
        modifier = GlanceModifier.fillMaxSize()
      )
    }
  )
}

@Composable
private fun GridView(
  items: List<ActionListItem>,
  checkedItems: List<String>,
  actionButtonOnClick: (String) -> Unit,
) {
  RoundedScrollingLazyVerticalGrid(
    gridCells = gridCells,
    items = items,
    cellSpacing = itemContentSpacing,
    itemContentProvider = { item ->
      FilledActionListItem(
        item = item,
        isChecked = checkedItems.contains(item.key),
        actionButtonClick = actionButtonOnClick,
        modifier = GlanceModifier.fillMaxSize()
      )
    },
    modifier = GlanceModifier.fillMaxSize(),
  )
}

/**
 * A filled list / grid item that displays a title, a supporting text, and a trailing state icon.
 *
 * Uses single line title (1-2 words), and 2-line supporting text (~ 50-55 characters)
 */
@OptIn(ExperimentalGlanceApi::class)
@Composable
private fun FilledActionListItem(
  item: ActionListItem,
  actionButtonClick: (String) -> Unit,
  modifier: GlanceModifier = GlanceModifier,
  isChecked: Boolean,
) {
  @Composable
  fun Title() {
    Text(
      text = item.title,
      style = ActionListLayoutTextStyles.titleText(isChecked),
      maxLines = 1,
      // Container's content description already reads this text
      modifier = GlanceModifier.semantics { contentDescription = "" }
    )
  }

  @Composable
  fun SupportingText() {
    Text(
      text = if (isChecked) {
        item.onSupportingText
      } else {
        item.offSupportingText
      },
      style = ActionListLayoutTextStyles.supportingText(isChecked),
      maxLines = 2,
      // Container's content description already reads this text
      modifier = GlanceModifier.semantics { contentDescription = "" }
    )
  }

  @Composable
  fun StateIndicatorIcon() {
    Box(
      GlanceModifier
        .size(stateIconBackgroundSize)
        .cornerRadius(circularCornerRadius),
      contentAlignment = Alignment.Center
    ) {
      Image(
        provider = ImageProvider(item.stateIconRes),
        modifier = modifier.size(stateIconSize),
        contentDescription = null, // already covered in list item container's description
        colorFilter = ColorFilter.tint(
          if (isChecked) {
            GlanceTheme.colors.onPrimary
          } else {
            GlanceTheme.colors.onSurfaceVariant
          }
        )
      )
    }
  }

  /**
   * Additional action that can be performed against the item.
   *
   * For example, edit the item. Onclick of the title toggles the state, however, sometimes, the
   * may be additional controls associated with the action e.g. changing temperature.
   */
  @Composable
  fun AdditionalActionButton(@DrawableRes resId: Int, contentDescription: String?) {
    CircleIconButton(
      imageProvider = ImageProvider(resId),
      contentDescription = contentDescription,
      onClick = actionStartDemoActivity("$contentDescription ${item.key}"),
      backgroundColor = null,
      contentColor = if (isChecked) {
        GlanceTheme.colors.onPrimary
      } else {
        GlanceTheme.colors.onSurface
      }
    )
  }

  /**
   * Returns a combined content description that can be set on entire list item.
   */
  fun combinedContentDescription(): String {
    val contentDescriptionBuilder = StringBuilder()
    contentDescriptionBuilder.append(item.title)
    contentDescriptionBuilder.append(" ")
    contentDescriptionBuilder.append(
      if (isChecked) {
        item.onSupportingText
      } else {
        item.offSupportingText
      }
    )
    contentDescriptionBuilder.append(" ")
    contentDescriptionBuilder.append(
      if (isChecked) {
        item.onStateActionContentDescription
      } else {
        item.offStartActionContentDescription
      }
    )
    return contentDescriptionBuilder.toString()
  }

  ListItem(
    modifier = modifier
      // We set a combined content description on list item since entire item is clickable.
      .semantics { contentDescription = combinedContentDescription() }
      .filledContainer(isChecked)
      .clickable(key = "${LocalSize.current} ${item.key}") { actionButtonClick(item.key) },
    contentSpacing = itemContentSpacing,
    leadingContent = takeComposableIf(ActionListLayoutSize.fromLocalSize() != Small) {
      StateIndicatorIcon()
    },
    headlineContent = { Title() },
    supportingContent = { SupportingText() },
    trailingContent = if (item.trailingIconButtonRes != null) {
      {
        AdditionalActionButton(
          item.trailingIconButtonRes,
          item.trailingIconButtonContentDescription
        )
      }
    } else null
  )
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

/**
 * Converts an item into a filled container by applying the background color, padding and an
 * appropriate corner radius.
 */
@Composable
private fun GlanceModifier.filledContainer(isChecked: Boolean): GlanceModifier {
  return cornerRadius(filledItemCornerRadius)
    .padding(filledItemPadding)
    .background(
      if (isChecked) {
        GlanceTheme.colors.primary
      } else {
        GlanceTheme.colors.secondaryContainer
      }
    )
}

/**
 * Holds data corresponding to each item in a
 * [com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayout].
 *
 * @param key a unique identifier for a specific item
 * @param title a short text (1-2 words) representing the item
 * @param onSupportingText a compact text (~20 characters) that supports the [title] and provides
 *                         textual indication of "ON" state of item; this allows keeping the title
 *                         short and glanceable.
 * @param offSupportingText a compact text (~20 characters) that supports the [title] and provides
 *  *                         textual indication of "OFF" state of item; this allows keeping the title
 *  *                         short and glanceable.
 * @param stateIconRes a tintable icon that can represents the item and that can be presented on
 *                        a colored background to indicate the current state (e.g. bulb ON / OFF).
 *                        On click of this icon, the item's state will be toggled.
 * @param onStateActionContentDescription text to describe what happens on click of the list item
 *                                        when item is in "ON" (checked) state; appended to the
 *                                        title and supporting text when used by the the
 *                                        accessibility services.
 * @param offStartActionContentDescription text to describe what happens on click of the list item
 *                                        when item is in "OFF" (un-checked) state; appended to the
 *                                        title and supporting text when used by the the
 *                                        accessibility services.
 */
data class ActionListItem(
  val key: String,
  val title: String,
  val onSupportingText: String,
  val offSupportingText: String,
  @DrawableRes val stateIconRes: Int,
  val onStateActionContentDescription: String,
  val offStartActionContentDescription: String,
  @DrawableRes val trailingIconButtonRes: Int? = null,
  val trailingIconButtonContentDescription: String? = null,
)

/**
 * Size of the widget per the reference breakpoints. Each size has its own display
 * characteristics such as - showing content as list versus grid, font sizes etc.
 *
 * In this layout, only width breakpoints are used to scale the layout.
 */
private enum class ActionListLayoutSize(val maxWidth: Dp) {
  // Single column list - compact view e.g. reduced fonts.
  Small(maxWidth = 260.dp),

  // Single column list
  Medium(maxWidth = 439.dp),

  // 2 Column Grid
  Large(maxWidth = 644.dp);

  companion object {
    /**
     * Returns the corresponding [ActionListLayoutSize] to be considered for the current
     * widget size.
     */
    @Composable
    fun fromLocalSize(): ActionListLayoutSize {
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
  }
}

private object ActionListLayoutTextStyles {
  /**
   * Style for the text displayed as title within each item.
   */
  @Composable
  fun titleText(checked: Boolean): TextStyle = TextStyle(
    fontWeight = FontWeight.Medium,
    fontSize = if (ActionListLayoutSize.fromLocalSize() == Small) {
      14.sp // M3 Title Small
    } else {
      16.sp // M3 Title Medium
    },
    color = if (checked) {
      GlanceTheme.colors.onPrimary
    } else {
      GlanceTheme.colors.onSurface
    }
  )

  /**
   * Style for the text displayed as supporting text within each item.
   */
  @Composable
  fun supportingText(isChecked: Boolean): TextStyle =
    TextStyle(
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp, // M3 Label Medium
      color = if (isChecked) {
        GlanceTheme.colors.onPrimary
      } else {
        GlanceTheme.colors.onSurfaceVariant
      }
    )
}

private object ActionListLayoutDimensions {
  /** Number of cells in the grid, when items are displayed as a grid. */
  const val gridCells = 2

  /** Padding applied at bottom of the widget content */
  val widgetPadding = 12.dp

  /** Corner radius for each filled list item. */
  val filledItemCornerRadius = 16.dp

  /** Padding for filled list items. */
  val filledItemPadding = 12.dp

  /** Vertical spacing between items in the list.*/
  val verticalSpacing = 4.dp

  /**
   * Spacing between individual sections in within the list item (for instance, horizontal spacing
   * between state icon and text section.
   */
  val itemContentSpacing = 4.dp

  /** Size of the background layer on the state icon. */
  val stateIconBackgroundSize = 48.dp

  /** Size of the state icon image. */
  val stateIconSize = 24.dp

  /**  Corner radius to achieve circular shape. */
  val circularCornerRadius = 200.dp
}

/**
 * Preview sizes for the widget covering the breakpoints.
 *
 * This allows verifying updates across multiple breakpoints.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 259, heightDp = 200)
@Preview(widthDp = 438, heightDp = 200)
@Preview(widthDp = 644, heightDp = 200)
private annotation class ActionListBreakpointPreviews

/**
 * Previews for the action list layout.
 *
 * First we look at the previews at defined breakpoints, tweaking them as necessary. In addition,
 * the previews at standard sizes allows us to quickly verify updates across min / max and common
 * widget sizes without needing to run the app or manually place the widget.
 */
@ActionListBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun ActionListLayoutPreview() {
  val context = LocalContext.current

  ActionListLayout(
    title = context.getString(R.string.sample_action_list_app_widget_name),
    titleIconRes = R.drawable.sample_home_icon,
    titleBarActionIconRes = R.drawable.sample_power_settings_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_action_list_settings_label
    ),
    titleBarAction = actionStartDemoActivity("Power settings title bar action"),
    items = demoData,
    checkedItems = listOf("1", "3"),
    actionButtonClick = {},
  )
}
