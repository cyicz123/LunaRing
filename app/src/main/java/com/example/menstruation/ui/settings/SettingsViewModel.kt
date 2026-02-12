package com.example.menstruation.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.data.repository.SettingsRepository
import com.example.menstruation.domain.usecase.ExportImportUseCase
import com.example.menstruation.domain.usecase.ExportResult
import com.example.menstruation.domain.usecase.ImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(periodLength = 5, cycleLength = 28),
    val isLoading: Boolean = true
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val exportImportUseCase: ExportImportUseCase
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
                _uiState.value = SettingsUiState(
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
}
