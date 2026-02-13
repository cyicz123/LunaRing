package com.example.menstruation.data.repository

import com.example.menstruation.data.local.dao.DailyRecordDao
import com.example.menstruation.data.local.entity.DailyRecordEntity
import com.example.menstruation.data.model.DailyRecord
import com.example.menstruation.data.model.FlowLevel
import com.example.menstruation.data.model.Mood
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
 * Tests for [DailyRecordRepository].
 */
@ExperimentalCoroutinesApi
class DailyRecordRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: DailyRecordRepository
    private lateinit var dailyRecordDao: DailyRecordDao

    @Before
    fun setup() {
        dailyRecordDao = mockk(relaxed = true)
        repository = DailyRecordRepository(dailyRecordDao)
    }

    // ==================== getAllRecords() Tests ====================

    @Test
    fun `getAllRecords returns mapped records from dao`() = runTest {
        // Given
        val entities = listOf(
            DailyRecordEntity(
                date = LocalDate.of(2025, 1, 15),
                isPeriodDay = true,
                flowLevel = "MEDIUM",
                painLevel = 5
            ),
            DailyRecordEntity(
                date = LocalDate.of(2025, 1, 16),
                isPeriodDay = true,
                flowLevel = "HEAVY",
                painLevel = 7
            )
        )
        every { dailyRecordDao.getAll() } returns flowOf(entities)

        // When
        val result = repository.getAllRecords().toList().first()

        // Then
        assertEquals(2, result.size)
        assertEquals(LocalDate.of(2025, 1, 15), result[0].date)
        assertEquals(FlowLevel.MEDIUM, result[0].flowLevel)
        assertEquals(5, result[0].painLevel)
    }

    @Test
    fun `getAllRecords with empty list returns empty flow`() = runTest {
        // Given
        every { dailyRecordDao.getAll() } returns flowOf(emptyList())

        // When
        val result = repository.getAllRecords().toList().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== getRecordsByDateRange() Tests ====================

    @Test
    fun `getRecordsByDateRange returns mapped records in range`() = runTest {
        // Given
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)
        val entities = listOf(
            DailyRecordEntity(
                date = LocalDate.of(2025, 1, 15),
                isPeriodDay = true,
                flowLevel = "MEDIUM"
            )
        )
        every { dailyRecordDao.getByDateRange(start, end) } returns flowOf(entities)

        // When
        val result = repository.getRecordsByDateRange(start, end).toList().first()

        // Then
        assertEquals(1, result.size)
        assertEquals(LocalDate.of(2025, 1, 15), result[0].date)
    }

    @Test
    fun `getRecordsByDateRange with no records returns empty flow`() = runTest {
        // Given
        val start = LocalDate.of(2025, 1, 1)
        val end = LocalDate.of(2025, 1, 31)
        every { dailyRecordDao.getByDateRange(start, end) } returns flowOf(emptyList())

        // When
        val result = repository.getRecordsByDateRange(start, end).toList().first()

        // Then
        assertTrue(result.isEmpty())
    }

    // ==================== getRecordByDate() Tests ====================

    @Test
    fun `getRecordByDate returns mapped record when found`() = runTest {
        // Given
        val date = LocalDate.of(2025, 2, 1)
        val entity = DailyRecordEntity(
            date = date,
            isPeriodDay = true,
            flowLevel = "LIGHT",
            painLevel = 3,
            mood = "HAPPY"
        )
        coEvery { dailyRecordDao.getByDate(date) } returns entity

        // When
        val result = repository.getRecordByDate(date)

        // Then
        assertNotNull(result)
        assertEquals(date, result?.date)
        assertEquals(FlowLevel.LIGHT, result?.flowLevel)
        assertEquals(3, result?.painLevel)
        assertEquals(Mood.HAPPY, result?.mood)
    }

    @Test
    fun `getRecordByDate returns null when not found`() = runTest {
        // Given
        val date = LocalDate.of(2025, 2, 1)
        coEvery { dailyRecordDao.getByDate(date) } returns null

        // When
        val result = repository.getRecordByDate(date)

        // Then
        assertNull(result)
    }

    // ==================== saveRecord() Tests ====================

    @Test
    fun `saveRecord converts to entity and inserts`() = runTest {
        // Given
        val record = DailyRecord(
            date = LocalDate.of(2025, 3, 1),
            isPeriodDay = true,
            flowLevel = FlowLevel.MEDIUM,
            painLevel = 5,
            hadSex = false,
            physicalSymptoms = emptyList(),
            mood = Mood.CALM,
            note = "Test note"
        )

        // When
        repository.saveRecord(record)

        // Then
        coVerify {
            dailyRecordDao.insert(
                match {
                    it.date == LocalDate.of(2025, 3, 1) &&
                    it.isPeriodDay == true &&
                    it.flowLevel == "MEDIUM" &&
                    it.painLevel == 5 &&
                    it.mood == "CALM" &&
                    it.note == "Test note"
                }
            )
        }
    }

    @Test
    fun `saveRecord with null values handles correctly`() = runTest {
        // Given
        val record = DailyRecord(
            date = LocalDate.of(2025, 3, 1),
            isPeriodDay = false,
            flowLevel = null,
            painLevel = null,
            hadSex = false,
            physicalSymptoms = emptyList(),
            mood = null,
            ovulationTest = null,
            note = null
        )

        // When
        repository.saveRecord(record)

        // Then
        coVerify {
            dailyRecordDao.insert(
                match {
                    it.date == LocalDate.of(2025, 3, 1) &&
                    it.isPeriodDay == false &&
                    it.flowLevel == null &&
                    it.painLevel == null &&
                    it.mood == null &&
                    it.note == null
                }
            )
        }
    }

    // ==================== deleteRecord() Tests ====================

    @Test
    fun `deleteRecord calls dao deleteByDate`() = runTest {
        // Given
        val date = LocalDate.of(2025, 4, 1)

        // When
        repository.deleteRecord(date)

        // Then
        coVerify { dailyRecordDao.deleteByDate(date) }
    }

    // ==================== deleteAllRecords() Tests ====================

    @Test
    fun `deleteAllRecords calls dao deleteAll`() = runTest {
        // When
        repository.deleteAllRecords()

        // Then
        coVerify { dailyRecordDao.deleteAll() }
    }
}
