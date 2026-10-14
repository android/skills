/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
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
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolBarButton
import com.example.platform.ui.appwidgets.glance.layout.toolbars.layout.ExpressiveToolbarLayout
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity

/**
 * A sample [GlanceAppWidget] that demonstrates the [ExpressiveToolbarLayout] that uses a background
 * of 4-sided cookie shape.
 *
 * Uses an example of a notes app with quick actions for
 * - adding a note (primary),
 * - starting a new note with microphone,
 * - starting a new note with camera,
 * - deeplink to sharing notes,
 * - starting a new note by uploading a file.
 */
class ExpressiveToolbarAppWidget : GlanceAppWidget() {
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
        ExpressiveToolbarLayout(
            centerButton = ExpressiveToolBarButton(
                iconRes = R.drawable.sample_add_icon,
                contentDescription = "Add notes",
                onClick = actionStartDemoActivity("add notes button")
            ),
            cornerButtons = listOf(
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_mic_icon,
                    contentDescription = "mic",
                    onClick = actionStartDemoActivity("mic button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_camera_icon,
                    contentDescription = "camera",
                    onClick = actionStartDemoActivity("camera button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_share_icon,
                    contentDescription = "share",
                    onClick = actionStartDemoActivity("share button")
                ),
                ExpressiveToolBarButton(
                    iconRes = R.drawable.sample_file_upload_icon,
                    contentDescription = "file upload",
                    onClick = actionStartDemoActivity("file upload button")
                ),
            )
        )
    }
}

class ExpressiveToolbarAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpressiveToolbarAppWidget()
}
