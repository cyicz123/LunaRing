package com.example.menstruation.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
                cycleLength = preferences[CYCLE_LENGTH] ?: DEFAULT_CYCLE_LENGTH
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

    companion object {
        private val PERIOD_LENGTH = intPreferencesKey("period_length")
        private val CYCLE_LENGTH = intPreferencesKey("cycle_length")

        const val DEFAULT_PERIOD_LENGTH = 5
        const val DEFAULT_CYCLE_LENGTH = 28
    }
}
