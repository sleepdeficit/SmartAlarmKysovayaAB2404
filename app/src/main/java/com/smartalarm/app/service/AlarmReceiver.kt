package com.smartalarm.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.smartalarm.app.SmartAlarmApp
import com.smartalarm.app.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val lbl = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_LABEL) ?: ""
        val vib = intent.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_VIBRATE, true)
        val mth = intent.getBooleanExtra(AlarmScheduler.EXTRA_ALARM_MATH, false)
        val ring = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_RINGTONE)

        // пинг проца
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myalarm:wakelock")
        wl.acquire(3000L)

        // старт фоновой службы
        val servIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            putExtra(AlarmScheduler.EXTRA_ALARM_LABEL, lbl)
            putExtra(AlarmScheduler.EXTRA_ALARM_VIBRATE, vib)
            putExtra(AlarmScheduler.EXTRA_ALARM_MATH, mth)
            putExtra(AlarmScheduler.EXTRA_ALARM_RINGTONE, ring)
            action = AlarmService.ACTION_START_ALARM
        }
        ContextCompat.startForegroundService(context, servIntent)

        // обновление в базе
        if (id != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SmartAlarmApp.repo.onAlarmTriggered(id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
