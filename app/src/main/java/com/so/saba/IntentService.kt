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

private val TAG = "IntentService"

const val ACTION_SERVICE_RESTART = "com.so.saba.action.ACTION_SERVICE_RESTART"

class IntentService : IntentService("IntentService") {
    var destroyFlag = false
    var trainScheduleConfig = TrainScheduleConfig()

    @RequiresApi(Build.VERSION_CODES.O)
    override  fun onDestroy(){
        Log.d(TAG, "onDestroy called.")
        destroyFlag = true
        //start IntentServiceRestarter
        var intent: Intent = Intent(this, com.so.saba.IntentServiceRestarter::class.java)
        intent.apply {action = ACTION_SERVICE_RESTART}
        intent.putExtra(TRAIN_SCHEDULE_CONFIG, trainScheduleConfig)
        startForegroundService(intent)
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
                val notification = MakeNotification()
                startForeground(1, notification)
                // update Widget
                val context = applicationContext
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, AppWidget::class.java))
                // TODO: replace to timer?
                while(destroyFlag == false){
                    //update train schedule
                    var views = RemoteViews(context.packageName, R.layout.app_widget)
                    views = UpdateWidgetViews(views, trainSchedule)
                    for (appWidgetId in appWidgetIds){
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                    Thread.sleep(1000)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun UpdateWidgetViews(views: RemoteViews, trainSchedule: TrainSchedule): RemoteViews {
        trainSchedule.updateTrainsByDaytype()
        // set TrainScheduleConfig
        views.setTextViewText(R.id.appwidget_textStation, trainSchedule.trainScheduleConfig.station)
        views.setTextViewText(R.id.appwidget_textLine, trainSchedule.trainScheduleConfig.line)
        views.setTextViewText(R.id.appwidget_textDestination, trainSchedule.trainScheduleConfig.destination)
        // set Next train
        val trainNext = trainSchedule.getNextTrain()
        val remainTimeString = trainSchedule.calcRemainTime(trainNext)
        views.setTextViewText(R.id.appwidget_textNext0DepartTime, "%02d:%02d".format(trainNext.hour, trainNext.minute))
        views.setTextViewText(R.id.appwidget_textNext0RemainMinute, remainTimeString.remainMinute)
        views.setTextViewText(R.id.appwidget_textNext0RemainSecond, remainTimeString.remainSecond)
        return views
    }

    private fun MakeNotification(): Notification {
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