package com.example.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ClassicWidgetSyncReceiver : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        val periodicRequest = PeriodicWorkRequestBuilder<SyncWidgetWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "widget_periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest,
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_SYNC) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWidgetWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget_action_sync",
                ExistingWorkPolicy.KEEP,
                oneTimeRequest,
            )
        }
    }

    companion object {
        const val ACTION_WIDGET_SYNC = "com.example.app.widget.ACTION_WIDGET_SYNC"
    }
}

fun updateClassicWidgetImage(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    safeBitmap: Bitmap,
) {
    val views = RemoteViews(context.packageName, android.R.layout.activity_list_item).apply {
        setTextViewText(android.R.id.text1, "Updated")
        setImageViewBitmap(android.R.id.icon, safeBitmap)
    }
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
