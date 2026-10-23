package com.example.platform.ui.appwidgets.glance.layout.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ImageUtils {

  // Default 1.5 MB per-image ceiling when callers do not supply a custom maxBytesPerImage.
  private const val DEFAULT_MAX_BYTES_PER_IMAGE = 1_500_000

  /**
   * Loads the image resource as bitmap by scaling for the requested size and enforcing
   * the per-image AppWidget memory ceiling.
   * https://developer.android.com/topic/performance/graphics/load-bitmap#load-bitmap
   */
  fun decodeSampledBitmapFromResource(
    res: Resources,
    resId: Int,
    requestedWidth: Int,
    requestedHeight: Int,
    preferredConfig: Bitmap.Config = Bitmap.Config.RGB_565,
    maxBytesPerImage: Int = DEFAULT_MAX_BYTES_PER_IMAGE,
  ): Bitmap {
    val bytesPerPixel = when (preferredConfig) {
      Bitmap.Config.ALPHA_8 -> 1
      Bitmap.Config.RGB_565 -> 2
      Bitmap.Config.RGBA_F16 -> 8
      else -> 4
    }

    fun calculateInSampleSize(
      originalSize: Size,
      requestedSize: Size,
      effectiveBytesPerPixel: Int = bytesPerPixel,
    ): Int {
      val (height: Int, width: Int) = originalSize.height to originalSize.width
      val reqHeight = max(1, requestedSize.height)
      val reqWidth = max(1, requestedSize.width)
      var inSampleSize = 1
      // Stage 1: Downsample to the highest quality the display can render for the viewable size,
      // and also halve whenever either dimension exceeds 2x the requested dimension (so wide
      // banners or tall portraits do not bypass downsampling when one axis already fits).
      while (
        (height / (inSampleSize * 2) >= reqHeight &&
          width / (inSampleSize * 2) >= reqWidth) ||
          (height / inSampleSize > reqHeight * 2) ||
          (width / inSampleSize > reqWidth * 2)
      ) {
        inSampleSize *= 2
      }
      // Stage 2: Enforce the exact per-image OS widget memory ceiling.
      while (
        (max(1, width / inSampleSize).toLong() *
          max(1, height / inSampleSize).toLong() *
          effectiveBytesPerPixel) > maxBytesPerImage
      ) {
        inSampleSize *= 2
      }
      return inSampleSize
    }

    // First decode with inJustDecodeBounds=true to check dimensions
    return BitmapFactory.Options().run {
      inJustDecodeBounds = true
      inPreferredConfig = preferredConfig
      inScaled = false
      BitmapFactory.decodeResource(res, resId, this)
      val originalWidth: Int = outWidth
      val originalHeight: Int = outHeight
      // If the resource has an alpha channel (e.g. non-opaque PNG/WebP), Android's built-in
      // BitmapFactory ignores inPreferredConfig = RGB_565 and sets outConfig = ARGB_8888 (4 B/px).
      val effectiveBytesPerPixel = when (outConfig ?: preferredConfig) {
        Bitmap.Config.ALPHA_8 -> 1
        Bitmap.Config.RGB_565 -> 2
        Bitmap.Config.RGBA_F16 -> 8
        else -> 4
      }

      inSampleSize =
        calculateInSampleSize(
          originalSize = Size(originalWidth, originalHeight),
          requestedSize = Size(requestedWidth, requestedHeight),
          effectiveBytesPerPixel = effectiveBytesPerPixel,
        )

      // Decode bitmap with inSampleSize and preferredConfig set
      inJustDecodeBounds = false

      BitmapFactory.decodeResource(res, resId, this)
    }
  }

  /**
   * Estimates the maximum bytes allowed per the appwidget limits for bitmaps in the remote views.
   * The system computes 6x the default display's real size once at boot and does not expose it
   * in a public API (b/327416107). Calling WindowManager from a non-UI widget Context logs a
   * nonfatal StrictMode violation on API 30+, which is expected for this estimate. Always size
   * images to the widget's own target dimensions first (Stage 1).
   */
  fun Context.getMaxWidgetMemoryAllowedSizeInBytes(): Int {
    val windowManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      getSystemService(WindowManager::class.java)
    } else {
      null
    }
    val size = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
      val bounds = windowManager.currentWindowMetrics.bounds
      Size(bounds.width(), bounds.height())
    } else {
      resources.displayMetrics.run { Size(widthPixels, heightPixels) }
    }
    // System cap is 1.5 times the size of the display at 4 bytes/pixel (6 * width * height).
    return 6 * size.width * size.height
  }

  /**
   * Returns maximum possible size for each image in the widget when provided as bitmap.
   */
  fun getMaxPossibleImageSize(aspectRatio: Double, memoryLimitBytes: Int, maxImages: Int): Size {
    // for each orientation (landscape, portrait, +2 for fold).
    val limit = (memoryLimitBytes / 4) / max(1, maxImages)
    val maxSizeAllowedPerPixel: Int = limit / BYTES_PER_PIXEL

    val side = sqrt(maxSizeAllowedPerPixel.toDouble()).toInt()
    val width = if (aspectRatio > 1) side else max(1, (side * aspectRatio).roundToInt())
    val height = if (aspectRatio > 1) max(1, (side / aspectRatio).roundToInt()) else side
    return Size(width, height)
  }

  private const val BYTES_PER_PIXEL = 4
}
