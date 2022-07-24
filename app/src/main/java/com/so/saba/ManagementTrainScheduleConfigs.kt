package com.so.saba

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi

private val TAG: String = ManagementTrainScheduleConfigs::class.java.simpleName
const val TRAIN_SCHEDULE_CONFIGS = "com.so.saba.action.TRAIN_SCHEDULE_CONFIGS"
const val ACTION_SET_ONCLICK_WIDGET_BUTTON = "com.so.saba.action.ACTION_SET_ONCLICK_WIDGET_BUTTON"


class ManagementTrainScheduleConfigs : AppCompatActivity() {
    var trainSchedules = TrainSchedules()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_management_train_schedule_configs)
        trainSchedules.loadTrainScheduleConfigs(this, resources)
        updateTableLayout()
        //add delete buttons
        findViewById<Button>(R.id.button0).setOnClickListener{delete(0)}
        findViewById<Button>(R.id.button1).setOnClickListener{delete(1)}
        findViewById<Button>(R.id.button2).setOnClickListener{delete(2)}
        findViewById<Button>(R.id.button3).setOnClickListener{delete(3)}
        findViewById<Button>(R.id.button4).setOnClickListener{delete(4)}
        findViewById<Button>(R.id.button5).setOnClickListener{delete(5)}
        findViewById<Button>(R.id.button6).setOnClickListener{delete(6)}
        findViewById<Button>(R.id.button7).setOnClickListener{delete(7)}
        findViewById<Button>(R.id.button8).setOnClickListener{delete(8)}
        findViewById<Button>(R.id.button9).setOnClickListener{delete(9)}
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun startService(view: View) {
        // Stop Old Service
        val intent: Intent = Intent(this, IntentService::class.java)
        intent.apply {action = ACTION_SERVICE_STOP}
        stopService(intent)

        // set onclick widget button
        val intentWidget: Intent = Intent(this, AppWidget::class.java)
        intentWidget.apply {action = ACTION_SET_ONCLICK_WIDGET_BUTTON}
        sendBroadcast(intentWidget)
        // wait for update widget
        Thread.sleep(2000)

        // Start Service
        val intentStartService: Intent = Intent(this, IntentService::class.java)
        intentStartService.apply {action = ACTION_SERVICE_START}
        intentStartService.putExtra(TRAIN_SCHEDULE_CONFIGS, trainSchedules.trainScheduleConfigs)
        startForegroundService(intentStartService)
        //startService(intentStartService)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun stopService(view: View) {
        // Stop Old Service
        val intent: Intent = Intent(this, IntentService::class.java)
        intent.apply {action = ACTION_SERVICE_STOP}
        stopService(intent)
    }

    fun updateTableLayout() {
        trainSchedules.trainScheduleConfigs.value
        val stationIds = arrayOf(R.id.station0, R.id.station1, R.id.station2, R.id.station3, R.id.station4, R.id.station5, R.id.station6, R.id.station7, R.id.station8, R.id.station9)
        val lineIds = arrayOf(R.id.line0, R.id.line1, R.id.line2, R.id.line3, R.id.line4, R.id.line5, R.id.line6, R.id.line7, R.id.line8, R.id.line9)
        val destinationIds = arrayOf(R.id.destination0, R.id.destination1, R.id.destination2, R.id.destination3, R.id.destination4, R.id.destination5, R.id.destination6, R.id.destination7, R.id.destination8, R.id.destination9)
        //reset text
        for (i in 0..9) {
            findViewById<TextView>(stationIds[i]).apply{text = ""}
            findViewById<TextView>(lineIds[i]).apply{text = ""}
            findViewById<TextView>(destinationIds[i]).apply{text = ""}
        }
        for (i in 0..(trainSchedules.trainSchedules.size-1)) {
            findViewById<TextView>(stationIds[i]).apply{text = trainSchedules.trainScheduleConfigs.value[i].station}
            findViewById<TextView>(lineIds[i]).apply{text = trainSchedules.trainScheduleConfigs.value[i].line}
            findViewById<TextView>(destinationIds[i]).apply{text = trainSchedules.trainScheduleConfigs.value[i].destination}
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun delete(index: Int) {
        trainSchedules.delete(resources, index)
        Log.d(TAG, "delete trainScheduleConfig %d".format(index))
        trainSchedules.saveTrainScheduleConfigs(this)
        updateTableLayout()
    }

    fun returnToMain(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}