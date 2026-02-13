package com.example.menstruation.data.repository

import com.example.menstruation.data.local.datastore.SettingsDataStore
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.ReminderTime
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.util.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [SettingsRepository].
 */
@ExperimentalCoroutinesApi
class SettingsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsDataStore: SettingsDataStore

    @Before
    fun setup() {
        settingsDataStore = mockk(relaxed = true)
    }

    private fun createRepository(settingsFlow: MutableStateFlow<UserSettings>): SettingsRepository {
        every { settingsDataStore.settings } returns settingsFlow
        return SettingsRepository(settingsDataStore)
    }

    // ==================== settings Flow Tests ====================

    @Test
    fun `settings flow returns data from datastore`() = runTest {
        // Given
        val userSettings = UserSettings(
            periodLength = 5,
            cycleLength = 28,
            themeMode = ThemeMode.SYSTEM,
            notificationSettings = NotificationSettings()
        )
        val settingsFlow = MutableStateFlow(userSettings)
        val repository = createRepository(settingsFlow)

        // When & Then
        repository.settings.test {
            val result = awaitItem()
            assertEquals(5, result.periodLength)
            assertEquals(28, result.cycleLength)
            assertEquals(ThemeMode.SYSTEM, result.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ==================== updatePeriodLength() Tests ====================

    @Test
    fun `updatePeriodLength calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val length = 7

        // When
        repository.updatePeriodLength(length)

        // Then
        coVerify { settingsDataStore.updatePeriodLength(length) }
    }

    // ==================== updateCycleLength() Tests ====================

    @Test
    fun `updateCycleLength calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val length = 30

        // When
        repository.updateCycleLength(length)

        // Then
        coVerify { settingsDataStore.updateCycleLength(length) }
    }

    // ==================== updateThemeMode() Tests ====================

    @Test
    fun `updateThemeMode calls datastore with correct mode`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val mode = ThemeMode.DARK

        // When
        repository.updateThemeMode(mode)

        // Then
        coVerify { settingsDataStore.updateThemeMode(mode) }
    }

    @Test
    fun `updateThemeMode with light mode calls datastore`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val mode = ThemeMode.LIGHT

        // When
        repository.updateThemeMode(mode)

        // Then
        coVerify { settingsDataStore.updateThemeMode(mode) }
    }

    // ==================== Notification Settings Tests ====================

    @Test
    fun `updateNotificationEnabled calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val enabled = true

        // When
        repository.updateNotificationEnabled(enabled)

        // Then
        coVerify { settingsDataStore.updateNotificationEnabled(enabled) }
    }

    @Test
    fun `updatePeriodStartReminder calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val enabled = true

        // When
        repository.updatePeriodStartReminder(enabled)

        // Then
        coVerify { settingsDataStore.updatePeriodStartReminder(enabled) }
    }

    @Test
    fun `updatePeriodEndReminder calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val enabled = false

        // When
        repository.updatePeriodEndReminder(enabled)

        // Then
        coVerify { settingsDataStore.updatePeriodEndReminder(enabled) }
    }

    @Test
    fun `updatePredictedPeriodReminder calls datastore with correct value`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val enabled = true

        // When
        repository.updatePredictedPeriodReminder(enabled)

        // Then
        coVerify { settingsDataStore.updatePredictedPeriodReminder(enabled) }
    }

    @Test
    fun `updateReminderTime calls datastore with correct time`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val time = ReminderTime(9, 0)

        // When
        repository.updateReminderTime(time)

        // Then
        coVerify { settingsDataStore.updateReminderTime(time) }
    }

    // ==================== updateNotificationSettings() Tests ====================

    @Test
    fun `updateNotificationSettings updates all notification settings`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val notificationSettings = NotificationSettings(
            enabled = true,
            periodStartReminder = true,
            periodEndReminder = true,
            predictedPeriodReminder = true,
            reminderTime = ReminderTime(8, 30)
        )

        // When
        repository.updateNotificationSettings(notificationSettings)

        // Then
        coVerify { settingsDataStore.updateNotificationEnabled(true) }
        coVerify { settingsDataStore.updatePeriodStartReminder(true) }
        coVerify { settingsDataStore.updatePeriodEndReminder(true) }
        coVerify { settingsDataStore.updatePredictedPeriodReminder(true) }
        coVerify { settingsDataStore.updateReminderTime(ReminderTime(8, 30)) }
    }

    @Test
    fun `updateNotificationSettings with disabled notifications updates correctly`() = runTest {
        // Given
        val settingsFlow = MutableStateFlow(UserSettings(periodLength = 5, cycleLength = 28))
        val repository = createRepository(settingsFlow)
        val notificationSettings = NotificationSettings(
            enabled = false,
            periodStartReminder = false,
            periodEndReminder = false,
            predictedPeriodReminder = false,
            reminderTime = ReminderTime(10, 0)
        )

        // When
        repository.updateNotificationSettings(notificationSettings)

        // Then
        coVerify { settingsDataStore.updateNotificationEnabled(false) }
        coVerify { settingsDataStore.updatePeriodStartReminder(false) }
        coVerify { settingsDataStore.updatePeriodEndReminder(false) }
        coVerify { settingsDataStore.updatePredictedPeriodReminder(false) }
        coVerify { settingsDataStore.updateReminderTime(ReminderTime(10, 0)) }
    }
}
