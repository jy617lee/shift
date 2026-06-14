package com.schedule.shift.domain.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
