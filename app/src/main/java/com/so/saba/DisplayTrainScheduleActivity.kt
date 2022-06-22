package com.so.saba

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson


const val ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG = "com.so.saba.action.ACTION_SET_WIDGET_TRAIN_SCHEDULE_CONFIG"
const val ACTION_SERVICE_START = "com.so.saba.action.SERVICE_START"
const val ACTION_SERVICE_STOP = "com.so.saba.action.SERVICE_STOP"

private val TAG: String = DisplayTrainScheduleActivity::class.java.simpleName

class DisplayTrainScheduleActivity : AppCompatActivity() {

    var trainScheduleConfig = TrainScheduleConfig()
    var trainSchedule = TrainSchedule(trainScheduleConfig)
    var trainSchedules = TrainSchedules()

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
        /*
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        val gson = Gson()
        val trainScheduleConfigsJson = sharedPref.getString("TrainScheduleConfigs", "Failed")
        Log.d(TAG, trainScheduleConfigsJson.toString())
         */
        trainSchedules.loadTrainScheduleConfigs(this, resources)
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
        intentStartService.putExtra(TRAIN_SCHEDULE_CONFIGS, trainSchedules.trainScheduleConfigs)
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

    fun save(view: View) {
        /*
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        /*with (sharedPref.edit()) {
            putInt("test", 100)
            apply()
        }
         */
        with (sharedPref.edit()) {
            val gson = Gson()
            val trainScheduleConfigsJson = gson.toJson(trainSchedules.trainScheduleConfigs)
            putString("TrainScheduleConfigs", trainScheduleConfigsJson)
            Log.d(TAG, "save called")
            Log.d(TAG, trainScheduleConfigsJson)
            putString("station", trainScheduleConfig.station)
            apply()
        }
         */
        trainSchedules.saveTrainScheduleConfigs(this)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun load(view: View) {
        /*
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return
        /*
        val defaultValue = 200
        val highScore = sharedPref.getInt("test", defaultValue)
         */
        val gson = Gson()
        val trainScheduleConfigsJson = sharedPref.getString("TrainScheduleConfigs", "Failed")
        Log.d(TAG, "load called")
        Log.d(TAG, trainScheduleConfigsJson.toString())
        trainSchedules.trainScheduleConfigs = gson.fromJson(trainScheduleConfigsJson, TrainScheduleConfigs::class.java)
         */
        trainSchedules.loadTrainScheduleConfigs(this, resources)
        val trains = trainSchedules.getNext3TrainsPrimary()
        val trainNext = trains[0]
        val remainTime = trainSchedules.trainSchedules[0].calcRemainTime(trainNext)
        val text = "%s　%02d:%02d　あと %3d分 %2d秒".format(trainNext.train_type, trainNext.hour, trainNext.minute, remainTime.remainMinute, remainTime.remainSecond)
        Log.d(TAG, text)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun add(view: View) {
        if (trainSchedules.trainSchedules.size < 10) {
            trainSchedules.add(resources, trainScheduleConfig)
            Log.d(TAG, "add trainScheduleConfig")
            trainSchedules.saveTrainScheduleConfigs(this)
            val intent = Intent(this, ManagementTrainScheduleConfigs::class.java)
            startActivity(intent)
        }
        else {
            Toast.makeText(applicationContext , "時刻表を追加できませんでした。\n不要な時刻表を削除してください。", Toast.LENGTH_LONG).show()
            val intent = Intent(this, ManagementTrainScheduleConfigs::class.java)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun remove(view: View) {
        val lastIndex = trainSchedules.trainScheduleConfigs.value.size - 1
        trainSchedules.delete(resources, lastIndex)
        Log.d(TAG, "delete trainScheduleConfig")
    }
}
