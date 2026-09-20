package com.smartalarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.smartalarm.app.data.AlarmRepository
import com.smartalarm.app.data.AppDatabase

class SmartAlarmApp : Application() {

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel_1"
        lateinit var db: AppDatabase
        lateinit var repo: AlarmRepository
    }

    override fun onCreate() {
        super.onCreate()
        
        // бд
        db = AppDatabase.getDatabase(this)
        repo = AlarmRepository(db.alarmDao(), this)

        createChannels()
    }

    private fun createChannels() {
        // для новых андроидов нужен канал для уведомлений
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Будильник",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для звонка будильника"
                enableVibration(true)
                enableLights(true)
                setSound(null, null) // звук отдельно через плеер
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(chan)
        }
    }
}
