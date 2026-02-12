package com.example.menstruation.ui.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.menstruation.ui.theme.DarkSurface
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.TextPrimary
import com.example.menstruation.ui.theme.TextSecondary

@Composable
fun CycleLengthChart(
    cycleLengths: List<Pair<Int, String>>,
    modifier: Modifier = Modifier
) {
    if (cycleLengths.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
        }
        return
    }

    val maxValue = cycleLengths.maxOf { it.first }.coerceAtLeast(35)
    val minValue = cycleLengths.minOf { it.first }.coerceAtMost(20)
    val range = (maxValue - minValue).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "周期长度趋势（天）",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 图表区域
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            cycleLengths.takeLast(6).forEach { (length, label) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    // 数值标签
                    Text(
                        text = "$length",
                        style = MaterialTheme.typography.bodySmall,
                        color = PinkPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 柱状图
                    val heightPercent = (length - minValue).toFloat() / range
                    val barHeight = (heightPercent * 120).coerceIn(20f, 120f)

                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (length in 26..32) PinkPrimary else PinkPrimary.copy(alpha = 0.6f)
                            )
                    )

                    // X轴标签
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // 参考线说明
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ReferenceLineItem(color = PinkPrimary, label = "正常范围 (26-32天)")
            ReferenceLineItem(color = PinkPrimary.copy(alpha = 0.6f), label = "异常周期")
        }
    }
}

@Composable
private fun ReferenceLineItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}
