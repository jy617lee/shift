package com.schedule.shift.domain.parser

import com.schedule.shift.domain.model.ScheduleWeek

sealed class ParseResult {
    data class Success(val weeks: List<ScheduleWeek>) : ParseResult()
    data class Failure(val reason: FailureReason) : ParseResult()
}
