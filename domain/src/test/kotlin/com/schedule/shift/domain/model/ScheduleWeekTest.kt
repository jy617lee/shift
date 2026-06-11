package com.schedule.shift.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleWeekTest {

    @Test
    fun `week creation succeeds with exactly 7 days`() {
        val week = buildWeek(LocalDate.of(2026, 6, 8))
        assertEquals(ScheduleWeek.DAYS_IN_WEEK, week.days.size)
    }

    @Test
    fun `week creation fails with fewer than 7 days`() {
        assertThrows(IllegalArgumentException::class.java) {
            ScheduleWeek(
                weekStartDate = LocalDate.of(2026, 6, 8),
                days = listOf(buildDay(LocalDate.of(2026, 6, 8))),
            )
        }
    }

    @Test
    fun `WORK day has required time fields`() {
        val day = ScheduleDay(
            date = LocalDate.of(2026, 6, 8),
            type = DayType.WORK,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(18, 0),
            codeLabel = "정상",
            source = SourceType.PARSED,
        )
        assertEquals(DayType.WORK, day.type)
        assertEquals(LocalTime.of(9, 0), day.startTime)
    }

    @Test
    fun `OFF day has null time fields`() {
        val day = ScheduleDay(
            date = LocalDate.of(2026, 6, 13),
            type = DayType.OFF,
            startTime = null,
            endTime = null,
            codeLabel = "정규휴일",
            source = SourceType.PARSED,
        )
        assertEquals(DayType.OFF, day.type)
        assertEquals(null, day.startTime)
    }

    private fun buildDay(date: LocalDate) = ScheduleDay(
        date = date,
        type = DayType.WORK,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(18, 0),
        codeLabel = "정상",
        source = SourceType.PARSED,
    )

    private fun buildWeek(monday: LocalDate) = ScheduleWeek(
        weekStartDate = monday,
        days = (0..6).map { offset ->
            buildDay(monday.plusDays(offset.toLong()))
        },
    )
}
