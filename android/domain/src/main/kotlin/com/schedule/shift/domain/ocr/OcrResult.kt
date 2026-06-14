package com.schedule.shift.domain.ocr

sealed class OcrResult {
    data class Success(val text: String) : OcrResult()
    data class Failure(val cause: Throwable) : OcrResult()
}
