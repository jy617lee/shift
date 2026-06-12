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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDefaults
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.WidgetState
import com.schedule.shift.domain.model.toWidgetState
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShiftWidget4x2Weekly : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_4X2_WEEKLY

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication<ShiftWidgetEntryPoint>(context.applicationContext)
            .scheduleRepository()
        val today = LocalDate.now()
        val week = repo.getWeekByDate(today)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.Center,
                ) {
                    Widget4x2WeeklyContent(today = today, week = week)
                }
            }
        }
    }

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x2WeeklyContent(today = today, week = null)
    }
}

@Suppress("LongMethod")
@Composable
internal fun Widget4x2WeeklyContent(today: LocalDate, week: ScheduleWeek?) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val dayFmt = DateTimeFormatter.ofPattern("E")

    val todayDay = week?.days?.find { it.date == today }
    val todayState = todayDay?.toWidgetState() ?: WidgetState.Unregistered

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        TodayHeader(today = today, state = todayState, timeFmt = timeFmt, dayFmt = dayFmt)
        Spacer(GlanceModifier.height(8.dp))
        WeekGrid(today = today, week = week, timeFmt = timeFmt, dayFmt = dayFmt)
    }
}

@Composable
private fun TodayHeader(
    today: LocalDate,
    state: WidgetState,
    timeFmt: DateTimeFormatter,
    dayFmt: DateTimeFormatter,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = today.dayOfMonth.toString(),
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = today.format(dayFmt),
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 11.sp,
                ),
            )
            when (state) {
                is WidgetState.WorkDay -> Text(
                    text = "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                is WidgetState.OffDay -> Text(
                    text = state.codeLabel.ifEmpty { "휴무" },
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
                is WidgetState.Unregistered -> Text(
                    text = "-",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 14.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun WeekGrid(
    today: LocalDate,
    week: ScheduleWeek?,
    timeFmt: DateTimeFormatter,
    dayFmt: DateTimeFormatter,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
    ) {
        (-2..2).forEach { offset ->
            val date = today.plusDays(offset.toLong())
            val dayData = week?.days?.find { it.date == date }
            val isToday = offset == 0
            DayCell(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                date = date,
                dayData = dayData,
                isToday = isToday,
                timeFmt = timeFmt,
                dayFmt = dayFmt,
            )
        }
    }
}

@Composable
private fun DayCell(
    modifier: GlanceModifier,
    date: LocalDate,
    dayData: ScheduleDay?,
    isToday: Boolean,
    timeFmt: DateTimeFormatter,
    dayFmt: DateTimeFormatter,
) {
    val state = dayData?.toWidgetState() ?: WidgetState.Unregistered
    val labelColor = if (isToday) WidgetPrimary else WidgetOnSurfaceVariant
    val valueColor = if (isToday) WidgetPrimary else WidgetOnSurface

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(dayFmt),
            style = TextDefaults.defaultTextStyle.copy(
                color = labelColor,
                fontSize = 10.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        when (state) {
            is WidgetState.WorkDay -> {
                val s = state.startTime.format(timeFmt)
                val e = state.endTime.format(timeFmt)
                Text(
                    text = "$s-",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = valueColor,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    text = e,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = valueColor,
                        fontSize = 9.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            is WidgetState.OffDay -> Text(
                text = state.codeLabel.ifEmpty { "휴무" }.take(MAX_CODE_CHARS),
                style = TextDefaults.defaultTextStyle.copy(
                    color = if (isToday) WidgetPrimary else WidgetOnSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
            )
            is WidgetState.Unregistered -> Text(
                text = "-",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}

private const val MAX_CODE_CHARS = 4

class ShiftWidget4x2WeeklyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x2Weekly()
}
