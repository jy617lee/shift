package com.schedule.shift.analytics

object RawTextMasker {
    private val SIX_PLUS_DIGITS = Regex("[0-9]{6,}")

    fun mask(text: String): String = SIX_PLUS_DIGITS.replace(text, "***")
}
