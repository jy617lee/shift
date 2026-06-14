package com.schedule.shift.domain.model

import java.time.LocalDate

data class ScheduleWeek(
    val weekStartDate: LocalDate,
    val days: List<ScheduleDay>,
) {
    init {
        require(days.size == DAYS_IN_WEEK) {
            "ScheduleWeek must have exactly $DAYS_IN_WEEK days, got ${days.size}"
        }
    }

    companion object {
        const val DAYS_IN_WEEK = 7
    }
}
