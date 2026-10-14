/*
 * Copyright 2026 The Android Open Source Project
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
 * See the License for the details; and limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout.text

import android.content.Context
import android.util.Log
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeImageGridDataRepository
import com.example.platform.ui.appwidgets.glance.layout.collections.data.FakeImageGridDataRepository.Companion.getImageGridDataRepo
import com.example.platform.ui.appwidgets.glance.layout.text.layout.FullBleedImageLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glance widget showcasing Full Bleed Snap Scrolling, powered by [FakeImageGridDataRepository].
 */
class FullBleedImageAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override val previewSizeMode = SizeMode.Responsive(
        setOf(
            DpSize(109.dp, 115.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = getImageGridDataRepo(id)

        val initialData = withContext(Dispatchers.IO) {
            repo.load(context)
        }

        provideContent {
            val data by repo.data().collectAsState(initial = initialData)

            GlanceTheme {
                FullBleedImageLayout(
                    data = data
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val repo = FakeImageGridDataRepository()

        val initialData = withContext(Dispatchers.IO) {
            repo.load(context)
        }

        provideContent {
            GlanceTheme {
                FullBleedImageLayout(
                    data = initialData.take(1)
                )
            }
        }
    }
}

/**
 * Receiver for the Full Bleed Snap Scrolling widget.
 */
class FullBleedImageAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullBleedImageAppWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val glanceAppWidgetManager = GlanceAppWidgetManager(context)
        appWidgetIds.forEach { id ->
            try {
                val glanceId = glanceAppWidgetManager.getGlanceIdBy(id)
                FakeImageGridDataRepository.cleanUp(glanceId)
            } catch (e: IllegalArgumentException) {
                Log.w("FullBleedImageReceiver", "Skipping cleanup for invalid AppWidget ID: $id", e)
            }
        }
        super.onDeleted(context, appWidgetIds)
    }
}
