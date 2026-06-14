package com.schedule.shift.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class WidgetStateTest {

    private val date = LocalDate.of(2026, 6, 12)
    private val start = LocalTime.of(14, 0)
    private val end = LocalTime.of(19, 30)

    private fun day(
        type: DayType,
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
        code: String = "",
    ) = ScheduleDay(date, type, startTime, endTime, code, SourceType.PARSED)

    @Test
    fun `WORK day with times maps to WorkDay`() {
        val state = day(DayType.WORK, start, end, "정상").toWidgetState()
        assertTrue(state is WidgetState.WorkDay)
        state as WidgetState.WorkDay
        assertEquals(date, state.date)
        assertEquals(start, state.startTime)
        assertEquals(end, state.endTime)
    }

    @Test
    fun `WORK day with null times maps to Unregistered`() {
        val state = day(DayType.WORK).toWidgetState()
        assertTrue(state is WidgetState.Unregistered)
    }

    @Test
    fun `OFF day maps to OffDay with codeLabel`() {
        val state = day(DayType.OFF, code = "정규휴일").toWidgetState()
        assertTrue(state is WidgetState.OffDay)
        assertEquals("정규휴일", (state as WidgetState.OffDay).codeLabel)
    }

    @Test
    fun `OTHER day maps to OffDay`() {
        val state = day(DayType.OTHER, code = "연차").toWidgetState()
        assertTrue(state is WidgetState.OffDay)
    }

    @Test
    fun `UNREGISTERED day maps to Unregistered`() {
        val state = day(DayType.UNREGISTERED).toWidgetState()
        assertTrue(state is WidgetState.Unregistered)
    }
}
