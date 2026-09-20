package com.smartalarm.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smartalarm.app.AlarmActivity
import com.smartalarm.app.R
import com.smartalarm.app.SmartAlarmApp
import com.smartalarm.app.util.AlarmScheduler

class AlarmService : Service() {

    companion object {
        const val NOTIF_ID = 777
        const val ACTION_START_ALARM = "START_ALARM"
        const val ACTION_DISMISS_ALARM = "DISMISS_ALARM"
        const val ACTION_SNOOZE_ALARM = "SNOOZE_ALARM"

        var isPlaying = false
    }

    private var player: MediaPlayer? = null
    private var vibro: Vibrator? = null
    private var wl: PowerManager.WakeLock? = null

    private var curId: Long = -1L
    private var curLabel: String = ""
    private var curMath: Boolean = false
    private var curVib: Boolean = true
    private var curRing: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val act = intent?.action ?: ACTION_START_ALARM

        when (act) {
            ACTION_START_ALARM -> {
                curId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L
                curLabel = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: ""
                curMath = intent?.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_MATH, false) ?: false
                curVib = intent?.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_VIBRATE, true) ?: true
                curRing = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_RINGTONE)

                startAlarmStuff()
            }
            ACTION_DISMISS_ALARM -> {
                stopEverything()
            }
            ACTION_SNOOZE_ALARM -> {
                snoozeIt()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmStuff() {
        if (isPlaying) return
        isPlaying = true

        // процессор вкл
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myalarm:service_wakelock").apply {
            acquire(15 * 60 * 1000L)
        }

        // интент для открытия активности поверх блокировки
        val fullScreenIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, curId)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, curLabel)
            putExtra(AlarmScheduler.EXTRA_ALARM_MATH, curMath)
        }

        val fullScreenPending = PendingIntent.getActivity(
            this,
            NOTIF_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // кнопка выключить в шторке
        val disIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_DISMISS_ALARM }
        val disPending = PendingIntent.getService(this, 1, disIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val snzIntent = Intent(this, AlarmService::class.java).apply { action = ACTION_SNOOZE_ALARM }
        val snzPending = PendingIntent.getService(this, 2, snzIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val title = if (curLabel.isNotBlank()) curLabel else "Будильник звонит!"

        val notif = NotificationCompat.Builder(this, SmartAlarmApp.ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText("Пора просыпаться!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(0, "Отложить", snzPending)
            .addAction(0, "Выключить", disPending)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, notif)
        }

        // музыка
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)

            val uri = if (!curRing.isNullOrBlank()) {
                Uri.parse(curRing)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            player = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM) // USAGE_ALARM
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("myAlarm", "ошибка звука: ${e.message}")
        }

        // вкл вибрацию
        if (curVib) {
            try {
                vibro = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vm.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }

                val pattern = longArrayOf(0, 800, 500, 800, 500)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibro?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibro?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.d("myAlarm", "startActivity fail: ${e.message}")
        }
    }

    private fun snoozeIt() {
        val sc = AlarmScheduler(this)
        sc.snooze(curId, 10, curLabel, curVib, curMath, curRing)
        stopEverything()
    }

    private fun stopEverything() {
        isPlaying = false

        try {
            player?.stop()
            player?.release()
        } catch (e: Exception) {}
        player = null

        try {
            vibro?.cancel()
        } catch (e: Exception) {}
        vibro = null

        if (wl?.isHeld == true) {
            wl?.release()
        }
        wl = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }
}
