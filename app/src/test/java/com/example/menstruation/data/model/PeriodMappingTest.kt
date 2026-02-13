package com.example.menstruation.data.model

import com.example.menstruation.data.local.entity.PeriodEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for Period model to entity mapping.
 */
class PeriodMappingTest {

    @Test
    fun `Period toEntity maps all fields correctly`() {
        val period = Period(
            id = 1L,
            startDate = LocalDate.of(2025, 1, 15),
            endDate = LocalDate.of(2025, 1, 19)
        )

        val entity = period.toEntity()

        assertEquals(1L, entity.id)
        assertEquals(LocalDate.of(2025, 1, 15), entity.startDate)
        assertEquals(LocalDate.of(2025, 1, 19), entity.endDate)
    }

    @Test
    fun `Period toEntity with null endDate maps correctly`() {
        val period = Period(
            id = 2L,
            startDate = LocalDate.of(2025, 2, 1),
            endDate = null
        )

        val entity = period.toEntity()

        assertEquals(2L, entity.id)
        assertEquals(LocalDate.of(2025, 2, 1), entity.startDate)
        assertNull(entity.endDate)
    }

    @Test
    fun `Period fromEntity maps all fields correctly`() {
        val entity = PeriodEntity(
            id = 3L,
            startDate = LocalDate.of(2025, 3, 10),
            endDate = LocalDate.of(2025, 3, 14)
        )

        val period = Period.fromEntity(entity)

        assertEquals(3L, period.id)
        assertEquals(LocalDate.of(2025, 3, 10), period.startDate)
        assertEquals(LocalDate.of(2025, 3, 14), period.endDate)
    }

    @Test
    fun `Period fromEntity with null endDate maps correctly`() {
        val entity = PeriodEntity(
            id = 4L,
            startDate = LocalDate.of(2025, 4, 1),
            endDate = null
        )

        val period = Period.fromEntity(entity)

        assertEquals(4L, period.id)
        assertEquals(LocalDate.of(2025, 4, 1), period.startDate)
        assertNull(period.endDate)
    }

    @Test
    fun `round trip conversion preserves data`() {
        val original = Period(
            id = 5L,
            startDate = LocalDate.of(2025, 5, 20),
            endDate = LocalDate.of(2025, 5, 24)
        )

        val entity = original.toEntity()
        val converted = Period.fromEntity(entity)

        assertEquals(original.id, converted.id)
        assertEquals(original.startDate, converted.startDate)
        assertEquals(original.endDate, converted.endDate)
    }

    @Test
    fun `round trip conversion with null endDate preserves data`() {
        val original = Period(
            id = 6L,
            startDate = LocalDate.of(2025, 6, 1),
            endDate = null
        )

        val entity = original.toEntity()
        val converted = Period.fromEntity(entity)

        assertEquals(original.id, converted.id)
        assertEquals(original.startDate, converted.startDate)
        assertEquals(original.endDate, converted.endDate)
    }
}
