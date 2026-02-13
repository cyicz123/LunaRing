package com.example.menstruation.data.repository

import com.example.menstruation.data.local.dao.PeriodDao
import com.example.menstruation.data.local.entity.PeriodEntity
import com.example.menstruation.data.model.Period
import com.example.menstruation.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for [PeriodRepository].
 */
@ExperimentalCoroutinesApi
class PeriodRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: PeriodRepository
    private lateinit var periodDao: PeriodDao

    @Before
    fun setup() {
        periodDao = mockk(relaxed = true)
        repository = PeriodRepository(periodDao)
    }

    // ==================== getAllPeriods() Tests ====================

    @Test
    fun `getAllPeriods returns mapped periods from dao`() = runTest {
        // Given
        val entities = listOf(
            PeriodEntity(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5)),
            PeriodEntity(id = 2, startDate = LocalDate.of(2025, 1, 29), endDate = LocalDate.of(2025, 2, 2))
        )
        every { periodDao.getAll() } returns flowOf(entities)

        // When
        val result = repository.getAllPeriods().toList().first()

        // Then
        assertEquals(2, result.size)
        assertEquals(1L, result[0].id)
        assertEquals(LocalDate.of(2025, 1, 1), result[0].startDate)
        assertEquals(LocalDate.of(2025, 1, 5), result[0].endDate)
    }

    @Test
    fun `getAllPeriods with empty list returns empty flow`() = runTest {
        // Given
        every { periodDao.getAll() } returns flowOf(emptyList())

        // When
        val result = repository.getAllPeriods().toList().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== getPeriodContaining() Tests ====================

    @Test
    fun `getPeriodContaining returns mapped period when found`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 3)
        val entity = PeriodEntity(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5))
        coEvery { periodDao.getPeriodContaining(date) } returns entity

        // When
        val result = repository.getPeriodContaining(date)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id)
        assertEquals(LocalDate.of(2025, 1, 1), result?.startDate)
    }

    @Test
    fun `getPeriodContaining returns null when not found`() = runTest {
        // Given
        val date = LocalDate.of(2025, 1, 10)
        coEvery { periodDao.getPeriodContaining(date) } returns null

        // When
        val result = repository.getPeriodContaining(date)

        // Then
        assertNull(result)
    }

    // ==================== getCurrentPeriod() Tests ====================

    @Test
    fun `getCurrentPeriod returns mapped period when ongoing`() = runTest {
        // Given
        val entity = PeriodEntity(id = 1, startDate = LocalDate.of(2025, 2, 1), endDate = null)
        coEvery { periodDao.getCurrentPeriod() } returns entity

        // When
        val result = repository.getCurrentPeriod()

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.id)
        assertNull(result?.endDate)
    }

    @Test
    fun `getCurrentPeriod returns null when no ongoing period`() = runTest {
        // Given
        coEvery { periodDao.getCurrentPeriod() } returns null

        // When
        val result = repository.getCurrentPeriod()

        // Then
        assertNull(result)
    }

    // ==================== startPeriod() Tests ====================

    @Test
    fun `startPeriod calculates end date and inserts entity`() = runTest {
        // Given
        val startDate = LocalDate.of(2025, 3, 1)
        val periodLength = 5
        coEvery { periodDao.insert(any()) } returns 1L

        // When
        val result = repository.startPeriod(startDate, periodLength)

        // Then
        coVerify {
            periodDao.insert(
                match {
                    it.startDate == startDate &&
                    it.endDate == startDate.plusDays((periodLength - 1).toLong())
                }
            )
        }
        assertEquals(1L, result)
    }

    // ==================== endPeriod() Tests ====================

    @Test
    fun `endPeriod updates current period with end date`() = runTest {
        // Given
        val endDate = LocalDate.of(2025, 3, 5)
        val currentEntity = PeriodEntity(id = 1, startDate = LocalDate.of(2025, 3, 1), endDate = null)
        coEvery { periodDao.getCurrentPeriod() } returns currentEntity

        // When
        repository.endPeriod(endDate)

        // Then
        coVerify {
            periodDao.update(
                match {
                    it.id == 1L && it.endDate == endDate
                }
            )
        }
    }

    @Test
    fun `endPeriod does nothing when no current period`() = runTest {
        // Given
        val endDate = LocalDate.of(2025, 3, 5)
        coEvery { periodDao.getCurrentPeriod() } returns null

        // When
        repository.endPeriod(endDate)

        // Then
        coVerify(exactly = 0) { periodDao.update(any()) }
    }

    // ==================== deletePeriod() Tests ====================

    @Test
    fun `deletePeriod calls dao deleteById`() = runTest {
        // Given
        val id = 1L

        // When
        repository.deletePeriod(id)

        // Then
        coVerify { periodDao.deleteById(id) }
    }

    // ==================== updatePeriod() Tests ====================

    @Test
    fun `updatePeriod converts to entity and updates`() = runTest {
        // Given
        val period = Period(id = 1, startDate = LocalDate.of(2025, 1, 1), endDate = LocalDate.of(2025, 1, 5))

        // When
        repository.updatePeriod(period)

        // Then
        coVerify {
            periodDao.update(
                match {
                    it.id == 1L &&
                    it.startDate == LocalDate.of(2025, 1, 1) &&
                    it.endDate == LocalDate.of(2025, 1, 5)
                }
            )
        }
    }

    // ==================== insertPeriod() Tests ====================

    @Test
    fun `insertPeriod converts to entity and inserts`() = runTest {
        // Given
        val period = Period(id = 0, startDate = LocalDate.of(2025, 4, 1), endDate = LocalDate.of(2025, 4, 5))
        coEvery { periodDao.insert(any()) } returns 2L

        // When
        val result = repository.insertPeriod(period)

        // Then
        coVerify { periodDao.insert(any()) }
        assertEquals(2L, result)
    }

    // ==================== deleteAllPeriods() Tests ====================

    @Test
    fun `deleteAllPeriods calls dao deleteAll`() = runTest {
        // When
        repository.deleteAllPeriods()

        // Then
        coVerify { periodDao.deleteAll() }
    }
}
