package com.so.saba

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable
import java.time.LocalDateTime

private val TAG: String = TrainSchedule::class.java.simpleName

data class TrainScheduleConfig(
    var station: String = "",
    var line: String = "",
    var destination: String = "",
    var weekdayPath: String = "",
    var holidayPath: String = ""
): Serializable

data class TrainScheduleConfigs(
    var value: Array<TrainScheduleConfig>
    ): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TrainScheduleConfigs

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }
}

data class TrainData(
    val hour: Int,
    val minute: Int,
    val destination: String = "",
    val train_type: String = "",
    val note: String = ""
)

data class RemainTime(
    val remainMinute: Int,
    val remainSecond: Int
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
            var train = TrainData(row[0].toInt(), row[1].toInt(), row[2], row[3], row[4])
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
    fun calcRemainTime(train: TrainData): RemainTime {
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
        return RemainTime(dtMinute, dtSecond)
    }

    fun getTableFormatString(start: Int, end: Int): Array<String> {
        val trains = trainsWeekday
        var texts = arrayOf<String>()
        for (hour in start..end) {
            var text = " "
            for (train in trains) {
                if((train.hour == hour)) {
                    if(train.minute < 10){
                        text += "  " + train.minute.toString() + "  "
                    }
                    else{
                        text += train.minute.toString() + "  "
                    }
                }
            }
            texts += text
        }
        return texts
    }
}

class TrainSchedules() {
    var trainScheduleConfigs = TrainScheduleConfigs(arrayOf<TrainScheduleConfig>())
    var trainSchedules = arrayOf<TrainSchedule>()
    var indexPrimary = 0

    @RequiresApi(Build.VERSION_CODES.O)
    fun add(resources: Resources, trainScheduleConfig: TrainScheduleConfig) {
        trainScheduleConfigs.value += trainScheduleConfig
        updateTrainSchedulesFromConfigs(resources)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun delete(resources: Resources, index: Int) {
        val indicesFiltered = trainScheduleConfigs.value.withIndex().filter{it.index != index}.map{it.index}
        trainScheduleConfigs.value = trainScheduleConfigs.value.slice(indicesFiltered).toTypedArray()
        updateTrainSchedulesFromConfigs(resources)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateTrainSchedulesFromConfigs(resources: Resources) {
        trainSchedules = arrayOf<TrainSchedule>()
        var index = 0
        for (trainScheduleConfig in trainScheduleConfigs.value) {
            trainSchedules += TrainSchedule(trainScheduleConfig)
            trainSchedules[index].loadTrains(resources.assets)
            index += 1
        }
    }

    fun saveTrainScheduleConfigs(activity: Activity) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            val gson = Gson()
            val trainScheduleConfigsJson = gson.toJson(trainScheduleConfigs)
            putString("TrainScheduleConfigs", trainScheduleConfigsJson)
            apply()
            Log.d(TAG, "save called")
            Log.d(TAG, trainScheduleConfigsJson)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun loadTrainScheduleConfigs(activity: Activity, resources: Resources) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        val gson = Gson()
        val trainScheduleConfigsJson = sharedPref.getString("TrainScheduleConfigs", "Failed")
        if (trainScheduleConfigsJson.toString() != "Failed") {
            trainScheduleConfigs = gson.fromJson(trainScheduleConfigsJson, TrainScheduleConfigs::class.java)
            updateTrainSchedulesFromConfigs(resources)
        }
        Log.d(TAG, "load called")
        Log.d(TAG, trainScheduleConfigsJson.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNext3TrainsPrimary(): List<TrainData> {
        var trainSchedulePrimary = trainSchedules[indexPrimary]
        trainSchedulePrimary.updateTrainsByDaytype()
        return trainSchedulePrimary.getNext3Trains()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun calcRemainTimePrimary(train: TrainData): RemainTime {
        return trainSchedules[indexPrimary].calcRemainTime(train)
    }
}