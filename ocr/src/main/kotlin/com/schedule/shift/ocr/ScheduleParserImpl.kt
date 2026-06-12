package com.schedule.shift.ocr

import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.parser.ScheduleParser
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class ScheduleParserImpl(
    private val validator: Stage1Validator,
    private val today: LocalDate = LocalDate.now(),
) : ScheduleParser {

    override suspend fun parse(recognizedText: String): ParseResult =
        when {
            !validator.isScheduleText(recognizedText) ->
                ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)
            else -> {
                val days = extractDays(recognizedText)
                if (days.isEmpty()) {
                    ParseResult.Failure(FailureReason.PARSE_ERROR)
                } else {
                    ParseResult.Success(groupIntoWeeks(days))
                }
            }
        }

    private fun extractDays(text: String): List<ScheduleDay> =
        text.lines().mapNotNull { line -> parseLine(line.trim()) }

    private fun parseLine(line: String): ScheduleDay? {
        val match = LINE_PATTERN.find(line) ?: return null
        val groups = match.groupValues
        val date = assignYear(groups[GROUP_MONTH].toInt(), groups[GROUP_DAY].toInt())
        val startTime = groups[GROUP_START_TIME].ifEmpty { null }?.let { LocalTime.parse(it) }
        val endTime = groups[GROUP_END_TIME].ifEmpty { null }?.let { LocalTime.parse(it) }
        return ScheduleDay(
            date = date,
            type = if (startTime != null) DayType.WORK else DayType.OTHER,
            startTime = startTime,
            endTime = endTime,
            codeLabel = groups[GROUP_CODE].trim(),
            source = SourceType.PARSED,
        )
    }

    private fun assignYear(month: Int, day: Int): LocalDate {
        val candidate = LocalDate.of(today.year, month, day)
        return when {
            candidate.isBefore(today.minusDays(PAST_DAYS_THRESHOLD)) ->
                candidate.plusYears(1)
            candidate.isAfter(today.plusDays(FUTURE_DAYS_THRESHOLD)) ->
                candidate.minusYears(1)
            else -> candidate
        }
    }

    private fun groupIntoWeeks(days: List<ScheduleDay>): List<ScheduleWeek> {
        val sortedDays = days.sortedBy { it.date }
        val firstDate = sortedDays.first().date
        val lastDate = sortedDays.last().date

        val weekStart = firstDate.with(DayOfWeek.MONDAY).let { monday ->
            if (monday.isAfter(firstDate)) monday.minusWeeks(1) else monday
        }
        val weekEnd = lastDate.with(DayOfWeek.MONDAY).let { monday ->
            if (monday.isAfter(lastDate)) monday.minusWeeks(1) else monday
        }

        val dayMap = sortedDays.associateBy { it.date }
        val weeks = mutableListOf<ScheduleWeek>()
        var current = weekStart

        while (!current.isAfter(weekEnd)) {
            val weekDays = (0 until DAYS_IN_WEEK).map { offset ->
                val date = current.plusDays(offset.toLong())
                dayMap[date] ?: ScheduleDay(
                    date = date,
                    type = DayType.UNREGISTERED,
                    startTime = null,
                    endTime = null,
                    codeLabel = "",
                    source = SourceType.PARSED,
                )
            }
            weeks.add(ScheduleWeek(weekStartDate = current, days = weekDays))
            current = current.plusWeeks(1)
        }

        return weeks
    }

    companion object {
        // [P@] handles plan-indicator badges that ML Kit may read as "P" or "@"; [~\s]+ allows tilde or space between times
        @Suppress("MaxLineLength")
        private val LINE_PATTERN =
            Regex("""(\d{1,2})/(\d{1,2})\([월화수목금토일]\)\s+(?:(?:[P@]\s+)?(\d{2}:\d{2})[~\s]+(?:[P@]\s+)?(\d{2}:\d{2})\s+(?:[P@]\s+)?)?(.+)""")
        private const val GROUP_MONTH = 1
        private const val GROUP_DAY = 2
        private const val GROUP_START_TIME = 3
        private const val GROUP_END_TIME = 4
        private const val GROUP_CODE = 5
        private const val PAST_DAYS_THRESHOLD = 7L
        private const val FUTURE_DAYS_THRESHOLD = 56L
        private const val DAYS_IN_WEEK = 7
    }
}
