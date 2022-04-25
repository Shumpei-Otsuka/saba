package com.so.saba

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val TAG: String = AppWidget::class.java.simpleName

/**
 * Implementation of App Widget functionality.
 */
class AppWidget : AppWidgetProvider() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUpdate(
    context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called.")
        // There may be multiple widgets active, so update all of them
        // TODO: Widget updated by IntentService. Remove this code.
        /*
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
         */
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Remove after Debug
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Log.d(TAG, log)
            }
        }
        when (intent?.action) {
            //screen size change detection
            ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                Log.d(TAG, "ACTION_APPWIDGET_OPTIONS_CHANGED")
                // TODO: Auto Text Sizing
            }
            ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG -> {
                // TODO: Remove this.
                /*
                Log.d(TAG, "Set Widget Train Schedule Config.")
                val trainScheduleConfig = intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIG) as TrainScheduleConfig
                // update Widget
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, AppWidget::class.java))
                val views = RemoteViews(context.packageName, R.layout.app_widget)
                for (appWidgetId in appWidgetIds) {
                    updateAppWidgetTrainScheduleConfig(context, appWidgetManager, appWidgetId, views, trainScheduleConfig)
                }
                */
            }
        }
    }

    /* //onReceiveがあると呼び出されない
    override fun onAppWidgetOptionsChanged(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle
    ) {
        val minWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val maxWidth = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
        val minHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val maxHeight = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
        Log.d(TAG, String.format("%d, %d, %d, %d", minWidth, maxWidth, minHeight, maxHeight))
        Log.d(TAG, "size changed.")
    }
    */
}

// TODO: Remove This
/*
@RequiresApi(Build.VERSION_CODES.O)
internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val views = RemoteViews(context.packageName, R.layout.app_widget)
    //views.setTextViewText(R.id.appwidget_text, widgetText)
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}
 */

internal fun updateAppWidgetTrainScheduleConfig(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, views: RemoteViews,trainScheduleConfig: TrainScheduleConfig) {
    //val views = RemoteViews(context.packageName, R.layout.app_widget)
    views.setTextViewText(R.id.appwidget_textStation, trainScheduleConfig.station)
    views.setTextViewText(R.id.appwidget_textLine, trainScheduleConfig.line)
    views.setTextViewText(R.id.appwidget_textDestination, trainScheduleConfig.destination)
    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
    Log.d(TAG, "update Widget Config")
}