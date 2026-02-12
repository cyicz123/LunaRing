package com.example.menstruation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.PredictNextPeriodUseCase
import com.example.menstruation.ui.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val periodRepository: PeriodRepository,
    private val dailyRecordRepository: DailyRecordRepository,
    private val settingsRepository: SettingsRepository,
    private val predictUseCase: PredictNextPeriodUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                periodRepository.getAllPeriods(),
                settingsRepository.settings
            ) { periods, settings ->
                // 预测完整的经期窗口（开始和结束日期）
                val predictedWindow = predictUseCase.predictPeriodWindow(
                    periods,
                    settings.cycleLength,
                    settings.periodLength
                )
                Triple(periods, settings, predictedWindow)
            }.collectLatest { (periods, settings, predictedWindow) ->
                _uiState.value = _uiState.value.copy(
                    periods = periods,
                    settings = settings,
                    predictedPeriod = predictedWindow
                )
            }
        }

        // 加载每日记录
        viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.minusMonths(12)
            val end = today.plusMonths(6)

            dailyRecordRepository.getRecordsByDateRange(start, end).collectLatest { records ->
                val recordsMap = records.associateBy { it.date }
                _uiState.value = _uiState.value.copy(
                    records = recordsMap
                )
            }
        }
    }

    fun startPeriod(date: LocalDate) {
        viewModelScope.launch {
            val settings = _uiState.value.settings
            periodRepository.startPeriod(date, settings.periodLength)
        }
    }

    fun endPeriod(date: LocalDate) {
        viewModelScope.launch {
            periodRepository.endPeriod(date)
        }
    }

    fun saveRecord(record: DailyRecord) {
        viewModelScope.launch {
            dailyRecordRepository.saveRecord(record)
        }
    }

    fun isInPeriod(date: LocalDate): Boolean {
        return _uiState.value.periods.any { period ->
            !date.isBefore(period.startDate) && (period.endDate == null || !date.isAfter(period.endDate))
        }
    }
}
