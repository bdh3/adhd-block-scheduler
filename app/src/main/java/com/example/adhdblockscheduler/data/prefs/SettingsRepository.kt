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
        val REST_MINUTES = intPreferencesKey("rest_minutes")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val ALARM_INTERVAL_MINUTES = intPreferencesKey("alarm_interval_minutes")
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

    val restMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[REST_MINUTES] ?: 0 // Default: 0 min rest (Continuous Focus)
        }

    val alarmIntervalMinutes: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[ALARM_INTERVAL_MINUTES] ?: 15 // Default: 15 min interval
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

    suspend fun setRestMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[REST_MINUTES] = minutes
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setAlarmIntervalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[ALARM_INTERVAL_MINUTES] = minutes
        }
    }

}
