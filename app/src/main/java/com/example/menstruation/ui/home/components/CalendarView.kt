package com.example.menstruation.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.Period
import com.example.menstruation.data.model.UserSettings
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.PinkDark
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun CalendarView(
    records: Map<LocalDate, DailyRecord>,
    periods: List<Period>,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    settings: UserSettings,
    currentDate: LocalDate,
    onDateClick: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    onJumpToDate: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var showHistorySheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // 月份选择器（带左右箭头）
        MonthSelector(
            selectedMonth = selectedMonth,
            onPreviousMonth = {
                selectedMonth = selectedMonth.minusMonths(1)
                onMonthChange(selectedMonth)
            },
            onNextMonth = {
                selectedMonth = selectedMonth.plusMonths(1)
                onMonthChange(selectedMonth)
            },
            onTitleClick = { showHistorySheet = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 单个月份视图
        MonthCalendar(
            yearMonth = selectedMonth,
            records = records,
            periods = periods,
            predictedPeriods = predictedPeriods,
            currentDate = currentDate,
            onDateClick = onDateClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 图例
        CalendarLegend()

        Spacer(modifier = Modifier.height(24.dp))

        // 周期信息卡片
        CycleInfoCard(
            periods = periods,
            currentDate = currentDate,
            settings = settings
        )
    }

    // 历史记录和预测弹窗
    if (showHistorySheet) {
        PeriodHistorySheet(
            periods = periods,
            predictedPeriods = predictedPeriods,
            cycleLength = settings.cycleLength,
            periodLength = settings.periodLength,
            onDismiss = { showHistorySheet = false },
            onJumpToDate = { date ->
                selectedMonth = YearMonth.of(date.year, date.month)
                onMonthChange(selectedMonth)
                onJumpToDate(date)
            }
        )
    }
}

@Composable
private fun MonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上个月",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = selectedMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE)),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.clickable { onTitleClick() }
        )

        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下个月",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    yearMonth: YearMonth,
    records: Map<LocalDate, DailyRecord>,
    periods: List<Period>,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    currentDate: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    Column {
        // 星期标题
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 日期网格
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            var currentDay = 1 - firstDayOfWeek
            while (currentDay <= daysInMonth) {
                Row {
                    repeat(7) { _ ->
                        val date = if (currentDay in 1..daysInMonth) {
                            yearMonth.atDay(currentDay)
                        } else null

                        DateCell(
                            date = date,
                            record = date?.let { records[it] },
                            isInPeriod = date?.let { d ->
                                periods.any { p ->
                                    !d.isBefore(p.startDate) &&
                                    (p.endDate == null || !d.isAfter(p.endDate))
                                }
                            } ?: false,
                            isPredictedPeriod = date?.let { d ->
                                predictedPeriods.any { (start, end) ->
                                    !d.isBefore(start) && !d.isAfter(end)
                                }
                            } ?: false,
                            isToday = date == currentDate,
                            onClick = { date?.let(onDateClick) },
                            modifier = Modifier.weight(1f)
                        )
                        currentDay++
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCell(
    date: LocalDate?,
    record: DailyRecord?,
    isInPeriod: Boolean,
    isPredictedPeriod: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                when {
                    isInPeriod -> Modifier.background(PinkPrimary)
                    isPredictedPeriod -> Modifier.border(2.dp, PinkPrimary, CircleShape)
                    else -> Modifier
                }
            )
            .then(
                if (isToday && !isInPeriod) {
                    Modifier.border(2.dp, PinkDark, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        date?.let {
            Text(
                text = "${it.dayOfMonth}",
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    isInPeriod -> Color.White
                    isPredictedPeriod -> PinkPrimary
                    isToday -> PinkDark
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 经期
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(PinkPrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "经期",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 预测经期
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .border(2.dp, PinkPrimary, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "预测经期",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CycleInfoCard(
    periods: List<Period>,
    currentDate: LocalDate,
    settings: UserSettings
) {
    val sortedPeriods = periods.sortedByDescending { it.startDate }
    val currentPeriod = sortedPeriods.find { p ->
        !currentDate.isBefore(p.startDate) &&
        (p.endDate == null || !currentDate.isAfter(p.endDate))
    }

    val lastPeriod = sortedPeriods.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (currentPeriod != null) {
                // 正在经期中
                val dayInPeriod = ChronoUnit.DAYS.between(currentPeriod.startDate, currentDate).toInt() + 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CycleStat(
                        label = "经期",
                        value = "第 ${dayInPeriod} 天"
                    )
                    CycleStat(
                        label = "周期",
                        value = "共 ${settings.cycleLength} 天"
                    )
                }
            } else if (lastPeriod != null) {
                // 不在经期中，显示距离下次预测
                val daysSinceLastPeriod = ChronoUnit.DAYS.between(lastPeriod.startDate, currentDate).toInt()
                val daysUntilNext = settings.cycleLength - daysSinceLastPeriod

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (daysUntilNext > 0) {
                        CycleStat(
                            label = "距离下次",
                            value = "${daysUntilNext} 天"
                        )
                    } else {
                        CycleStat(
                            label = "经期延迟",
                            value = "${-daysUntilNext} 天"
                        )
                    }
                    CycleStat(
                        label = "周期",
                        value = "共 ${settings.cycleLength} 天"
                    )
                }
            } else {
                // 没有记录
                Text(
                    text = "记录你的第一个经期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 健康知识提示
            Text(
                text = "月经周期的长短取决于卵巢周期的长短，一般为28-32天。记录经期有助于更好地了解自己的身体状况。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CycleStat(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = PinkPrimary
        )
    }
}
