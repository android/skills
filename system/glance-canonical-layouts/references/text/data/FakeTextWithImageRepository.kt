package com.example.platform.ui.appwidgets.glance.layout.text.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio.Companion.asDouble
import com.example.platform.ui.appwidgets.glance.layout.text.layout.ImageData
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextData
import com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageData
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
/**
 * An fake in-memory repository to provide data for displaying different demo samples in
 * [com.example.platform.ui.appwidgets.glance.layout.text.layout.TextWithImageLayout]
 */
class FakeTextWithImageRepository {
  private val data = MutableStateFlow<TextWithImageData?>(null)
  private var itemIndex = 0
  private var items = demoItems

  fun data(): Flow<TextWithImageData?> = data

  suspend fun refresh(context: Context) {
    val itemCount = items.size
    itemIndex = (itemIndex + 1) % itemCount

    this.load(context)
  }

  suspend fun load(context: Context): TextWithImageData? {
    val item = items[itemIndex]
    val bitmap = fetchImage(context, item.url)
    val mappedImageData = ImageData(
      bitmap = bitmap,
      contentDescription = item.imageContentDescription
    )

    data.value = TextWithImageData(
      textData = TextData(
        key = "$itemIndex",
        primary = item.primary,
        secondary = item.secondary,
        caption = item.caption
      ),
      imageData = mappedImageData
    )
    return data.value
  }

  private suspend fun fetchImage(context: Context, url: String): Bitmap? {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val imageSizeLimit = ImageUtils.getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio16x9.asDouble(),
      memoryLimitBytes = maxAllowedBytes,
      maxImages = 1
    )
    val maxWidth = imageSizeLimit.width
    val maxHeight = imageSizeLimit.height

    var bitmap: Bitmap? = null

    val result = ImageLoader(context).execute(
      ImageRequest.Builder(context)
        .data(url)
        .size(maxWidth, maxHeight)
        .target { res: Drawable ->
          bitmap = (res as BitmapDrawable).bitmap
        }.build()
    )

    if (result is ErrorResult) {
      Log.e(TAG, "Failed to load the image:", result.throwable)
    }

    return bitmap
  }

  companion object {
    private const val TAG = "FTWIR"

    private val repositories = mutableMapOf<GlanceId, FakeTextWithImageRepository>()

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getRepo(glanceId: GlanceId): FakeTextWithImageRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) {
          FakeTextWithImageRepository()
        }!!
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

    data class DemoData(
      val url: String,
      val imageContentDescription: String? = null,
      val primary: String,
      val secondary: String,
      val caption: String,
    )

      // Courtesy of https://unsplash.com/@iamliam
      val demoItems = listOf(
          DemoData(
              primary = "Davos at sunrise, viewed from Schatzalp",
              secondary = "Golden light washes over the alpine village of Davos, a breathtaking view unfolding from Schatzalp's vantage point.",
              caption = "33,822 views",
              url = "https://images.unsplash.com/photo-1671525737370-1d490286372e",
              imageContentDescription = "Davos at sunrise, viewed from Schatzalp",
          ),
          DemoData(
              primary = "Flowers at a wedding reception",
              secondary = "I took this photo at a wedding reception. The flowers were in a vase next to a window",
              caption = "33,822 views",
              url = "https://images.unsplash.com/photo-1531306760863-7fb02a41db12",
              imageContentDescription = "Flowers at a wedding reception",
          ),
          DemoData(
              primary = "Blushing Bride",
              secondary = "An up-close look at a Blushing Bride Protea flower, native to South Africa",
              caption = "31,072 views",
              url = "https://images.unsplash.com/photo-1566964423430-3e52903303a5",
              imageContentDescription = "Blushing Bride flower",
          ),
          DemoData(
              primary = "Winter in Switzerland",
              secondary = "A snow-shoer walking up Strelapass on snow lined with deep trails from skiiers",
              caption = "15 min",
              url = "https://images.unsplash.com/photo-1671525784444-392a8f8daa3f",
              imageContentDescription = "A snow-shoer walking up Strelapass on snow lined with deep trails from skiiers",
          ),
          DemoData(
              primary = "A single water droplet rests in a budding red pansy.",
              secondary = "Secrets held within a drop: A microcosm of beauty in a world of red.",
              caption = "193 views",
              url = "https://images.unsplash.com/photo-1685540466252-8c21e7c37624",
              imageContentDescription = "A single water droplet rests in a budding red pansy.",
          )
      )
  }
}
