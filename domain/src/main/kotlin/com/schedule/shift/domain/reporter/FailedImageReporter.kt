package com.schedule.shift.domain.reporter

import android.graphics.Bitmap

interface FailedImageReporter {
    fun reportFailure(bitmap: Bitmap, errorReason: String)
}
