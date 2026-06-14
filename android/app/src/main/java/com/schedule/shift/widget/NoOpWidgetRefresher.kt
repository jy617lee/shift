package com.schedule.shift.widget

import com.schedule.shift.domain.widget.WidgetRefresher

class NoOpWidgetRefresher : WidgetRefresher {
    override suspend fun refreshAll() = Unit
}
