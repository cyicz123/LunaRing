package com.example.menstruation.data.repository

import com.example.menstruation.data.local.datastore.SettingsDataStore
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
}
