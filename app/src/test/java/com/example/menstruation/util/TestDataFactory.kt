package com.example.menstruation.util

import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.FlowLevel
import com.example.menstruation.data.model.Mood
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.OvulationResult
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.Symptom
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import java.time.LocalDate

/**
 * Factory class for creating test data objects.
 * Centralizes test data creation to ensure consistency across tests.
 */
object TestDataFactory {

    // ==================== Date Constants ====================
    val TODAY: LocalDate = LocalDate.of(2025, 2, 13)
    val YESTERDAY: LocalDate = TODAY.minusDays(1)
    val TOMORROW: LocalDate = TODAY.plusDays(1)
    val ONE_WEEK_AGO: LocalDate = TODAY.minusWeeks(1)
    val ONE_MONTH_AGO: LocalDate = TODAY.minusMonths(1)
    val ONE_MONTH_LATER: LocalDate = TODAY.plusMonths(1)

    // ==================== UserSettings ====================
    fun createUserSettings(
        periodLength: Int = 5,
        cycleLength: Int = 28,
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        notificationSettings: NotificationSettings = NotificationSettings()
    ): UserSettings = UserSettings(
        periodLength = periodLength,
        cycleLength = cycleLength,
        themeMode = themeMode,
        notificationSettings = notificationSettings
    )

    // ==================== Period ====================
    fun createPeriod(
        startDate: LocalDate = TODAY,
        endDate: LocalDate? = startDate.plusDays(4),
        id: Long = 0
    ): Period = Period(
        id = id,
        startDate = startDate,
        endDate = endDate
    )

    fun createPeriodsRegularCycles(
        count: Int = 6,
        cycleLength: Int = 28,
        periodLength: Int = 5,
        endDate: LocalDate = TODAY
    ): List<Period> {
        val periods = mutableListOf<Period>()
        var currentStart = endDate

        repeat(count) { index ->
            val period = createPeriod(
                startDate = currentStart,
                endDate = currentStart.plusDays((periodLength - 1).toLong()),
                id = (count - index).toLong()
            )
            periods.add(0, period) // Add to beginning to maintain chronological order
            currentStart = currentStart.minusDays(cycleLength.toLong())
        }

        return periods
    }

    fun createPeriodsIrregularCycles(
        endDate: LocalDate = TODAY
    ): List<Period> = listOf(
        createPeriod(startDate = endDate, endDate = endDate.plusDays(4), id = 6),
        createPeriod(startDate = endDate.minusDays(25), endDate = endDate.minusDays(21), id = 5),
        createPeriod(startDate = endDate.minusDays(55), endDate = endDate.minusDays(50), id = 4),
        createPeriod(startDate = endDate.minusDays(80), endDate = endDate.minusDays(76), id = 3),
        createPeriod(startDate = endDate.minusDays(110), endDate = endDate.minusDays(105), id = 2),
        createPeriod(startDate = endDate.minusDays(135), endDate = endDate.minusDays(130), id = 1)
    )

    fun createSinglePeriod(
        startDate: LocalDate = TODAY.minusDays(30),
        endDate: LocalDate = startDate.plusDays(4)
    ): List<Period> = listOf(
        createPeriod(startDate = startDate, endDate = endDate, id = 1)
    )

    fun createOngoingPeriod(
        startDate: LocalDate = TODAY.minusDays(2)
    ): Period = createPeriod(
        startDate = startDate,
        endDate = null,
        id = 1
    )

    // ==================== DailyRecord ====================
    fun createDailyRecord(
        date: LocalDate = TODAY,
        isPeriodDay: Boolean = false,
        flowLevel: FlowLevel? = null,
        painLevel: Int? = null,
        hadSex: Boolean = false,
        physicalSymptoms: List<Symptom> = emptyList(),
        mood: Mood? = null,
        ovulationTest: OvulationResult? = null,
        note: String? = null
    ): DailyRecord = DailyRecord(
        date = date,
        isPeriodDay = isPeriodDay,
        flowLevel = flowLevel,
        painLevel = painLevel,
        hadSex = hadSex,
        physicalSymptoms = physicalSymptoms,
        mood = mood,
        ovulationTest = ovulationTest,
        note = note
    )

    fun createDailyRecordsForPeriod(
        periodStart: LocalDate,
        periodLength: Int = 5
    ): List<DailyRecord> {
        return (0 until periodLength).map { dayOffset ->
            createDailyRecord(
                date = periodStart.plusDays(dayOffset.toLong()),
                isPeriodDay = true,
                flowLevel = when (dayOffset) {
                    0, periodLength - 1 -> FlowLevel.LIGHT
                    in 1..2 -> FlowLevel.MEDIUM
                    else -> FlowLevel.HEAVY
                }
            )
        }
    }

    // ==================== Symptoms ====================
    fun createSymptomsList(): List<Symptom> = listOf(
        Symptom.CRAMPS,
        Symptom.BLOATING,
        Symptom.HEADACHE,
        Symptom.FATIGUE
    )

    fun createAllSymptoms(): List<Symptom> = Symptom.values().toList()

    // ==================== Moods ====================
    fun createMoodsList(): List<Mood> = listOf(
        Mood.HAPPY,
        Mood.IRRITABLE,
        Mood.TIRED
    )

    // ==================== Complex Scenarios ====================
    fun createCompleteScenario(): Triple<List<Period>, List<DailyRecord>, UserSettings> {
        val periods = createPeriodsRegularCycles(count = 6)
        val records = periods.flatMap { period ->
            period.endDate?.let { endDate ->
                val length = java.time.temporal.ChronoUnit.DAYS.between(
                    period.startDate,
                    endDate
                ) + 1
                createDailyRecordsForPeriod(
                    periodStart = period.startDate,
                    periodLength = length.toInt()
                )
            } ?: emptyList()
        }
        val settings = createUserSettings()
        return Triple(periods, records, settings)
    }
}
