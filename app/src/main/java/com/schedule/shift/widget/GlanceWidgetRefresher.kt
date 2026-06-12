package com.schedule.shift.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.schedule.shift.domain.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class GlanceWidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetRefresher {

    override suspend fun refreshAll() {
        ShiftWidget2x1().updateAll(context)
        ShiftWidget2x2().updateAll(context)
        ShiftWidget4x1().updateAll(context)
    }
}
