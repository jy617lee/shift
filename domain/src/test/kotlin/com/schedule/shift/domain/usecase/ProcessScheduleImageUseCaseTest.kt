package com.schedule.shift.domain.usecase

import android.graphics.Bitmap
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.ocr.OcrEngine
import com.schedule.shift.domain.ocr.OcrResult
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.parser.ScheduleParser
import com.schedule.shift.domain.reporter.FailedImageReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ProcessScheduleImageUseCaseTest {

    private lateinit var ocrEngine: OcrEngine
    private lateinit var parser: ScheduleParser
    private lateinit var reporter: FailedImageReporter
    private lateinit var useCase: ProcessScheduleImageUseCase
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        ocrEngine = mockk()
        parser = mockk()
        reporter = mockk(relaxed = true)
        bitmap = mockk(relaxed = true)
        useCase = ProcessScheduleImageUseCase(ocrEngine, parser, reporter)
    }

    @Test
    fun `returns ParseResult Success when OCR and parsing succeed`() = runTest {
        val week = buildTestWeek()
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Success("스케쥴 텍스트")
        coEvery { parser.parse(any()) } returns ParseResult.Success(listOf(week))

        val result = useCase(bitmap)

        assertTrue(result is ParseResult.Success)
        assertEquals(listOf(week), (result as ParseResult.Success).weeks)
    }

    @Test
    fun `returns NOT_A_SCHEDULE when OCR succeeds but parsing fails with NOT_A_SCHEDULE`() = runTest {
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Success("관련 없는 텍스트")
        coEvery { parser.parse(any()) } returns ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)

        val result = useCase(bitmap)

        assertTrue(result is ParseResult.Failure)
        assertEquals(FailureReason.NOT_A_SCHEDULE, (result as ParseResult.Failure).reason)
    }

    @Test
    fun `returns PARSE_ERROR when OCR engine fails`() = runTest {
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Failure(RuntimeException("ML Kit 오류"))

        val result = useCase(bitmap)

        assertTrue(result is ParseResult.Failure)
        assertEquals(FailureReason.PARSE_ERROR, (result as ParseResult.Failure).reason)
    }

    @Test
    fun `does NOT auto-upload on failure — reporter not called without user action`() = runTest {
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Failure(RuntimeException("오류"))

        useCase(bitmap)

        verify(exactly = 0) { reporter.reportFailure(any(), any()) }
    }

    @Test
    fun `reportFailure delegates to reporter when user opts in`() = runTest {
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Failure(RuntimeException("오류"))
        every { reporter.reportFailure(bitmap, any()) } returns Unit

        useCase(bitmap)
        useCase.reportFailure(bitmap, "ML Kit 오류")

        verify(exactly = 1) { reporter.reportFailure(bitmap, "ML Kit 오류") }
    }

    @Test
    fun `passes recognized text to parser`() = runTest {
        val recognizedText = "06/08(월) 스케쥴"
        coEvery { ocrEngine.recognizeText(bitmap) } returns OcrResult.Success(recognizedText)
        coEvery { parser.parse(recognizedText) } returns ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)

        useCase(bitmap)

        coVerify(exactly = 1) { parser.parse(recognizedText) }
    }

    private fun buildTestWeek() = ScheduleWeek(
        weekStartDate = LocalDate.of(2026, 6, 8),
        days = (0..6).map { offset ->
            ScheduleDay(
                date = LocalDate.of(2026, 6, 8).plusDays(offset.toLong()),
                type = if (offset < 5) DayType.WORK else DayType.OFF,
                startTime = if (offset < 5) LocalTime.of(9, 0) else null,
                endTime = if (offset < 5) LocalTime.of(18, 0) else null,
                codeLabel = if (offset < 5) "정상" else "정규휴일",
                source = SourceType.PARSED,
            )
        },
    )
}
