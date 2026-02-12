package com.example.menstruation.data.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 导出数据结构
 */
@Serializable
data class ExportData(
    val version: Int = 1,
    val exportDate: String,
    val settings: ExportSettings,
    val periods: List<ExportPeriod>,
    val dailyRecords: List<ExportDailyRecord>
)

@Serializable
data class ExportSettings(
    val periodLength: Int,
    val cycleLength: Int,
    val themeMode: String = "DARK",
    val notificationSettings: ExportNotificationSettings? = null
)

@Serializable
data class ExportNotificationSettings(
    val enabled: Boolean = true,
    val periodStartReminder: Boolean = true,
    val periodEndReminder: Boolean = true,
    val predictedPeriodReminder: Boolean = true,
    val reminderTime: String = "09:00"
)

@Serializable
data class ExportPeriod(
    val startDate: String,
    val endDate: String?
)

@Serializable
data class ExportDailyRecord(
    val date: String,
    val isPeriodDay: Boolean = false,
    val flowLevel: String? = null,
    val painLevel: Int? = null,
    val hadSex: Boolean = false,
    val physicalSymptoms: List<String> = emptyList(),
    val mood: String? = null,
    val ovulationTest: String? = null,
    val note: String? = null
)

// 扩展函数：转换领域模型到导出模型
fun UserSettings.toExportSettings(): ExportSettings = ExportSettings(
    periodLength = periodLength,
    cycleLength = cycleLength,
    themeMode = themeMode.name,
    notificationSettings = notificationSettings.toExportNotificationSettings()
)

fun NotificationSettings.toExportNotificationSettings(): ExportNotificationSettings =
    ExportNotificationSettings(
        enabled = enabled,
        periodStartReminder = periodStartReminder,
        periodEndReminder = periodEndReminder,
        predictedPeriodReminder = predictedPeriodReminder,
        reminderTime = reminderTime.toString()
    )

fun Period.toExportPeriod(): ExportPeriod = ExportPeriod(
    startDate = startDate.toString(),
    endDate = endDate?.toString()
)

fun DailyRecord.toExportDailyRecord(): ExportDailyRecord = ExportDailyRecord(
    date = date.toString(),
    isPeriodDay = isPeriodDay,
    flowLevel = flowLevel?.name,
    painLevel = painLevel,
    hadSex = hadSex,
    physicalSymptoms = physicalSymptoms.map { it.name },
    mood = mood?.name,
    ovulationTest = ovulationTest?.name,
    note = note
)

// 扩展函数：转换导出模型到领域模型
fun ExportSettings.toUserSettings(): UserSettings = UserSettings(
    periodLength = periodLength,
    cycleLength = cycleLength,
    themeMode = try {
        ThemeMode.valueOf(themeMode)
    } catch (e: IllegalArgumentException) {
        ThemeMode.DARK
    },
    notificationSettings = notificationSettings?.toNotificationSettings() ?: NotificationSettings()
)

fun ExportNotificationSettings.toNotificationSettings(): NotificationSettings =
    NotificationSettings(
        enabled = enabled,
        periodStartReminder = periodStartReminder,
        periodEndReminder = periodEndReminder,
        predictedPeriodReminder = predictedPeriodReminder,
        reminderTime = ReminderTime.fromString(reminderTime)
    )

fun ExportPeriod.toPeriod(): Period = Period(
    startDate = LocalDate.parse(startDate),
    endDate = endDate?.let { LocalDate.parse(it) }
)

fun ExportDailyRecord.toDailyRecord(): DailyRecord = DailyRecord(
    date = LocalDate.parse(date),
    isPeriodDay = isPeriodDay,
    flowLevel = flowLevel?.let { try {
        FlowLevel.valueOf(it)
    } catch (e: IllegalArgumentException) { null } },
    painLevel = painLevel,
    hadSex = hadSex,
    physicalSymptoms = physicalSymptoms.mapNotNull { try {
        Symptom.valueOf(it)
    } catch (e: IllegalArgumentException) { null } },
    mood = mood?.let { try {
        Mood.valueOf(it)
    } catch (e: IllegalArgumentException) { null } },
    ovulationTest = ovulationTest?.let { try {
        OvulationResult.valueOf(it)
    } catch (e: IllegalArgumentException) { null } },
    note = note
)
