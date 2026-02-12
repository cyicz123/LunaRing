package com.example.menstruation.data.repository

import com.example.menstruation.data.local.datastore.SettingsDataStore
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.ReminderTime
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    val settings: Flow<UserSettings> = settingsDataStore.settings

    suspend fun updatePeriodLength(length: Int) {
        settingsDataStore.updatePeriodLength(length)
    }

    suspend fun updateCycleLength(length: Int) {
        settingsDataStore.updateCycleLength(length)
    }

    suspend fun updateThemeMode(mode: ThemeMode) {
        settingsDataStore.updateThemeMode(mode)
    }

    // Notification settings
    suspend fun updateNotificationEnabled(enabled: Boolean) {
        settingsDataStore.updateNotificationEnabled(enabled)
    }

    suspend fun updatePeriodStartReminder(enabled: Boolean) {
        settingsDataStore.updatePeriodStartReminder(enabled)
    }

    suspend fun updatePeriodEndReminder(enabled: Boolean) {
        settingsDataStore.updatePeriodEndReminder(enabled)
    }

    suspend fun updatePredictedPeriodReminder(enabled: Boolean) {
        settingsDataStore.updatePredictedPeriodReminder(enabled)
    }

    suspend fun updateReminderTime(time: ReminderTime) {
        settingsDataStore.updateReminderTime(time)
    }

    suspend fun updateNotificationSettings(settings: NotificationSettings) {
        settingsDataStore.updateNotificationEnabled(settings.enabled)
        settingsDataStore.updatePeriodStartReminder(settings.periodStartReminder)
        settingsDataStore.updatePeriodEndReminder(settings.periodEndReminder)
        settingsDataStore.updatePredictedPeriodReminder(settings.predictedPeriodReminder)
        settingsDataStore.updateReminderTime(settings.reminderTime)
    }
}
