package com.example.adhdblockscheduler.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        val CALENDAR_SYNC_ENABLED = booleanPreferencesKey("calendar_sync_enabled")
        val BLOCK_DURATION_MINUTES = floatPreferencesKey("block_duration_minutes")
    }

    val calendarSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] ?: false // 기본값은 꺼짐(false)
        }

    val blockDurationMinutes: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[BLOCK_DURATION_MINUTES] ?: 15f
        }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CALENDAR_SYNC_ENABLED] = enabled
        }
    }

    suspend fun setBlockDurationMinutes(minutes: Float) {
        context.dataStore.edit { preferences ->
            preferences[BLOCK_DURATION_MINUTES] = minutes
        }
    }
}
