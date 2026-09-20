package com.smartalarm.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    // все будильники отсортированные по времени
    @Query("SELECT * FROM alarms_table ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms_table WHERE isEnabled = 1")
    suspend fun getActive(): List<Alarm>

    @Query("SELECT * FROM alarms_table WHERE id = :alarmId LIMIT 1")
    suspend fun getById(alarmId: Long): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun removeAlarm(alarm: Alarm)

    @Query("UPDATE alarms_table SET isEnabled = :status WHERE id = :id")
    suspend fun switchStatus(id: Long, status: Boolean)
}
