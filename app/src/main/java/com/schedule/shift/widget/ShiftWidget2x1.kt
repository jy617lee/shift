package com.schedule.shift.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import com.schedule.shift.domain.model.WidgetState
import com.schedule.shift.domain.model.toWidgetState
import com.schedule.shift.domain.repository.ScheduleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShiftWidget2x1 : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun scheduleRepository(): ScheduleRepository
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication<WidgetEntryPoint>(context.applicationContext)
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
                        .clickable(actionStartActivity(widgetIntent(context, SOURCE_WIDGET_2X1))),
                    contentAlignment = Alignment.Center,
                ) {
                    Widget2x1Content(state = state, today = today)
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget2x1Content(state: WidgetState, today: LocalDate) {
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EE"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d"))

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            when (state) {
                is WidgetState.WorkDay -> {
                    val start = state.startTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    val end = state.endTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    Text(
                        text = "$start–$end",
                        style = TextDefaults.defaultTextStyle.copy(
                            color = WidgetPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                is WidgetState.OffDay -> Text(
                    text = state.codeLabel,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                )
                is WidgetState.Unregistered -> Text(
                    text = "미등록",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 11.sp,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier.wrapContentWidth().fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = dateLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

class ShiftWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget2x1()
}
