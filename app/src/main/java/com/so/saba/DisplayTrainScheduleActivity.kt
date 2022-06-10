package com.so.saba

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.res.AssetManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDateTime

const val ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG = "com.so.saba.action.ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG"
const val ACTION_SERVICE_START = "com.so.saba.action.SERVICE_START"
const val ACTION_SERVICE_STOP = "com.so.saba.action.SERVICE_STOP"

private val TAG: String = DisplayTrainScheduleActivity::class.java.simpleName

class DisplayTrainScheduleActivity : AppCompatActivity() {

    var trainScheduleConfig = TrainScheduleConfig()
    var trainSchedule = TrainSchedule(trainScheduleConfig)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_train_schedule)
        // Get the Intent that started this activity and extract the trainScheduleConfig
        trainScheduleConfig = intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIG) as TrainScheduleConfig
        //val message = intent.getStringExtra(EXTRA_MESSAGE) //Example
        trainSchedule = TrainSchedule(trainScheduleConfig)
        trainSchedule.loadTrains(resources.assets)
        // update Table Format Train Schedule
        val trainScheduleConfigText = trainScheduleConfig.station + "駅, " + trainScheduleConfig.line + ", " + trainScheduleConfig.destination
        findViewById<TextView>(R.id.trainScheduleConfig).apply {
            text = trainScheduleConfigText
        }
        val tableTexts = trainSchedule.getTableFormatString(start = 0, end = 23)
        updateTable(tableTexts)
    }

    /** Called when the user taps the Send button */
    @RequiresApi(Build.VERSION_CODES.O)
    fun startService(view: View) {
        /*
        val intent = Intent(this, AppWidget::class.java)
        intent.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
        intent.apply {action = ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG}
        // TODO: update Widget station config often fail. Conflict IntentService below.
        sendBroadcast(intent)
        */
        // Stop Old Service
        val intent: Intent = Intent(this, IntentService::class.java)
        intent.apply {action = ACTION_SERVICE_STOP}
        stopService(intent)

        // Start Service
        val intentStartService: Intent = Intent(this, IntentService::class.java)
        intentStartService.apply {action = ACTION_SERVICE_START}
        intentStartService.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
        startForegroundService(intentStartService)
        //startService(intentStartService)
    }

    fun stopService(view: View) {
        val intent: Intent = Intent(this, IntentService::class.java)
        intent.apply {action = ACTION_SERVICE_STOP}
        stopService(intent)
    }

    private fun updateTable(tableTexts: Array<String>) {
        val ids = listOf(R.id.tableRow0, R.id.tableRow1, R.id.tableRow2, R.id.tableRow3, R.id.tableRow4,
            R.id.tableRow5, R.id.tableRow6, R.id.tableRow7, R.id.tableRow8, R.id.tableRow9, R.id.tableRow10,
            R.id.tableRow11, R.id.tableRow12, R.id.tableRow13, R.id.tableRow14, R.id.tableRow15, R.id.tableRow16,
            R.id.tableRow17, R.id.tableRow18, R.id.tableRow19, R.id.tableRow20, R.id.tableRow21, R.id.tableRow22,
            R.id.tableRow23)
        for (i in 0..23) {
            findViewById<TextView>(ids[i]).apply {
                text = tableTexts[i]
            }
        }
    }
}
