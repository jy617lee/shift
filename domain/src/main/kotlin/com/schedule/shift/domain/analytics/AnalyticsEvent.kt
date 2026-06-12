package com.schedule.shift.domain.analytics

sealed class AnalyticsEvent {

    // ── §11.2 사용·리텐션 ─────────────────────────────────────────────────────

    data class AppOpen(val source: AppOpenSource) : AnalyticsEvent()

    data class WidgetActive(val types: List<String>) : AnalyticsEvent()

    data class HomeWeekViewed(val offset: Int) : AnalyticsEvent()

    data class SettingChanged(val key: SettingKey, val value: String) : AnalyticsEvent()

    // ── §11.3 등록 퍼널 ───────────────────────────────────────────────────────

    data class RegisterStart(val sessionId: String) : AnalyticsEvent()

    data class ImageSelected(
        val sessionId: String,
        val imageWidth: Int,
        val imageHeight: Int,
    ) : AnalyticsEvent()

    data class Stage1Result(
        val sessionId: String,
        val pass: Boolean,
        val failReason: String?,
    ) : AnalyticsEvent()

    data class ParseResult(
        val sessionId: String,
        val failedRows: Int,
        val durationMs: Long,
        val ocrConfidenceAvg: Float,
    ) : AnalyticsEvent()

    data class RowFailDetail(
        val sessionId: String,
        val rowIndex: Int,
        val failReason: String,
        val rawTextMasked: String,
        val cellConfidence: Float,
    ) : AnalyticsEvent()

    data class SendDialog(val sessionId: String, val consented: Boolean) : AnalyticsEvent()

    data class ConfirmShown(val sessionId: String, val skipped: Boolean) : AnalyticsEvent()

    data class UserEdit(
        val sessionId: String,
        val rowIndex: Int,
        val field: String,
        val parsedValue: String,
        val correctedValue: String,
        val wasFailedRow: Boolean,
        val editSource: String,
    ) : AnalyticsEvent()

    data class RegisterComplete(
        val sessionId: String,
        val editedRows: Int,
        val manualRows: Int,
        val replace: Boolean,
        val totalDurationMs: Long,
    ) : AnalyticsEvent()

    data class RegisterAbandon(val sessionId: String, val lastStep: String) : AnalyticsEvent()
}

enum class AppOpenSource(val value: String) {
    ICON("icon"),
    WIDGET_2X1("widget_2x1"),
    WIDGET_2X2("widget_2x2"),
    WIDGET_4X1("widget_4x1"),
    WIDGET_4X2_COUNTDOWN("widget_4x2_countdown"),
    WIDGET_4X2_WEEKLY("widget_4x2_weekly"),
}

enum class SettingKey(val value: String) {
    SKIP_CONFIRM("skip_confirm"),
}
