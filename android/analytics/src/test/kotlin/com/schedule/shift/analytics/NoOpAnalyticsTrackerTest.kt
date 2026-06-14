package com.schedule.shift.analytics

import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AppOpenSource
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoOpAnalyticsTrackerTest {

    private val tracker = NoOpAnalyticsTracker()

    @Test
    fun `tracker is not null`() {
        assertNotNull(tracker)
    }

    @Test
    fun `track does not throw for AppOpen`() {
        tracker.track(AnalyticsEvent.AppOpen(AppOpenSource.ICON))
    }

    @Test
    fun `track does not throw for WidgetActive`() {
        tracker.track(AnalyticsEvent.WidgetActive(listOf("widget_2x1")))
    }

    @Test
    fun `track does not throw for HomeWeekViewed`() {
        tracker.track(AnalyticsEvent.HomeWeekViewed(offset = 0))
    }

    @Test
    fun `track does not throw for RegisterStart`() {
        tracker.track(AnalyticsEvent.RegisterStart("sess-001"))
    }

    @Test
    fun `track does not throw for RegisterComplete`() {
        tracker.track(
            AnalyticsEvent.RegisterComplete(
                sessionId = "sess-001",
                editedRows = 0,
                manualRows = 0,
                replace = false,
                totalDurationMs = 1000L,
            ),
        )
    }
}
