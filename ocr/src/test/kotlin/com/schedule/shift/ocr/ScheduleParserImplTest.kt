package com.schedule.shift.ocr

import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleParserImplTest {

    private lateinit var parser: ScheduleParserImpl
    private val fixedToday = LocalDate.of(2026, 6, 12)

    @Before
    fun setUp() {
        parser = ScheduleParserImpl(
            validator = Stage1Validator(),
            today = fixedToday,
        )
    }

    // ── Stage 1 실패 케이스 ─────────────────────────────────────────────────

    @Test
    fun `returns NOT_A_SCHEDULE when text has no schedule markers`() = runTest {
        val result = parser.parse("오늘 점심은 김치찌개입니다.")
        assertTrue(result is ParseResult.Failure)
        assertEquals(FailureReason.NOT_A_SCHEDULE, (result as ParseResult.Failure).reason)
    }

    @Test
    fun `returns NOT_A_SCHEDULE when text has keyword but no date pattern`() = runTest {
        val result = parser.parse("근무 관련 공지사항입니다.")
        assertEquals(FailureReason.NOT_A_SCHEDULE, (result as ParseResult.Failure).reason)
    }

    // ── 파싱 성공 케이스 ────────────────────────────────────────────────────

    @Test
    fun `parses single week with work and off days`() = runTest {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 09:00~18:00 정상
            06/10(수) 09:00~18:00 정상
            06/11(목) 09:00~18:00 정상
            06/12(금) 09:00~18:00 정상
            06/13(토) 정규휴일
            06/14(일) 정규휴일
        """.trimIndent()

        val result = parser.parse(text)

        assertTrue(result is ParseResult.Success)
        val weeks = (result as ParseResult.Success).weeks
        assertEquals(1, weeks.size)
        val week = weeks.first()
        assertEquals(LocalDate.of(2026, 6, 8), week.weekStartDate)
        assertEquals(DayType.WORK, week.days[0].type)
        assertEquals(DayType.OTHER, week.days[5].type)
    }

    @Test
    fun `parses work day times correctly`() = runTest {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 09:00~18:00 정상
            06/10(수) 09:00~18:00 정상
            06/11(목) 09:00~18:00 정상
            06/12(금) 09:00~18:00 정상
            06/13(토) 정규휴일
            06/14(일) 정규휴일
        """.trimIndent()

        val result = parser.parse(text) as ParseResult.Success
        val monday = result.weeks.first().days.first()

        assertEquals(LocalTime.of(9, 0), monday.startTime)
        assertEquals(LocalTime.of(18, 0), monday.endTime)
    }

    @Test
    fun `stores code label verbatim without mapping`() = runTest {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 연차
            06/10(수) 09:00~18:00 정상
            06/11(목) 반차
            06/12(금) 09:00~18:00 정상
            06/13(토) 정규휴일
            06/14(일) 정규휴일
        """.trimIndent()

        val result = parser.parse(text) as ParseResult.Success
        val days = result.weeks.first().days
        assertEquals("정상", days[0].codeLabel)
        assertEquals("연차", days[1].codeLabel)
        assertEquals("반차", days[3].codeLabel)
        assertEquals("정규휴일", days[5].codeLabel)
    }

    @Test
    fun `day without times is DayType OTHER`() = runTest {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 연차
            06/10(수) 09:00~18:00 정상
            06/11(목) 09:00~18:00 정상
            06/12(금) 09:00~18:00 정상
            06/13(토) 정규휴일
            06/14(일) 정규휴일
        """.trimIndent()

        val result = parser.parse(text) as ParseResult.Success
        val tuesday = result.weeks.first().days[1]
        assertEquals(DayType.OTHER, tuesday.type)
        assertTrue(tuesday.startTime == null)
        assertTrue(tuesday.endTime == null)
    }

    @Test
    fun `parses two consecutive weeks`() = runTest {
        val text = loadSample("sample_07_two_weeks.txt")
        val result = parser.parse(text)

        assertTrue(result is ParseResult.Success)
        assertEquals(2, (result as ParseResult.Success).weeks.size)
    }

    @Test
    fun `assigns correct year to dates`() = runTest {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 09:00~18:00 정상
            06/10(수) 09:00~18:00 정상
            06/11(목) 09:00~18:00 정상
            06/12(금) 09:00~18:00 정상
            06/13(토) 정규휴일
            06/14(일) 정규휴일
        """.trimIndent()

        val result = parser.parse(text) as ParseResult.Success
        val date = result.weeks.first().days.first().date
        assertEquals(2026, date.year)
        assertEquals(6, date.monthValue)
        assertEquals(8, date.dayOfMonth)
    }

    @Test
    fun `fills missing days with UNREGISTERED when week is incomplete`() = runTest {
        val text = """
            근무 스케쥴
            06/10(수) 09:00~18:00 정상
            06/11(목) 09:00~18:00 정상
            06/12(금) 09:00~18:00 정상
        """.trimIndent()

        val result = parser.parse(text) as ParseResult.Success
        val week = result.weeks.first()
        assertEquals(ScheduleWeek.DAYS_IN_WEEK, week.days.size)
        assertEquals(DayType.UNREGISTERED, week.days[0].type)
        assertEquals(DayType.UNREGISTERED, week.days[1].type)
        assertEquals(DayType.WORK, week.days[2].type)
    }

    @Test
    fun `parses sample_01_june_full_month`() = runTest {
        val text = loadSample("sample_01_june_full_month.txt")
        val result = parser.parse(text)
        assertTrue(result is ParseResult.Success)
        val weeks = (result as ParseResult.Success).weeks
        assertTrue(weeks.isNotEmpty())
        assertTrue(weeks.all { it.days.size == ScheduleWeek.DAYS_IN_WEEK })
    }

    @Test
    fun `parses sample_03_shift_work with different shift codes`() = runTest {
        val text = loadSample("sample_03_shift_work.txt")
        val result = parser.parse(text) as ParseResult.Success
        val days = result.weeks.flatMap { it.days }.filter { it.type == DayType.WORK }
        assertTrue(days.any { it.codeLabel == "조번" })
        assertTrue(days.any { it.codeLabel == "석번" })
    }

    @Test
    fun `parses sample_05_ocr_noise with extra spaces`() = runTest {
        val text = loadSample("sample_05_ocr_noise.txt")
        val result = parser.parse(text)
        assertTrue(result is ParseResult.Success)
    }

    @Test
    fun `parses sample_06_single_digit_dates`() = runTest {
        val text = loadSample("sample_06_single_digit_dates.txt")
        val result = parser.parse(text) as ParseResult.Success
        val firstDay = result.weeks.first().days.first { it.type == DayType.WORK }
        assertEquals(7, firstDay.date.monthValue)
        assertEquals(6, firstDay.date.dayOfMonth)
    }

    // ── 실이미지 OCR 출력 검증 (test_img/test_1.jpeg) ─────────────────────

    @Test
    fun `sample_11 real image jun22 parses 7 days correctly`() = runTest {
        val text = loadSample("sample_11_real_schedule_jun22.txt")
        val result = parser.parse(text) as ParseResult.Success

        assertEquals(1, result.weeks.size)
        val days = result.weeks.first().days
        assertEquals(ScheduleWeek.DAYS_IN_WEEK, days.size)
        assertEquals(LocalDate.of(2026, 6, 22), result.weeks.first().weekStartDate)
    }

    @Test
    fun `sample_11 work days have correct times`() = runTest {
        val text = loadSample("sample_11_real_schedule_jun22.txt")
        val days = (parser.parse(text) as ParseResult.Success).weeks.first().days

        assertEquals(LocalTime.of(14, 0), days[0].startTime)
        assertEquals(LocalTime.of(19, 30), days[0].endTime)

        assertEquals(LocalTime.of(15, 0), days[1].startTime)
        assertEquals(LocalTime.of(20, 30), days[1].endTime)

        assertEquals(LocalTime.of(15, 0), days[3].startTime)
        assertEquals(LocalTime.of(20, 30), days[3].endTime)

        assertEquals(LocalTime.of(6, 30), days[6].startTime)
        assertEquals(LocalTime.of(12, 0), days[6].endTime)
    }

    @Test
    fun `sample_11 day types are correct`() = runTest {
        val text = loadSample("sample_11_real_schedule_jun22.txt")
        val days = (parser.parse(text) as ParseResult.Success).weeks.first().days

        assertEquals(DayType.WORK, days[0].type)  // 월 14:00~19:30
        assertEquals(DayType.WORK, days[1].type)  // 화 15:00~20:30
        assertEquals(DayType.OTHER, days[2].type) // 수 정규휴일
        assertEquals(DayType.WORK, days[3].type)  // 목 15:00~20:30
        assertEquals(DayType.WORK, days[4].type)  // 금 15:00~20:30
        assertEquals(DayType.OTHER, days[5].type) // 토 정규휴일
        assertEquals(DayType.WORK, days[6].type)  // 일 06:30~12:00
    }

    @Test
    fun `sample_11 code labels stored verbatim`() = runTest {
        val text = loadSample("sample_11_real_schedule_jun22.txt")
        val days = (parser.parse(text) as ParseResult.Success).weeks.first().days

        assertEquals("정상", days[0].codeLabel)
        assertEquals("정규휴일", days[2].codeLabel)
        assertEquals("정상", days[6].codeLabel)
    }

    private fun loadSample(fileName: String): String =
        javaClass.classLoader
            ?.getResourceAsStream("ocr_samples/$fileName")
            ?.bufferedReader()
            ?.readText()
            ?: error("샘플 파일을 찾을 수 없음: $fileName")
}
