package com.example.adhdblockscheduler.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.adhdblockscheduler.model.DailyStats
import com.example.adhdblockscheduler.model.ScheduleBlock
import com.example.adhdblockscheduler.model.Task

@Database(entities = [Task::class, DailyStats::class, ScheduleBlock::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun statsDao(): StatsDao
    abstract fun scheduleDao(): ScheduleDao
}
