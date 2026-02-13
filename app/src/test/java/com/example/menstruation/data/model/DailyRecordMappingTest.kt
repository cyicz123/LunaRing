package com.example.menstruation.data.model

import com.example.menstruation.data.local.entity.DailyRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for DailyRecord model to entity mapping.
 */
class DailyRecordMappingTest {

    @Test
    fun `DailyRecord toEntity maps all fields correctly`() {
        val record = DailyRecord(
            date = LocalDate.of(2025, 1, 15),
            isPeriodDay = true,
            flowLevel = FlowLevel.MEDIUM,
            painLevel = 5,
            hadSex = true,
            physicalSymptoms = listOf(Symptom.CRAMPS, Symptom.BLOATING),
            mood = Mood.HAPPY,
            ovulationTest = OvulationResult.NEGATIVE,
            note = "Test note"
        )

        val entity = record.toEntity()

        assertEquals(LocalDate.of(2025, 1, 15), entity.date)
        assertTrue(entity.isPeriodDay)
        assertEquals("MEDIUM", entity.flowLevel)
        assertEquals(5, entity.painLevel)
        assertTrue(entity.hadSex)
        assertTrue(entity.physicalSymptoms.contains("CRAMPS"))
        assertTrue(entity.physicalSymptoms.contains("BLOATING"))
        assertEquals("HAPPY", entity.mood)
        assertEquals("NEGATIVE", entity.ovulationTest)
        assertEquals("Test note", entity.note)
    }

    @Test
    fun `DailyRecord toEntity with null values maps correctly`() {
        val record = DailyRecord(
            date = LocalDate.of(2025, 2, 1),
            isPeriodDay = false,
            flowLevel = null,
            painLevel = null,
            hadSex = false,
            physicalSymptoms = emptyList(),
            mood = null,
            ovulationTest = null,
            note = null
        )

        val entity = record.toEntity()

        assertEquals(LocalDate.of(2025, 2, 1), entity.date)
        assertFalse(entity.isPeriodDay)
        assertNull(entity.flowLevel)
        assertNull(entity.painLevel)
        assertFalse(entity.hadSex)
        assertEquals("[]", entity.physicalSymptoms)
        assertNull(entity.mood)
        assertNull(entity.ovulationTest)
        assertNull(entity.note)
    }

    @Test
    fun `DailyRecord fromEntity maps all fields correctly`() {
        val entity = DailyRecordEntity(
            date = LocalDate.of(2025, 3, 10),
            isPeriodDay = true,
            flowLevel = "HEAVY",
            painLevel = 8,
            hadSex = false,
            physicalSymptoms = "[\"HEADACHE\",\"FATIGUE\"]",
            mood = "IRRITABLE",
            ovulationTest = "WEAK_POSITIVE",
            note = "Bad day"
        )

        val record = DailyRecord.fromEntity(entity)

        assertEquals(LocalDate.of(2025, 3, 10), record.date)
        assertTrue(record.isPeriodDay)
        assertEquals(FlowLevel.HEAVY, record.flowLevel)
        assertEquals(8, record.painLevel)
        assertFalse(record.hadSex)
        assertEquals(2, record.physicalSymptoms.size)
        assertTrue(record.physicalSymptoms.contains(Symptom.HEADACHE))
        assertTrue(record.physicalSymptoms.contains(Symptom.FATIGUE))
        assertEquals(Mood.IRRITABLE, record.mood)
        assertEquals(OvulationResult.WEAK_POSITIVE, record.ovulationTest)
        assertEquals("Bad day", record.note)
    }

    @Test
    fun `DailyRecord fromEntity with null values maps correctly`() {
        val entity = DailyRecordEntity(
            date = LocalDate.of(2025, 4, 1),
            isPeriodDay = false,
            flowLevel = null,
            painLevel = null,
            hadSex = false,
            physicalSymptoms = "",
            mood = null,
            ovulationTest = null,
            note = null
        )

        val record = DailyRecord.fromEntity(entity)

        assertEquals(LocalDate.of(2025, 4, 1), record.date)
        assertFalse(record.isPeriodDay)
        assertNull(record.flowLevel)
        assertNull(record.painLevel)
        assertFalse(record.hadSex)
        assertTrue(record.physicalSymptoms.isEmpty())
        assertNull(record.mood)
        assertNull(record.ovulationTest)
        assertNull(record.note)
    }

    @Test
    fun `round trip conversion preserves data`() {
        val original = DailyRecord(
            date = LocalDate.of(2025, 5, 20),
            isPeriodDay = true,
            flowLevel = FlowLevel.LIGHT,
            painLevel = 3,
            hadSex = true,
            physicalSymptoms = listOf(Symptom.ACNE, Symptom.BACK_PAIN),
            mood = Mood.TIRED,
            ovulationTest = OvulationResult.STRONG_POSITIVE,
            note = "Round trip test"
        )

        val entity = original.toEntity()
        val converted = DailyRecord.fromEntity(entity)

        assertEquals(original.date, converted.date)
        assertEquals(original.isPeriodDay, converted.isPeriodDay)
        assertEquals(original.flowLevel, converted.flowLevel)
        assertEquals(original.painLevel, converted.painLevel)
        assertEquals(original.hadSex, converted.hadSex)
        assertEquals(original.physicalSymptoms.size, converted.physicalSymptoms.size)
        assertTrue(converted.physicalSymptoms.containsAll(original.physicalSymptoms))
        assertEquals(original.mood, converted.mood)
        assertEquals(original.ovulationTest, converted.ovulationTest)
        assertEquals(original.note, converted.note)
    }

    @Test
    fun `round trip conversion with empty symptoms preserves data`() {
        val original = DailyRecord(
            date = LocalDate.of(2025, 6, 15),
            isPeriodDay = false,
            flowLevel = null,
            painLevel = null,
            hadSex = false,
            physicalSymptoms = emptyList(),
            mood = null,
            ovulationTest = null,
            note = null
        )

        val entity = original.toEntity()
        val converted = DailyRecord.fromEntity(entity)

        assertEquals(original.date, converted.date)
        assertTrue(converted.physicalSymptoms.isEmpty())
    }

    @Test
    fun `fromEntity handles all flow levels`() {
        FlowLevel.values().forEach { flowLevel ->
            val entity = DailyRecordEntity(
                date = LocalDate.of(2025, 7, 1),
                flowLevel = flowLevel.name
            )
            val record = DailyRecord.fromEntity(entity)
            assertEquals(flowLevel, record.flowLevel)
        }
    }

    @Test
    fun `fromEntity handles all moods`() {
        Mood.values().forEach { mood ->
            val entity = DailyRecordEntity(
                date = LocalDate.of(2025, 7, 2),
                mood = mood.name
            )
            val record = DailyRecord.fromEntity(entity)
            assertEquals(mood, record.mood)
        }
    }

    @Test
    fun `fromEntity handles all ovulation results`() {
        OvulationResult.values().forEach { result ->
            val entity = DailyRecordEntity(
                date = LocalDate.of(2025, 7, 3),
                ovulationTest = result.name
            )
            val record = DailyRecord.fromEntity(entity)
            assertEquals(result, record.ovulationTest)
        }
    }
}
