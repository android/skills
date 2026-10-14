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
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeActionListDataRepository
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeActionListDataRepository.Companion.getActionListDataRepo
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListItem
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ActionListLayout
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A sample [GlanceAppWidget] demonstrating the [ActionListLayout]. */
class ActionListAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val repo = getActionListDataRepo(id)

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
          checkItemAction = { key -> repo.checkItem(key) }
        )
      }
    }
  }

  @Composable
  fun WidgetContent(
    items: List<ActionListItem>,
    checkedItems: List<String>,
    checkItemAction: (String) -> Unit,
  ) {
    val context = LocalContext.current

    ActionListLayout(
      title = context.getString(R.string.sample_action_list_app_widget_name),
      titleIconRes = R.drawable.sample_home_icon,
      titleBarActionIconRes = R.drawable.sample_power_settings_icon,
      titleBarActionIconContentDescription = context.getString(
        R.string.sample_action_list_settings_label
      ),
      titleBarAction = actionStartDemoActivity("Power settings title bar action"),
      items = items,
      checkedItems = checkedItems,
      actionButtonClick = checkItemAction,
    )
  }
}

class ActionListAppWidgetAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget = ActionListAppWidget()

  @SuppressLint("RestrictedApi")
  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach {
      FakeActionListDataRepository.cleanUp(AppWidgetId(appWidgetId = it))
    }
    super.onDeleted(context, appWidgetIds)
  }
}
