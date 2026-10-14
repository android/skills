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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.platform.ui.appwidgets.glance.layout.text.layout

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridItemData
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.NoDataContent
import com.example.platform.ui.appwidgets.glance.layout.utils.ActionUtils.actionStartDemoActivity
import com.example.platform.ui.appwidgets.glance.layout.utils.MediumWidgetPreview
import com.example.platform.ui.appwidgets.glance.layout.utils.SmallWidgetPreview

/**
 * A full bleed snap scrolling gallery canonical layout using [ImageGridItemData].
 *
 * Each item displays an edge-to-edge background photo with overlaid
 * title and caption details that auto-scale to fit the current widget size.
 */
@Composable
fun FullBleedImageLayout(
    data: List<ImageGridItemData>? = null,
) {
    val size = LocalSize.current
    val isSmall = size.height <= 110.dp
    val appName = LocalContext.current.getString(R.string.sample_full_bleed_image_app_widget_name)

    val scrollMode = if (Build.VERSION.SDK_INT >= 37) {
        VerticalScrollMode.SnapScrollMatchHeight(size.height)
    } else {
        VerticalScrollMode.Normal
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
    ) {
        if (data.isNullOrEmpty()) {
            val context = LocalContext.current
            NoDataContent(
                noDataText = context.getString(R.string.sample_no_data_text),
                noDataIconRes = R.drawable.sample_no_data_icon,
                actionButtonText = context.getString(R.string.sample_learn_more_button_text),
                actionButtonIcon = R.drawable.sample_info_icon,
                actionButtonOnClick = actionStartDemoActivity("on-click of info button in no data view")
            )
        } else {
            SnapScrollingGallery(
                data = data,
                isSmall = isSmall,
                appName = appName,
                size = size,
                scrollMode = scrollMode
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
@Composable
private fun SnapScrollingGallery(
    data: List<ImageGridItemData>?,
    isSmall: Boolean,
    appName: String,
    size: DpSize,
    scrollMode: VerticalScrollMode,
) {
    val galleryData = data ?: return
    if (galleryData.isEmpty()) return

    if (galleryData.size == 1) {
        // If there's only 1 item (like in the widget preview), render with fillMaxSize to
        // bypass LazyColumn measurement issues where the generated widget preview item doesn't
        // fill the widget bounds.
        GalleryItemCard(
            item = galleryData[0],
            isSmall = isSmall,
            appName = appName,
            modifier = GlanceModifier.fillMaxSize()
        )
    } else {
        LazyColumn(
            modifier = GlanceModifier.fillMaxSize(),
            verticalScrollMode = scrollMode
        ) {
            items(galleryData.take(5), itemId = { it.key.hashCode().toLong() }) { item ->
                GalleryItemCard(
                    item = item,
                    isSmall = isSmall,
                    appName = appName,
                    modifier = GlanceModifier.size(size.width, size.height)
                )
            }
        }
    }
}

@Composable
private fun GalleryItemCard(
    item: ImageGridItemData,
    isSmall: Boolean,
    appName: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    val itemTitle = item.title ?: ""

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart
    ) {
        val imageProvider = item.image?.let { ImageProvider(it) } ?: ImageProvider(R.drawable.sample_placeholder_image)

        Image(
            provider = imageProvider,
            contentDescription = item.imageContentDescription ?: itemTitle,
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.fillMaxSize()
        )

        val titleFontSize = if (isSmall) {
            WidgetTextDimensions.primaryTextFontSizeAndMaxLines(itemTitle).first
        } else {
            WidgetTextDimensions.maxPrimaryTextFontSize
        }

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                // Implementing a partial gradient scrim by applying a background modifier directly
                // to a text Column results in the gradient stretching to fill the entire widget.
                .background(ImageProvider(R.drawable.sample_scrim_gradient))
                .padding(WidgetTextDimensions.widgetPadding),
            verticalAlignment = Alignment.Bottom,
        ) {
            // App Logo Icon styled as a fixed size monochrome asset above the Title as the Caption
            Image(
                provider = ImageProvider(R.drawable.sample_app_logo),
                contentDescription = appName,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.size(24.dp)
            )

            if (itemTitle.isNotEmpty()) {
                Text(
                    text = itemTitle,
                    style = TextStyle(
                        color = ColorProvider(Color.White, Color.White),
                        fontWeight = FontWeight.Bold,
                        fontSize = titleFontSize
                    ),
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES_FULL.BAKLAVA_1)
@SmallWidgetPreview
@MediumWidgetPreview
@Composable
private fun FullBleedImageLayoutPreview() {
    val previewItems = listOf(
        ImageGridItemData(
            key = "0",
            title = "Yosemite Valley under clear blue skies",
            image = null,
            imageContentDescription = null
        ),
        ImageGridItemData(
            key = "1",
            title = "Mystical forest lake reflection at sunrise",
            image = null,
            imageContentDescription = null
        )
    )

    FullBleedImageLayout(
        data = previewItems
    )
}
