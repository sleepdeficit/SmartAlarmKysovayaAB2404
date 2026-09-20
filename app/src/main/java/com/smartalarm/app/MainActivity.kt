package com.smartalarm.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smartalarm.app.ui.screens.MainAlarmScreen
import com.smartalarm.app.ui.theme.SmartAlarmTheme
import com.smartalarm.app.util.PermissionHelper

class MainActivity : ComponentActivity() {

    // доступ для пушей
    private val reqNotif = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // для 13 андроида нужно запросить показ увед
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.isNotificationPermissionGranted(this)) {
                reqNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            SmartAlarmTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAlarmScreen(
                        onOpenExactAlarmSettings = {
                            PermissionHelper.openExactAlarmSettings(this)
                        },
                        onOpenNotificationListenerSettings = {
                            PermissionHelper.openNotificationListenerSettings(this)
                        }
                    )
                }
            }
        }
    }
}
