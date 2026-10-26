package com.example.app.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

// Calling WindowManager from a non-UI widget or worker Context logs a nonfatal
// StrictMode violation on API 30+, which is expected until a public widget cap API exists.
fun calculateWidgetMemoryCapBytes(context: Context): Long {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
        if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
            return 6L * bounds.width().toLong() * bounds.height().toLong()
        }
    }
    val dm = context.resources.displayMetrics
    return 6L * dm.widthPixels.toLong() * dm.heightPixels.toLong()
}

fun decodeDownsampledWidgetBitmap(
    context: Context,
    sourceFile: File,
    distinctImageCount: Int = 1,
    maxWidthDp: Int = 240,
    maxHeightDp: Int = 240,
    config: Bitmap.Config = Bitmap.Config.RGB_565,
): Bitmap? {
    if (!sourceFile.exists()) return null
    val density = context.resources.displayMetrics.density
    val targetWidthPx = max(1, (maxWidthDp * density).toInt())
    val targetHeightPx = max(1, (maxHeightDp * density).toInt())
    val requestedBytesPerPixel = when (config) {
        Bitmap.Config.ALPHA_8 -> 1L
        Bitmap.Config.RGB_565 -> 2L
        else -> 4L
    }
    val perImageCapBytes = calculateWidgetMemoryCapBytes(context) / max(1, distinctImageCount)
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        inPreferredConfig = config
    }
    BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
    val bytesPerPixel = if (bounds.outConfig != null && bounds.outConfig != config) {
        4L
    } else {
        requestedBytesPerPixel
    }
    var sampleSize = 1
    while (
        (bounds.outWidth / (sampleSize * 2) >= targetWidthPx &&
            bounds.outHeight / (sampleSize * 2) >= targetHeightPx) ||
            (bounds.outWidth / sampleSize > targetWidthPx * 2) ||
            (bounds.outHeight / sampleSize > targetHeightPx * 2)
    ) {
        sampleSize *= 2
    }
    while ((max(1, bounds.outWidth / sampleSize).toLong() * max(1, bounds.outHeight / sampleSize).toLong() * bytesPerPixel) > perImageCapBytes) {
        sampleSize *= 2
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = config
    }
    return BitmapFactory.decodeFile(sourceFile.absolutePath, opts)
}

fun cacheWidgetImageUri(
    context: Context,
    sourceFile: File,
    cacheFileName: String,
    cacheSubdir: String = "widget_images",
    distinctImageCount: Int = 1,
    maxWidthDp: Int = 240,
    maxHeightDp: Int = 240,
    config: Bitmap.Config = Bitmap.Config.RGB_565,
): Uri {
    val sampled = checkNotNull(
        decodeDownsampledWidgetBitmap(context, sourceFile, distinctImageCount, maxWidthDp, maxHeightDp, config)
    )
    val outFile = File(File(context.cacheDir, cacheSubdir).apply { mkdirs() }, cacheFileName)
    FileOutputStream(outFile).use { out ->
        val fmt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        sampled.compress(fmt, 85, out)
    }
    sampled.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
    val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.packageManager.queryIntentActivities(
            homeIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
        )
    } else {
        @Suppress("DEPRECATION")
        context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    for (info in resolveInfos) {
        val pkg = info.activityInfo?.packageName ?: continue
        context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return uri
}
