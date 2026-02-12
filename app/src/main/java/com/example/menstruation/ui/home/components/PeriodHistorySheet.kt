package com.example.menstruation.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.menstruation.data.model.Period
import com.example.menstruation.ui.theme.PinkPrimary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodHistorySheet(
    periods: List<Period>,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    cycleLength: Int,
    periodLength: Int,
    onDismiss: () -> Unit,
    onJumpToDate: (LocalDate) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        PeriodHistoryContent(
            periods = periods,
            predictedPeriods = predictedPeriods,
            cycleLength = cycleLength,
            periodLength = periodLength,
            onDismiss = onDismiss,
            onJumpToDate = onJumpToDate
        )
    }
}

@Composable
private fun PeriodHistoryContent(
    periods: List<Period>,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    cycleLength: Int,
    periodLength: Int,
    onDismiss: () -> Unit,
    onJumpToDate: (LocalDate) -> Unit
) {
    // 计算显示的月份范围：从最早记录月份到预测月份之后
    val today = LocalDate.now()
    val sortedPeriods = periods.sortedBy { it.startDate }

    // 确定起始月份（最早记录或当前月份前3个月）
    val startMonth = if (sortedPeriods.isNotEmpty()) {
        val earliestPeriod = sortedPeriods.first().startDate
        earliestPeriod.minusMonths(1).let { YearMonth.of(it.year, it.month) }
    } else {
        today.minusMonths(3).let { YearMonth.of(it.year, it.month) }
    }

    // 确定结束月份（预测月份后2个月或当前月份后6个月）
    val endMonth = if (predictedPeriods.isNotEmpty()) {
        predictedPeriods.last().second.plusMonths(2).let { YearMonth.of(it.year, it.month) }
    } else {
        today.plusMonths(6).let { YearMonth.of(it.year, it.month) }
    }

    // 生成月份列表
    val months = generateSequence(startMonth) { it.plusMonths(1) }
        .takeWhile { !it.isAfter(endMonth) }
        .toList()

    Column(
        modifier = Modifier
            .fillMaxHeight(0.9f)
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "选择时间",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                // 日历图标按钮 - 快速回到今天
                IconButton(
                    onClick = {
                        onJumpToDate(today)
                        onDismiss()
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "今天",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 关闭按钮
                IconButton(onClick = onDismiss) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 星期标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
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

        // 多个月份滚动列表
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            months.forEach { yearMonth ->
                MonthHistoryView(
                    yearMonth = yearMonth,
                    periods = periods,
                    predictedPeriods = predictedPeriods,
                    today = today,
                    onDateClick = { date ->
                        onJumpToDate(date)
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MonthHistoryView(
    yearMonth: YearMonth,
    periods: List<Period>,
    predictedPeriods: List<Pair<LocalDate, LocalDate>>,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    Column {
        // 月份标题
        Text(
            text = yearMonth.format(DateTimeFormatter.ofPattern("yyyy年M月", Locale.CHINESE)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )

        // 日期网格
        val daysInMonth = yearMonth.lengthOfMonth()
        val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value % 7

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var currentDay = 1 - firstDayOfWeek
            while (currentDay <= daysInMonth) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(7) { _ ->
                        val date = if (currentDay in 1..daysInMonth) {
                            yearMonth.atDay(currentDay)
                        } else null

                        if (date != null) {
                            // 判断日期状态
                            val isInPeriod = periods.any { p ->
                                !date.isBefore(p.startDate) &&
                                        (p.endDate == null || !date.isAfter(p.endDate))
                            }
                            val isPredictedPeriod = predictedPeriods.any { (start, end) ->
                                !date.isBefore(start) && !date.isAfter(end)
                            }
                            val isToday = date == today

                            DateCellWithConnector(
                                date = date,
                                isInPeriod = isInPeriod,
                                isPredictedPeriod = isPredictedPeriod,
                                isToday = isToday,
                                isInMonth = true,
                                onClick = { onDateClick(date) }
                            )
                        } else {
                            // 空日期占位
                            Box(modifier = Modifier.size(40.dp))
                        }
                        currentDay++
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCellWithConnector(
    date: LocalDate,
    isInPeriod: Boolean,
    isPredictedPeriod: Boolean,
    isToday: Boolean,
    isInMonth: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when {
        isInPeriod -> PinkPrimary
        isPredictedPeriod -> Color.Transparent
        else -> Color.Transparent
    }

    val borderModifier = when {
        isPredictedPeriod -> Modifier.border(2.dp, PinkPrimary, CircleShape)
        else -> Modifier
    }

    val textColor = when {
        isInPeriod -> Color.White
        isPredictedPeriod -> PinkPrimary
        isToday -> PinkPrimary
        else -> MaterialTheme.colorScheme.onBackground
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(borderModifier)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = if (isToday) androidx.compose.ui.text.font.FontWeight.Bold else null
        )

        // 今天标记 - 右上角小点
        if (isToday && !isInPeriod) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PinkPrimary)
            )
        }
    }
}
