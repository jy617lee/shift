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
import androidx.glance.layout.width
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

@Composable
internal fun Widget4x2WeeklyContent(today: LocalDate, week: ScheduleWeek?) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val todayDay = week?.days?.find { it.date == today }
    val todayState = todayDay?.toWidgetState() ?: WidgetState.Unregistered

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        TodayHeader(today = today, state = todayState, timeFmt = timeFmt)
        Spacer(GlanceModifier.height(4.dp))
        WeekGrid(today = today, week = week, timeFmt = timeFmt)
    }
}

@Composable
private fun TodayHeader(
    today: LocalDate,
    state: WidgetState,
    timeFmt: DateTimeFormatter,
) {
    val dateDayLabel = today.format(DateTimeFormatter.ofPattern("M/d(E)"))

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.width(HEADER_DATE_WIDTH_DP.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = dateDayLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(modifier = GlanceModifier.defaultWeight())
        TodayStateText(state = state, timeFmt = timeFmt)
    }
}

@Composable
private fun TodayStateText(state: WidgetState, timeFmt: DateTimeFormatter) {
    val (text, color) = when (state) {
        is WidgetState.WorkDay ->
            "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}" to WidgetOnSurface
        is WidgetState.OffDay ->
            state.codeLabel.ifEmpty { "휴무" } to WidgetOnSurfaceVariant
        is WidgetState.Unregistered ->
            "-" to WidgetOnSurfaceVariant
    }
    Text(
        text = text,
        style = TextDefaults.defaultTextStyle.copy(
            color = color,
            fontSize = 16.sp,
            fontWeight = if (state is WidgetState.WorkDay) FontWeight.Medium else FontWeight.Normal,
        ),
    )
}

@Composable
private fun WeekGrid(
    today: LocalDate,
    week: ScheduleWeek?,
    timeFmt: DateTimeFormatter,
) {
    val dayFmt = DateTimeFormatter.ofPattern("E")
    Row(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
        for (offset in GRID_OFFSET_START..GRID_OFFSET_END) {
            val date = today.plusDays(offset.toLong())
            DayCell(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                date = date,
                dayData = week?.days?.find { it.date == date },
                isToday = offset == 0,
                timeFmt = timeFmt,
                dayFmt = dayFmt,
            )
        }
    }
}

@Suppress("LongMethod")
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
    val accent = if (isToday) WidgetPrimary else WidgetOnSurfaceVariant
    val weight = if (isToday) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = date.format(dayFmt),
            style = TextDefaults.defaultTextStyle.copy(
                color = accent,
                fontSize = 10.sp,
                fontWeight = weight,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        DayCellValue(state = state, isToday = isToday, timeFmt = timeFmt, weight = weight)
    }
}

@Composable
private fun DayCellValue(
    state: WidgetState,
    isToday: Boolean,
    timeFmt: DateTimeFormatter,
    weight: FontWeight,
) {
    val valueColor = if (isToday) WidgetPrimary else WidgetOnSurface
    when (state) {
        is WidgetState.WorkDay -> WorkTimeLines(state = state, timeFmt = timeFmt, color = valueColor, weight = weight)
        is WidgetState.OffDay -> Text(
            text = state.codeLabel.ifEmpty { "휴무" }.take(MAX_CODE_CHARS),
            style = TextDefaults.defaultTextStyle.copy(
                color = if (isToday) WidgetPrimary else WidgetOnSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
        is WidgetState.Unregistered -> Text(
            text = "-",
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun WorkTimeLines(
    state: WidgetState.WorkDay,
    timeFmt: DateTimeFormatter,
    color: androidx.glance.unit.ColorProvider,
    weight: FontWeight,
) {
    val cellStyle = TextDefaults.defaultTextStyle.copy(
        color = color,
        fontSize = 11.sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
    )
    Text(text = "${state.startTime.format(timeFmt)}-", style = cellStyle)
    Text(text = state.endTime.format(timeFmt), style = cellStyle)
}

private const val MAX_CODE_CHARS = 4
private const val GRID_OFFSET_START = -2
private const val GRID_OFFSET_END = 2
private const val HEADER_DATE_WIDTH_DP = 100

class ShiftWidget4x2WeeklyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x2Weekly()
}
