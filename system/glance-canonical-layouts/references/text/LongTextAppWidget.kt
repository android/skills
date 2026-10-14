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
package com.example.platform.ui.appwidgets.glance.layout.text

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.text.data.FakeLongTextRepository
import com.example.platform.ui.appwidgets.glance.layout.text.layout.LongTextLayout
import com.example.platform.ui.appwidgets.glance.layout.text.layout.LongTextLayoutData
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LongTextAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val repo = FakeLongTextRepository.getRepo(id)

    val initialData = withContext(Dispatchers.Default) {
      repo.load()
    }

    provideContent {
      val data by repo.data().collectAsState(initial = initialData)

      GlanceTheme {
        LongTextAppWidgetContent(
          data = data,
          refreshDataAction = { repo.refresh() })
      }
    }
  }

  @Composable
  fun LongTextAppWidgetContent(
    data: LongTextLayoutData,
    refreshDataAction: () -> Unit,
  ) {
    val context = LocalContext.current

    LongTextLayout(
      title = context.getString(R.string.sample_long_text_app_widget_name),
      titleIconRes = R.drawable.sample_text_icon,
      titleBarActionIconRes = R.drawable.sample_refresh_icon,
      titleBarActionIconContentDescription = context.getString(
        R.string.sample_refresh_icon_button_label
      ),
      titleBarAction = refreshDataAction,
      data = data,
      action = actionStartDemoActivity(data.key),
    )
  }
}

class LongTextAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = LongTextAppWidget()
}
