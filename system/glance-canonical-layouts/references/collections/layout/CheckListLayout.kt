package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.Scaffold
import androidx.glance.appwidget.components.TitleBar
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeCheckListDataRepository.Companion.demoData
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutDimensions.checkListRowStartPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutDimensions.checkListRowEndPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutDimensions.scaffoldHorizontalPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutDimensions.verticalItemSpacing
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutDimensions.widgetPadding
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutSize.Companion.isWiderThan
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutSize.Companion.showTitleBar
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayoutSize.Small
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.LargeWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A layout focused on presenting list of items in a check list. Content is displayed in a
 * [Scaffold] below an app-specific title bar.
 *
 * The layout is a variation of [ActionListLayout], where on-check, item is removed from list once
 * updated in the backend. The layout assumes checklist items follow a certain order, so, in
 * larger sizes, instead of displaying items in a grid, the layout shows additional trailing
 * actions per item. However, if grid works for your use case, you can switch over to it to
 * support large sizes.
 *
 * In this sample, user sees the checked state until the item is removed from the list in the
 * backing database. By showing the checked state, it is clear to the user that tapping did
 * something. Specifically, for similar looking texts, it may not be immediately obvious to the
 * user that item was removed; the intermediate checked state gives that clarity.
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
 * @param items list of items to be included in the list; typically includes a short title and a
 *              supporting text.
 * @param checkedItems list of keys of items that are in checked state.
 * @param checkedIconRes tintable icon to be pressed to clear an item.
 * @param unCheckedIconRes tintable icon to be pressed to check an item.
 * @param checkButtonContentDescription description of the unchecked button to be used by the
 *                                     accessibility services; in checked state, button is
 *                                     not clickable.
 * @param onCheck handler to perform the specific action on click of icon; in
 *                          a to-do list this can be the handler removing the item from the list.

 *
 * @see [CheckListItem] for accepted inputs.
 * @see [com.example.platform.ui.appwidgets.glance.layout.collections.CheckListAppWidgetReceiver]
 */
