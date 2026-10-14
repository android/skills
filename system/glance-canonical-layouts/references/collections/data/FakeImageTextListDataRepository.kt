package com.example.platform.ui.appwidgets.glance.layout.collections.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.glance.GlanceId
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import com.example.platform.ui.appwidgets.R
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio
import com.example.platform.ui.appwidgets.glance.layout.utils.AspectRatio.Companion.asDouble
import com.example.platform.ui.appwidgets.glance.layout.collections.layout.ImageTextListItemData
import com.example.platform.ui.appwidgets.glance.layout.computeIfAbsent as computeIfAbsentExt
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils
import com.example.platform.ui.appwidgets.glance.layout.utils.ImageUtils.getMaxWidgetMemoryAllowedSizeInBytes
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

/**
 * A fake in-memory implementation of repository that produces list of [ImageTextListItemData]
 */
class FakeImageTextListDataRepository {
  private val data = MutableStateFlow(listOf<ImageTextListItemData>())
  private var items = demoItems.take(MAX_ITEMS)

  /**
   * Flow of [ImageTextListItemData]s that can be listened to during a Glance session.
   */
  fun data(): Flow<List<ImageTextListItemData>> = data

  /**
   * Reloads items (which due to shuffling) helps mimic a refresh
   */
  suspend fun refresh(context: Context) {
    val showData = Random.nextInt(50) < 5

    items = if (showData) {
        demoItems.take(MAX_ITEMS).shuffled()
    } else {
      listOf()
    }
    this.load(context)
  }

  /**
   * Loads the list of [ImageTextListItemData]s.
   */
  suspend fun load(context: Context): List<ImageTextListItemData> {
    data.value = if (items.isNotEmpty()) {
      processImagesAndBuildData(context, items)
    } else {
      listOf()
    }

    return data.value
  }

  private suspend fun processImagesAndBuildData(
    context: Context,
    items: List<DemoDataItem>,
  ): List<ImageTextListItemData> {
    val maxAllowedBytes = context.getMaxWidgetMemoryAllowedSizeInBytes()
    val maxAllowedBytesPerImage = maxAllowedBytes / items.size
    val imageSizeLimit = ImageUtils.getMaxPossibleImageSize(
      aspectRatio = AspectRatio.Ratio1x1.asDouble(),
      memoryLimitBytes = maxAllowedBytesPerImage,
      maxImages = 1
    )

    val imageSize =
      IMAGE_SIZE.coerceAtMost(imageSizeLimit.width.coerceAtMost(imageSizeLimit.height))

    val mappedItems = runBlocking {
      items.map { item ->
        async(Dispatchers.IO) {
          var bitmap: Bitmap? = null

          val result = ImageLoader(context).execute(
            ImageRequest.Builder(context)
              .data(item.supportingImageUrl)
              .size(imageSize, imageSize)
              .networkCachePolicy(CachePolicy.ENABLED)
              .target { res: Drawable ->
                bitmap = (res as BitmapDrawable).bitmap
              }.build()
          )

          if (result is ErrorResult) {
            Log.e(TAG, "Failed to load the image:", result.throwable)
          }

          return@async ImageTextListItemData(
            key = item.key,
            title = item.title,
            supportingText = item.supportingText,
            supportingImageBitmap = bitmap,
            // In this example we are using same icon, but could potentially be different.
            trailingIconButton = R.drawable.sample_heart_icon,
            trailingIconButtonContentDescription = "Like"
          )
        }
      }.awaitAll()
    }

    return mappedItems
  }

  private data class DemoDataItem(
    val key: String,
    val title: String,
    val supportingText: String,
    val supportingImageUrl: String,
    @DrawableRes val trailingIconButton: Int? = null,
    val trailingIconButtonContentDescription: String? = null,
  )

  companion object {
    // Default size to load (that will be capped by the max allowed bytes in remote views)
    private const val IMAGE_SIZE = 200
    private const val MAX_ITEMS = 10
    private const val TAG = "FITLDR"

      // Courtesy of https://unsplash.com/@iamliam
      private val demoItems = listOf(
          DemoDataItem(
              key = "1",
              title = "Flowers at a wedding reception",
              supportingText = "33,822 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1531306760863-7fb02a41db12"
          ),
          DemoDataItem(
              key = "2",
              title = "An up-close look at a Blushing Bride Protea flower.",
              supportingText = "31,072 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1566964423430-3e52903303a5",
          ),
          DemoDataItem(
              key = "3",
              supportingImageUrl = "https://images.unsplash.com/photo-1685540466252-8c21e7c37624",
              title = "A single water droplet rests in a budding red pansy.",
              supportingText = "193 views",
          ),
          DemoDataItem(
              key = "4",
              title = "Blossom, petal, flower",
              supportingText = "23,815 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1582817954171-c3533fffde89",
          ),
          DemoDataItem(
              key = "5",
              title = "Orchids at New York Botanical Garden",
              supportingText = "205,481 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1565357153781-98bf8686488a",
          ),
          DemoDataItem(
              key = "6",
              title = "Tabletop composition with flower",
              supportingText = "85,060 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1591404789216-d03646c78f73",
          ),
          DemoDataItem(
              key = "7",
              title = "Wild bee on flower",
              supportingText = "6,692 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1653927050791-5fc981435d12",
          ),
          DemoDataItem(
              key = "8",
              title = "Flowers on a vine",
              supportingText = "31,862 views",
              supportingImageUrl = "https://images.unsplash.com/photo-1531307119710-accdb402fe03",
          ),
      )

    private val repositories = mutableMapOf<GlanceId, FakeImageTextListDataRepository>()

    /**
     * Returns the repository instance for the given widget represented by [glanceId].
     */
    fun getImageTextListDataRepo(glanceId: GlanceId): FakeImageTextListDataRepository {
      return synchronized(repositories) {
        repositories.computeIfAbsentExt(glanceId) { FakeImageTextListDataRepository() }!!
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
  }
}
