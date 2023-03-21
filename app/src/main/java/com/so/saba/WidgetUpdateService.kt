package com.so.saba

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat


private val TAG: String = WidgetUpdateService::class.java.simpleName

class WidgetUpdateService : Service() {
    var trainScheduleConfig = TrainScheduleConfig()
    var trainSchedules = TrainSchedules()
    val handler = Handler()
    var runnable = Runnable {}


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // load trainSchedule
        if (intent != null) {
            trainSchedules.trainScheduleConfigs =
                intent.getSerializableExtra(TRAIN_SCHEDULE_CONFIGS) as TrainScheduleConfigs
        }
        trainSchedules.updateTrainSchedulesFromConfigs(resources)
        // start Foreground
        val notification = makeNotification()
        startForeground(1, notification)
        Log.d(TAG, "foreground service started.")
        // HandlerとRunnableを作成して、1秒ごとにTextViewを更新します
        runnable = object : Runnable {
            override fun run() {
                // 0.2秒ごとにupdate
                updateWidget()
                setOnclickWidgetButton()
                // 再度ハンドラーを実行する
                handler.postDelayed(this, 200)
            }
        }
        handler.post(runnable)
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called.")
        // Serviceが終了するときに呼び出されます
        // Handlerを停止して、メモリリークを回避します
        handler.removeCallbacks(runnable)
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWidget() {
        // get indexPrimary
        val context = applicationContext
        val sharedPref =
            context.getSharedPreferences("TrainScheduleConfigs", Context.MODE_PRIVATE)
        var indexPrimary = sharedPref.getInt("IndexPrimary", 0)
        if (indexPrimary > trainSchedules.trainSchedules.size - 1) {
            //invalid old value exists
            indexPrimary = 0
        }
        trainSchedules.indexPrimary = indexPrimary
        // get appWidget
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val componentName = ComponentName(this, AppWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        var views = RemoteViews(context.packageName, R.layout.app_widget)
        // get new views
        views = updateWidgetViews(views, trainSchedules)
        // update appWidget
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun setOnclickWidgetButton() {
        // set onclick widget button
        val intentWidget: Intent = Intent(this, AppWidget::class.java)
        intentWidget.apply { action = ACTION_SET_ONCLICK_WIDGET_BUTTON }
        sendBroadcast(intentWidget)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateWidgetViews(views: RemoteViews, trainSchedules: TrainSchedules): RemoteViews {
        // set Next 3 trains
        val trainsNext3 = trainSchedules.getNext3TrainsPrimary()
        val trainScheduleConfig =
            trainSchedules.trainScheduleConfigs.value[trainSchedules.indexPrimary]

        // set TrainScheduleConfig
        views.setTextViewText(R.id.widgetStation, "%s駅".format(trainScheduleConfig.station))
        views.setTextViewText(R.id.widgetLine, "%s".format(trainScheduleConfig.line))
        views.setTextViewText(R.id.widgetDestination, "%s".format(trainScheduleConfig.destination))
        views.setTextViewText(R.id.widgetNote, trainsNext3[0].note)

        var trainNext = trainsNext3[0]
        var remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination1, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType1, "%s".format(trainNext.train_type))
        views.setTextViewText(
            R.id.widgetTime1,
            "%02d:%02d".format(trainNext.hour, trainNext.minute)
        )
        views.setTextViewText(
            R.id.widgetRemain1,
            "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond)
        )

        trainNext = trainsNext3[1]
        remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination2, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType2, "%s".format(trainNext.train_type))
        views.setTextViewText(
            R.id.widgetTime2,
            "%02d:%02d".format(trainNext.hour, trainNext.minute)
        )
        views.setTextViewText(
            R.id.widgetRemain2,
            "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond)
        )

        trainNext = trainsNext3[2]
        remainTime = trainSchedules.calcRemainTimePrimary(trainNext)
        views.setTextViewText(R.id.widgetDestination3, "%s".format(trainNext.destination))
        views.setTextViewText(R.id.widgetTrainType3, "%s".format(trainNext.train_type))
        views.setTextViewText(
            R.id.widgetTime3,
            "%02d:%02d".format(trainNext.hour, trainNext.minute)
        )
        views.setTextViewText(
            R.id.widgetRemain3,
            "あと %3d分 %3d秒".format(remainTime.remainMinute, remainTime.remainSecond)
        )

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
            val mChannel =
                NotificationChannel(ChannelID, textTitle, NotificationManager.IMPORTANCE_HIGH)
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