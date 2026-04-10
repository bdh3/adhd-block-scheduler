package com.focusflow.app.data.repository

import com.focusflow.app.data.local.StatsDao
import com.focusflow.app.model.DailyStats
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class StatsRepository(private val statsDao: StatsDao) {
    val recentStats: Flow<List<DailyStats>> = statsDao.getRecentStats()

    suspend fun addFocusMinutes(minutes: Int) {
        val today = LocalDate.now().toString()
        val currentStats = statsDao.getStatsForDate(today) ?: DailyStats(date = today)
        
        val updatedStats = currentStats.copy(
            totalFocusMinutes = currentStats.totalFocusMinutes + minutes,
            completedBlocksCount = currentStats.completedBlocksCount + 1
        )
        statsDao.insertOrUpdateStats(updatedStats)
    }

    suspend fun incrementTaskCount() {
        val today = LocalDate.now().toString()
        val currentStats = statsDao.getStatsForDate(today) ?: DailyStats(date = today)
        
        val updatedStats = currentStats.copy(
            completedTasksCount = currentStats.completedTasksCount + 1
        )
        statsDao.insertOrUpdateStats(updatedStats)
    }
}
