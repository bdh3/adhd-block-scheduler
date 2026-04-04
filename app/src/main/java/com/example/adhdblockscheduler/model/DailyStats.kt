package com.example.adhdblockscheduler.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStats(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val totalFocusMinutes: Int = 0,
    val completedTasksCount: Int = 0,
    val completedBlocksCount: Int = 0
)
