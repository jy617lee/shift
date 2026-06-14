package com.schedule.shift.widget

internal fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / SECONDS_PER_HOUR
    val m = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val s = totalSeconds % SECONDS_PER_MINUTE
    return when {
        h > 0 -> "${h}시간 ${m}분 ${s}초"
        m > 0 -> "${m}분 ${s}초"
        else -> "${s}초"
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
