package com.example.menstruation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(periodLength = 5, cycleLength = 28),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
}
