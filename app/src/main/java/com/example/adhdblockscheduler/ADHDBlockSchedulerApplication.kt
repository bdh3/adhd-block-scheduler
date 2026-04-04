package com.example.adhdblockscheduler

import android.app.Application
import androidx.room.Room
import com.example.adhdblockscheduler.data.local.AppDatabase
import com.example.adhdblockscheduler.data.prefs.SettingsRepository
import com.example.adhdblockscheduler.data.repository.ScheduleRepository
import com.example.adhdblockscheduler.data.repository.StatsRepository
import com.example.adhdblockscheduler.data.repository.TaskRepository

class ADHDBlockSchedulerApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "adhd_block_scheduler_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val taskRepository by lazy {
        TaskRepository(database.taskDao())
    }

    val statsRepository by lazy {
        StatsRepository(database.statsDao())
    }

    val scheduleRepository by lazy {
        ScheduleRepository(database.scheduleDao())
    }

    val settingsRepository by lazy {
        SettingsRepository(this)
    }
}
