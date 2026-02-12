package com.example.menstruation.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.data.model.Mood
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.Symptom
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.PredictNextPeriodUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class StatsUiState(
    val isLoading: Boolean = false,
    val periods: List<Period> = emptyList(),
    val cycleLengths: List<Pair<Int, String>> = emptyList(), // (长度, 月份标签)
    val avgCycleLength: Double = 0.0,
    val avgPeriodLength: Double = 0.0,
    val symptomStats: List<SymptomStat> = emptyList(),
    val moodStats: List<MoodStat> = emptyList(),
    val totalRecords: Int = 0
)

data class SymptomStat(
    val symptom: Symptom,
    val count: Int,
    val percentage: Float
)

data class MoodStat(
    val mood: Mood,
    val count: Int,
    val percentage: Float
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val periodRepository: PeriodRepository,
    private val dailyRecordRepository: DailyRecordRepository,
    private val settingsRepository: SettingsRepository,
    private val predictNextPeriodUseCase: PredictNextPeriodUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            combine(
                periodRepository.getAllPeriods(),
                dailyRecordRepository.getAllRecords(),
                settingsRepository.settings
            ) { periods, records, settings ->
                val sortedPeriods = periods.sortedBy { it.startDate }

                // 计算周期长度趋势
                val cycleLengths = calculateCycleLengths(sortedPeriods, settings.cycleLength)

                // 计算平均周期长度
                val avgCycle = if (cycleLengths.isNotEmpty()) {
                    cycleLengths.map { it.first }.average()
                } else 0.0

                // 计算平均经期长度
                val avgPeriod = if (sortedPeriods.isNotEmpty()) {
                    sortedPeriods.map { period ->
                        if (period.endDate != null) {
                            ChronoUnit.DAYS.between(period.startDate, period.endDate).toInt() + 1
                        } else {
                            5 // 默认
                        }
                    }.average()
                } else 0.0

                // 统计症状
                val allSymptoms = records.flatMap { it.physicalSymptoms }
                val symptomCounts = allSymptoms.groupingBy { it }.eachCount()
                val totalSymptomRecords = records.count { it.physicalSymptoms.isNotEmpty() }
                val symptomStats = symptomCounts.map { (symptom, count) ->
                    SymptomStat(
                        symptom = symptom,
                        count = count,
                        percentage = if (totalSymptomRecords > 0) {
                            count.toFloat() / totalSymptomRecords
                        } else 0f
                    )
                }.sortedByDescending { it.count }

                // 统计心情
                val allMoods = records.mapNotNull { it.mood }
                val moodCounts = allMoods.groupingBy { it }.eachCount()
                val totalMoodRecords = allMoods.size
                val moodStats = moodCounts.map { (mood, count) ->
                    MoodStat(
                        mood = mood,
                        count = count,
                        percentage = if (totalMoodRecords > 0) {
                            count.toFloat() / totalMoodRecords
                        } else 0f
                    )
                }.sortedByDescending { it.count }

                StatsUiState(
                    isLoading = false,
                    periods = sortedPeriods,
                    cycleLengths = cycleLengths,
                    avgCycleLength = avgCycle,
                    avgPeriodLength = avgPeriod,
                    symptomStats = symptomStats,
                    moodStats = moodStats,
                    totalRecords = records.size
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    private fun calculateCycleLengths(periods: List<Period>, cycleLengthSetting: Int): List<Pair<Int, String>> {
        if (periods.size < 2) return emptyList()

        return periods.zipWithNext { a, b ->
            val length = ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
            val label = "${b.startDate.monthValue}月"
            length to label
        }.filter { (length, _) ->
            predictNextPeriodUseCase.isPlausibleCycleLength(length, cycleLengthSetting)
        }
    }
}
