package com.example.menstruation.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menstruation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
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
        containerColor = DarkBackground
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
                onPeriodLengthChange = viewModel::updatePeriodLength,
                onCycleLengthChange = viewModel::updateCycleLength,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        }
    }
}

@Composable
private fun SettingsContent(
    periodLength: Int,
    cycleLength: Int,
    onPeriodLengthChange: (Int) -> Unit,
    onCycleLengthChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    color = DarkSurfaceElevated,
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

        // 说明卡片
        InfoCard()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
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
                    color = TextPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
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
                            color = DarkSurfaceElevated,
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
                            color = DarkSurfaceElevated,
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
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "说明",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
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
            color = TextSecondary
        )
    }
}
