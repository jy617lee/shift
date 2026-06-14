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

    // Two-pass: collect full date+time lines, then carry-forward the last date
    // to orphaned time lines (계획/실적 sub-rows that appear without a date).
    @Suppress("CyclomaticComplexMethod", "CognitiveComplexMethod", "NestedBlockDepth")
    private fun extractDays(text: String): List<ScheduleDay> {
        val dayMap = linkedMapOf<LocalDate, ScheduleDay>()
        var lastDate: LocalDate? = null
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            val full = parseLine(line)
            if (full != null) {
                if (!dayMap.containsKey(full.date)) dayMap[full.date] = full
                lastDate = full.date
            } else {
                val dateOnly = DATE_ONLY_PATTERN.matchEntire(line)
                if (dateOnly != null) {
                    val month = dateOnly.groupValues[GROUP_MONTH].toInt()
                    val day = dateOnly.groupValues[GROUP_DAY].toInt()
                    lastDate = assignYear(month, day)
                } else {
                    val ld = lastDate
                    if (ld != null && !dayMap.containsKey(ld)) {
                        parseOrphanedTimeLine(line, ld)?.let { dayMap[it.date] = it }
                    }
                }
            }
        }
        return dayMap.values.toList()
    }

    private fun parseLine(line: String): ScheduleDay? {
        val match = LINE_PATTERN.find(line) ?: return null
        val groups = match.groupValues
        val startTime = groups[GROUP_START_TIME].ifEmpty { null }?.let { LocalTime.parse(it) }
        val endTime = groups[GROUP_END_TIME].ifEmpty { null }?.let { LocalTime.parse(it) }
        val code = groups[GROUP_CODE].trim()
        return if (startTime == null && code.isEmpty()) null
        else ScheduleDay(
            date = assignYear(groups[GROUP_MONTH].toInt(), groups[GROUP_DAY].toInt()),
            type = if (startTime != null) DayType.WORK else DayType.OTHER,
            startTime = startTime,
            endTime = endTime,
            codeLabel = code,
            source = SourceType.PARSED,
        )
    }

    private fun parseOrphanedTimeLine(line: String, date: LocalDate): ScheduleDay? {
        val match = ORPHAN_TIME_PATTERN.find(line) ?: return null
        val groups = match.groupValues
        val startTime = groups[1].ifEmpty { null }?.let { LocalTime.parse(it) }
        val endTime = groups[2].ifEmpty { null }?.let { LocalTime.parse(it) }
        return if (startTime == null || endTime == null) null
        else ScheduleDay(
            date = date,
            type = DayType.WORK,
            startTime = startTime,
            endTime = endTime,
            codeLabel = groups[3].trim(),
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
        // Indicators (P/계획, R/실적) may be OCR'd as @, O, 0, R with or without trailing space.
        // Parens may contain any char (OCR may produce %, empty, etc. instead of Korean day char).
        // Time separator may be absent (O15:0020:30 = indicator+start+end merged).
        @Suppress("MaxLineLength")
        private val LINE_PATTERN =
            Regex("""(\d{1,2})/(\d{1,2})\([^\)]*\)\s*(?:(?:[P@O0R]\s*)*(\d{2}:\d{2})[~\s]*(?:[P@O0R]\s*)*(\d{2}:\d{2})\s*)?(?:(?:[P@O0R]\s*)*(.*))?""")

        // Date-only line — no time or code after the date, used for carry-forward.
        private val DATE_ONLY_PATTERN =
            Regex("""(\d{1,2})/(\d{1,2})\([^\)]*\)\s*""")

        // Orphaned time line — has times but no date prefix (계획/실적 sub-rows).
        @Suppress("MaxLineLength")
        private val ORPHAN_TIME_PATTERN =
            Regex("""(?:[P@O0R]\s*)*(\d{2}:\d{2})[~\s]*(?:[P@O0R]\s*)*(\d{2}:\d{2})\s*(?:(?:[P@O0R]\s*)*(.*))?""")

        private const val GROUP_MONTH = 1
        private const val GROUP_DAY = 2
        private const val GROUP_START_TIME = 3
        private const val GROUP_END_TIME = 4
        private const val GROUP_CODE = 5
        private const val PAST_DAYS_THRESHOLD = 90L
        private const val FUTURE_DAYS_THRESHOLD = 56L
        private const val DAYS_IN_WEEK = 7
    }
}
