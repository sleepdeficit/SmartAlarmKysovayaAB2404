package com.smartalarm.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Entity(tableName = "alarms_table")
@TypeConverters(DaysConverter::class)
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<Int> = emptySet(), // 1=пн, 2=вт ... 7=вс
    val label: String = "",
    val isEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val smartDismissMath: Boolean = false,
    val ringtoneUri: String? = null,
    val timeCreated: Long = System.currentTimeMillis()
) {
    // форматируем время для экрана типа 08:05
    fun getTimeStr(): String {
        val h = if (hour < 10) "0$hour" else "$hour"
        val m = if (minute < 10) "0$minute" else "$minute"
        return "$h:$m"
    }

    // проверка дней недели для текста
    fun getDaysText(): String {
        if (daysOfWeek.isEmpty()) return "Один раз"
        if (daysOfWeek.size == 7) return "Каждый день"
        if (daysOfWeek == setOf(1, 2, 3, 4, 5)) return "По будням"
        if (daysOfWeek == setOf(6, 7)) return "Выходные"

        val names = mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
        return daysOfWeek.sorted().mapNotNull { names[it] }.joinToString(", ")
    }
}

// конвертер чтобы сохранять список дней через запятую
class DaysConverter {
    @TypeConverter
    fun fromSet(set: Set<Int>?): String {
        if (set == null || set.isEmpty()) return ""
        return set.joinToString(",")
    }

    @TypeConverter
    fun toSet(str: String?): Set<Int> {
        if (str.isNullOrBlank()) return emptySet()
        return str.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toSet()
    }
}
