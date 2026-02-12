package com.example.menstruation.domain.usecase

import com.example.menstruation.data.model.Period
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

class PredictNextPeriodUseCase @Inject constructor() {

    operator fun invoke(
        periods: List<Period>,
        cycleLengthSetting: Int
    ): LocalDate? {
        if (periods.isEmpty()) {
            return null
        }

        val sortedPeriods = periods.sortedBy { it.startDate }
        val estimatedCycleLength = estimateCycleLength(sortedPeriods, cycleLengthSetting)
        val basePrediction = sortedPeriods.last().startDate.plusDays(estimatedCycleLength.toLong())
        return alignPredictionToFuture(basePrediction, estimatedCycleLength)
    }

    /**
     * 预测整个经期窗口（开始日期到结束日期）
     */
    fun predictPeriodWindow(
        periods: List<Period>,
        cycleLengthSetting: Int,
        periodLengthSetting: Int
    ): Pair<LocalDate, LocalDate>? {
        val nextStart = invoke(periods, cycleLengthSetting) ?: predictBasedOnSettingsOnly(cycleLengthSetting)
        val estimatedPeriodLength = estimatePeriodLength(periods, periodLengthSetting)
        val nextEnd = nextStart.plusDays((estimatedPeriodLength - 1).toLong())
        return nextStart to nextEnd
    }

    fun predictFuturePeriods(
        periods: List<Period>,
        cycleLengthSetting: Int,
        periodLengthSetting: Int,
        count: Int
    ): List<Pair<LocalDate, LocalDate>> {
        if (count <= 0) return emptyList()

        val horizon = LocalDate.now().plusDays((count * cycleLengthSetting).toLong())
        return predictFuturePeriodsUntil(
            periods = periods,
            cycleLengthSetting = cycleLengthSetting,
            periodLengthSetting = periodLengthSetting,
            endDate = horizon
        ).take(count)
    }

    fun predictFuturePeriodsUntil(
        periods: List<Period>,
        cycleLengthSetting: Int,
        periodLengthSetting: Int,
        endDate: LocalDate
    ): List<Pair<LocalDate, LocalDate>> {
        val sortedPeriods = periods.sortedBy { it.startDate }
        val estimatedCycleLength = estimateCycleLength(sortedPeriods, cycleLengthSetting)
        val estimatedPeriodLength = estimatePeriodLength(sortedPeriods, periodLengthSetting)
        val firstStart = invoke(sortedPeriods, cycleLengthSetting) ?: predictBasedOnSettingsOnly(cycleLengthSetting)

        if (firstStart.isAfter(endDate)) {
            val firstEnd = firstStart.plusDays((estimatedPeriodLength - 1).toLong())
            return listOf(firstStart to firstEnd)
        }

        val predictions = mutableListOf<Pair<LocalDate, LocalDate>>()
        var currentStart = firstStart
        while (!currentStart.isAfter(endDate)) {
            val currentEnd = currentStart.plusDays((estimatedPeriodLength - 1).toLong())
            predictions += currentStart to currentEnd
            currentStart = currentStart.plusDays(estimatedCycleLength.toLong())
        }
        return predictions
    }

    fun isPlausibleCycleLength(length: Int, cycleLengthSetting: Int): Boolean {
        val minPlausible = MIN_PLAUSIBLE_CYCLE_LENGTH
        val maxPlausible = max(BASE_MAX_PLAUSIBLE_CYCLE_LENGTH, (cycleLengthSetting * MAX_CYCLE_MULTIPLIER).roundToInt())
        return length in minPlausible..maxPlausible
    }

    /**
     * 当没有历史记录时，基于设置预测下一个经期
     * 使用当前日期作为参考点
     */
    private fun predictBasedOnSettingsOnly(cycleLengthSetting: Int): LocalDate {
        return LocalDate.now().plusDays(cycleLengthSetting.toLong())
    }

    private fun estimateCycleLength(periods: List<Period>, cycleLengthSetting: Int): Int {
        if (periods.size < 2) return cycleLengthSetting

        val recentPeriods = periods.takeLast(RECENT_PERIOD_COUNT_FOR_ESTIMATE)
        val validCycleLengths = recentPeriods.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
        }.filter { isPlausibleCycleLength(it, cycleLengthSetting) }

        if (validCycleLengths.isEmpty()) return cycleLengthSetting

        val weightedObservedSum = validCycleLengths.reversed().mapIndexed { index, length ->
            val weight = index + 1
            length * weight
        }.sum()
        val observedWeightSum = (1..validCycleLengths.size).sum()
        val totalWeight = observedWeightSum + PRIOR_WEIGHT
        val blended = (weightedObservedSum + cycleLengthSetting * PRIOR_WEIGHT).toDouble() / totalWeight

        return blended.roundToInt().coerceAtLeast(MIN_PLAUSIBLE_CYCLE_LENGTH)
    }

    private fun estimatePeriodLength(periods: List<Period>, periodLengthSetting: Int): Int {
        val observedLengths = periods.mapNotNull { period ->
            val endDate = period.endDate ?: return@mapNotNull null
            (ChronoUnit.DAYS.between(period.startDate, endDate).toInt() + 1)
                .takeIf { it in MIN_PLAUSIBLE_PERIOD_LENGTH..MAX_PLAUSIBLE_PERIOD_LENGTH }
        }

        if (observedLengths.isEmpty()) return periodLengthSetting

        val blended = (observedLengths.average() * OBSERVED_PERIOD_WEIGHT +
            periodLengthSetting * PRIOR_WEIGHT).toDouble() / (OBSERVED_PERIOD_WEIGHT + PRIOR_WEIGHT)
        return blended.roundToInt().coerceIn(MIN_PLAUSIBLE_PERIOD_LENGTH, MAX_PLAUSIBLE_PERIOD_LENGTH)
    }

    private fun alignPredictionToFuture(prediction: LocalDate, cycleLength: Int): LocalDate {
        val today = LocalDate.now()
        var adjusted = prediction

        while (!adjusted.isAfter(today)) {
            adjusted = adjusted.plusDays(cycleLength.toLong())
        }

        return adjusted
    }

    companion object {
        private const val RECENT_PERIOD_COUNT_FOR_ESTIMATE = 6
        private const val PRIOR_WEIGHT = 2
        private const val OBSERVED_PERIOD_WEIGHT = 1
        private const val MIN_PLAUSIBLE_CYCLE_LENGTH = 15
        private const val BASE_MAX_PLAUSIBLE_CYCLE_LENGTH = 50
        private const val MAX_CYCLE_MULTIPLIER = 1.8
        private const val MIN_PLAUSIBLE_PERIOD_LENGTH = 1
        private const val MAX_PLAUSIBLE_PERIOD_LENGTH = 15
    }
}
