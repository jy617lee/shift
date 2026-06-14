package com.schedule.shift.domain.ocr

import android.graphics.Bitmap

interface OcrEngine {
    suspend fun recognizeText(bitmap: Bitmap): OcrResult
}
