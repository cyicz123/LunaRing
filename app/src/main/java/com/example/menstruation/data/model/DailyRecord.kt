package com.example.menstruation.data.model

import com.example.menstruation.data.local.entity.DailyRecordEntity
import java.time.LocalDate

data class DailyRecord(
    val date: LocalDate,
    val isPeriodDay: Boolean = false,
    val flowLevel: FlowLevel? = null,
    val painLevel: Int? = null,
    val hadSex: Boolean = false,
    val physicalSymptoms: List<Symptom> = emptyList(),
    val mood: Mood? = null,
    val ovulationTest: OvulationResult? = null,
    val note: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toEntity(): DailyRecordEntity {
        return DailyRecordEntity(
            date = date,
            isPeriodDay = isPeriodDay,
            flowLevel = flowLevel?.name,
            painLevel = painLevel,
            hadSex = hadSex,
            physicalSymptoms = Symptom.toJson(physicalSymptoms),
            mood = mood?.name,
            ovulationTest = ovulationTest?.name,
            note = note,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromEntity(entity: DailyRecordEntity): DailyRecord {
            return DailyRecord(
                date = entity.date,
                isPeriodDay = entity.isPeriodDay,
                flowLevel = entity.flowLevel?.let { FlowLevel.valueOf(it) },
                painLevel = entity.painLevel,
                hadSex = entity.hadSex,
                physicalSymptoms = Symptom.fromJson(entity.physicalSymptoms),
                mood = entity.mood?.let { Mood.valueOf(it) },
                ovulationTest = entity.ovulationTest?.let { OvulationResult.valueOf(it) },
                note = entity.note,
                updatedAt = entity.updatedAt
            )
        }
    }
}
