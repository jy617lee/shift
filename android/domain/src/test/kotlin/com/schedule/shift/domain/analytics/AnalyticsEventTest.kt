package com.schedule.shift.domain.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun `AppOpen holds correct source`() {
        val event = AnalyticsEvent.AppOpen(AppOpenSource.ICON)
        assertEquals(AppOpenSource.ICON, event.source)
    }

    @Test
    fun `AppOpen widget sources are distinct`() {
        val sources = AppOpenSource.entries.map { it.value }.toSet()
        assertEquals(AppOpenSource.entries.size, sources.size)
    }

    @Test
    fun `WidgetActive holds type list`() {
        val types = listOf("widget_2x1", "widget_4x1")
        val event = AnalyticsEvent.WidgetActive(types)
        assertEquals(types, event.types)
    }

    @Test
    fun `HomeWeekViewed holds offset`() {
        val event = AnalyticsEvent.HomeWeekViewed(offset = -1)
        assertEquals(-1, event.offset)
    }

    @Test
    fun `SettingChanged holds key and value`() {
        val event = AnalyticsEvent.SettingChanged(SettingKey.SKIP_CONFIRM, "true")
        assertEquals(SettingKey.SKIP_CONFIRM, event.key)
        assertEquals("true", event.value)
    }

    @Test
    fun `RegisterStart holds sessionId`() {
        val event = AnalyticsEvent.RegisterStart("sess-001")
        assertEquals("sess-001", event.sessionId)
    }

    @Test
    fun `ImageSelected holds dimensions and sessionId`() {
        val event = AnalyticsEvent.ImageSelected("sess-001", 1080, 1920)
        assertEquals(1080, event.imageWidth)
        assertEquals(1920, event.imageHeight)
        assertEquals("sess-001", event.sessionId)
    }

    @Test
    fun `Stage1Result pass true`() {
        val event = AnalyticsEvent.Stage1Result("sess-001", pass = true, failReason = null)
        assertEquals(true, event.pass)
        assertEquals(null, event.failReason)
    }

    @Test
    fun `Stage1Result pass false with reason`() {
        val event = AnalyticsEvent.Stage1Result("sess-001", pass = false, failReason = "not_schedule")
        assertEquals(false, event.pass)
        assertEquals("not_schedule", event.failReason)
    }

    @Test
    fun `ParseResult holds metrics`() {
        val event = AnalyticsEvent.ParseResult("sess-001", failedRows = 2, durationMs = 350L, ocrConfidenceAvg = 0.95f)
        assertEquals(2, event.failedRows)
        assertEquals(350L, event.durationMs)
        assertEquals(0.95f, event.ocrConfidenceAvg, 0.001f)
    }

    @Test
    fun `RowFailDetail holds row info and masked text`() {
        val event = AnalyticsEvent.RowFailDetail(
            sessionId = "sess-001",
            rowIndex = 2,
            failReason = "no_time",
            rawTextMasked = "06/10(수) ***",
            cellConfidence = 0.8f,
        )
        assertEquals(2, event.rowIndex)
        assertEquals("no_time", event.failReason)
        assertEquals("06/10(수) ***", event.rawTextMasked)
    }

    @Test
    fun `SendDialog records consent`() {
        val event = AnalyticsEvent.SendDialog("sess-001", consented = true)
        assertEquals(true, event.consented)
    }

    @Test
    fun `ConfirmShown records skip`() {
        val event = AnalyticsEvent.ConfirmShown("sess-001", skipped = false)
        assertEquals(false, event.skipped)
    }

    @Test
    fun `UserEdit holds all edit fields`() {
        val event = AnalyticsEvent.UserEdit(
            sessionId = "sess-001",
            rowIndex = 1,
            field = "start_time",
            parsedValue = "08:30",
            correctedValue = "09:00",
            wasFailedRow = false,
            editSource = "manual",
        )
        assertEquals("start_time", event.field)
        assertEquals("08:30", event.parsedValue)
        assertEquals("09:00", event.correctedValue)
    }

    @Test
    fun `RegisterComplete holds completion stats`() {
        val event = AnalyticsEvent.RegisterComplete(
            sessionId = "sess-001",
            editedRows = 1,
            manualRows = 0,
            replace = false,
            totalDurationMs = 5000L,
        )
        assertEquals(1, event.editedRows)
        assertEquals(false, event.replace)
        assertEquals(5000L, event.totalDurationMs)
    }

    @Test
    fun `RegisterAbandon holds last step`() {
        val event = AnalyticsEvent.RegisterAbandon("sess-001", lastStep = "confirm")
        assertEquals("confirm", event.lastStep)
    }

    @Test
    fun `AppOpenSource icon value is icon`() {
        assertEquals("icon", AppOpenSource.ICON.value)
    }

    @Test
    fun `SettingKey skip_confirm value matches`() {
        assertEquals("skip_confirm", SettingKey.SKIP_CONFIRM.value)
    }

    @Test
    fun `events with same data are equal`() {
        val a = AnalyticsEvent.AppOpen(AppOpenSource.WIDGET_4X1)
        val b = AnalyticsEvent.AppOpen(AppOpenSource.WIDGET_4X1)
        assertEquals(a, b)
    }

    @Test
    fun `events with different data are not equal`() {
        val a = AnalyticsEvent.AppOpen(AppOpenSource.ICON)
        val b = AnalyticsEvent.AppOpen(AppOpenSource.WIDGET_4X1)
        assertNotEquals(a, b)
    }

    @Test
    fun `AnalyticsTracker NoOp does not throw`() {
        val tracker = object : AnalyticsTracker {
            override fun track(event: AnalyticsEvent) = Unit
        }
        assertNotNull(tracker)
        tracker.track(AnalyticsEvent.AppOpen(AppOpenSource.ICON))
    }

    @Test
    fun `AnonymousIdProvider interface can be implemented`() {
        val provider = object : AnonymousIdProvider {
            override fun getAnonymousId() = "test-uuid"
        }
        assertEquals("test-uuid", provider.getAnonymousId())
    }
}
