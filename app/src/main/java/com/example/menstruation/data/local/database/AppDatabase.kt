package com.example.menstruation.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.menstruation.data.local.dao.DailyRecordDao
import com.example.menstruation.data.local.dao.PeriodDao
import com.example.menstruation.data.local.entity.DailyRecordEntity
import com.example.menstruation.data.local.entity.PeriodEntity

@Database(
    entities = [DailyRecordEntity::class, PeriodEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun periodDao(): PeriodDao
}
