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
const val ACTION_CHANGE_TRAIN_SCHEDULES_PRIMARY = "com.so.saba.action.ACTION_CHANGE_TRAIN_SCHEDULES_PRIMARY"

class IntentService : IntentService("IntentService") {
    var destroyFlag = false
    var restartFlag = true
    var trainScheduleConfig = TrainScheduleConfig()
    var trainSchedules = TrainSchedules()

    @RequiresApi(Build.VERSION_CODES.O)
    override  fun onDestroy(){
        Log.d(TAG, "onDestroy called.")
        destroyFlag = true
        //start IntentServiceRestarter
        if(restartFlag == true) {
            var intent: Intent = Intent(this, com.so.saba.IntentServiceRestarter::class.java)
            intent.apply {action = ACTION_SERVICE_RESTART}
            intent.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
            //startForegroundService(intent)
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
                trainSchedules.trainScheduleConfigs = intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIGS) as TrainScheduleConfigs
                trainSchedules.updateTrainSchedulesFromConfigs(resources)
                // start Foreground
                val notification = makeNotification()
                startForeground(1, notification)
                // TODO: replace to timer?
                while(destroyFlag == false){
                    val context = applicationContext
                    val sharedPref = context.getSharedPreferences("TrainScheduleConfigs", Context.MODE_PRIVATE) ?: return
                    var indexPrimary = sharedPref.getInt("IndexPrimary", 0)
                    if(indexPrimary > trainSchedules.trainSchedules.size - 1) {
                        //invalid old value exists
                        indexPrimary = 0
                    }
                    trainSchedules.indexPrimary = indexPrimary
                    // update Widget
                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, AppWidget::class.java))
                    var views = RemoteViews(context.packageName, R.layout.app_widget)
                    views = updateWidgetViews(views, trainSchedules)
                    for (appWidgetId in appWidgetIds){
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    Thread.sleep(200)
                }
            }
            ACTION_SERVICE_STOP -> {
                restartFlag = false
                Log.d(TAG, "Service Stop Called.")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWidgetViews(views: RemoteViews, trainSchedules: TrainSchedules): RemoteViews {
        // set Next 3 trains
        val trainsNext3 = trainSchedules.getNext3TrainsPrimary()
        val trainScheduleConfig = trainSchedules.trainScheduleConfigs.value[trainSchedules.indexPrimary]

        // set TrainScheduleConfig
        views.setTextViewText(R.id.widgetStation, "%s駅".format(trainScheduleConfig.station))
        views.setTextViewText(R.id.widgetLine, "%s".format(trainScheduleConfig.line))
        views.setTextViewText(R.id.widgetDestination, "%s".format(trainScheduleConfig.destination))
        views.setTextViewText(R.id.widgetNote, trainsNext3[0].note)

        var trainNext = trainsNext3[0]
        var remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination1, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType1,   "%s".format(trainNext.train_type))
        views.setTextViewText(R.id.widgetTime1,        "%02d:%02d".format(trainNext.hour, trainNext.minute))
        views.setTextViewText(R.id.widgetRemain1,      "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond))

        trainNext = trainsNext3[1]
        remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination2, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType2,   "%s".format(trainNext.train_type))
        views.setTextViewText(R.id.widgetTime2,        "%02d:%02d".format(trainNext.hour, trainNext.minute))
        views.setTextViewText(R.id.widgetRemain2,      "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond))

        trainNext = trainsNext3[2]
        remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination3, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType3,   "%s".format(trainNext.train_type))
        views.setTextViewText(R.id.widgetTime3,        "%02d:%02d".format(trainNext.hour, trainNext.minute))
        views.setTextViewText(R.id.widgetRemain3,      "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond))

        return views
    }

    private fun makeNotification(): Notification {
        //利用者への通知をサービス作成から５秒以内に作成（IntentServiceの仕様上必須）
        val ChannelID = "SABA_Widget_foreground"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val textTitle = "SABA"
        val textContent = "時刻表ウィジェットが動作中"

        @RequiresApi(Build.VERSION_CODES.O)
        if (manager.getNotificationChannel(ChannelID) == null) {
            val mChannel = NotificationChannel(ChannelID, textTitle, NotificationManager.IMPORTANCE_HIGH)
            mChannel.apply {
                description = textContent
            }
            manager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(this, ChannelID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(textTitle)
            .setContentText(textContent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return notification
    }
}