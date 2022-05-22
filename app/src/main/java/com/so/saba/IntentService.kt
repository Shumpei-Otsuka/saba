package com.so.saba

import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.Context
import android.content.Intent.ACTION_SCREEN_ON
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.time.format.DateTimeFormatter

private val TAG: String = IntentService::class.java.simpleName

const val ACTION_SERVICE_RESTART = "com.so.saba.action.ACTION_SERVICE_RESTART"

class IntentService : IntentService("IntentService") {
    var destroyFlag = false
    var restartFlag = true
    var trainScheduleConfig = TrainScheduleConfig()

    @RequiresApi(Build.VERSION_CODES.O)
    override  fun onDestroy(){
        Log.d(TAG, "onDestroy called.")
        destroyFlag = true
        //start IntentServiceRestarter
        if(restartFlag == true) {
            var intent: Intent = Intent(this, com.so.saba.IntentServiceRestarter::class.java)
            intent.apply {action = ACTION_SERVICE_RESTART}
            intent.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
            startForegroundService(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "onHandleIntent called.")
        when (intent?.action) {
            ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen On")
                // TODO: Control Service by Screen On for battery　saving.
            }
            ACTION_SERVICE_START -> {
                Log.d(TAG, "Service Started.")
                // load trainSchedule
                val trainScheduleConfig = intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIG) as TrainScheduleConfig
                val trainSchedule = TrainSchedule(trainScheduleConfig)
                trainSchedule.loadTrains(resources.assets)
                // start Foreground
                val notification = makeNotification()
                startForeground(1, notification)
                // TODO: replace to timer?
                while(destroyFlag == false){
                    // update Widget
                    val context = applicationContext
                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, AppWidget::class.java))
                    var views = RemoteViews(context.packageName, R.layout.app_widget)
                    views = updateWidgetViews(views, trainSchedule)
                    for (appWidgetId in appWidgetIds){
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    Thread.sleep(1000)
                }
            }
            ACTION_SERVICE_STOP -> {
                restartFlag = false
                Log.d(TAG, "Service Stop Called.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWidgetViews(views: RemoteViews, trainSchedule: TrainSchedule): RemoteViews {
        trainSchedule.updateTrainsByDaytype()
        // set TrainScheduleConfig
        var row1 = "%s駅　%s　%s".format(trainSchedule.trainScheduleConfig.station,
            trainSchedule.trainScheduleConfig.line,
            trainSchedule.trainScheduleConfig.destination)
        views.setTextViewText(R.id.appwidget_row1, row1)
        // set Next 3 trains
        val trainsNext3 = trainSchedule.getNext3Trains()
        val idsRows = arrayOf(R.id.appwidget_row2, R.id.appwidget_row3, R.id.appwidget_row4)
        val suffix = arrayOf("　先発", "　次発", "次々発")
        for (trainIndex in 0..2) {
            var trainNext = trainsNext3[trainIndex]
            var remainTimeString = trainSchedule.calcRemainTime(trainNext)
            var row = "%s　%5s　%02d:%02d　あと %2s分 %2s秒".format(suffix[trainIndex],
                trainNext.train_type,
                trainNext.hour,
                trainNext.minute,
                remainTimeString.remainMinute,
                remainTimeString.remainSecond)
            views.setTextViewText(idsRows[trainIndex], row)
        }
        return views
    }

    private fun makeNotification(): Notification {
        //利用者への通知をサービス作成から５秒以内に作成（IntentServiceの仕様上必須）
        val id = "SABA_Widget_foreground"
        val name = "SABAの時刻表ウィジェット"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifyDescription = "時刻表のウィジェットが動作中です。"

        @RequiresApi(Build.VERSION_CODES.O)
        if (manager.getNotificationChannel(id) == null) {
            val mChannel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH)
            mChannel.apply {
                description = notifyDescription
            }
            manager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(this, id).apply {
            //mContentTitle = "通知のタイトル"
            //mContentText = "通知の内容"
            setSmallIcon(R.drawable.ic_launcher_background)
        }.build()
        return notification
    }
}