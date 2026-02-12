package com.example.menstruation.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
// import androidx.compose.material.icons.filled.AccessTime  // 不可用，使用DateRange代替
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menstruation.MainActivity
import com.example.menstruation.data.model.NotificationSettings
import com.example.menstruation.data.model.ReminderTime
import com.example.menstruation.data.model.ThemeMode
import com.example.menstruation.notification.NotificationPermissionHelper
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.PinkTransparent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onResetDataClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showNotifRationale by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.DateRange, null) },
                    label = { Text("日历") },
                    selected = false,
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("统计") },
                    selected = false,
                    onClick = onNavigateToStats
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PinkPrimary)
            }
        } else {
            SettingsContent(
                periodLength = uiState.settings.periodLength,
                cycleLength = uiState.settings.cycleLength,
                themeMode = uiState.settings.themeMode,
                notificationSettings = uiState.settings.notificationSettings,
                showTimePicker = uiState.showTimePicker,
                onPeriodLengthChange = viewModel::updatePeriodLength,
                onCycleLengthChange = viewModel::updateCycleLength,
                onThemeModeChange = viewModel::updateThemeMode,
                onNotificationEnabledChange = { enabled ->
                    val hasPerm = NotificationPermissionHelper.hasNotificationPermission(context)
                    if (enabled && Build.VERSION.SDK_INT >= 33 && !hasPerm) {
                        showNotifRationale = true
                        // Keep switch OFF until permission granted.
                        viewModel.updateNotificationEnabled(false)
                    } else {
                        viewModel.updateNotificationEnabled(enabled)
                    }
                },
                onPeriodStartReminderChange = viewModel::updatePeriodStartReminder,
                onPeriodEndReminderChange = viewModel::updatePeriodEndReminder,
                onPredictedPeriodReminderChange = viewModel::updatePredictedPeriodReminder,
                onReminderTimeChange = viewModel::updateReminderTime,
                onShowTimePicker = viewModel::showTimePicker,
                onHideTimePicker = viewModel::hideTimePicker,
                onExportClick = onExportClick,
                onImportClick = onImportClick,
                onResetDataClick = { showResetConfirmDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }

    if (showNotifRationale) {
        NotificationPermissionRationaleSheetForSettings(
            onEnable = {
                showNotifRationale = false
                (context as? MainActivity)?.requestPostNotificationsPermissionFromUi()
            },
            onLater = {
                showNotifRationale = false
                viewModel.updateNotificationEnabled(false)
            },
            onDismiss = {
                showNotifRationale = false
                viewModel.updateNotificationEnabled(false)
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("确认重置数据") },
            text = {
                Text("将清空经期记录和每日记录，设置项不会重置。此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirmDialog = false
                        onResetDataClick()
                    }
                ) {
                    Text("确认重置", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationPermissionRationaleSheetForSettings(
    onEnable: () -> Unit,
    onLater: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = PinkPrimary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "开启通知提醒",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "用于经期开始/结束提醒，以及预测经期提醒。你可以随时在设置里关闭。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = PinkTransparent),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "我们会在这些时刻通知你：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "• 经期开始 / 结束\n• 预测经期到来前 1 天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PinkPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("开启通知")
            }
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("暂不")
            }
        }
    }
}

@Composable
private fun SettingsContent(
    periodLength: Int,
    cycleLength: Int,
    themeMode: ThemeMode,
    notificationSettings: NotificationSettings,
    showTimePicker: Boolean,
    onPeriodLengthChange: (Int) -> Unit,
    onCycleLengthChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onPeriodStartReminderChange: (Boolean) -> Unit,
    onPeriodEndReminderChange: (Boolean) -> Unit,
    onPredictedPeriodReminderChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit,
    onShowTimePicker: () -> Unit,
    onHideTimePicker: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onResetDataClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 外观设置卡片
        SettingsCard(
            title = "外观",
            content = {
                ThemeModeSelector(
                    currentMode = themeMode,
                    onModeChange = onThemeModeChange
                )
            }
        )

        // 通知设置卡片
        NotificationSettingsCard(
            notificationSettings = notificationSettings,
            onNotificationEnabledChange = onNotificationEnabledChange,
            onPeriodStartReminderChange = onPeriodStartReminderChange,
            onPeriodEndReminderChange = onPeriodEndReminderChange,
            onPredictedPeriodReminderChange = onPredictedPeriodReminderChange,
            onShowTimePicker = onShowTimePicker
        )

        // 周期设置卡片
        SettingsCard(
            title = "周期设置",
            content = {
                SettingItem(
                    label = "经期长度",
                    value = periodLength,
                    unit = "天",
                    onValueChange = onPeriodLengthChange,
                    description = "每次月经持续的天数"
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                SettingItem(
                    label = "周期长度",
                    value = cycleLength,
                    unit = "天",
                    onValueChange = onCycleLengthChange,
                    description = "两次月经第一天的间隔天数"
                )
            }
        )

        // 数据管理卡片
        DataManagementCard(
            onExportClick = onExportClick,
            onImportClick = onImportClick,
            onResetDataClick = onResetDataClick
        )

        // 说明卡片
        InfoCard()

        Spacer(modifier = Modifier.height(32.dp))
    }

    // 时间选择器对话框
    if (showTimePicker) {
        ReminderTimePickerDialog(
            currentTime = notificationSettings.reminderTime,
            onTimeSelected = onReminderTimeChange,
            onDismiss = onHideTimePicker
        )
    }
}

@Composable
private fun NotificationSettingsCard(
    notificationSettings: NotificationSettings,
    onNotificationEnabledChange: (Boolean) -> Unit,
    onPeriodStartReminderChange: (Boolean) -> Unit,
    onPeriodEndReminderChange: (Boolean) -> Unit,
    onPredictedPeriodReminderChange: (Boolean) -> Unit,
    onShowTimePicker: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "通知提醒",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Switch(
                    checked = notificationSettings.enabled,
                    onCheckedChange = onNotificationEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PinkPrimary,
                        checkedTrackColor = PinkPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            if (notificationSettings.enabled) {
                Spacer(modifier = Modifier.height(16.dp))

                // 提醒时间选择
                ReminderTimeSelector(
                    reminderTime = notificationSettings.reminderTime,
                    onClick = onShowTimePicker
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 经期开始提醒
                NotificationToggleItem(
                    label = "经期开始提醒",
                    description = "在经期开始时发送通知",
                    checked = notificationSettings.periodStartReminder,
                    onCheckedChange = onPeriodStartReminderChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 经期结束提醒
                NotificationToggleItem(
                    label = "经期结束提醒",
                    description = "在经期结束时发送通知",
                    checked = notificationSettings.periodEndReminder,
                    onCheckedChange = onPeriodEndReminderChange
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // 预测经期提醒
                NotificationToggleItem(
                    label = "预测经期提醒",
                    description = "在预测经期到来前发送通知",
                    checked = notificationSettings.predictedPeriodReminder,
                    onCheckedChange = onPredictedPeriodReminderChange
                )
            }
        }
    }
}

@Composable
private fun ReminderTimeSelector(
    reminderTime: ReminderTime,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "提醒时间",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "每天发送提醒的时间",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PinkPrimary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = reminderTime.toString(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NotificationToggleItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PinkPrimary,
                checkedTrackColor = PinkPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    currentTime: ReminderTime,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = currentTime.hour,
        initialMinute = currentTime.minute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "选择提醒时间",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = PinkPrimary,
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        periodSelectorBorderColor = PinkPrimary,
                        periodSelectorSelectedContainerColor = PinkPrimary,
                        periodSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContainerColor = PinkPrimary,
                        timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
                    onDismiss()
                }
            ) {
                Text("确定", color = PinkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            content()
        }
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: Int,
    unit: String,
    onValueChange: (Int) -> Unit,
    description: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 减少按钮
                IconButton(
                    onClick = { if (value > 1) onValueChange(value - 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "-",
                        style = MaterialTheme.typography.titleMedium,
                        color = PinkPrimary
                    )
                }

                // 数值显示
                Box(
                    modifier = Modifier.widthIn(min = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$value",
                        style = MaterialTheme.typography.titleMedium,
                        color = PinkPrimary
                    )
                }

                // 增加按钮
                IconButton(
                    onClick = { if (value < 99) onValueChange(value + 1) },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Text(
                        text = "+",
                        style = MaterialTheme.typography.titleMedium,
                        color = PinkPrimary
                    )
                }

                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DataManagementCard(
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onResetDataClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "数据管理",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 导出按钮
            Button(
                onClick = onExportClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出数据")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 导入按钮
            OutlinedButton(
                onClick = onImportClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导入数据")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onResetDataClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("重置数据")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "导出将生成JSON格式的备份文件，包含所有记录和设置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "说明",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            InfoItem("经期长度", "每次月经从开始到结束持续的天数")
            InfoItem("周期长度", "从本次月经第一天到下次月经第一天的间隔天数")
            InfoItem("预测算法", "基于历史数据使用加权平均计算预测日期")
        }
    }
}

@Composable
private fun InfoItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = PinkPrimary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeModeSelector(
    currentMode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit
) {
    val options = listOf(
        ThemeMode.LIGHT to "浅色",
        ThemeMode.DARK to "深色",
        ThemeMode.SYSTEM to "跟随系统"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (mode, label) ->
            val selected = mode == currentMode
            FilterChip(
                selected = selected,
                onClick = { onModeChange(mode) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PinkPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
