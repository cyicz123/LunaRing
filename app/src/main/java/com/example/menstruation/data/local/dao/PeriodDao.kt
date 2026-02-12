package com.example.menstruation.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.menstruation.data.local.entity.PeriodEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PeriodDao {
    @Query("SELECT * FROM periods ORDER BY startDate DESC")
    fun getAll(): Flow<List<PeriodEntity>>

    @Query("SELECT * FROM periods WHERE startDate <= :date AND (endDate IS NULL OR endDate >= :date)")
    suspend fun getPeriodContaining(date: LocalDate): PeriodEntity?

    @Query("SELECT * FROM periods WHERE endDate IS NULL ORDER BY startDate DESC LIMIT 1")
    suspend fun getCurrentPeriod(): PeriodEntity?

    @Query("SELECT * FROM periods WHERE startDate = :date LIMIT 1")
    suspend fun getByStartDate(date: LocalDate): PeriodEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(period: PeriodEntity): Long

    @Update
    suspend fun update(period: PeriodEntity)

    @Delete
    suspend fun delete(period: PeriodEntity)

    @Query("DELETE FROM periods WHERE id = :id")
    suspend fun deleteById(id: Long)
}