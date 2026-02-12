package com.example.menstruation.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.ReminderTime
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.ExportImportUseCase
import com.example.menstruation.domain.usecase.ExportResult
import com.example.menstruation.domain.usecase.ImportResult
import com.example.menstruation.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(periodLength = 5, cycleLength = 28),
    val isLoading: Boolean = true,
    val showTimePicker: Boolean = false
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val dailyRecordRepository: DailyRecordRepository,
    private val periodRepository: PeriodRepository,
    private val exportImportUseCase: ExportImportUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        settingsRepository.settings
            .onEach { settings ->
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }

    fun updatePeriodLength(length: Int) {
        viewModelScope.launch {
            settingsRepository.updatePeriodLength(length)
        }
    }

    fun updateCycleLength(length: Int) {
        viewModelScope.launch {
            settingsRepository.updateCycleLength(length)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    // Notification settings
    fun updateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationEnabled(enabled)
            scheduleNotifications()
        }
    }

    fun updatePeriodStartReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePeriodStartReminder(enabled)
            scheduleNotifications()
        }
    }

    fun updatePeriodEndReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePeriodEndReminder(enabled)
            scheduleNotifications()
        }
    }

    fun updatePredictedPeriodReminder(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updatePredictedPeriodReminder(enabled)
            scheduleNotifications()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderTime(ReminderTime(hour, minute))
            scheduleNotifications()
        }
    }

    fun showTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = true)
    }

    fun hideTimePicker() {
        _uiState.value = _uiState.value.copy(showTimePicker = false)
    }

    private fun scheduleNotifications() {
        viewModelScope.launch {
            val settings = uiState.value.settings
            val periods = periodRepository.getAllPeriods().first()

            val reminderTime = LocalTime.of(
                settings.notificationSettings.reminderTime.hour,
                settings.notificationSettings.reminderTime.minute
            )

            notificationScheduler.scheduleAllNotifications(
                periods = periods,
                cycleLength = settings.cycleLength,
                periodLength = settings.periodLength,
                reminderTime = reminderTime,
                enabled = settings.notificationSettings.enabled,
                periodStartReminder = settings.notificationSettings.periodStartReminder,
                periodEndReminder = settings.notificationSettings.periodEndReminder,
                predictedPeriodReminder = settings.notificationSettings.predictedPeriodReminder
            )
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch {
            when (val result = exportImportUseCase.exportToJson(uri)) {
                is ExportResult.Success -> {
                    _events.emit(SettingsEvent.ShowMessage("导出成功"))
                }
                is ExportResult.Error -> {
                    _events.emit(SettingsEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            when (val result = exportImportUseCase.importFromJson(uri)) {
                is ImportResult.Success -> {
                    _events.emit(SettingsEvent.ShowMessage("导入成功"))
                }
                is ImportResult.Error -> {
                    _events.emit(SettingsEvent.ShowMessage(result.message))
                }
                is ImportResult.PartialSuccess -> {
                    _events.emit(SettingsEvent.ShowMessage(
                        "部分导入成功：${result.importedPeriods}个周期, ${result.importedRecords}条记录"
                    ))
                }
            }
        }
    }

    fun generateExportFileName(): String {
        return exportImportUseCase.generateExportFileName()
    }

    fun resetRecordsData() {
        viewModelScope.launch {
            runCatching {
                dailyRecordRepository.deleteAllRecords()
                periodRepository.deleteAllPeriods()
                notificationScheduler.cancelAllNotifications()
            }.onSuccess {
                _events.emit(SettingsEvent.ShowMessage("重置成功：已清空经期与每日记录"))
            }.onFailure { error ->
                _events.emit(SettingsEvent.ShowMessage("重置失败：${error.message ?: "请稍后重试"}"))
            }
        }
    }
}
