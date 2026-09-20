package com.smartalarm.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartalarm.app.SmartAlarmApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// слушатель перезагрузки телефона
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val a = intent.action
        if (a == Intent.ACTION_BOOT_COMPLETED || a == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("myAlarm", "тел перезагрузился")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SmartAlarmApp.repo.rescheduleAllActiveAlarms()
                } catch (e: Exception) {
                    Log.e("myAlarm", "ошибка ресивера: ${e.message}")
                }
            }
        }
    }
}
