package com.so.saba

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable

const val EXTRA_MESSAGE = "com.so.saba.MESSAGE"

const val TRAIN_SCHEDULE_CONFIG = "com.so.saba.TRAIN_SCHEDULE_CONFIG"

private val TAG: String = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    /** Called when the user taps the button */
    fun sendMessage(view: View) {
        Log.d(TAG, "sendMessage called.")
        // TODO: Example, must be replace
        val trainScheduleConfig = TrainScheduleConfig("東京", "東海道線", "大阪", "exampleTrainScheduleWeekday.csv", "exampleTrainScheduleHoliday.csv")
        val intent = Intent(this, DisplayTrainScheduleActivity::class.java).apply {
            putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
        }
        //intent.putExtra(EXTRA_MESSAGE, message) // Example
        startActivity(intent)
    }
}

data class TrainScheduleConfig(
    var station: String = "",
    var line: String = "",
    var destination: String = "",
    var weekdayPath: String = "",
    var holidayPath: String = ""
): Serializable