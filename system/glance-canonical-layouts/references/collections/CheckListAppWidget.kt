package com.example.platform.ui.appwidgets.glance.layout.collections

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeCheckListDataRepository
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeCheckListDataRepository.Companion.getCheckListDataRepo
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListItem
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.CheckListLayout
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A sample [GlanceAppWidget] demonstrating the [CheckListLayout].
 *
 * Has ability to toggle between placeholder text and real-like text.
 */
class CheckListAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val repo = getCheckListDataRepo(id)

    val initialItems = withContext(Dispatchers.Default) {
      repo.load()
    }

    provideContent {
      val items by repo.items().collectAsState(initial = initialItems)
      val checkedItems by repo.checkedItems().collectAsState(initial = emptyList())

      GlanceTheme {
        WidgetContent(
          items = items,
          checkedItems = checkedItems,
          checkItemAction = { key: String -> repo.checkItem(key) }
        )
      }
    }
  }

  @Composable
  fun WidgetContent(
    items: List<CheckListItem>,
    checkedItems: List<String>,
    checkItemAction: (String) -> Unit,
  ) {
    val context = LocalContext.current

    CheckListLayout(
      title = context.getString(R.string.sample_check_list_app_widget_name),
      titleIconRes = R.drawable.sample_pin_icon,
      titleBarActionIconRes = R.drawable.sample_add_icon,
      titleBarActionIconContentDescription = context.getString(
        R.string.sample_add_button_text
      ),
      titleBarAction = actionStartDemoActivity("Add icon in title bar"),
      items = items,
      checkedItems = checkedItems,
      checkButtonContentDescription = context.getString(
        R.string.sample_mark_done_button_content_description
      ),
      checkedIconRes = R.drawable.sample_checked_circle_icon,
      unCheckedIconRes = R.drawable.sample_circle_icon,
      onCheck = checkItemAction
    )
  }
}

class CheckListAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget = CheckListAppWidget()

  @SuppressLint("RestrictedApi")
  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach {
      FakeCheckListDataRepository.cleanUp(AppWidgetId(appWidgetId = it))
    }
    super.onDeleted(context, appWidgetIds)
  }
}
