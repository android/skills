package com.example.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

suspend fun pushWidgetRevisionUpdate(context: Context) {
    val widget = SampleGlanceWidget()
    val glanceIds = GlanceAppWidgetManager(context).getGlanceIds(SampleGlanceWidget::class.java)
    for (glanceId in glanceIds) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[SampleGlanceWidget.KEY_REVISION] = (prefs[SampleGlanceWidget.KEY_REVISION] ?: 0) + 1
        }
        widget.update(context, glanceId)
    }
}

class SyncWidgetWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        pushWidgetRevisionUpdate(applicationContext)
        return Result.success()
    }
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val itemId = parameters[KEY_ITEM_ID] ?: "default"
        val request = OneTimeWorkRequestBuilder<SyncWidgetWorker>()
            .setInputData(workDataOf("item_id" to itemId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "widget_sync",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        val KEY_ITEM_ID = ActionParameters.Key<String>("item_id")
    }
}
