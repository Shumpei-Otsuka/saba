package com.so.saba

import android.content.Context
import androidx.room.*


//Databaseの定義
@Database(entities = arrayOf(TimeScheduleTable::class), version = 1, exportSchema = false)
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
    @ColumnInfo(name = "direction") val direction: String,
    @ColumnInfo(name = "dayType") val dayType: String,
    @ColumnInfo(name = "csv") val csv: String,
    @ColumnInfo(name = "last_update") val last_update: String,
    @ColumnInfo(name = "error_code") val error_code: Int
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
    @Query("SELECT DISTINCT direction FROM TimeScheduleTable WHERE station = :station AND line = :line")
    //駅名・路線から方向のリストを取得
    fun getDirectionByStationLines(station: String,line:String): List<String>

    @Query("SELECT DISTINCT csv FROM TimeScheduleTable WHERE station = :station AND line = :line AND direction= :direction AND dayType = :dayType")
    //駅名・路線・方向・曜日タイプから時刻表のレコードを取得
    fun getCsvnameByInfo(station: String,line: String?,direction: String,dayType: String): String

    @Query("SELECT DISTINCT error_code FROM TimeScheduleTable WHERE station = :station AND line = :line AND direction= :direction AND dayType = :dayType")
    //駅名・路線・方向・曜日タイプから時刻表のエラーコードを取得
    fun getErrorCodeByInfo(station: String,line: String?,direction: String,dayType: String): Int

    @Query("DELETE FROM TimeScheduleTable")
    //時刻表のレコードをすべてデータベースから削除
    fun deleteTimeScheduleTable()

    @Query("INSERT INTO TimeScheduleTable(station,line,direction,dayType,csv,last_update,error_code) VALUES (:station,:line,:direction,:dayType,:csv,:last_update,:error_code)")
    //時刻表のレコードを１件追加
    fun insertTimeScheduleTable(station: String,line: String?,direction: String,dayType: String, csv:String, last_update: String, error_code:Int)
}

