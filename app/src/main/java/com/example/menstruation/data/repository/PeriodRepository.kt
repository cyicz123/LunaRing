package com.example.menstruation.data.repository

import com.example.menstruation.data.local.dao.PeriodDao
import com.example.menstruation.data.model.Period
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeriodRepository @Inject constructor(
    private val periodDao: PeriodDao
) {
    fun getAllPeriods(): Flow<List<Period>> =
        periodDao.getAll().map { list ->
            list.map { Period.fromEntity(it) }
        }

    suspend fun getPeriodContaining(date: LocalDate): Period? =
        periodDao.getPeriodContaining(date)?.let { Period.fromEntity(it) }

    suspend fun getCurrentPeriod(): Period? =
        periodDao.getCurrentPeriod()?.let { Period.fromEntity(it) }

    suspend fun startPeriod(date: LocalDate): Long {
        return periodDao.insert(
            com.example.menstruation.data.local.entity.PeriodEntity(
                startDate = date
            )
        )
    }

    suspend fun endPeriod(date: LocalDate) {
        val currentPeriod = periodDao.getCurrentPeriod()
        if (currentPeriod != null) {
            periodDao.update(
                currentPeriod.copy(endDate = date)
            )
        }
    }

    suspend fun deletePeriod(id: Long) {
        periodDao.deleteById(id)
    }

    suspend fun updatePeriod(period: Period) {
        periodDao.update(period.toEntity())
    }
}
