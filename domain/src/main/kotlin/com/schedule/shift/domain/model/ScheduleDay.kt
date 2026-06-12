package com.schedule.shift.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class ScheduleDay(
    val date: LocalDate,
    val type: DayType,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val codeLabel: String,
    val source: SourceType,
)
