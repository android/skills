package com.example.platform.ui.appwidgets.glance.layout.collections.layout

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils

/**
 * Content to be displayed when there are no items in the list. To be displayed below the
 * app-specific title bar in the [androidx.glance.appwidget.components.Scaffold] .
 */
@Composable
internal fun EmptyListContent() {
  val context = LocalContext.current

  NoDataContent(
    noDataText = context.getString(R.string.sample_no_data_text),
    noDataIconRes = R.drawable.sample_no_data_icon,
    actionButtonText = context.getString(R.string.sample_add_button_text),
    actionButtonIcon = R.drawable.sample_add_icon,
    actionButtonOnClick = ActionUtils.actionStartDemoActivity("on-click of add item button")
  )
}
