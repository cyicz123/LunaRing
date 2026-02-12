package com.example.menstruation.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.menstruation.data.local.entity.DailyRecordEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyRecordEntity?

    @Query("SELECT * FROM daily_records WHERE date BETWEEN :start AND :end ORDER BY date")
    fun getByDateRange(start: LocalDate, end: LocalDate): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    fun getAll(): Flow<List<DailyRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: DailyRecordEntity)

    @Delete
    suspend fun delete(record: DailyRecordEntity)

    @Query("DELETE FROM daily_records WHERE date = :date")
    suspend fun deleteByDate(date: LocalDate)
}
