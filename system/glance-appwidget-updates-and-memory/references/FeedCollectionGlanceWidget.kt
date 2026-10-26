package com.example.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import java.io.File
import kotlin.math.max

data class FeedItem(val id: String, val title: String, val thumbnailFile: File)

class FeedCollectionGlanceWidget(
    private val feedItems: List<FeedItem> = emptyList(),
) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        var cachedRevision = -1
        var cachedThumbnails = emptyMap<String, Bitmap>()
        provideContent {
            val revision = currentState<Preferences>()[SampleGlanceWidget.KEY_REVISION] ?: 0
            if (revision != cachedRevision) {
                cachedRevision = revision
                cachedThumbnails = feedItems.mapNotNull { item ->
                    decodeDownsampledWidgetBitmap(
                        context = context,
                        sourceFile = item.thumbnailFile,
                        distinctImageCount = max(1, feedItems.size),
                        maxWidthDp = 64,
                        maxHeightDp = 64,
                        config = Bitmap.Config.RGB_565,
                    )?.let { item.id to it }
                }.toMap()
            }
            LazyColumn {
                // Prefer a persistent numeric ID; String.hashCode().toLong() widens a 32-bit hash.
                items(items = feedItems, itemId = { item -> item.id.hashCode().toLong() }) { item ->
                    val bitmap = cachedThumbnails[item.id]
                    if (bitmap != null) {
                        Image(provider = ImageProvider(bitmap), contentDescription = item.title)
                    } // If null, omit the Image; never fall back to Uri.fromFile(...) in an else branch.
                }
            }
        }
    }
}
