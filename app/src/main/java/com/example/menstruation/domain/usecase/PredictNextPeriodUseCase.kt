package com.example.menstruation.domain.usecase

import com.example.menstruation.data.model.Period
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class PredictNextPeriodUseCase @Inject constructor() {

    operator fun invoke(
        periods: List<Period>,
        cycleLengthSetting: Int
    ): LocalDate? {
        if (periods.isEmpty()) {
            // 无历史数据，使用设置值从上次经期推算
            return null
        }

        val sortedPeriods = periods.sortedBy { it.startDate }
        val recentPeriods = sortedPeriods.takeLast(6) // 最近6个周期

        val cycleLengths = recentPeriods.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
        }.filter { it > 0 } // 过滤掉无效数据

        if (cycleLengths.isEmpty()) {
            // 只有一个周期记录，使用设置值
            return sortedPeriods.last().startDate.plusDays(cycleLengthSetting.toLong())
        }

        // 加权平均：越近的周期权重越高
        val weightedSum = cycleLengths.reversed().mapIndexed { index, length ->
            length * (index + 1)
        }.sum()
        val weightsSum = (1..cycleLengths.size).sum()
        val averageCycle = weightedSum / weightsSum

        return sortedPeriods.last().startDate.plusDays(averageCycle.toLong())
    }

    /**
     * 预测整个经期窗口（开始日期到结束日期）
     */
    fun predictPeriodWindow(
        periods: List<Period>,
        cycleLengthSetting: Int,
        periodLengthSetting: Int
    ): Pair<LocalDate, LocalDate>? {
        val nextStart = invoke(periods, cycleLengthSetting)
            ?: predictBasedOnSettingsOnly(cycleLengthSetting)
            ?: return null
        val nextEnd = nextStart.plusDays((periodLengthSetting - 1).toLong())
        return nextStart to nextEnd
    }

    /**
     * 当没有历史记录时，基于设置预测下一个经期
     * 使用当前日期作为参考点
     */
    private fun predictBasedOnSettingsOnly(cycleLengthSetting: Int): LocalDate? {
        val today = LocalDate.now()
        // 预测从今天开始的一个周期后
        return today.plusDays(cycleLengthSetting.toLong())
    }
}
