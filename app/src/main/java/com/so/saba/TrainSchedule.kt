package com.so.saba

import android.content.res.AssetManager
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable
import java.time.LocalDateTime

data class TrainScheduleConfig(
    var station: String = "",
    var line: String = "",
    var destination: String = "",
    var weekdayPath: String = "",
    var holidayPath: String = ""
): Serializable

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