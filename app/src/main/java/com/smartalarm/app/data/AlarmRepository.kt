package com.smartalarm.app.data

import android.content.Context
import com.smartalarm.app.util.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AlarmRepository(
    private val dao: AlarmDao,
    context: Context
) {
    private val myScheduler = AlarmScheduler(context)

    // список для главного экрана
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun insertAlarm(alarm: Alarm): Long = withContext(Dispatchers.IO) {
        val newId = dao.addAlarm(alarm)
        val copy = alarm.copy(id = newId)
        if (copy.isEnabled) {
            myScheduler.schedule(copy)
        }
        return@withContext newId
    }

    suspend fun updateAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        dao.updateAlarm(alarm)
        if (alarm.isEnabled) {
            myScheduler.schedule(alarm)
        } else {
            myScheduler.cancel(alarm.id)
        }
    }

    suspend fun toggleAlarm(alarm: Alarm, enabled: Boolean) = withContext(Dispatchers.IO) {
        dao.switchStatus(alarm.id, enabled)
        val upd = alarm.copy(isEnabled = enabled)
        if (enabled) {
            myScheduler.schedule(upd)
        } else {
            myScheduler.cancel(alarm.id)
        }
    }

    suspend fun deleteAlarm(alarm: Alarm) = withContext(Dispatchers.IO) {
        myScheduler.cancel(alarm.id)
        dao.removeAlarm(alarm)
    }

    // когда сработал будильник если он разовый то откл его
    suspend fun onAlarmTriggered(alarmId: Long) = withContext(Dispatchers.IO) {
        val al = dao.getById(alarmId) ?: return@withContext
        if (al.daysOfWeek.isEmpty()) {
            dao.switchStatus(alarmId, false)
        } else {
            myScheduler.schedule(al)
        }
    }

    // после перезагрузки
    suspend fun rescheduleAllActiveAlarms() = withContext(Dispatchers.IO) {
        val list = dao.getActive()
        for (item in list) {
            myScheduler.schedule(item)
        }
    }
}
