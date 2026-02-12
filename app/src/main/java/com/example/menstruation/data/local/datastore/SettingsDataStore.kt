package com.example.menstruation.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.ReminderTime
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    val settings: Flow<UserSettings> = dataStore.data
        .map { preferences ->
            UserSettings(
                periodLength = preferences[PERIOD_LENGTH] ?: DEFAULT_PERIOD_LENGTH,
                cycleLength = preferences[CYCLE_LENGTH] ?: DEFAULT_CYCLE_LENGTH,
                themeMode = preferences[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.DARK,
                notificationSettings = NotificationSettings(
                    enabled = preferences[NOTIFICATION_ENABLED] ?: true,
                    periodStartReminder = preferences[PERIOD_START_REMINDER] ?: true,
                    periodEndReminder = preferences[PERIOD_END_REMINDER] ?: true,
                    predictedPeriodReminder = preferences[PREDICTED_PERIOD_REMINDER] ?: true,
                    reminderTime = preferences[REMINDER_TIME]?.let {
                        ReminderTime.fromString(it)
                    } ?: ReminderTime.DEFAULT
                )
            )
        }

    suspend fun updatePeriodLength(length: Int) {
        dataStore.edit { preferences ->
            preferences[PERIOD_LENGTH] = length
        }
    }

    suspend fun updateCycleLength(length: Int) {
        dataStore.edit { preferences ->
            preferences[CYCLE_LENGTH] = length
        }
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode.name
        }
    }

    // Notification settings
    suspend fun updateNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun updatePeriodStartReminder(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PERIOD_START_REMINDER] = enabled
        }
    }

    suspend fun updatePeriodEndReminder(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PERIOD_END_REMINDER] = enabled
        }
    }

    suspend fun updatePredictedPeriodReminder(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PREDICTED_PERIOD_REMINDER] = enabled
        }
    }

    suspend fun updateReminderTime(time: ReminderTime) {
        dataStore.edit { preferences ->
            preferences[REMINDER_TIME] = time.toString()
        }
    }

    companion object {
        private val PERIOD_LENGTH = intPreferencesKey("period_length")
        private val CYCLE_LENGTH = intPreferencesKey("cycle_length")
        private val THEME_MODE = stringPreferencesKey("theme_mode")

        // Notification keys
        private val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        private val PERIOD_START_REMINDER = booleanPreferencesKey("period_start_reminder")
        private val PERIOD_END_REMINDER = booleanPreferencesKey("period_end_reminder")
        private val PREDICTED_PERIOD_REMINDER = booleanPreferencesKey("predicted_period_reminder")
        private val REMINDER_TIME = stringPreferencesKey("reminder_time")

        const val DEFAULT_PERIOD_LENGTH = 5
        const val DEFAULT_CYCLE_LENGTH = 28
    }
}
