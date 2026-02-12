package com.example.menstruation.ui.home.state

import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.UserSettings
import java.time.LocalDate

data class HomeUiState(
    val records: Map<LocalDate, DailyRecord> = emptyMap(),
    val periods: List<Period> = emptyList(),
    val predictedPeriod: Pair<LocalDate, LocalDate>? = null, // 预测经期窗口（开始和结束）
    val predictedPeriods: List<Pair<LocalDate, LocalDate>> = emptyList(), // 未来多周期预测
    val settings: UserSettings = UserSettings(5, 28),
    val isLoading: Boolean = false,
    val error: String? = null
)