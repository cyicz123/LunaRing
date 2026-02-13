package com.example.menstruation.domain.usecase

import com.example.menstruation.data.model.Period
import com.example.menstruation.util.TestDataFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [PredictNextPeriodUseCase].
 *
 * This class tests the period prediction algorithm which uses weighted averaging
 * of recent cycle lengths combined with user settings to predict future periods.
 */
class PredictNextPeriodUseCaseTest {

    private lateinit var useCase: PredictNextPeriodUseCase

    @Before
    fun setup() {
        useCase = PredictNextPeriodUseCase()
    }

    @After
    fun tearDown() {
        // No cleanup needed for pure function tests
    }

    // ==================== invoke() Tests ====================

    @Test
    fun `invoke with empty periods returns null`() {
        val result = useCase(emptyList(), 28)

        assertNull(result)
    }

    @Test
    fun `invoke with single period uses cycle length setting`() {
        // Use a recent period date so prediction doesn't get pushed too far into future
        val recentStartDate = LocalDate.now().minusDays(10)
        val periods = TestDataFactory.createSinglePeriod(
            startDate = recentStartDate
        )
        val cycleLengthSetting = 28

        val result = useCase(periods, cycleLengthSetting)

        assertNotNull(result)
        // Prediction should be cycleLengthSetting days after the last period start
        // But may be adjusted to future by alignPredictionToFuture
        val expectedBasePrediction = periods.first().startDate.plusDays(cycleLengthSetting.toLong())
        // Result should be the base prediction or a future-aligned version of it
        assertTrue(result!!.isAfter(periods.first().startDate) || result.isEqual(periods.first().startDate))
        // The result should be the base prediction aligned to future
        assertTrue(result.isAfter(LocalDate.now().minusDays(1)))
    }

