package com.example.adhdblockscheduler.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val BLOCKS_PER_HOUR = intPreferencesKey("blocks_per_hour")
        val FOCUS_BLOCKS_COUNT = intPreferencesKey("focus_blocks_count")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFICATION_INTERVAL_MINUTES = floatPreferencesKey("notification_interval_minutes")
    }

    val calendarSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] ?: false
        }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATION_ENABLED] ?: true
        }

    val blocksPerHour: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[BLOCKS_PER_HOUR] ?: 4 // Default: 4 blocks (15 min each)
        }

    val focusBlocksCount: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[FOCUS_BLOCKS_COUNT] ?: 3 // Default: 3 focus blocks
        }

    val notificationIntervalMinutes: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATION_INTERVAL_MINUTES] ?: 0f // 0 means no intermediate notifications
        }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setBlocksPerHour(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[BLOCKS_PER_HOUR] = count
        }
    }

    suspend fun setFocusBlocksCount(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[FOCUS_BLOCKS_COUNT] = count
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setNotificationIntervalMinutes(minutes: Float) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATION_INTERVAL_MINUTES] = minutes
        }
    }
}
