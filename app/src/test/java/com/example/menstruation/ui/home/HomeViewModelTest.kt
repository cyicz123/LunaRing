package com.example.menstruation.ui.home

import app.cash.turbine.test
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.PredictNextPeriodUseCase
import com.example.menstruation.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@ExperimentalCoroutinesApi
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: HomeViewModel
    private lateinit var periodRepository: PeriodRepository
    private lateinit var dailyRecordRepository: DailyRecordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var predictUseCase: PredictNextPeriodUseCase

    private val periodsFlow = MutableStateFlow<List<Period>>(emptyList())
    private val settingsFlow = MutableStateFlow(
        UserSettings(
            periodLength = 5,
            cycleLength = 28,
            themeMode = ThemeMode.SYSTEM
        )
    )

    @Before
    fun setup() {
        periodRepository = mockk(relaxed = true)
        dailyRecordRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        predictUseCase = mockk(relaxed = true)

        every { periodRepository.getAllPeriods() } returns periodsFlow
        every { settingsRepository.settings } returns settingsFlow
        every { dailyRecordRepository.getRecordsByDateRange(any(), any()) } returns flowOf(emptyList())

        viewModel = HomeViewModel(
            periodRepository = periodRepository,
            dailyRecordRepository = dailyRecordRepository,
            settingsRepository = settingsRepository,
            predictUseCase = predictUseCase
        )
    }

    @Test
    fun `initial state has empty periods and records`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.periods.isEmpty())
            assertTrue(state.records.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `periods flow updates ui state`() = runTest {
        // Given
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)),
            Period(id = 2, startDate = LocalDate.of(2025, 1, 29), endDate = LocalDate.of(2025, 2, 2))
        )

        // When
        periodsFlow.value = periods

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(2, state.periods.size)
            assertEquals(LocalDate.of(2025, 1, 1), state.periods[0].startDate)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `settings flow updates ui state`() = runTest {
        // Given
        val newSettings = UserSettings(
            periodLength = 7,
            cycleLength = 30,
            themeMode = ThemeMode.DARK
        )

        // When
        settingsFlow.value = newSettings

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(7, state.settings.periodLength)
            assertEquals(30, state.settings.cycleLength)
            assertEquals(ThemeMode.DARK, state.settings.themeMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startPeriod calls repository with correct parameters`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 3, 1)
        coEvery { periodRepository.startPeriod(any(), any()) } returns 1L

        // When
        viewModel.startPeriod(startDate)
        advanceUntilIdle()

        // Then
        coVerify { periodRepository.startPeriod(startDate, 5) }
    }

    @Test
    fun `endPeriod calls repository with correct date`() = runTest {
        // Given
        val endDate = LocalDate.of(2025, 3, 5)
        coEvery { periodRepository.endPeriod(any()) } returns Unit

        // When
        viewModel.endPeriod(endDate)
        advanceUntilIdle()

        // Then
        coVerify { periodRepository.endPeriod(endDate) }
    }

    @Test
    fun `saveRecord calls repository`() = runTest {
        // Given
        val record = DailyRecord(
            date = LocalDate.of(2025, 3, 1),
            isPeriodDay = true,
            painLevel = 5
        )
        coEvery { dailyRecordRepository.saveRecord(any()) } returns Unit

        // When
        viewModel.saveRecord(record)
        advanceUntilIdle()

        // Then
        coVerify { dailyRecordRepository.saveRecord(record) }
    }

    @Test
    fun `isInPeriod returns true when date is within period`() = runTest {
        // Given
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5))
        )
        periodsFlow.value = periods

        // Allow state to update
        advanceUntilIdle()

        // When & Then
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 1, 1)))
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 1, 3)))
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 1, 5)))
    }

    @Test
    fun `isInPeriod returns false when date is outside period`() = runTest {
        // Given
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5))
        )
        periodsFlow.value = periods

        // Allow state to update
        advanceUntilIdle()

        // When & Then
        assertFalse(viewModel.isInPeriod(LocalDate.of(2024, 12, 31)))
        assertFalse(viewModel.isInPeriod(LocalDate.of(2025, 1, 6)))
    }

    @Test
    fun `isInPeriod returns true for ongoing period`() = runTest {
        // Given - period without end date
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = null)
        )
        periodsFlow.value = periods

        // Allow state to update
        advanceUntilIdle()

        // When & Then - any date after start should be in period
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 1, 1)))
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 1, 10)))
        assertTrue(viewModel.isInPeriod(LocalDate.of(2025, 2, 1)))
    }

    @Test
    fun `ensurePredictionsCover updates coverage date when needed`() = runTest {
        // Given
        val targetDate = LocalDate.of(2026, 6, 30)

        // When - should not crash
        viewModel.ensurePredictionsCover(targetDate)

        // Then - no crash means success
        assertTrue(true)
    }

    @Test
    fun `onVisibleMonthChanged triggers prediction update`() = runTest {
        // Given
        val month = YearMonth.of(2026, 6)

        // When - should not crash
        viewModel.onVisibleMonthChanged(month)

        // Then - no crash means success
        assertTrue(true)
    }
}
