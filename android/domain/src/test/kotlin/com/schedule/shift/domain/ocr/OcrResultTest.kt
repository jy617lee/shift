package com.schedule.shift.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrResultTest {

    @Test
    fun `Success holds recognized text`() {
        val result = OcrResult.Success("스케쥴 텍스트")
        assertTrue(result is OcrResult.Success)
        assertEquals("스케쥴 텍스트", result.text)
    }

    @Test
    fun `Failure holds cause throwable`() {
        val cause = RuntimeException("ML Kit 오류")
        val result = OcrResult.Failure(cause)
        assertTrue(result is OcrResult.Failure)
        assertEquals(cause, result.cause)
    }

    @Test
    fun `Success and Failure are distinct subtypes`() {
        val success = OcrResult.Success("텍스트")
        val failure = OcrResult.Failure(RuntimeException())
        assertTrue(success !is OcrResult.Failure)
        assertTrue(failure !is OcrResult.Success)
    }
}
