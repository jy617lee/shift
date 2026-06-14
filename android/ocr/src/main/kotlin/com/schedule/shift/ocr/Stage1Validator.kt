package com.schedule.shift.ocr

class Stage1Validator {

    fun isScheduleText(text: String): Boolean =
        containsDatePattern(text) && containsHeaderKeyword(text)

    private fun containsDatePattern(text: String): Boolean =
        DATE_PATTERN.containsMatchIn(text)

    private fun containsHeaderKeyword(text: String): Boolean =
        HEADER_KEYWORDS.any { text.contains(it) }

    companion object {
        private val DATE_PATTERN = Regex("""\d{1,2}/\d{1,2}\([월화수목금토일]\)""")
        private val HEADER_KEYWORDS = listOf(
            "근무", "출근", "시간표", "스케줄", "스케쥴", "근태", "일정표",
        )
    }
}
