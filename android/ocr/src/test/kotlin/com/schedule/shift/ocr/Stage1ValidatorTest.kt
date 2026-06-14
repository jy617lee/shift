package com.schedule.shift.ocr

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Stage1ValidatorTest {

    private lateinit var validator: Stage1Validator

    @Before
    fun setUp() {
        validator = Stage1Validator()
    }

    @Test
    fun `returns true when text has date pattern and header keyword`() {
        val text = """
            근무 스케쥴
            06/08(월) 09:00~18:00 정상
            06/09(화) 09:00~18:00 정상
        """.trimIndent()
        assertTrue(validator.isScheduleText(text))
    }

    @Test
    fun `returns false when text has date pattern but no header keyword`() {
        val text = """
            06/08(월) 09:00~18:00
            06/09(화) 09:00~18:00
        """.trimIndent()
        assertFalse(validator.isScheduleText(text))
    }

    @Test
    fun `returns false when text has header keyword but no date pattern`() {
        val text = "이번 달 근무 일정을 알려드립니다."
        assertFalse(validator.isScheduleText(text))
    }

    @Test
    fun `returns false for empty text`() {
        assertFalse(validator.isScheduleText(""))
    }

    @Test
    fun `returns false for blank text`() {
        assertFalse(validator.isScheduleText("   \n   "))
    }

    @Test
    fun `recognizes 스케줄 as header keyword`() {
        val text = "스케줄표\n06/08(월) 정상"
        assertTrue(validator.isScheduleText(text))
    }

    @Test
    fun `recognizes 근태 as header keyword`() {
        val text = "근태 현황\n06/08(월) 정상"
        assertTrue(validator.isScheduleText(text))
    }

    @Test
    fun `recognizes 시간표 as header keyword`() {
        val text = "시간표\n06/08(월) 09:00~18:00"
        assertTrue(validator.isScheduleText(text))
    }

    @Test
    fun `matches single-digit month and day`() {
        val text = "근무 일정\n6/8(월) 정상"
        assertTrue(validator.isScheduleText(text))
    }

    @Test
    fun `matches all Korean weekday characters`() {
        val weekdays = listOf("월", "화", "수", "목", "금", "토", "일")
        weekdays.forEach { day ->
            val text = "근무 스케쥴\n06/08($day) 정상"
            assertTrue("$day 요일을 인식해야 함", validator.isScheduleText(text))
        }
    }

    @Test
    fun `returns false when date pattern uses non-Korean weekday`() {
        val text = "근무 스케쥴\n06/08(Mon) 정상"
        assertFalse(validator.isScheduleText(text))
    }

    @Test
    fun `returns false for random text without schedule markers`() {
        val text = "오늘 날씨가 맑습니다. 점심 메뉴는 김치찌개입니다."
        assertFalse(validator.isScheduleText(text))
    }
}
