package com.smartalarm.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
@TypeConverters(DaysConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    companion object {
        private var instance: AppDatabase? = null

        // синглтон
        fun getDatabase(context: Context): AppDatabase {
            if (instance == null) {
                synchronized(AppDatabase::class.java) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "alarm_database.db"
                        ).fallbackToDestructiveMigration().build()
                    }
                }
            }
            return instance!!
        }
    }
}
