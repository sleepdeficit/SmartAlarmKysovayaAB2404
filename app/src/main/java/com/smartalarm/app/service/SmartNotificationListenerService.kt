package com.smartalarm.app.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long
) {
    fun getTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

// служба для перехвата уведомлений
class SmartNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _list = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val capturedNotifications: StateFlow<List<CapturedNotification>> = _list.asStateFlow()

        // проверка доступа
        fun isPermissionGranted(context: Context): Boolean {
            val pkg = context.packageName
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat != null && flat.contains(pkg)
        }

        fun openSettings(context: Context) {
            val i = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(i)
        }

        fun clearAll() {
            _list.value = emptyList()
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        // без учета своих увед
        if (sbn.isOngoing || sbn.packageName == packageName) return

        val ext = sbn.notification.extras ?: return
        val t = ext.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""
        val txt = ext.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""

        if (t.isEmpty() && txt.isEmpty()) return

        val pm = applicationContext.packageManager
        val name = try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(sbn.packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(sbn.packageName, 0)
            }
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val item = CapturedNotification(
            id = "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
            packageName = sbn.packageName,
            appName = name,
            title = t,
            text = txt,
            timestamp = sbn.postTime
        )

        val cur = _list.value.toMutableList()
        cur.removeAll { it.id == item.id }
        cur.add(0, item)

        // макс 30 шт
        if (cur.size > 30) {
            _list.value = cur.take(30)
        } else {
            _list.value = cur
        }
    }
}
