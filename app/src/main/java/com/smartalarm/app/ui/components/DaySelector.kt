package com.smartalarm.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// компонент с днями недели
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySelector(
    selectedDays: Set<Int>,
    onDaysChanged: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val daysList = listOf(
        1 to "Пн",
        2 to "Вт",
        3 to "Ср",
        4 to "Чт",
        5 to "Пт",
        6 to "Сб",
        7 to "Вс"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Дни повтора:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // кружки с днями
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysList.forEach { (index, title) ->
                val checked = selectedDays.contains(index)
                Surface(
                    onClick = {
                        val updated = if (checked) selectedDays - index else selectedDays + index
                        onDaysChanged(updated)
                    },
                    shape = CircleShape,
                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // быстрые кнопки
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onDaysChanged(setOf(1, 2, 3, 4, 5)) },
                label = { Text("Будни") }
            )
            AssistChip(
                onClick = { onDaysChanged(setOf(6, 7)) },
                label = { Text("Выходные") }
            )
            AssistChip(
                onClick = { onDaysChanged((1..7).toSet()) },
                label = { Text("Каждый день") }
            )
            if (selectedDays.isNotEmpty()) {
                AssistChip(
                    onClick = { onDaysChanged(emptySet()) },
                    label = { Text("Очистить") }
                )
            }
        }
    }
}
