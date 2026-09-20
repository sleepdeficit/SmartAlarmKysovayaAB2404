package com.smartalarm.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.smartalarm.app.MainActivity
import com.smartalarm.app.data.Alarm
import com.smartalarm.app.service.AlarmReceiver
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_LABEL = "alarm_label"
        const val EXTRA_ALARM_VIBRATE = "alarm_vibrate"
        const val EXTRA_ALARM_MATH = "alarm_math"
        const val EXTRA_ALARM_RINGTONE = "alarm_ringtone"
    }

    fun schedule(alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancel(alarm.id)
            return
        }

        // проверка доступа андроид 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Log.d("myAlarm", "нет прав на точный будильник!")
            }
        }

        val triggerTime = calculateNextTime(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_VIBRATE, alarm.isVibrate)
            putExtra(EXTRA_ALARM_MATH, alarm.smartDismissMath)
            putExtra(EXTRA_ALARM_RINGTONE, alarm.ringtoneUri)
        }

        val pIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // setalarmclock!
        val info = AlarmManager.AlarmClockInfo(triggerTime, showPIntent)
        try {
            am.setAlarmClock(info, pIntent)
        } catch (e: Exception) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pIntent)
        }

        Log.d("myAlarm", "поставили будильник на $triggerTime")
    }

    fun cancel(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pIntent)
    }

    fun snooze(alarmId: Long, minutes: Int = 10, label: String = "Отложенный будильник", vibrate: Boolean = true, math: Boolean = false, ringtone: String? = null) {
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_VIBRATE, vibrate)
            putExtra(EXTRA_ALARM_MATH, math)
            putExtra(EXTRA_ALARM_RINGTONE, ringtone)
        }

        val pIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val info = AlarmManager.AlarmClockInfo(triggerTime, showPIntent)
        try {
            am.setAlarmClock(info, pIntent)
        } catch (e: Exception) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pIntent)
        }
    }

    // время след звонка
    fun calculateNextTime(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val h = alarm.hour
        val m = alarm.minute

        // если разовый будильник
        if (alarm.daysOfWeek.isEmpty()) {
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= now.timeInMillis) {
                c.add(Calendar.DAY_OF_YEAR, 1)
            }
            return c.timeInMillis
        }

        // если по дням недели
        var minTime = Long.MAX_VALUE
        for (day in alarm.daysOfWeek) {
            val calDay = when (day) {
                1 -> Calendar.MONDAY
                2 -> Calendar.TUESDAY
                3 -> Calendar.WEDNESDAY
                4 -> Calendar.THURSDAY
                5 -> Calendar.FRIDAY
                6 -> Calendar.SATURDAY
                7 -> Calendar.SUNDAY
                else -> Calendar.MONDAY
            }

            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, calDay)
            }

            if (c.timeInMillis <= now.timeInMillis) {
                c.add(Calendar.WEEK_OF_YEAR, 1)
            }

            if (c.timeInMillis < minTime) {
                minTime = c.timeInMillis
            }
        }

        return minTime
    }

    fun formatRemainingTime(targetTime: Long): String {
        val diff = targetTime - System.currentTimeMillis()
        if (diff <= 0) return "Сейчас"

        val minsTotal = diff / 60000L
        val hours = minsTotal / 60L
        val mins = minsTotal % 60L

        if (hours > 0 && mins > 0) {
            return "через $hours ч $mins мин"
        }
        if (hours > 0) {
            return "через $hours ч"
        }
        if (mins > 0) {
            return "через $mins мин"
        }
        return "меньше минуты"
    }
}
