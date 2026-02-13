package com.example.menstruation.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.menstruation.update.ApkInstaller
import com.example.menstruation.update.AppUpdateRepository
import com.example.menstruation.update.InstallResult
import com.example.menstruation.update.model.ReleaseInfo
import com.example.menstruation.update.model.UpdateCheckResult
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
import dagger.hilt.android.qualifiers.ApplicationContext
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

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Int = 0,
    val currentVersion: String = "",
    val latestRelease: ReleaseInfo? = null,
    val statusMessage: String = "点击“检查更新”获取最新版本"
)

sealed class SettingsEvent {
    data class ShowMessage(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val dailyRecordRepository: DailyRecordRepository,
    private val periodRepository: PeriodRepository,
    private val exportImportUseCase: ExportImportUseCase,
    private val notificationScheduler: NotificationScheduler,
    private val appUpdateRepository: AppUpdateRepository,
    private val apkInstaller: ApkInstaller
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()
    private val _updateUiState = MutableStateFlow(
        AppUpdateUiState(currentVersion = getCurrentVersionName())
    )
    val updateUiState: StateFlow<AppUpdateUiState> = _updateUiState.asStateFlow()

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

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateUiState.value = _updateUiState.value.copy(
                isChecking = true,
                statusMessage = "正在检查更新..."
            )

            when (val result = appUpdateRepository.checkForUpdate(getCurrentVersionName())) {
                is UpdateCheckResult.UpdateAvailable -> {
                    _updateUiState.value = _updateUiState.value.copy(
                        isChecking = false,
                        latestRelease = result.releaseInfo,
                        statusMessage = "发现新版本 ${result.releaseInfo.versionName}"
                    )
                }

                is UpdateCheckResult.UpToDate -> {
                    _updateUiState.value = _updateUiState.value.copy(
                        isChecking = false,
                        latestRelease = null,
                        statusMessage = "当前已是最新版本"
                    )
                }

                is UpdateCheckResult.Error -> {
                    _updateUiState.value = _updateUiState.value.copy(
                        isChecking = false,
                        latestRelease = null,
                        statusMessage = "检查更新失败"
                    )
                    _events.emit(SettingsEvent.ShowMessage(result.message))
                }
            }
        }
    }

    fun downloadAndInstallUpdate() {
        val releaseInfo = _updateUiState.value.latestRelease ?: return
        viewModelScope.launch {
            _updateUiState.value = _updateUiState.value.copy(
                isDownloading = true,
                downloadProgress = 0,
                statusMessage = "正在下载更新..."
            )

            val downloadResult = appUpdateRepository.downloadReleaseApk(releaseInfo) { progress ->
                _updateUiState.value = _updateUiState.value.copy(downloadProgress = progress)
            }

            downloadResult.fold(
                onSuccess = { apkFile ->
                    _updateUiState.value = _updateUiState.value.copy(
                        isDownloading = false,
                        downloadProgress = 100,
                        statusMessage = "下载完成，正在打开安装器"
                    )
                    when (val installResult = apkInstaller.installApk(apkFile)) {
                        InstallResult.StartedInstaller -> {
                            _events.emit(SettingsEvent.ShowMessage("已打开安装器，请按提示完成更新"))
                        }

                        InstallResult.NeedUnknownSourcePermission -> {
                            _events.emit(
                                SettingsEvent.ShowMessage("请先允许安装未知来源应用，然后重试安装")
                            )
                        }

                        is InstallResult.Error -> {
                            _events.emit(SettingsEvent.ShowMessage(installResult.message))
                        }
                    }
                },
                onFailure = { error ->
                    _updateUiState.value = _updateUiState.value.copy(
                        isDownloading = false,
                        statusMessage = "下载失败，请稍后重试"
                    )
                    _events.emit(
                        SettingsEvent.ShowMessage(error.message ?: "下载失败，请检查网络后重试")
                    )
                }
            )
        }
    }

    private fun getCurrentVersionName(): String {
        return runCatching {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        }.getOrDefault("0.0.0")
    }
}
