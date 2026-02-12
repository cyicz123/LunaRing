package com.example.menstruation.data.local.database

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? =
        value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? =
        date?.toEpochDay()
}
