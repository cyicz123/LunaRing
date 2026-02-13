package com.example.menstruation.ui.stats

import app.cash.turbine.test
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.FlowLevel
import com.example.menstruation.data.model.Mood
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.Symptom
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.PredictNextPeriodUseCase
import com.example.menstruation.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: StatsViewModel
    private lateinit var periodRepository: PeriodRepository
    private lateinit var dailyRecordRepository: DailyRecordRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var predictUseCase: PredictNextPeriodUseCase

    private val periodsFlow = MutableStateFlow<List<Period>>(emptyList())
    private val recordsFlow = MutableStateFlow<List<DailyRecord>>(emptyList())
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
        every { dailyRecordRepository.getAllRecords() } returns recordsFlow
        every { settingsRepository.settings } returns settingsFlow
        every { predictUseCase.isPlausibleCycleLength(any(), any()) } returns true

        viewModel = StatsViewModel(
            periodRepository = periodRepository,
            dailyRecordRepository = dailyRecordRepository,
            settingsRepository = settingsRepository,
            predictNextPeriodUseCase = predictUseCase
        )
    }

    @Test
    fun `initial state is loading with empty data`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.periods.isEmpty())
            assertTrue(state.cycleLengths.isEmpty())
            assertEquals(0.0, state.avgCycleLength, 0.01)
            assertEquals(0.0, state.avgPeriodLength, 0.01)
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
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `calculates average cycle length correctly`() = runTest {
        // Given - periods with 28-day cycle
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)),
            Period(id = 2, startDate = LocalDate.of(2025, 1, 29), endDate = LocalDate.of(2025, 2, 2)),
            Period(id = 3, startDate = LocalDate.of(2025, 2, 26), endDate = LocalDate.of(2025, 3, 2))
        )
        periodsFlow.value = periods

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(28.0, state.avgCycleLength, 0.01)
            assertEquals(2, state.cycleLengths.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `calculates average period length correctly`() = runTest {
        // Given - periods with 5 days each
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)),
            Period(id = 2, startDate = LocalDate.of(2025, 1, 29), endDate = LocalDate.of(2025, 2, 2))
        )
        periodsFlow.value = periods

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(5.0, state.avgPeriodLength, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `uses default period length for ongoing periods`() = runTest {
        // Given - one ongoing period (no end date)
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = null)
        )
        periodsFlow.value = periods

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(5.0, state.avgPeriodLength, 0.01) // Default value
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `symptom stats are calculated correctly`() = runTest {
        // Given
        val records = listOf(
            DailyRecord(
                date = LocalDate.of(2025, 1, 1),
                physicalSymptoms = listOf(Symptom.CRAMPS, Symptom.HEADACHE)
            ),
            DailyRecord(
                date = LocalDate.of(2025, 1, 2),
                physicalSymptoms = listOf(Symptom.CRAMPS)
            ),
            DailyRecord(
                date = LocalDate.of(2025, 1, 3),
                physicalSymptoms = listOf(Symptom.BLOATING)
            )
        )
        recordsFlow.value = records

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(3, state.symptomStats.size)

            // CRAMPS should be first (most common) - 2 out of 3 records have cramps
            val crampsStat = state.symptomStats.find { it.symptom == Symptom.CRAMPS }
            assertEquals(2, crampsStat?.count)
            assertEquals(0.67f, crampsStat?.percentage ?: 0f, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `mood stats are calculated correctly`() = runTest {
        // Given
        val records = listOf(
            DailyRecord(date = LocalDate.of(2025, 1, 1), mood = Mood.HAPPY),
            DailyRecord(date = LocalDate.of(2025, 1, 2), mood = Mood.HAPPY),
            DailyRecord(date = LocalDate.of(2025, 1, 3), mood = Mood.DEPRESSED)
        )
        recordsFlow.value = records

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(2, state.moodStats.size)

            val happyStat = state.moodStats.find { it.mood == Mood.HAPPY }
            assertEquals(2, happyStat?.count)
            assertEquals(0.67f, happyStat?.percentage ?: 0f, 0.01f)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty symptoms returns empty symptom stats`() = runTest {
        // Given - records without symptoms
        val records = listOf(
            DailyRecord(date = LocalDate.of(2025, 1, 1), physicalSymptoms = emptyList()),
            DailyRecord(date = LocalDate.of(2025, 1, 2), physicalSymptoms = emptyList())
        )
        recordsFlow.value = records

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertTrue(state.symptomStats.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty moods returns empty mood stats`() = runTest {
        // Given - records without moods
        val records = listOf(
            DailyRecord(date = LocalDate.of(2025, 1, 1), mood = null),
            DailyRecord(date = LocalDate.of(2025, 1, 2), mood = null)
        )
        recordsFlow.value = records

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertTrue(state.moodStats.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `total records count is correct`() = runTest {
        // Given
        val records = listOf(
            DailyRecord(date = LocalDate.of(2025, 1, 1)),
            DailyRecord(date = LocalDate.of(2025, 1, 2)),
            DailyRecord(date = LocalDate.of(2025, 1, 3))
        )
        recordsFlow.value = records

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertEquals(3, state.totalRecords)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cycle lengths excludes implausible values`() = runTest {
        // Given - periods with implausible cycle
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)),
            Period(id = 2, startDate = LocalDate.of(2025, 1, 2), endDate = LocalDate.of(2025, 1, 6)) // Only 1 day later
        )
        periodsFlow.value = periods

        every { predictUseCase.isPlausibleCycleLength(1, 28) } returns false

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertTrue(state.cycleLengths.isEmpty()) // Implausible cycle filtered out
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single period results in empty cycle lengths`() = runTest {
        // Given - only one period
        val periods = listOf(
            Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5))
        )
        periodsFlow.value = periods

        // Then
        viewModel.uiState.test {
            skipItems(1) // Skip initial state
            val state = awaitItem()
            assertTrue(state.cycleLengths.isEmpty())
            assertEquals(0.0, state.avgCycleLength, 0.01)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
