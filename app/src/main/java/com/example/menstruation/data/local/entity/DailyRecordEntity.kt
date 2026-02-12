package com.example.menstruation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey
    val date: LocalDate,
    val isPeriodDay: Boolean = false,
    val flowLevel: String? = null,
    val painLevel: Int? = null,
    val hadSex: Boolean = false,
    val physicalSymptoms: String = "", // JSON string of List<Symptom>
    val mood: String? = null,
    val ovulationTest: String? = null,
    val note: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
