package com.example.menstruation.data.repository

import com.example.menstruation.data.local.dao.DailyRecordDao
import com.example.menstruation.data.model.DailyRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyRecordRepository @Inject constructor(
    private val dailyRecordDao: DailyRecordDao
) {
    fun getAllRecords(): Flow<List<DailyRecord>> =
        dailyRecordDao.getAll().map { list ->
            list.map { DailyRecord.fromEntity(it) }
        }

    fun getRecordsByDateRange(start: LocalDate, end: LocalDate): Flow<List<DailyRecord>> =
        dailyRecordDao.getByDateRange(start, end).map { list ->
            list.map { DailyRecord.fromEntity(it) }
        }

    suspend fun getRecordByDate(date: LocalDate): DailyRecord? =
        dailyRecordDao.getByDate(date)?.let { DailyRecord.fromEntity(it) }

    suspend fun saveRecord(record: DailyRecord) {
        dailyRecordDao.insert(record.toEntity())
    }

    suspend fun deleteRecord(date: LocalDate) {
        dailyRecordDao.deleteByDate(date)
    }
}
