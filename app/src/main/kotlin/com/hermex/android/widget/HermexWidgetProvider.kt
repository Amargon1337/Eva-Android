package com.hermex.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.hermex.android.R
import com.hermex.android.MainActivity
import com.hermex.android.core.notifications.HermexNotificationRoutes

class HermexWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.hermex_widget)

        // Audit P2: mode-reactive widget -- title tint follows the active duality mode.
        // DataStore read must not race the main thread -- hop to IO inside the blocking wait.
        val dualityMode: com.hermex.android.eva.DualityMode = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.hermex.android.core.storage.DataStoreAppearancePreferencesStore(context).loadDualityMode()
            }
        }
        val modeColor = if (dualityMode == com.hermex.android.eva.DualityMode.SAKURA)
            android.graphics.Color.parseColor("#F472B6") else android.graphics.Color.parseColor("#EF4444")
        views.setTextColor(R.id.widget_title, modeColor)
        views.setTextViewText(R.id.widget_title, "EVA // HERMEX · ${dualityMode.badge.uppercase()}")

        // Tap on the root: open sessions
        val sessionsIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse(HermexNotificationRoutes.sessions())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionsPendingIntent = PendingIntent.getActivity(
            context,
            0,
            sessionsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, sessionsPendingIntent)

        // Tap on the new-chat button: create a new session
        val newChatIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("hermex://new-chat")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val newChatPendingIntent = PendingIntent.getActivity(
            context,
            1,
            newChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_new_chat, newChatPendingIntent)

        // Tap on the diary button: open Eva's diary
        val diaryIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("hermex://diary")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val diaryPendingIntent = PendingIntent.getActivity(
            context,
            2,
            diaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_diary, diaryPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}