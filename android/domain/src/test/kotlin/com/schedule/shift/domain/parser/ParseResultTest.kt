package com.schedule.shift.domain.parser

import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ParseResultTest {

    @Test
    fun `Success holds list of ScheduleWeek`() {
        val week = buildTestWeek(LocalDate.of(2026, 6, 8))
        val result = ParseResult.Success(listOf(week))
        assertTrue(result is ParseResult.Success)
        assertEquals(1, result.weeks.size)
        assertEquals(week, result.weeks.first())
    }

    @Test
    fun `Failure NOT_A_SCHEDULE reason is preserved`() {
        val result = ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)
        assertTrue(result is ParseResult.Failure)
        assertEquals(FailureReason.NOT_A_SCHEDULE, result.reason)
    }

    @Test
    fun `Failure PARSE_ERROR reason is preserved`() {
        val result = ParseResult.Failure(FailureReason.PARSE_ERROR)
        assertEquals(FailureReason.PARSE_ERROR, result.reason)
    }

    @Test
    fun `Success with empty weeks is valid`() {
        val result = ParseResult.Success(emptyList())
        assertTrue(result.weeks.isEmpty())
    }

    private fun buildTestWeek(monday: LocalDate) = ScheduleWeek(
        weekStartDate = monday,
        days = (0..6).map { offset ->
            ScheduleDay(
                date = monday.plusDays(offset.toLong()),
                type = if (offset < 5) DayType.WORK else DayType.OFF,
                startTime = if (offset < 5) LocalTime.of(9, 0) else null,
                endTime = if (offset < 5) LocalTime.of(18, 0) else null,
                codeLabel = if (offset < 5) "정상" else "정규휴일",
                source = SourceType.PARSED,
            )
        },
    )
}
