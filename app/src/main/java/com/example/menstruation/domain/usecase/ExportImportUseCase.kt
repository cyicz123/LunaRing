package com.example.menstruation.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.menstruation.data.model.*
import com.example.menstruation.data.repository.DailyRecordRepository
import com.example.menstruation.data.repository.PeriodRepository
import com.example.menstruation.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class ExportResult {
    data class Success(val uri: Uri) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    object Success : ImportResult()
    data class Error(val message: String) : ImportResult()
    data class PartialSuccess(
        val importedPeriods: Int,
        val importedRecords: Int,
        val errors: List<String>
    ) : ImportResult()
}

class ExportImportUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val periodRepository: PeriodRepository,
    private val dailyRecordRepository: DailyRecordRepository
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 导出所有数据到JSON文件
     */
    suspend fun exportToJson(uri: Uri): ExportResult {
        return try {
            // 收集所有数据
            val settings = settingsRepository.settings.first()
            val periods = periodRepository.getAllPeriods().first()
            val records = dailyRecordRepository.getAllRecords().first()

            // 构建导出数据
            val exportData = ExportData(
                version = 1,
                exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                settings = settings.toExportSettings(),
                periods = periods.map { it.toExportPeriod() },
                dailyRecords = records.map { it.toExportDailyRecord() }
            )

            // 序列化为JSON
            val jsonString = json.encodeToString(exportData)

            // 写入文件
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(jsonString)
                }
            } ?: return ExportResult.Error("无法打开输出流")

            ExportResult.Success(uri)
        } catch (e: Exception) {
            ExportResult.Error("导出失败: ${e.message}")
        }
    }

    /**
     * 从JSON文件导入数据
     * @param mergeExisting 是否合并现有数据（true=合并，false=覆盖）
     */
    suspend fun importFromJson(uri: Uri, mergeExisting: Boolean = true): ImportResult {
        return try {
            // 读取文件内容
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            } ?: return ImportResult.Error("无法打开文件")

            // 解析JSON
            val exportData = try {
                json.decodeFromString<ExportData>(jsonString)
            } catch (e: Exception) {
                return ImportResult.Error("JSON格式错误: ${e.message}")
            }

            // 验证版本
            if (exportData.version > 1) {
                return ImportResult.Error("不支持的文件版本: ${exportData.version}")
            }

            val errors = mutableListOf<String>()
            var importedPeriods = 0
            var importedRecords = 0

            // 如果不合并，先清空现有数据
            if (!mergeExisting) {
                periodRepository.deleteAllPeriods()
                dailyRecordRepository.deleteAllRecords()
            }

            // 导入设置
            try {
                val settings = exportData.settings.toUserSettings()
                settingsRepository.updatePeriodLength(settings.periodLength)
                settingsRepository.updateCycleLength(settings.cycleLength)
                settingsRepository.updateThemeMode(settings.themeMode)
            } catch (e: Exception) {
                errors.add("导入设置失败: ${e.message}")
            }

            // 导入周期记录
            exportData.periods.forEach { exportPeriod ->
                try {
                    val period = exportPeriod.toPeriod()
                    periodRepository.insertPeriod(period)
                    importedPeriods++
                } catch (e: Exception) {
                    errors.add("导入周期记录失败: ${exportPeriod.startDate}")
                }
            }

            // 导入每日记录
            exportData.dailyRecords.forEach { exportRecord ->
                try {
                    val record = exportRecord.toDailyRecord()
                    dailyRecordRepository.saveRecord(record)
                    importedRecords++
                } catch (e: Exception) {
                    errors.add("导入每日记录失败: ${exportRecord.date}")
                }
            }

            when {
                errors.isEmpty() -> ImportResult.Success
                importedPeriods > 0 || importedRecords > 0 -> ImportResult.PartialSuccess(
                    importedPeriods,
                    importedRecords,
                    errors
                )
                else -> ImportResult.Error("导入失败: ${errors.first()}")
            }
        } catch (e: Exception) {
            ImportResult.Error("导入失败: ${e.message}")
        }
    }

    /**
     * 生成默认导出文件名
     */
    fun generateExportFileName(): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        return "menstruation_backup_$timestamp.json"
    }
}
