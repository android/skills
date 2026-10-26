package com.example.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import java.io.File
import kotlin.math.roundToInt

class SampleGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SampleGlanceWidget()
}

class SampleGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    // Use SizeMode.Exact (for glance-canonical-layouts) or SizeMode.Responsive(RESPONSIVE_BUCKETS).
    // In either mode, decoding sharedBitmap before provideContent deduplicates it across sizes.
    override val sizeMode: SizeMode = SizeMode.Responsive(RESPONSIVE_BUCKETS)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val sharedBitmap = decodeDownsampledWidgetBitmap(
            context = context,
            sourceFile = File(context.filesDir, "widget_hero.jpg"),
            distinctImageCount = 1,
            maxWidthDp = 240,
            maxHeightDp = 240,
            config = Bitmap.Config.RGB_565,
        )
        provideContent {
            val revision = currentState<Preferences>()[KEY_REVISION] ?: 0
            val size = LocalSize.current
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(8.dp)
                ) {
                    Text(text = "Updated #$revision (${size.width.value.roundToInt()}x${size.height.value.roundToInt()}dp)")
                    if (sharedBitmap != null) {
                        Image(
                            provider = ImageProvider(sharedBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxWidth().height(96.dp),
                        )
                    }
                    Button(text = "Refresh", onClick = actionRunCallback<RefreshWidgetAction>())
                }
            }
        }
    }

    override fun onCompositionError(
        context: Context,
        glanceId: GlanceId,
        appWidgetId: Int,
        throwable: Throwable,
    ) {
        val fallback = RemoteViews(context.packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Can't show widget content")
        }
        AppWidgetManager.getInstance(context).updateAppWidget(appWidgetId, fallback)
    }

    companion object {
        val KEY_REVISION = intPreferencesKey("widget_revision")
        private val RESPONSIVE_BUCKETS = setOf(
            DpSize(120.dp, 120.dp),
            DpSize(240.dp, 120.dp),
            DpSize(240.dp, 240.dp),
        )
    }
}