@Composable
fun CheckListLayout(
  title: String,
  @DrawableRes titleIconRes: Int,
  @DrawableRes titleBarActionIconRes: Int,
  titleBarActionIconContentDescription: String,
  titleBarAction: Action,
  items: List<CheckListItem>,
  checkedItems: List<String>,
  @DrawableRes checkedIconRes: Int,
  @DrawableRes unCheckedIconRes: Int,
  checkButtonContentDescription: String,
  onCheck: (String) -> Unit,
) {
  val checkListLayoutSize = CheckListLayoutSize.fromLocalSize()

  fun titleBar(): @Composable (() -> Unit) = {
    TitleBar(
      startIcon = ImageProvider(titleIconRes),
      // Based on your widget content, you may skip the title in smaller sizes.
      title = title.takeIf { checkListLayoutSize != Small } ?: "",
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
    horizontalPadding = scaffoldHorizontalPadding,
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
    if (items.isEmpty()) {
      EmptyListContent()
    } else {
      Content(
        items = items,
        checkedItems = checkedItems,
        onCheck = onCheck,
        checkedIconRes = checkedIconRes,
        unCheckedIconRes = unCheckedIconRes,
        checkButtonContentDescription = checkButtonContentDescription,
      )
    }
  }
}

@Composable
private fun Content(
  items: List<CheckListItem>,
  checkedItems: List<String>,
  onCheck: (String) -> Unit,
  @DrawableRes checkedIconRes: Int,
  @DrawableRes unCheckedIconRes: Int,
  checkButtonContentDescription: String,
) {
  RoundedScrollingLazyColumn(
    modifier = GlanceModifier.fillMaxSize(),
    items = items,
    verticalItemsSpacing = verticalItemSpacing,
    itemContentProvider = { item ->
      CheckListItem(
        item = item,
        isChecked = checkedItems.contains(item.key),
        onCheck = onCheck,
        checkedIconRes = checkedIconRes,
        unCheckedIconRes = unCheckedIconRes,
        checkButtonContentDescription = checkButtonContentDescription,
      )
    }
  )
}

/**
 * A list item that displays a checkable item with a title, a supporting text, and trailing icon
 * buttons.
 *
 * Uses single line title (1-2 words), and 2-line supporting text (~ 50-55 characters)
 */
@Composable
private fun CheckListItem(
  item: CheckListItem,
  @DrawableRes checkedIconRes: Int,
  @DrawableRes unCheckedIconRes: Int,
  checkButtonContentDescription: String,
  onCheck: (String) -> Unit,
  modifier: GlanceModifier = GlanceModifier,
  isChecked: Boolean,
) {
    val listItemEndPadding = if (item.hasTrailingIcons) {
        0.dp
    } else {
        checkListRowEndPadding
    }

  @Composable
  fun CheckButton() {
    CircleIconButton(
      imageProvider = if (isChecked) {
        ImageProvider(checkedIconRes)
      } else {
        ImageProvider(unCheckedIconRes)
      },
      backgroundColor = null, // to show transparent background
      contentColor = GlanceTheme.colors.secondary,
      contentDescription = checkButtonContentDescription,
      enabled = !isChecked,
      onClick = { onCheck(item.key) },
      key = "${LocalSize.current} ${item.key}"
    )
  }

  @Composable
  fun Title() {
    Text(
      text = item.title,
      style = CheckListLayoutTextStyles.titleText,
      maxLines = 2,
    )
  }

  @Composable
  fun SupportingText() {
    Text(
      text = item.supportingText,
      style = CheckListLayoutTextStyles.supportingText,
      maxLines = 2,
    )
  }

  @Composable
  fun TrailingActions() {
    TrailingIconButtonSet(
      leadingButtonRes = R.drawable.sample_edit_icon,
      leadingButtonContentDescription = "Edit",
      leadingButtonOnClick = actionStartDemoActivity(message = "Edit click on item: ${item.key}"),
      middleButtonRes = R.drawable.sample_snooze_icon,
      middleButtonContentDescription = "Snooze",
      middleButtonOnClick = actionStartDemoActivity(message = "Snooze click on item: ${item.key}"),
      trailingButtonRes = R.drawable.sample_delete_icon,
      trailingButtonContentDescription = "Delete",
      trailingButtonOnClick = actionStartDemoActivity(message = "Delete click on item: ${item.key}"),
    )
  }

  // List item itself is not clickable, as it contains more trailing actions.
  ListItem(
      modifier = modifier.fillMaxWidth()
          .padding(start = checkListRowStartPadding, end = listItemEndPadding),
    contentSpacing = 0.dp, // Since check box's tap target covers the needed visual spacing
    leadingContent = { CheckButton() },
    headlineContent = { Title() },
    supportingContent = { SupportingText() },
    trailingContent = takeComposableIf(item.hasTrailingIcons)
    { TrailingActions() },
  )
}

@Composable
private fun TrailingIconButtonSet(
  @DrawableRes leadingButtonRes: Int,
  leadingButtonContentDescription: String,
  leadingButtonOnClick: Action,
  @DrawableRes middleButtonRes: Int,
  middleButtonContentDescription: String,
  middleButtonOnClick: Action,
  @DrawableRes trailingButtonRes: Int,
  trailingButtonContentDescription: String,
  trailingButtonOnClick: Action,
) {
  val checkListLayoutSize = CheckListLayoutSize.fromLocalSize()

  if (checkListLayoutSize.isWiderThan(CheckListLayoutSize.Medium)) {
    CircleIconButton(
      imageProvider = ImageProvider(leadingButtonRes),
      backgroundColor = null, // to show transparent background
      contentColor = GlanceTheme.colors.secondary,
      contentDescription = leadingButtonContentDescription,
      onClick = leadingButtonOnClick,
    )
    if (checkListLayoutSize.isWiderThan(CheckListLayoutSize.Large)) {
      CircleIconButton(
        imageProvider = ImageProvider(middleButtonRes),
        backgroundColor = null, // to show transparent background
        contentColor = GlanceTheme.colors.secondary,
        contentDescription = middleButtonContentDescription,
        onClick = middleButtonOnClick,
      )
    }
    if (checkListLayoutSize.isWiderThan(CheckListLayoutSize.XLarge)) {
      CircleIconButton(
        imageProvider = ImageProvider(trailingButtonRes),
        backgroundColor = null, // to show transparent background
        contentColor = GlanceTheme.colors.secondary,
        contentDescription = trailingButtonContentDescription,
        onClick = trailingButtonOnClick,
      )
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

/**
 * Holds data corresponding to each item in a
 * [com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayout].
 *
 * @param key a unique identifier for a specific item
 * @param title a short text (1-2 words) representing the item
 * @param supportingText a compact text (~50-55 characters) supporting the [title]; this allows
 *                       keeping the title short and glanceable, as well as helps support smaller
 *                       widget sizes.
 */
data class CheckListItem(
  val key: String,
  val title: String,
  val supportingText: String,
  val hasTrailingIcons: Boolean = false,
)

/**
 * Size of the widget per the reference breakpoints. Each size has its own display
 * characteristics such as - displaying containers on list items, font sizes, etc.
 *
 * In this layout, only width breakpoints are used to scale the layout.
 */
private enum class CheckListLayoutSize(val maxWidth: Dp) {
  // Smaller fonts, no title in title-bar
  Small(maxWidth = 260.dp),

  // larger fonts, title present, no trailing actions
  Medium(maxWidth = 304.dp),

  // 1 trailing action
  Large(maxWidth = 348.dp),

  // 2 trailing actions
  XLarge(maxWidth = 396.dp),

  // 3 trailing actions
  XXLarge(maxWidth = Dp.Infinity);

  companion object {
    /**
     * Returns the corresponding [CheckListLayoutSize] to be considered for the current widget size.
     */
    @Composable
    fun fromLocalSize(): CheckListLayoutSize {
      val size = LocalSize.current

      CheckListLayoutSize.values().forEach {
        if (size.width < it.maxWidth) {
          return it
        }
      }
      throw IllegalStateException("No mapped size ")
    }

    fun CheckListLayoutSize.isWiderThan(checkListLayoutSize: CheckListLayoutSize): Boolean {
      return this.maxWidth > checkListLayoutSize.maxWidth
    }

    @Composable
    fun showTitleBar(): Boolean {
      return LocalSize.current.height >= 180.dp
    }
  }
}

private object CheckListLayoutTextStyles {
  /**
   * Style for the text displayed as title within each item.
   */
  val titleText: TextStyle
    @Composable get() = TextStyle(
      fontWeight = FontWeight.Medium,
      fontSize = if (CheckListLayoutSize.fromLocalSize() == Small) {
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

private object CheckListLayoutDimensions {
  val widgetPadding = 12.dp

  val verticalItemSpacing = 4.dp

  // Full width scrollable content
  val scaffoldHorizontalPadding = 0.dp
  // Match with the padding applied to the app icon in title bar; this allow us to vertically align
  // the app icon with check icon button.
  val checkListRowStartPadding = 2.dp
  // Padding to be applied on right of each item if there isn't a icon button on right.
  val checkListRowEndPadding = widgetPadding
}

/**
 * Preview sizes of layout at the configured width breakpoints.
 */
@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 259, heightDp = 200)
@Preview(widthDp = 303, heightDp = 200)
@Preview(widthDp = 350, heightDp = 200)
private annotation class CheckListBreakpointPreviews

/**
 * Previews for the check list layout.
 *
 * First we look at the previews at defined breakpoints, tweaking them as necessary. In addition,
 * the previews at standard sizes allows us to quickly verify updates across min / max and common
 * widget sizes without needing to run the app or manually place the widget.
 */
@CheckListBreakpointPreviews
@SmallWidgetPreview
@MediumWidgetPreview
@LargeWidgetPreview
@Composable
private fun CheckListLayoutPreview() {
  val context = LocalContext.current
  CheckListLayout(
    title = context.getString(R.string.sample_check_list_app_widget_name),
    titleIconRes = R.drawable.sample_pin_icon,
    titleBarActionIconRes = R.drawable.sample_add_icon,
    titleBarActionIconContentDescription = context.getString(
      R.string.sample_add_button_text,
    ),
    titleBarAction = actionStartDemoActivity("Add icon in title bar"),
    items = demoData,
    checkedItems = listOf("1"),
    checkButtonContentDescription = context.getString(
      R.string.sample_mark_done_button_content_description,
    ),
    checkedIconRes = R.drawable.sample_checked_circle_icon,
    unCheckedIconRes = R.drawable.sample_circle_icon,
    onCheck = {},
  )
}
