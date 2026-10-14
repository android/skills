/*
 * Copyright 2024 The Android Open Source Project
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
package com.example.platform.ui.appwidgets.glance.layout.toolbars

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarButton
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.SearchToolBarLayout
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity

/**
 * A widget to demonstrate the [SearchToolBarLayout].
 */
class SearchToolBarAppWidget : GlanceAppWidget() {
  // Unlike the "Single" size mode, using "Exact" allows us to have better control over rendering in
  // different sizes. And, unlike the "Responsive" mode, it doesn't cause several views for each
  // supported size to be held in the widget host's memory.
  override val sizeMode: SizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    provideContent {
      GlanceTheme {
        WidgetContent()
      }
    }
  }

  @Composable
  fun WidgetContent() {
    SearchToolBarLayout(
      searchButton = SearchToolBarButton(
        iconRes = R.drawable.sample_search_icon,
        contentDescription = "Search notes",
        text = "Search",
        onClick = actionStartDemoActivity("search notes button")
      ),
      trailingButtons = listOf(
        SearchToolBarButton(
          iconRes = R.drawable.sample_mic_icon,
          contentDescription = "audio",
          onClick = actionStartDemoActivity("audio button")
        ),
        SearchToolBarButton(
          iconRes = R.drawable.sample_videocam_icon,
          contentDescription = "video note",
          onClick = actionStartDemoActivity("video note button")
        ),
        SearchToolBarButton(
          iconRes = R.drawable.sample_camera_icon,
          contentDescription = "camera",
          onClick = actionStartDemoActivity("camera button")
        ),
        SearchToolBarButton(
          iconRes = R.drawable.sample_share_icon,
          contentDescription = "share",
          onClick = actionStartDemoActivity("share button")
        ),
      )
    )
  }
}

/**
 * Receiver registered in the manifest for the [SearchToolBarAppWidget].
 */
class SearchToolBarAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SearchToolBarAppWidget()
}
