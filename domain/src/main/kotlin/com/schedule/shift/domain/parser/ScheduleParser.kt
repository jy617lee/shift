package com.schedule.shift.domain.parser

interface ScheduleParser {
    suspend fun parse(recognizedText: String): ParseResult
}
