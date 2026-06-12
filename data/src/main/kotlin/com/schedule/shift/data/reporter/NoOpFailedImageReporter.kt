package com.schedule.shift.data.reporter

import android.graphics.Bitmap
import com.schedule.shift.domain.reporter.FailedImageReporter

class NoOpFailedImageReporter : FailedImageReporter {
    override fun reportFailure(bitmap: Bitmap, errorReason: String) = Unit
}
