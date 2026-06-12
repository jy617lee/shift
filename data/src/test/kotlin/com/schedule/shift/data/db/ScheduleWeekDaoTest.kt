package com.schedule.shift.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35])
class ScheduleWeekDaoTest {

    private lateinit var db: ShiftDatabase
    private lateinit var dao: ScheduleWeekDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ShiftDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.scheduleWeekDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `insert and getByWeekStart returns the inserted week`() = runTest {
        val week = buildTestWeek(LocalDate.of(2026, 6, 8))

        dao.insertWeek(week.toEntity())
        val result = dao.getByWeekStart(LocalDate.of(2026, 6, 8))

        assertEquals(week.weekStartDate, result?.toDomain()?.weekStartDate)
    }

    @Test
    fun `getByWeekStart returns null when no data`() = runTest {
        val result = dao.getByWeekStart(LocalDate.of(2026, 6, 1))
        assertNull(result)
    }

    @Test
    fun `insertWeek replaces on conflict`() = runTest {
        val date = LocalDate.of(2026, 6, 8)
        val original = buildTestWeek(date, startHour = 9)
        val updated = buildTestWeek(date, startHour = 10)

        dao.insertWeek(original.toEntity())
        dao.insertWeek(updated.toEntity())

        val result = dao.getByWeekStart(date)
        assertEquals(
            LocalTime.of(10, 0),
            result?.toDomain()?.days?.first { it.type == DayType.WORK }?.startTime,
        )
    }

    @Test
    fun `getWeeksInRange returns correct weeks`() = runTest {
        val week1 = buildTestWeek(LocalDate.of(2026, 6, 1))
        val week2 = buildTestWeek(LocalDate.of(2026, 6, 8))
        val week3 = buildTestWeek(LocalDate.of(2026, 6, 15))

        dao.insertWeek(week1.toEntity())
        dao.insertWeek(week2.toEntity())
        dao.insertWeek(week3.toEntity())

        val result = dao.getWeeksInRange(
            LocalDate.of(2026, 6, 1),
            LocalDate.of(2026, 6, 8),
        )

        assertEquals(2, result.size)
    }

    @Test
    fun `deleteWeek removes the week`() = runTest {
        val date = LocalDate.of(2026, 6, 8)
        dao.insertWeek(buildTestWeek(date).toEntity())
        dao.deleteWeek(date)
        assertNull(dao.getByWeekStart(date))
    }

    @Test
    fun `getAllWeeks returns all inserted weeks ordered by date`() = runTest {
        dao.insertWeek(buildTestWeek(LocalDate.of(2026, 6, 15)).toEntity())
        dao.insertWeek(buildTestWeek(LocalDate.of(2026, 6, 1)).toEntity())
        dao.insertWeek(buildTestWeek(LocalDate.of(2026, 6, 8)).toEntity())

        val result = dao.getAllWeeks()

        assertEquals(3, result.size)
        assertTrue(result[0].weekStartDate <= result[1].weekStartDate)
        assertTrue(result[1].weekStartDate <= result[2].weekStartDate)
    }

    private fun buildTestWeek(
        monday: LocalDate,
        startHour: Int = 9,
    ): ScheduleWeek = ScheduleWeek(
        weekStartDate = monday,
        days = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            if (offset < 5) {
                ScheduleDay(
                    date = date,
                    type = DayType.WORK,
                    startTime = LocalTime.of(startHour, 0),
                    endTime = LocalTime.of(18, 0),
                    codeLabel = "정상",
                    source = SourceType.PARSED,
                )
            } else {
                ScheduleDay(
                    date = date,
                    type = DayType.OFF,
                    startTime = null,
                    endTime = null,
                    codeLabel = "정규휴일",
                    source = SourceType.PARSED,
                )
            }
        },
    )
}
