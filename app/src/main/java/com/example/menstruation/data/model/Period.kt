package com.example.menstruation.data.model

import com.example.menstruation.data.local.entity.PeriodEntity
import java.time.LocalDate

data class Period(
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null
) {
    fun toEntity(): PeriodEntity {
        return PeriodEntity(
            id = id,
            startDate = startDate,
            endDate = endDate
        )
    }

    companion object {
        fun fromEntity(entity: PeriodEntity): Period {
            return Period(
                id = entity.id,
                startDate = entity.startDate,
                endDate = entity.endDate
            )
        }
    }
}