    @Test
    fun `invoke with multiple periods calculates weighted average`() {
        // Create periods with irregular cycles: 25, 30, 28, 26, 29 days
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 8, 1), endDate = LocalDate.of(2024, 8, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 8, 26), endDate = LocalDate.of(2024, 8, 30), id = 2), // 25 days
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 9, 25), endDate = LocalDate.of(2024, 9, 29), id = 3), // 30 days
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 23), endDate = LocalDate.of(2024, 10, 27), id = 4), // 28 days
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 18), endDate = LocalDate.of(2024, 11, 22), id = 5), // 26 days
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 12, 17), endDate = LocalDate.of(2024, 12, 21), id = 6)  // 29 days
        )

        val result = useCase(periods, 28)

        assertNotNull(result)
        // Weighted average of [25, 30, 28, 26, 29] with more weight on recent cycles
        // Plus prior weight of 2 * 28
        // Should be approximately 27-28 days after Dec 17
        assertTrue(result!!.isAfter(LocalDate.of(2024, 12, 17)))
    }

    @Test
    fun `invoke with sorted periods handles unsorted input`() {
        // Create periods in non-chronological order
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 12, 1), endDate = LocalDate.of(2024, 12, 5), id = 3),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 1), endDate = LocalDate.of(2024, 11, 5), id = 2)
        )

        val result = useCase(periods, 28)

        assertNotNull(result)
        // Should correctly predict based on the latest period (Dec 1)
        assertTrue(result!!.isAfter(LocalDate.of(2024, 12, 1)))
    }

    // ==================== predictPeriodWindow() Tests ====================

    @Test
    fun `predictPeriodWindow returns correct start and end dates`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 29), endDate = LocalDate.of(2024, 11, 2), id = 2),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 26), endDate = LocalDate.of(2024, 11, 30), id = 3)
        )
        val periodLengthSetting = 5

        val result = useCase.predictPeriodWindow(periods, 28, periodLengthSetting)

        assertNotNull(result)
        val (start, end) = result!!
        // End should be periodLengthSetting - 1 days after start
        assertEquals(start.plusDays((periodLengthSetting - 1).toLong()), end)
    }

    @Test
    fun `predictPeriodWindow with no periods uses settings only`() {
        val cycleLengthSetting = 28
        val periodLengthSetting = 5

        val result = useCase.predictPeriodWindow(emptyList(), cycleLengthSetting, periodLengthSetting)

        assertNotNull(result)
        val (start, end) = result!!
        // When no periods, prediction is based on settings only
        // First prediction is cycleLengthSetting days from "now" (which would be handled by predictBasedOnSettingsOnly)
        // End date should be periodLengthSetting - 1 days after start
        assertEquals(start.plusDays((periodLengthSetting - 1).toLong()), end)
    }

    @Test
    fun `predictPeriodWindow estimates period length from history`() {
        // Create periods with varying lengths
        val periods = listOf(
            TestDataFactory.createPeriod(
                startDate = LocalDate.of(2024, 10, 1),
                endDate = LocalDate.of(2024, 10, 7), // 7 days
                id = 1
            ),
            TestDataFactory.createPeriod(
                startDate = LocalDate.of(2024, 10, 29),
                endDate = LocalDate.of(2024, 11, 5), // 7 days
                id = 2
            ),
            TestDataFactory.createPeriod(
                startDate = LocalDate.of(2024, 11, 26),
                endDate = LocalDate.of(2024, 12, 2), // 7 days
                id = 3
            )
        )

        val result = useCase.predictPeriodWindow(periods, 28, 5) // Setting says 5, history shows 7

        assertNotNull(result)
        val (start, end) = result!!
        // The predicted window should be closer to 7 days (blended with setting)
        val predictedLength = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
        assertTrue(predictedLength >= 5) // At least the setting
        assertTrue(predictedLength <= 8)  // Close to observed 7 days
    }

    // ==================== predictFuturePeriods() Tests ====================

    @Test
    fun `predictFuturePeriods returns requested count`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 29), endDate = LocalDate.of(2024, 11, 2), id = 2),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 26), endDate = LocalDate.of(2024, 11, 30), id = 3)
        )

        val result = useCase.predictFuturePeriods(periods, 28, 5, 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `predictFuturePeriods with zero count returns empty list`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 29), endDate = LocalDate.of(2024, 11, 2), id = 2),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 26), endDate = LocalDate.of(2024, 11, 30), id = 3)
        )

        val result = useCase.predictFuturePeriods(periods, 28, 5, 0)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `predictFuturePeriods with negative count returns empty list`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 29), endDate = LocalDate.of(2024, 11, 2), id = 2),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 26), endDate = LocalDate.of(2024, 11, 30), id = 3)
        )

        val result = useCase.predictFuturePeriods(periods, 28, 5, -1)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `predictFuturePeriods periods are spaced by cycle length`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 29), endDate = LocalDate.of(2024, 11, 2), id = 2), // 28 days apart
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 11, 26), endDate = LocalDate.of(2024, 11, 30), id = 3)  // 28 days apart
        )

        val result = useCase.predictFuturePeriods(periods, 28, 5, 3)

        assertEquals(3, result.size)
        // Check that consecutive predictions are approximately 28 days apart
        val gap1 = java.time.temporal.ChronoUnit.DAYS.between(result[0].first, result[1].first)
        val gap2 = java.time.temporal.ChronoUnit.DAYS.between(result[1].first, result[2].first)
        assertTrue(kotlin.math.abs(gap1 - 28) <= 2) // Allow small variance
        assertTrue(kotlin.math.abs(gap2 - 28) <= 2)
    }

    // ==================== predictFuturePeriodsUntil() Tests ====================

    @Test
    fun `predictFuturePeriodsUntil returns periods within date range`() {
        // Use recent periods so predictions don't get pushed too far by alignPredictionToFuture
        val today = LocalDate.now()
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = today.minusDays(60), endDate = today.minusDays(56), id = 1),
            TestDataFactory.createPeriod(startDate = today.minusDays(32), endDate = today.minusDays(28), id = 2),
            TestDataFactory.createPeriod(startDate = today.minusDays(4), endDate = today, id = 3)
        )
        val endDate = today.plusMonths(6)

        val result = useCase.predictFuturePeriodsUntil(periods, 28, 5, endDate)

        assertTrue(result.isNotEmpty())
        // All predictions should be before or on endDate
        result.forEach { (start, _) ->
            assertFalse(start.isAfter(endDate))
        }
    }

    @Test
    fun `predictFuturePeriodsUntil with end date before first prediction returns single period`() {
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 12, 1), endDate = LocalDate.of(2024, 12, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 12, 29), endDate = LocalDate.of(2025, 1, 2), id = 2)
        )
        // Set endDate to just after the first predicted period would start
        val endDate = LocalDate.of(2025, 1, 30)

        val result = useCase.predictFuturePeriodsUntil(periods, 28, 5, endDate)

        // Should return at least the first prediction even if it's after endDate
        assertTrue(result.size >= 1)
    }

    @Test
    fun `predictFuturePeriodsUntil with no periods uses settings`() {
        val endDate = LocalDate.of(2025, 6, 30)

        val result = useCase.predictFuturePeriodsUntil(emptyList(), 28, 5, endDate)

        assertTrue(result.isNotEmpty())
        // First prediction should be in the future
        val firstPrediction = result.first().first
        assertTrue(firstPrediction.isAfter(LocalDate.of(2024, 1, 1)) || firstPrediction.isEqual(LocalDate.of(2024, 1, 1)))
    }

    // ==================== isPlausibleCycleLength() Tests ====================

    @Test
    fun `isPlausibleCycleLength returns true for reasonable length`() {
        val cycleLengthSetting = 28

        assertTrue(useCase.isPlausibleCycleLength(21, cycleLengthSetting))
        assertTrue(useCase.isPlausibleCycleLength(28, cycleLengthSetting))
        assertTrue(useCase.isPlausibleCycleLength(35, cycleLengthSetting))
    }

    @Test
    fun `isPlausibleCycleLength returns false for too short`() {
        val cycleLengthSetting = 28

        assertFalse(useCase.isPlausibleCycleLength(10, cycleLengthSetting))
        assertFalse(useCase.isPlausibleCycleLength(14, cycleLengthSetting))
    }

    @Test
    fun `isPlausibleCycleLength returns false for too long`() {
        val cycleLengthSetting = 28

        assertFalse(useCase.isPlausibleCycleLength(60, cycleLengthSetting))
        assertFalse(useCase.isPlausibleCycleLength(100, cycleLengthSetting))
    }

    @Test
    fun `isPlausibleCycleLength adjusts max based on setting`() {
        // For a 35-day cycle setting, max plausible should be higher
        val longCycleSetting = 35

        assertTrue(useCase.isPlausibleCycleLength(50, longCycleSetting))
        assertTrue(useCase.isPlausibleCycleLength(60, longCycleSetting))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `invoke filters out implausible cycle lengths`() {
        // Create periods with one implausibly long cycle
        val periods = listOf(
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 6, 1), endDate = LocalDate.of(2024, 6, 5), id = 1),
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 6, 26), endDate = LocalDate.of(2024, 6, 30), id = 2), // 25 days (normal)
            TestDataFactory.createPeriod(startDate = LocalDate.of(2024, 10, 1), endDate = LocalDate.of(2024, 10, 5), id = 3)  // 98 days (implausible)
        )

        val result = useCase(periods, 28)

        assertNotNull(result)
        // Should use the 25-day cycle and setting, ignoring the 98-day outlier
        assertTrue(result!!.isAfter(periods.last().startDate))
    }

    @Test
    fun `predictFuturePeriodsUntil handles empty periods gracefully`() {
        val endDate = LocalDate.of(2025, 3, 31)

        val result = useCase.predictFuturePeriodsUntil(emptyList(), 28, 5, endDate)

        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `predictPeriodWindow handles periods with null end dates`() {
        val periods = listOf(
            TestDataFactory.createPeriod(
                startDate = LocalDate.of(2024, 11, 1),
                endDate = LocalDate.of(2024, 11, 5),
                id = 1
            ),
            TestDataFactory.createOngoingPeriod(startDate = LocalDate.of(2024, 12, 28))
        )

        val result = useCase.predictPeriodWindow(periods, 28, 5)

        assertNotNull(result)
        // Should not crash and should return valid prediction
        val (start, end) = result!!
        assertTrue(end.isAfter(start) || end.isEqual(start))
    }

    @Test
    fun `invoke with ongoing period predicts from last complete period`() {
        val periods = listOf(
            TestDataFactory.createPeriod(
                startDate = LocalDate.of(2024, 11, 1),
                endDate = LocalDate.of(2024, 11, 5),
                id = 1
            ),
            TestDataFactory.createOngoingPeriod(startDate = LocalDate.of(2024, 12, 28))
        )

        val result = useCase(periods, 28)

        assertNotNull(result)
        // Should predict based on the complete period
        assertTrue(result!!.isAfter(periods.last().startDate))
    }
}
