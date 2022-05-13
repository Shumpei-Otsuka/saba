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

data class TrainData(
    val hour: Int,
    val minute: Int
)

data class RemainTimeString(
    val remainMinute: String,
    val remainSecond: String
)

class TrainSchedule(val trainScheduleConfig: TrainScheduleConfig) {
    var trainsToday = arrayOf<TrainData>()
    var trainsTomorrow = arrayOf<TrainData>()
    var trainsWeekday = arrayOf<TrainData>()
    var trainsHoliday = arrayOf<TrainData>()

    // TODO: Remove this
    fun loadTestTrains(){
        //for debug
        trainsWeekday = arrayOf<TrainData>(TrainData(0, 0), TrainData(1, 10))
        trainsHoliday = arrayOf<TrainData>(TrainData(2, 22), TrainData(3, 33))
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun readCSV(path: String, assetManager: AssetManager): Array<TrainData> {
        //read csv file and return arrayOf<TrainData>(N) //N = number of train
        var trains = arrayOf<TrainData>()
        val fileInputStream  = assetManager.open(path)
        val reader = BufferedReader(InputStreamReader(fileInputStream, "UTF-8"))
        var line = reader.readLine()
        val columns = line.split(",")
        while(true) {
            line = reader.readLine()
            if (line == null) {
                break
            }
            var row = line.split(",")
            var train = TrainData(row[0].toInt(), row[1].toInt())
            trains += train
        }
        return trains
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTrains(assetManager: AssetManager) {
        // load trainsWeekday and trainsHoliday
        trainsWeekday = readCSV(trainScheduleConfig.weekdayPath, assetManager)
        trainsHoliday = readCSV(trainScheduleConfig.holidayPath, assetManager)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTrainsByDaytype(){
        val now: LocalDateTime = LocalDateTime.now()
        when(now.dayOfWeek.value){
            // TODO: National holiday
            5 -> {
                //FRIDAY
                trainsToday = trainsWeekday
                trainsTomorrow = trainsHoliday
            }
            6 -> {
                //SATURDAY
                trainsToday = trainsHoliday
                trainsTomorrow = trainsHoliday
            }
            7 -> {
                //SUNDAY
                trainsToday = trainsHoliday
                trainsTomorrow = trainsWeekday
            }
            else -> {
                //MONDAY to THURSDAY
                trainsToday = trainsWeekday
                trainsTomorrow = trainsWeekday
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNextTrain(): TrainData {
        val now: LocalDateTime = LocalDateTime.now()
        //total seconds from 00:00:00
        val totalSecNow = now.hour*60*60 + now.minute*60 + now.second

        //init values
        var dtBest = 24*60*60*2 // = 2 Days
        var nextTrain = TrainData(-1, -1)
        //get best train today
        for (train in trainsToday) {
            var totalSecTrain = train.hour*60*60 + train.minute*60
            var dt = totalSecTrain - totalSecNow
            if((dt > 0) and (dt < dtBest)){
                nextTrain = train
                dtBest = dt
            }
        }
        //get best train tomorrow
        for (train in trainsTomorrow) {
            var totalSecTrain = train.hour*60*60 + train.minute*60 + 24*60*60
            var dt = totalSecTrain - totalSecNow
            if((dt > 0) and (dt < dtBest)){
                nextTrain = train
                dtBest = dt
            }
        }
        return nextTrain
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNext3Trains(): List<TrainData> {
        val now: LocalDateTime = LocalDateTime.now()
        //total seconds from 00:00:00
        val totalSecNow = now.hour*60*60 + now.minute*60 + now.second
        // calc remain time
        var deltaTimes = arrayOf<Int>()
        for (train in trainsToday) {
            var totalSecTrain = train.hour*60*60 + train.minute*60
            var dt = totalSecTrain - totalSecNow
            deltaTimes += dt
        }
        for (train in trainsTomorrow) {
            var totalSecTrain = train.hour*60*60 + train.minute*60 + 24*60*60
            var dt = totalSecTrain - totalSecNow
            deltaTimes += dt
        }
        // sort and query next 3 trains
        val trains = trainsToday + trainsTomorrow
        //sort by remain time
        val indicesSorted = deltaTimes.withIndex().sortedBy{it.value}.map{it.index}
        val deltaTimesSorted = deltaTimes.sliceArray(indicesSorted)
        // query remain time > 0
        val filter = deltaTimesSorted.withIndex().filter{it.value > 0}.map{it.index}
        val indicesSortedFiltered = indicesSorted.slice(filter)
        // get trains
        val next3Trains = trains.slice(indicesSortedFiltered).slice(0..2)
        return next3Trains
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calcRemainTime(train: TrainData): RemainTimeString {
        val now: LocalDateTime = LocalDateTime.now()
        val totalSecNow = now.hour * 60 * 60 + now.minute * 60 + now.second
        val totalSecTrain = train.hour * 60 * 60 + train.minute * 60
        var dt = totalSecTrain - totalSecNow
        if (dt < 0) {
            // set tomorrow
            dt += 24 * 60 * 60
        }
        val dtMinute = dt / 60
        val dtSecond = dt % 60
        return RemainTimeString(dtMinute.toString(), dtSecond.toString())
    }

    fun getTableFormatString(start: Int, end: Int): Array<String> {
        val trains = trainsWeekday
        var texts = arrayOf<String>()
        for (hour in start..end) {
            var text = ""
            for (train in trains) {
                if(train.hour == hour) {
                    text += train.minute.toString() + " "
                }
            }
            texts += text
        }
        return texts
    }
}

