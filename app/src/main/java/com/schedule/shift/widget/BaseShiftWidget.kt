package com.schedule.shift.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import com.schedule.shift.domain.model.WidgetState
import com.schedule.shift.domain.model.toWidgetState
import com.schedule.shift.domain.repository.ScheduleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate

abstract class BaseShiftWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ShiftWidgetEntryPoint {
        fun scheduleRepository(): ScheduleRepository
    }

    protected abstract val widgetSource: String

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication<ShiftWidgetEntryPoint>(context.applicationContext)
            .scheduleRepository()
        val today = LocalDate.now()
        val state = repo.getWeekByDate(today)
            ?.days?.find { it.date == today }
            ?.toWidgetState()
            ?: WidgetState.Unregistered

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.Center,
                ) {
                    WidgetBody(state = state, today = today)
                }
            }
        }
    }

    @Composable
    protected abstract fun WidgetBody(state: WidgetState, today: LocalDate)
}
