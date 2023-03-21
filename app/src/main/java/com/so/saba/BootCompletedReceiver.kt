package com.so.saba

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

private val TAG: String = BootCompletedReceiver::class.java.simpleName

class BootCompletedReceiver : BroadcastReceiver() {
    var trainSchedules = TrainSchedules()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        Log.d(TAG, "Receive : ${intent.action}")
        if (intent.action == ACTION_BOOT_COMPLETED) {
            val resources = context.resources
            trainSchedules.loadTrainScheduleConfigs(context, resources)
            val serviceIntent = Intent(context, WidgetUpdateService::class.java)
            serviceIntent.apply { action = ACTION_WIDGET_UPDATE_SERVICE }
            serviceIntent.putExtra(TRAIN_SCHEDULE_CONFIGS, trainSchedules.trainScheduleConfigs)
            context.startForegroundService(serviceIntent)
        }
    }
}