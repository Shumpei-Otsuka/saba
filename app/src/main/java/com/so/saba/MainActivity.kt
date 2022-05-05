package com.so.saba

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.Serializable
import android.content.res.AssetManager
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

const val EXTRA_MESSAGE = "com.so.saba.MESSAGE"

const val TRAIN_SCHEDULE_CONFIG = "com.so.saba.TRAIN_SCHEDULE_CONFIG"

private val TAG: String = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity() ,CoroutineScope{
    override val coroutineContext: CoroutineContext //DBアクセス用にコルーチンの準備
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //--DB用の初期化処理--
        val db = AppDatabase.getInstance(applicationContext)//DB用のインスタンスを取得
        launch { //DBアクセスのため別のコルーチンをlaunch
            withContext(Dispatchers.IO) {
                db.TimeScheduleTableDao().deleteTimeScheduleTable()  //DBのテーブルを空にする
                val inputTrainSchList = inputTrainScheduleConfigFile() //DBの初期値をassets配下のファイルから取得
                insertListToDB(inputTrainSchList, db)  //初期化したDBのテーブルに初期値を代入
            }
        }
        //--DB用の初期化処理--
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
    private fun inputTrainScheduleConfigFile():MutableList<TrainScheduleConfig>{
        //TrainScheduleConfigのデータをアセットのファイルから読み込んでリストにして返す
        val assetManager: AssetManager = resources.assets
        val fileInputStream  = assetManager.open("exampleTrainScheduleConfig.csv")
        val reader = BufferedReader(InputStreamReader(fileInputStream, "UTF-8"))
        reader.readLine()
        var lineBuffer = ""
        val stationList : ArrayList<String> = arrayListOf()
        var k = 0
        //assetsのファイルを１行ごとに読み込んで変数に代入
        while (lineBuffer != null) {
            val tempLine = reader.readLine()
            if(tempLine != null){
                lineBuffer = tempLine
            }else{
                lineBuffer = "end"
            }
            if (lineBuffer != "end") {
                stationList.add(lineBuffer) //１行で読み込まれる
                k++
            } else {
                break
            }
        }

        val returnList = mutableListOf<TrainScheduleConfig>()
        var j = 0
        while (j < stationList.size) {
            val temp = stationList[j].split(",")
            val tempTrainSch = TrainScheduleConfig(temp[0],temp[1],temp[2],temp[3],temp[4])
            returnList.add(tempTrainSch)
            j++
        }
        return returnList
    }

     private suspend fun insertListToDB(list: MutableList<TrainScheduleConfig>, db :AppDatabase){
         var i = 0
         withContext(Dispatchers.IO){
            while(i < list.size){
                db.TimeScheduleTableDao().insertTimeScheduleTable(
                    list[i].station, list[i].line, list[i].destination, list[i].weekdayPath, list[i].holidayPath
                )
                i++
            }
        }
    }
}

data class TrainScheduleConfig(
    var station: String = "",
    var line: String = "",
    var destination: String = "",
    var weekdayPath: String = "",
    var holidayPath: String = ""
): Serializable