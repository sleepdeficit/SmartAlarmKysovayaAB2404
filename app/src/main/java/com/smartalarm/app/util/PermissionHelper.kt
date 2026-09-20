package com.smartalarm.app.util

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.smartalarm.app.service.SmartNotificationListenerService

// подсказка для разрешений
object PermissionHelper {

    // для андроид 12+ точные будильники
    fun canScheduleExactAlarms(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${ctx.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ctx.startActivity(intent)
        }
    }

    // показ увед андроид 13+
    fun isNotificationPermissionGranted(ctx: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isNotificationListenerGranted(ctx: Context): Boolean {
        return SmartNotificationListenerService.isPermissionGranted(ctx)
    }

    fun openNotificationListenerSettings(ctx: Context) {
        SmartNotificationListenerService.openSettings(ctx)
    }
}
