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

package com.example.platform.ui.appwidgets.glance.layout.collections.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio.Companion.asDouble
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageGridItemData
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxPossibleImageSize
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
/**
 * A fake in-memory implementation of repository that produces a list of
 * [ImageGridItemData].
 *
 * During the data mapping, loads resources as bitmaps, and scales them down to size within the
 * limits allowed for widgets.
 *
 */
class FakeImageGridDataRepository {
  private val data = MutableStateFlow(listOf<ImageGridItemData>())
  private var items = demoItems.take(MAX_ITEMS_PER_WIDGET)

  /**
   * Flow of list of [ImageGridItemData]s that can be listened to during a Glance session.
   */
  fun data(): Flow<List<ImageGridItemData>> = data

  suspend fun refresh(context: Context) {
    items = items.shuffled()

    this.load(context)
  }

  /**
   * Loads the [ImageGridItemData] items from the currently selected data source.
   *
   * User selected data source is set by configuration activity via [selectDataSource].
   */
  suspend fun load(context: Context): List<ImageGridItemData> {
    data.value = processImagesAndBuildData(
      context = context,
      items = items
    )

    return data.value
  }

  private suspend fun processImagesAndBuildData(
    context: Context,
    items: List<ImageGridItemBackendData>,
  ): List<ImageGridItemData> {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val maxAllowedBytesPerImage = maxAllowedBytes / items.size
    val imageSizeLimit = getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio16x9.asDouble(),
      memoryLimitBytes = maxAllowedBytesPerImage,
      maxImages = 1
    )

    val width = IMAGE_SIZE.coerceAtMost(imageSizeLimit.width)
    val height = width * 9 / 16

    val mappedItems = coroutineScope {
      items.map { item ->
        async(Dispatchers.IO) {
          var bitmap: Bitmap? = null

          val result = ImageLoader(context).execute(
            ImageRequest.Builder(context)
              .data(item.imageUrl)
              .size(width, height)
              .networkCachePolicy(CachePolicy.ENABLED)
              .target { res: Drawable ->
                bitmap = (res as BitmapDrawable).bitmap
              }.build()
          )

          if (result is ErrorResult) {
            Log.e(TAG, "Failed to load the image:", result.throwable)
          }

          return@async ImageGridItemData(
            key = item.key,
            title = item.title,
            supportingText = item.supportingText,
            image = bitmap,
            imageContentDescription = item.imageContentDescription
          )
        }
      }.awaitAll()
    }

    return mappedItems
  }

  private data class ImageGridItemBackendData(
    val key: String,
    val imageUrl: String,
    val imageContentDescription: String?,
    val title: String? = null,
    val supportingText: String? = null,
  )

    companion object {
        private val repositories = mutableMapOf<GlanceId, FakeImageGridDataRepository>()

        // Courtesy of https://unsplash.com/@iamliam
        // 16:9 images
        private val demoItems = listOf(
            ImageGridItemBackendData(
                key = "1",
                imageUrl = "https://images.unsplash.com/photo-1531306760863-7fb02a41db12",
                imageContentDescription = "Flowers at a wedding reception",
                title = "Flowers at a wedding reception",
                supportingText = "33,822 views"
            ),
            ImageGridItemBackendData(
                key = "2",
                imageUrl = "https://images.unsplash.com/photo-1566964423430-3e52903303a5",
                imageContentDescription = "An up-close look at a Blushing Bride Protea flower.",
                title = "An up-close look at a Blushing Bride Protea flower.",
                supportingText = "31,072 views"
            ),
            ImageGridItemBackendData(
                key = "3",
                imageUrl = "https://images.unsplash.com/photo-1685540466252-8c21e7c37624",
                imageContentDescription = "A single water droplet rests in a budding red pansy.",
                title = "A single water droplet rests in a budding red pansy.",
                supportingText = "193 views"
            ),
            ImageGridItemBackendData(
                key = "4",
                imageUrl = "https://images.unsplash.com/photo-1582817954171-c3533fffde89",
                imageContentDescription = "Blossom, petal, flower",
                title = "Blossom, petal, flower",
                supportingText = "23,815 views",
            ),
            ImageGridItemBackendData(
                key = "5",
                imageUrl = "https://images.unsplash.com/photo-1565314912546-0d18918fdc8f",
                imageContentDescription = "Green plant, sky and flowers",
                title = "Green plant, sky and flowers",
                supportingText = "99,467 views"
            ),
            ImageGridItemBackendData(
                key = "6",
                imageUrl = "https://images.unsplash.com/photo-1671525784444-392a8f8daa3f",
                imageContentDescription = "A snow-shoer walking up Strelapass",
                title = "A snow-shoer walking up Strelapass",
                supportingText = "3,033 views",
            ),
            ImageGridItemBackendData(
                key = "7",
                imageUrl = "https://images.unsplash.com/photo-1671525737370-1d490286372e",
                imageContentDescription = "Davos at sunrise, viewed from Schatzalp",
                title = "Davos at sunrise, viewed from Schatzalp",
                supportingText = "4,054 views",
            ),
            ImageGridItemBackendData(
                key = "8",
                imageUrl = "https://images.unsplash.com/photo-1629027272726-2eed15f90e8e",
                imageContentDescription = "Nasturtium plants",
                title = "Nasturtium plants",
                supportingText = "975 views",
            )
        )
    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getImageGridDataRepo(glanceId: GlanceId): FakeImageGridDataRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) { FakeImageGridDataRepository() }!!
      }
    }

    /**
     * Cleans up local data associated with the provided [glanceId].
     */
    fun cleanUp(glanceId: GlanceId) {
      synchronized(repositories) {
        repositories.remove(glanceId)
      }
    }

    // Capped at 8 to limit amount of memory consumed by bitmaps
    const val MAX_ITEMS_PER_WIDGET = 8
    const val IMAGE_SIZE = 200
    const val TAG = "FIGDR"
  }
}
