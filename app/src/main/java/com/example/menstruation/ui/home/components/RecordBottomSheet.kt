package com.example.menstruation.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.FlowLevel
import com.example.menstruation.data.model.Mood
import com.example.menstruation.data.model.OvulationResult
import com.example.menstruation.data.model.Symptom
import com.example.menstruation.data.model.SymptomCategory
import com.example.menstruation.ui.theme.PinkPrimary
import com.example.menstruation.ui.theme.PinkTransparent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordBottomSheet(
    date: LocalDate,
    record: DailyRecord?,
    isInPeriod: Boolean,
    onDismiss: () -> Unit,
    onSave: (DailyRecord) -> Unit,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        RecordBottomSheetContent(
            date = date,
            record = record,
            isInPeriod = isInPeriod,
            onDismiss = onDismiss,
            onSave = onSave,
            onStartPeriod = onStartPeriod,
            onEndPeriod = onEndPeriod
        )
    }
}

@Composable
private fun RecordBottomSheetContent(
    date: LocalDate,
    record: DailyRecord?,
    isInPeriod: Boolean,
    onDismiss: () -> Unit,
    onSave: (DailyRecord) -> Unit,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit
) {
    // 状态管理
    var selectedFlow by remember { mutableStateOf(record?.flowLevel) }
    var painLevel by remember { mutableIntStateOf(record?.painLevel ?: 0) }
    var hadSex by remember { mutableStateOf(record?.hadSex ?: false) }
    var selectedSymptoms by remember { mutableStateOf(record?.physicalSymptoms ?: emptyList()) }
    var selectedMood by remember { mutableStateOf(record?.mood) }
    var ovulationResult by remember { mutableStateOf(record?.ovulationTest) }
    var note by remember { mutableStateOf(record?.note ?: "") }

    // 检查是否是未来日期
    val isFutureDate = date.isAfter(LocalDate.now())

    Column(
        modifier = Modifier
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINESE)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 经期控制卡片
        PeriodControlCard(
            isInPeriod = isInPeriod,
            isFutureDate = isFutureDate,
            onStartPeriod = onStartPeriod,
            onEndPeriod = onEndPeriod
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 流量选择（仅在经期日显示）
        if (isInPeriod) {
            FlowCard(
                selectedFlow = selectedFlow,
                onFlowSelected = { selectedFlow = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 疼痛程度卡片
        PainCard(
            painLevel = painLevel,
            onPainLevelChange = { painLevel = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 同房记录卡片
        SexCard(
            hadSex = hadSex,
            onSexChange = { hadSex = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 症状卡片
        SymptomsCard(
            selectedSymptoms = selectedSymptoms,
            onSymptomsChange = { selectedSymptoms = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 心情卡片
        MoodCard(
            selectedMood = selectedMood,
            onMoodSelected = { selectedMood = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 排卵试纸卡片
        OvulationCard(
            result = ovulationResult,
            onResultSelected = { ovulationResult = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 备注卡片
        NoteCard(
            note = note,
            onNoteChange = { note = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 保存按钮
        Button(
            onClick = {
                val newRecord = DailyRecord(
                    date = date,
                    isPeriodDay = isInPeriod,
                    flowLevel = selectedFlow,
                    painLevel = if (painLevel > 0) painLevel else null,
                    hadSex = hadSex,
                    physicalSymptoms = selectedSymptoms,
                    mood = selectedMood,
                    ovulationTest = ovulationResult,
                    note = note.takeIf { it.isNotBlank() }
                )
                onSave(newRecord)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = PinkPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "保存记录",
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PeriodControlCard(
    isInPeriod: Boolean,
    isFutureDate: Boolean,
    onStartPeriod: () -> Unit,
    onEndPeriod: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PinkTransparent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = PinkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "经期状态",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isInPeriod -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PinkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "经期中",
                            style = MaterialTheme.typography.bodyLarge,
                            color = PinkPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = onEndPeriod,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
                            )
                        ) {
                            Text("结束经期")
                        }
                    }
                }
                isFutureDate -> {
                    // 未来日期显示提示
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "未来的日子不能记录哦",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onStartPeriod,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PinkPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("开始经期")
                    }
                }
            }
        }
    }
}

@Composable
private fun FlowCard(
    selectedFlow: FlowLevel?,
    onFlowSelected: (FlowLevel?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "流量",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowLevel.values().forEach { level ->
                    val isSelected = selectedFlow == level
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFlowSelected(if (isSelected) null else level) },
                        label = { Text(level.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = PinkPrimary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PainCard(
    painLevel: Int,
    onPainLevelChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "疼痛程度",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (painLevel > 0) {
                    Text(
                        text = "$painLevel / 10",
                        style = MaterialTheme.typography.bodyLarge,
                        color = PinkPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..10).forEach { level ->
                    val isSelected = painLevel >= level
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected && level <= 3 -> Color(0xFFFF9500)
                                    isSelected && level <= 6 -> Color(0xFFFF6B35)
                                    isSelected -> Color(0xFFFF3B30)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            )
                            .clickable { onPainLevelChange(if (painLevel == level) 0 else level) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$level",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SexCard(
    hadSex: Boolean,
    onSexChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "同房记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Switch(
                checked = hadSex,
                onCheckedChange = onSexChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PinkPrimary,
                    checkedTrackColor = PinkTransparent
                )
            )
        }
    }
}

@Composable
private fun SymptomsCard(
    selectedSymptoms: List<Symptom>,
    onSymptomsChange: (List<Symptom>) -> Unit
) {
    val symptomsByCategory = Symptom.values().groupBy { it.category }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "症状",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            symptomsByCategory.forEach { (category, symptoms) ->
                Text(
                    text = category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                // 使用 Column + Row 代替 FlowRow
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    symptoms.chunked(3).forEach { chunk ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chunk.forEach { symptom ->
                                val isSelected = selectedSymptoms.contains(symptom)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        onSymptomsChange(
                                            if (isSelected) selectedSymptoms - symptom
                                            else selectedSymptoms + symptom
                                        )
                                    },
                                    label = { Text(symptom.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PinkPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderColor = PinkPrimary,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MoodCard(
    selectedMood: Mood?,
    onMoodSelected: (Mood?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "心情",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            // 使用 Column + Row 代替 FlowRow
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Mood.values().toList().chunked(3).forEach { chunk ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        chunk.forEach { mood ->
                            val isSelected = selectedMood == mood
                            FilterChip(
                                selected = isSelected,
                                onClick = { onMoodSelected(if (isSelected) null else mood) },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(mood.emoji)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(mood.label)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PinkPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = MaterialTheme.colorScheme.outline,
                                    selectedBorderColor = PinkPrimary,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OvulationCard(
    result: OvulationResult?,
    onResultSelected: (OvulationResult?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "排卵试纸",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OvulationResult.values().forEach { ovulationResult ->
                    val isSelected = result == ovulationResult
                    FilterChip(
                        selected = isSelected,
                        onClick = { onResultSelected(if (isSelected) null else ovulationResult) },
                        label = { Text(ovulationResult.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PinkPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = PinkPrimary,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: String,
    onNoteChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "备注",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("添加备注...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedBorderColor = PinkPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
