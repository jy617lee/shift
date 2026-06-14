package com.schedule.shift.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnonymousIdProvider
import com.schedule.shift.domain.analytics.AnalyticsTracker

class FirebaseAnalyticsTracker(
    private val firebaseAnalytics: FirebaseAnalytics,
    anonymousIdProvider: AnonymousIdProvider,
) : AnalyticsTracker {

    init {
        firebaseAnalytics.setUserId(anonymousIdProvider.getAnonymousId())
    }

    override fun track(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.eventName(), event.eventParams())
    }

    @Suppress("CyclomaticComplexMethod")
    private fun AnalyticsEvent.eventName(): String = when (this) {
        is AnalyticsEvent.AppOpen -> "app_launched"
        is AnalyticsEvent.WidgetActive -> "widget_active"
        is AnalyticsEvent.HomeWeekViewed -> "home_week_viewed"
        is AnalyticsEvent.SettingChanged -> "setting_changed"
        is AnalyticsEvent.RegisterStart -> "register_start"
        is AnalyticsEvent.ImageSelected -> "image_selected"
        is AnalyticsEvent.Stage1Result -> "stage1_result"
        is AnalyticsEvent.ParseResult -> "parse_result"
        is AnalyticsEvent.RowFailDetail -> "row_fail_detail"
        is AnalyticsEvent.SendDialog -> "send_dialog"
        is AnalyticsEvent.ConfirmShown -> "confirm_shown"
        is AnalyticsEvent.UserEdit -> "user_edit"
        is AnalyticsEvent.RegisterComplete -> "register_complete"
        is AnalyticsEvent.RegisterAbandon -> "register_abandon"
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod")
    private fun AnalyticsEvent.eventParams(): Bundle = when (this) {
        is AnalyticsEvent.AppOpen -> Bundle().apply {
            putString("source", source.value)
        }
        is AnalyticsEvent.WidgetActive -> Bundle().apply {
            putString("types", types.joinToString(","))
        }
        is AnalyticsEvent.HomeWeekViewed -> Bundle().apply {
            putInt("offset", offset)
        }
        is AnalyticsEvent.SettingChanged -> Bundle().apply {
            putString("key", key.value)
            putString("value", value)
        }
        is AnalyticsEvent.RegisterStart -> Bundle().apply {
            putString("session_id", sessionId)
        }
        is AnalyticsEvent.ImageSelected -> Bundle().apply {
            putString("session_id", sessionId)
            putInt("image_width", imageWidth)
            putInt("image_height", imageHeight)
        }
        is AnalyticsEvent.Stage1Result -> Bundle().apply {
            putString("session_id", sessionId)
            putBoolean("pass", pass)
            failReason?.let { putString("fail_reason", it) }
        }
        is AnalyticsEvent.ParseResult -> Bundle().apply {
            putString("session_id", sessionId)
            putInt("failed_rows", failedRows)
            putLong("duration_ms", durationMs)
            putFloat("ocr_confidence_avg", ocrConfidenceAvg)
        }
        is AnalyticsEvent.RowFailDetail -> Bundle().apply {
            putString("session_id", sessionId)
            putInt("row_index", rowIndex)
            putString("fail_reason", failReason)
            putString("raw_text_masked", rawTextMasked)
            putFloat("cell_confidence", cellConfidence)
        }
        is AnalyticsEvent.SendDialog -> Bundle().apply {
            putString("session_id", sessionId)
            putBoolean("consented", consented)
        }
        is AnalyticsEvent.ConfirmShown -> Bundle().apply {
            putString("session_id", sessionId)
            putBoolean("skipped", skipped)
        }
        is AnalyticsEvent.UserEdit -> Bundle().apply {
            putString("session_id", sessionId)
            putInt("row_index", rowIndex)
            putString("field", field)
            putString("parsed_value", parsedValue)
            putString("corrected_value", correctedValue)
            putBoolean("was_failed_row", wasFailedRow)
            putString("edit_source", editSource)
        }
        is AnalyticsEvent.RegisterComplete -> Bundle().apply {
            putString("session_id", sessionId)
            putInt("edited_rows", editedRows)
            putInt("manual_rows", manualRows)
            putBoolean("replace", replace)
            putLong("total_duration_ms", totalDurationMs)
        }
        is AnalyticsEvent.RegisterAbandon -> Bundle().apply {
            putString("session_id", sessionId)
            putString("last_step", lastStep)
        }
    }
}
