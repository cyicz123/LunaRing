package com.example.menstruation.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.menstruation.ui.home.components.CalendarView
import com.example.menstruation.ui.home.components.RecordBottomSheet
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.PinkTransparent
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToStats: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("生理期记录") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
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
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, null) },
                    label = { Text("统计") },
                    selected = false,
                    onClick = onNavigateToStats
                )
            }
        }
    ) { padding ->
        CalendarView(
            records = uiState.records,
            periods = uiState.periods,
            predictedPeriods = uiState.predictedPeriods,
            settings = uiState.settings,
            currentDate = LocalDate.now(),
            onDateClick = {
                viewModel.ensurePredictionsCover(it.plusMonths(3))
                selectedDate = it
            },
            onMonthChange = { month ->
                viewModel.onVisibleMonthChanged(month)
            },
            onJumpToDate = { date ->
                viewModel.ensurePredictionsCover(date.plusMonths(3))
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )

        // 记录面板或预测信息面板
        selectedDate?.let { date ->
            val today = LocalDate.now()
            if (date.isAfter(today)) {
                // 未来日期：显示预测信息面板
                FutureDatePredictionPanel(
                    selectedDate = date,
                    predictedPeriods = uiState.predictedPeriods,
                    cycleLength = uiState.settings.cycleLength,
                    periodLength = uiState.settings.periodLength,
                    onDismiss = { selectedDate = null }
                )
            } else {
                // 今天或过去日期：显示记录面板
                val isInPeriod = viewModel.isInPeriod(date)
                RecordBottomSheet(
                    date = date,
                    record = uiState.records[date],
                    isInPeriod = isInPeriod,
                    onDismiss = { selectedDate = null },
                    onSave = { viewModel.saveRecord(it) },
                    onStartPeriod = {
                        viewModel.startPeriod(date)
                    },
                    onEndPeriod = {
                        viewModel.endPeriod(date)
                    }
                )
            }
        }
    }
}

/**
 * 未来日期预测信息面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FutureDatePredictionPanel(
    selectedDate: LocalDate,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    cycleLength: Int,
    periodLength: Int,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val daysUntilSelected = ChronoUnit.DAYS.between(today, selectedDate).toInt()
    val targetPrediction = findClosestPredictionWindow(selectedDate, predictedPeriods)

    // 计算距离最近预测经期的天数
    val daysUntilNextFromSelected = targetPrediction?.first?.let {
        ChronoUnit.DAYS.between(selectedDate, it).toInt()
    }
    val daysUntilNextFromToday = targetPrediction?.first?.let {
        ChronoUnit.DAYS.between(today, it).toInt()
    }

    // 判断选中日期在哪个阶段
    val phaseInfo = calculatePhaseInfo(selectedDate, targetPrediction)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 日期标题
            Text(
                text = "${selectedDate.monthValue}月${selectedDate.dayOfMonth}日",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 提示信息
            Text(
                text = "未来的日子不能记录哦",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 预测信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = PinkTransparent
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 距离选中日期还有X天
                    Text(
                        text = "距离这天还有",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${daysUntilSelected}天",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PinkPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider(
                        color = PinkPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 当前阶段信息
                    Text(
                        text = phaseInfo.first,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = phaseInfo.second,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 周期信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 预测经期信息
                    targetPrediction?.let { (start, end) ->
                        InfoRow(
                            label = "预测下次经期",
                            value = "${start.monthValue}月${start.dayOfMonth}日 - ${end.monthValue}月${end.dayOfMonth}日"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        daysUntilNextFromSelected?.let { days ->
                            InfoRow(
                                label = "距所选日期的下次经期",
                                value = if (days > 0) "还有${days}天" else if (days == 0) "当天开始" else "进行中"
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        daysUntilNextFromToday?.let { days ->
                            InfoRow(
                                label = "距离最近经期",
                                value = if (days > 0) "还有${days}天" else if (days == 0) "今天开始" else "已开始"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    InfoRow(
                        label = "周期长度",
                        value = "${cycleLength}天"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow(
                        label = "经期长度",
                        value = "${periodLength}天"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 关闭按钮
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PinkPrimary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("知道了", fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * 计算选中日期所处的周期阶段
 */
private fun calculatePhaseInfo(
    selectedDate: LocalDate,
    targetPrediction: Pair<LocalDate, LocalDate>?
): Pair<String, String> {
    return when {
        targetPrediction == null -> {
            "周期信息不足" to "记录更多经期数据以获得准确预测"
        }
        selectedDate >= targetPrediction.first && selectedDate <= targetPrediction.second -> {
            "预测经期期间" to "预计月经将在这一天到来"
        }
        else -> {
            // 计算是否在排卵期（下次经期前14天左右）
            val ovulationDate = targetPrediction.first.minusDays(14)
            val ovulationStart = ovulationDate.minusDays(3)
            val ovulationEnd = ovulationDate.plusDays(3)

            when {
                selectedDate in ovulationStart..ovulationEnd -> {
                    "排卵期" to "受孕几率较高的时期"
                }
                selectedDate < targetPrediction.first -> {
                    "卵泡期" to "月经周期中的准备阶段"
                }
                else -> {
                    "黄体期" to "月经周期的后期阶段"
                }
            }
        }
    }
}

private fun findClosestPredictionWindow(
    selectedDate: LocalDate,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>
): Pair<LocalDate, LocalDate>? {
    if (predictedPeriods.isEmpty()) return null

    val activeWindow = predictedPeriods.firstOrNull { (start, end) ->
        selectedDate >= start && selectedDate <= end
    }
    if (activeWindow != null) return activeWindow

    val nextWindow = predictedPeriods.firstOrNull { (start, _) ->
        !start.isBefore(selectedDate)
    }
    return nextWindow ?: predictedPeriods.lastOrNull()
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
