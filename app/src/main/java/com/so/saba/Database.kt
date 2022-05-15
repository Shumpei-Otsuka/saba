package com.so.saba

import android.content.Context
import androidx.room.*


//Databaseの定義
@Database(entities = arrayOf(TimeScheduleTable::class), version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun TimeScheduleTableDao(): TimeScheduleTableDao

    companion object {
        val DB_NAME = "application.db"
        private lateinit var instance: AppDatabase

        //instanceの取得
        fun getInstance(context: Context): AppDatabase {
            if (!Companion::instance.isInitialized) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration().build()
                //fallbackはmigration書くの面倒で入れているだけなので、本番環境では.buildのみでいい
            }
            return instance
        }
    }
}

//Database内の時刻表テーブル定義
@Entity
data class TimeScheduleTable(
    @PrimaryKey(autoGenerate = true) val id: Int,
    @ColumnInfo(name = "station") val station: String?,
    @ColumnInfo(name = "line") val line: String?,
    @ColumnInfo(name = "destination") val destination: String,
    @ColumnInfo(name = "weekdayPath") val weekdayPath: String,
    @ColumnInfo(name = "holidayPath") val holidayPath: String,
    //@ColumnInfo(name = "last_update") val last_update: String,
    //@ColumnInfo(name = "error_code") val error_code: Int
)

@Dao
interface TimeScheduleTableDao {

    @Query("SELECT * FROM TimeScheduleTable WHERE station = :station")
    //指定した駅名の情報をすべて取得
    fun  loadAllByStation(station: String): List<TimeScheduleTable>

    @Query("SELECT DISTINCT line FROM TimeScheduleTable WHERE station = :station")
    //駅名から路線のリストを取得
    fun  getLineByStation(station: String): List<String>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT DISTINCT destination FROM TimeScheduleTable WHERE station = :station AND line = :line")
    //駅名・路線から方向のリストを取得
    fun getDestinationByStationLines(station: String,line:String): List<String>

    @Query("SELECT DISTINCT * FROM TimeScheduleTable WHERE station = :station AND line = :line AND destination= :destination")
    //駅名・路線・方向から時刻表のcsvファイル名を取得
    fun getCsvnameByInfo(station: String,line: String?,destination: String): List<TrainScheduleConfig>

    @Query("DELETE FROM TimeScheduleTable")
    //時刻表のレコードをすべてデータベースから削除
    fun deleteTimeScheduleTable()

    @Query("INSERT INTO TimeScheduleTable(station,line,destination,weekdayPath,holidayPath) VALUES (:station,:line,:destination,:weekdayPath,:holidayPath)")
    //時刻表のレコードを１件追加
    fun insertTimeScheduleTable(station: String,line: String?,destination: String,weekdayPath: String, holidayPath:String)
}

