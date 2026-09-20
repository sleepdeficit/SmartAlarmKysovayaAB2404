package com.smartalarm.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartalarm.app.SmartAlarmApp
import com.smartalarm.app.data.Alarm
import com.smartalarm.app.service.CapturedNotification
import com.smartalarm.app.service.SmartNotificationListenerService
import com.smartalarm.app.ui.components.AlarmCard
import com.smartalarm.app.util.AlarmScheduler
import com.smartalarm.app.util.PermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// главный экран
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAlarmScreen(
    onOpenExactAlarmSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sch = remember { AlarmScheduler(ctx) }

    val alarms by SmartAlarmApp.repo.allAlarms.collectAsState(initial = emptyList())
    val notifs by SmartNotificationListenerService.capturedNotifications.collectAsState()

    var showEdit by remember { mutableStateOf(false) }
    var curAlarm by remember { mutableStateOf<Alarm?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showNotifsDialog by remember { mutableStateOf(false) }

    var hasNotifListener by remember { mutableStateOf(PermissionHelper.isNotificationListenerGranted(ctx)) }
    var hasExactAlarm by remember { mutableStateOf(PermissionHelper.canScheduleExactAlarms(ctx)) }

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000L)
            tick++
            hasNotifListener = PermissionHelper.isNotificationListenerGranted(ctx)
            hasExactAlarm = PermissionHelper.canScheduleExactAlarms(ctx)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Будильник", fontWeight = FontWeight.Bold)
                },
                actions = {
                    // кнопка с колокольчиком...
                    IconButton(onClick = { showNotifsDialog = true }) {
                        BadgedBox(
                            badge = {
                                if (notifs.isNotEmpty()) {
                                    Badge { Text("${notifs.size}") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    curAlarm = null
                    showEdit = true
                },
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // если нет разрешения на точные будильники...
            if (!hasExactAlarm) {
                item {
                    WarningCard(
                        txt = "Нужно разрешение на точные будильники, иначе система заглушит звонок когда тел заблокирован",
                        btn = "Разрешить",
                        onClick = onOpenExactAlarmSettings
                    )
                }
            }

            if (!hasNotifListener) {
                item {
                    WarningCard(
                        txt = "Нет доступа к уведомлениям. Включите, чтобы будильник мог читать сообщения пока вы спите",
                        btn = "Включить доступ",
                        onClick = onOpenNotificationListenerSettings
                    )
                }
            } else {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (notifs.isNotEmpty()) "Служба уведомлений работает (${notifs.size} поймано)" else "Служба уведомлений активна",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            if (alarms.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(90.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "Будильников пока нет", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Нажмите на плюс снизу чтобы добавить расписание",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(alarms, key = { it.id }) { item ->
                    val remText = remember(item, tick) {
                        if (item.isEnabled) {
                            val next = sch.calculateNextTime(item)
                            sch.formatRemainingTime(next)
                        } else ""
                    }

                    AlarmCard(
                        alarm = item,
                        remainingText = remText,
                        onToggle = { isCheck ->
                            scope.launch {
                                SmartAlarmApp.repo.toggleAlarm(item, isCheck)
                            }
                        },
                        onClick = {
                            curAlarm = item
                            showEdit = true
                        },
                        onDelete = {
                            scope.launch {
                                SmartAlarmApp.repo.deleteAlarm(item)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showEdit) {
        EditAlarmSheet(
            sheetState = sheetState,
            alarm = curAlarm,
            onDismiss = { showEdit = false },
            onSave = { saved ->
                scope.launch {
                    if (saved.id == 0L) {
                        SmartAlarmApp.repo.insertAlarm(saved)
                    } else {
                        SmartAlarmApp.repo.updateAlarm(saved)
                    }
                    showEdit = false
                }
            }
        )
    }

    // просмотр уведомлений
    if (showNotifsDialog) {
        ModalBottomSheet(
            onDismissRequest = { showNotifsDialog = false },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 30.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Уведомления за ночь", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    if (notifs.isNotEmpty()) {
                        Button(
                            onClick = { SmartNotificationListenerService.clearAll() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Очистить")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (notifs.isEmpty()) {
                    Text(
                        text = "Пока пусто. Когда придут сообщения в телегу или ватсап они тут появятся.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifs, key = { it.id }) { n ->
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(n.appName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text(n.getTime(), style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (n.title.isNotBlank()) {
                                        Text(n.title, fontWeight = FontWeight.Medium)
                                    }
                                    if (n.text.isNotBlank()) {
                                        Text(n.text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WarningCard(txt: String, btn: String, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Внимание", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(txt, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(btn)
            }
        }
    }
}
