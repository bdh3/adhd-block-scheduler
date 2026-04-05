package com.example.adhdblockscheduler.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "schedule_blocks")
data class ScheduleBlock(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val taskTitle: String,
    val startTimeMillis: Long,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val color: Int = 0,
    val intervalMinutes: Int = 15,
    val restMinutes: Int = 0
)
