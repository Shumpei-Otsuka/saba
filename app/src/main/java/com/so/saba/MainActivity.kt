package com.so.saba

import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.coroutines.CoroutineContext


const val EXTRA_MESSAGE = "com.so.saba.MESSAGE"

const val TRAIN_SCHEDULE_CONFIG = "com.so.saba.TRAIN_SCHEDULE_CONFIG"

private val TAG: String = MainActivity::class.java.simpleName

class MainActivity : AppCompatActivity(), CoroutineScope{
    override val coroutineContext: CoroutineContext //DBアクセス用にコルーチンの準備
        get() = Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var stationList = listOf<String>()//autoComplete用の駅名リスト

        //--DB用の初期化処理--
        val db = AppDatabase.getInstance(applicationContext)//DB用のインスタンスを取得
        launch { //DBアクセスのため別のコルーチンをlaunch
            withContext(Dispatchers.IO) {
                db.TimeScheduleTableDao().deleteTimeScheduleTable()  //DBのテーブルを空にする
                val inputTrainSchList = inputTrainScheduleConfigFile() //DBの初期値をassets配下のファイルから取得
                insertListToDB(inputTrainSchList, db)  //初期化したDBのテーブルに初期値を代入
                stationList = db.TimeScheduleTableDao().getAllStation()
                setAutoCompletelist(stationList)

            }
        }
        //--DB用の初期化処理終了--


        /** Called when the user taps the button */
        //xml上ではDB処理用の関数を呼べないため、Activity(kotlin)側でonClickをoverride
        findViewById<Button>(R.id.stationButton).setOnClickListener(object : View.OnClickListener {
            //駅名から路線を検索をするボタンの処理
            override fun onClick(v: View) {
                if(findViewById<EditText>(R.id.registeredStation).text != null){
                    launch{
                        updateLineSpinnerList(db)
                    }
                }
            }
        })
        findViewById<Button>(R.id.directionButton).setOnClickListener(object : View.OnClickListener {
            //駅名・路線から路線を検索をするボタンの処理
            override fun onClick(v: View) {
                if(findViewById<Spinner>(R.id.routespinner).selectedItem != null){
                    launch{
                        updateDestinationSpinnerList(db)
                    }
                }
            }
        })
        findViewById<Button>(R.id.registButton).setOnClickListener(object : View.OnClickListener {
            //csv名をDatabaseから検索し、次ページへの遷移するボタンの処理
            override fun onClick(v: View) {
                if(findViewById<Spinner>(R.id.destinationSpinner).selectedItem != null){
                    launch{
                        sendMessage(db)
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.option, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val intent = Intent(this, Tutorial::class.java)
        startActivity(intent)
        return super.onOptionsItemSelected(item)
    }

    suspend fun setAutoCompletelist(stationList : List<String>){
        //autoCompleteをテキストに適用
        this.runOnUiThread(java.lang.Runnable {
            val adapter: ArrayAdapter<String> = ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, stationList)
            val textView = findViewById<View>(R.id.registeredStation) as AutoCompleteTextView
            textView.setAdapter(adapter)
        })
    }

    fun sendMessage(db:AppDatabase) {
        Log.d(TAG, "sendMessage called.")
        // TODO: Example, must be replace
        //入力されている駅名・路線・方向を取得
        val station = findViewById<EditText>(R.id.registeredStation).text.toString()
        val line = findViewById<Spinner>(R.id.routespinner).selectedItem.toString()
        val destination = findViewById<Spinner>(R.id.destinationSpinner).selectedItem.toString()

        //csv名をDatabaseより取得
        var weekdayPath = ""
        var holidayPath = ""
        var csvNamePair = listOf<TrainScheduleConfig>()
        GlobalScope.launch{
            csvNamePair = getCsvNamePair(db)
            weekdayPath = csvNamePair[0].weekdayPath
            holidayPath = csvNamePair[0].holidayPath
        }
        while(weekdayPath == ""){
            Thread.sleep(10)
        }
        val trainScheduleConfig = TrainScheduleConfig(station, line, destination, weekdayPath, holidayPath)
        val intent = Intent(this, DisplayTrainScheduleActivity::class.java).apply {
            putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
        }
        //intent.putExtra(EXTRA_MESSAGE, message) // Example
        startActivity(intent)
    }

    fun toManagement(view: View) {
        val intent = Intent(this, ManagementTrainScheduleConfigs::class.java)
        startActivity(intent)
    }

    suspend fun updateLineSpinnerList(db:AppDatabase){
        val stationName = findViewById<EditText>(R.id.registeredStation).text.toString()
        val routeList:List<String> = getLineFromDB(stationName, db)
        val routeSpinner = findViewById<Spinner>(R.id.routespinner)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            routeList
        )
        runOnUiThread(java.lang.Runnable {routeSpinner.adapter = adapter})
    }
    suspend fun updateDestinationSpinnerList(db:AppDatabase){
        val stationName = findViewById<EditText>(R.id.registeredStation).text.toString()
        val lineName = findViewById<Spinner>(R.id.routespinner).selectedItem.toString()
        val destinationList = getDestinationFromDB(stationName, lineName, db)
        val destinationSpinner = findViewById<Spinner>(R.id.destinationSpinner)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            destinationList
        )
        runOnUiThread(java.lang.Runnable {destinationSpinner.adapter = adapter})
    }

    suspend fun getCsvNamePair(db:AppDatabase): List<TrainScheduleConfig> {
        val stationName = findViewById<EditText>(R.id.registeredStation).text.toString()
        val lineName = findViewById<Spinner>(R.id.routespinner).selectedItem.toString()
        val destinationSpinner = findViewById<Spinner>(R.id.destinationSpinner).selectedItem.toString()
        val csvNamePair = getCsvNameFromDB(stationName,lineName,destinationSpinner,db)
        return csvNamePair
    }


    private fun inputTrainScheduleConfigFile():MutableList<TrainScheduleConfig>{
        //TrainScheduleConfigのデータをアセットのファイルから読み込んでリストにして返す
        val assetManager: AssetManager = resources.assets
        val fileInputStream  = assetManager.open("TrainScheduleConfig.csv")
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

    private suspend fun getLineFromDB(stationName :String , db :AppDatabase): List<String> {
        val returnList : List<String>
        withContext(Dispatchers.IO){
            returnList = db.TimeScheduleTableDao().getLineByStation(stationName)
        }
        return returnList
    }

    private suspend fun getDestinationFromDB(stationName :String , routeName :String , db :AppDatabase): List<String> {
        val returnList : List<String>
        withContext(Dispatchers.IO){
            returnList = db.TimeScheduleTableDao().getDestinationByStationLines(stationName,routeName)
        }
        return returnList
    }

    private suspend fun getCsvNameFromDB(stationName :String , lineName :String , DestinationName :String , db :AppDatabase): List<TrainScheduleConfig> {
        val returnList : List<TrainScheduleConfig>
        withContext(Dispatchers.IO){
            returnList = db.TimeScheduleTableDao().getCsvnameByInfo(stationName,lineName,DestinationName)
        }
        return returnList
    }
}
