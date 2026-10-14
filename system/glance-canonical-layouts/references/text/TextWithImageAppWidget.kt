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

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.AppWidgetId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.text.data.FakeTextWithImageRepository
import com.example.platform.ui.appwidgets.glance.layout.text.data.FakeTextWithImageRepository.Companion.getRepo
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageData
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TextWithImageAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val repo = getRepo(id)

    val initialData = withContext(Dispatchers.Default) {
      repo.load(context)
    }

    provideContent {
      val data by repo.data().collectAsState(initial = initialData)
      val coroutineScope = rememberCoroutineScope()

      GlanceTheme {
        key(LocalSize.current) {
          WidgetContent(
            data = data,
            refreshDataAction = {
              coroutineScope.launch {
                withContext(Dispatchers.Default) {
                  repo.refresh(context)
                }
              }
            })
        }
      }
    }
  }

  @Composable
  fun WidgetContent(data: TextWithImageData?, refreshDataAction: () -> Unit) {
    val context = LocalContext.current

    TextWithImageLayout(
      title = context.getString(R.string.sample_text_and_image_app_widget_name),
      titleIconRes = R.drawable.sample_text_icon,
      titleBarActionIconRes = R.drawable.sample_refresh_icon,
      titleBarActionIconContentDescription = context.getString(
        R.string.sample_refresh_icon_button_label
      ),
      titleBarAction = refreshDataAction,
      data = data
    )
  }
}

class TextWithImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = TextWithImageAppWidget()

  @SuppressLint("RestrictedApi")
  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    appWidgetIds.forEach {
      FakeTextWithImageRepository.cleanUp(AppWidgetId(appWidgetId = it))
    }
    super.onDeleted(context, appWidgetIds)
  }
}
