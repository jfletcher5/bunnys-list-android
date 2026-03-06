package com.bunnyslist.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.bunnyslist.android.MainActivity
import com.bunnyslist.android.R

/**
 * Home screen widget that shows currently needed items and offers quick actions.
 * Data is backed by a RemoteViewsService so the list can scroll.
 */
class NeededItemsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NeededItemsWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            updateAllWidgets(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetIds.isEmpty()) return
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                buildRemoteViews(context, appWidgetId),
            )
        }
        // Trigger a data refresh for the list adapter after rebinding widget views.
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_items_list)
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
    ): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_needed_items)
        remoteViews.setTextViewText(R.id.widget_subtitle, context.getString(R.string.widget_subtitle))

        // Bind scrollable needed-items list with a unique data Uri per widget instance.
        val serviceIntent = Intent(context, NeededItemsWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        remoteViews.setRemoteAdapter(R.id.widget_items_list, serviceIntent)
        remoteViews.setEmptyView(R.id.widget_items_list, R.id.widget_empty_text)

        // Refresh action forces a new Firestore read for this widget.
        val refreshIntent = Intent(context, NeededItemsWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            2001,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        // Add action opens the app and immediately pops the Add Item dialog.
        val addIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_OPEN_ADD_ITEM, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val addPendingIntent = PendingIntent.getActivity(
            context,
            2002,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_add_button, addPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.widget_title, addPendingIntent)
        remoteViews.setOnClickPendingIntent(R.id.widget_logo, addPendingIntent)

        // Optional row tap action: open app and jump to add flow for quick correction/edit.
        remoteViews.setPendingIntentTemplate(R.id.widget_items_list, addPendingIntent)

        return remoteViews
    }

    private companion object {
        const val ACTION_REFRESH = "com.bunnyslist.android.widget.ACTION_REFRESH"
    }
}
