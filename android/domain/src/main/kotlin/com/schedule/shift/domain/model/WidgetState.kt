package com.schedule.shift.domain.model

import java.time.LocalDate
import java.time.LocalTime

sealed class WidgetState {
    data class WorkDay(
        val date: LocalDate,
        val startTime: LocalTime,
        val endTime: LocalTime,
    ) : WidgetState()

    data class OffDay(
        val date: LocalDate,
        val codeLabel: String,
    ) : WidgetState()

    data object Unregistered : WidgetState()
}

fun ScheduleDay.toWidgetState(): WidgetState = when (type) {
    DayType.WORK -> {
        val start = startTime
        val end = endTime
        if (start != null && end != null) {
            WidgetState.WorkDay(date = date, startTime = start, endTime = end)
        } else {
            WidgetState.Unregistered
        }
    }
    DayType.OFF, DayType.OTHER -> WidgetState.OffDay(date = date, codeLabel = codeLabel)
    DayType.UNREGISTERED -> WidgetState.Unregistered
}
