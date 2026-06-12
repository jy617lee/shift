package com.schedule.shift.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class RawTextMaskerTest {

    @Test
    fun `short number under 6 digits not masked`() {
        assertEquals("12345", RawTextMasker.mask("12345"))
    }

    @Test
    fun `exactly 6 digits are masked`() {
        assertEquals("***", RawTextMasker.mask("123456"))
    }

    @Test
    fun `7 digits are masked`() {
        assertEquals("***", RawTextMasker.mask("1234567"))
    }

    @Test
    fun `employee id in bracket masked`() {
        assertEquals("[***] 슈퍼바이저", RawTextMasker.mask("[21080203] 슈퍼바이저"))
    }

    @Test
    fun `name and employee id masked preserving rest`() {
        val input = "전지윤 [21080203]"
        val result = RawTextMasker.mask(input)
        assertEquals("전지윤 [***]", result)
    }

    @Test
    fun `text without long numbers not changed`() {
        val input = "06/22(월) 14:00 19:30 정상"
        assertEquals(input, RawTextMasker.mask(input))
    }

    @Test
    fun `multiple long number sequences all masked`() {
        val input = "123456 정상 789012"
        assertEquals("*** 정상 ***", RawTextMasker.mask(input))
    }

    @Test
    fun `5 digit adjacent to separator not masked`() {
        assertEquals("1234 5678", RawTextMasker.mask("1234 5678"))
    }

    @Test
    fun `empty string returns empty string`() {
        assertEquals("", RawTextMasker.mask(""))
    }

    @Test
    fun `mixed korean and numbers preserves korean`() {
        val input = "근무 123456 확인"
        assertEquals("근무 *** 확인", RawTextMasker.mask(input))
    }
}
