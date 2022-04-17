package com.so.saba

import android.app.IntentService
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

private val TAG = "IntentServiceRestarter"

class IntentServiceRestarter : IntentService("IntentServiceRestarter") {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_SERVICE_RESTART -> {
                Log.d(TAG, "Service Restart called.")
                val trainScheduleConfig = intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIG) as TrainScheduleConfig
                // start IntentService
                val intentStartService: Intent = Intent(this, com.so.saba.IntentService::class.java)
                intentStartService.apply {action = ACTION_SERVICE_START}
                intentStartService.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
                startForegroundService(intentStartService)
                // TODO: need startForeground?
            }
        }
    }
}