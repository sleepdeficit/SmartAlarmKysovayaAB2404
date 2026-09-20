package com.smartalarm.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartalarm.app.data.Alarm
import com.smartalarm.app.ui.components.DaySelector

// шторка добавления или изменения
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAlarmSheet(
    sheetState: SheetState,
    alarm: Alarm?,
    onDismiss: () -> Unit,
    onSave: (Alarm) -> Unit
) {
    // время из редактируемого...
    val curH = alarm?.hour ?: 7
    val curM = alarm?.minute ?: 0
    val timeState = rememberTimePickerState(
        initialHour = curH,
        initialMinute = curM,
        is24Hour = true
    )

    var name by remember { mutableStateOf(alarm?.label ?: "") }
    var days by remember { mutableStateOf(alarm?.daysOfWeek ?: emptySet()) }
    var vibroEnabled by remember { mutableStateOf(alarm?.isVibrate ?: true) }
    var mathEnabled by remember { mutableStateOf(alarm?.smartDismissMath ?: false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (alarm == null) "Новый будильник" else "Настройка",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // таймпикер из матириал 3
            TimePicker(state = timeState)

            Spacer(modifier = Modifier.height(14.dp))

            DaySelector(
                selectedDays = days,
                onDaysChanged = { days = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название (например: Пары)") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Label, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Outlined.Vibration, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Вибрация", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Вибрировать при звонке",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = vibroEnabled, onCheckedChange = { vibroEnabled = it })
            }

            Spacer(modifier = Modifier.height(10.dp))

            // переключатель матем задач
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = Icons.Outlined.Calculate, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Решить пример", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Задачка по математике чтобы проснуться",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = mathEnabled, onCheckedChange = { mathEnabled = it })
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Отмена")
                }

                Button(
                    onClick = {
                        val res = (alarm ?: Alarm(hour = 7, minute = 0)).copy(
                            hour = timeState.hour,
                            minute = timeState.minute,
                            daysOfWeek = days,
                            label = name.trim(),
                            isVibrate = vibroEnabled,
                            smartDismissMath = mathEnabled,
                            isEnabled = true
                        )
                        onSave(res)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово")
                }
            }
        }
    }
}
