package com.schedule.shift.domain.usecase

import android.graphics.Bitmap
import com.schedule.shift.domain.ocr.OcrEngine
import com.schedule.shift.domain.ocr.OcrResult
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.parser.ScheduleParser
import com.schedule.shift.domain.reporter.FailedImageReporter

class ProcessScheduleImageUseCase(
    private val ocrEngine: OcrEngine,
    private val parser: ScheduleParser,
    private val reporter: FailedImageReporter,
) {
    suspend operator fun invoke(bitmap: Bitmap): ParseResult =
        when (val ocrResult = ocrEngine.recognizeText(bitmap)) {
            is OcrResult.Success -> parser.parse(ocrResult.text)
            is OcrResult.Failure -> ParseResult.Failure(FailureReason.PARSE_ERROR)
        }

    fun reportFailure(bitmap: Bitmap, errorReason: String) {
        reporter.reportFailure(bitmap, errorReason)
    }
}
