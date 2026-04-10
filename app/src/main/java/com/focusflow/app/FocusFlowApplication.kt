package com.focusflow.app

import android.app.Application
import androidx.room.Room
import com.focusflow.app.data.local.AppDatabase
import com.focusflow.app.data.prefs.SettingsRepository
import com.focusflow.app.data.repository.ScheduleRepository
import com.focusflow.app.data.repository.StatsRepository
import com.focusflow.app.data.repository.TaskRepository

class FocusFlowApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "focus_flow_db"
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
