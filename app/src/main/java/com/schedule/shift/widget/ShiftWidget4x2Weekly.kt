package com.schedule.shift.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
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
import androidx.glance.unit.ColorProvider
import com.schedule.shift.R
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
        val from = today.minusDays(GRID_RADIUS.toLong())
        val to = today.plusDays(GRID_RADIUS.toLong())
        val allDays = repo.getWeeksInRange(from, to).flatMap { it.days }
        val todayWeek = repo.getWeekByDate(today)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetPrimary)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.TopStart,
                ) {
                    Widget4x2WeeklyContent(today = today, allDays = allDays, todayWeek = todayWeek)
                }
            }
        }
    }

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x2WeeklyContent(today = today, allDays = emptyList(), todayWeek = null)
    }

    companion object {
        private const val GRID_RADIUS = 2
    }
}

@Composable
internal fun Widget4x2WeeklyContent(
    today: LocalDate,
    allDays: List<ScheduleDay>,
    todayWeek: ScheduleWeek?,
) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEE"))
    val dateLabel = today.dayOfMonth.toString()
    val todayDay = allDays.find { it.date == today }
        ?: todayWeek?.days?.find { it.date == today }
    val todayState = todayDay?.toWidgetState() ?: WidgetState.Unregistered

    Column(modifier = GlanceModifier.fillMaxSize()) {
        TodayHeader(
            dayLabel = dayLabel,
            dateLabel = dateLabel,
            state = todayState,
            timeFmt = timeFmt,
            modifier = GlanceModifier.fillMaxWidth().height(HEADER_HEIGHT_DP.dp),
        )
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.TopStart,
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_weekly_grid_bg),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                WeekGrid(today = today, allDays = allDays, timeFmt = timeFmt)
            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@Composable
private fun TodayHeader(
    dayLabel: String,
    dateLabel: String,
    state: WidgetState,
    timeFmt: DateTimeFormatter,
    modifier: GlanceModifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.width(HEADER_LEFT_WIDTH_DP.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = dateLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = ColorProvider(Color.White),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier.width(1.dp).height(HEADER_DIVIDER_HEIGHT_DP.dp)
                .background(ColorProvider(HeaderDividerColor)),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TodayStateText(state = state, timeFmt = timeFmt)
        }
    }
}

@Composable
private fun TodayStateText(state: WidgetState, timeFmt: DateTimeFormatter) {
    val (text, alpha) = when (state) {
        is WidgetState.WorkDay ->
            "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}" to 1f
        is WidgetState.OffDay ->
            state.codeLabel.ifEmpty { "휴무" } to ALPHA_OFF_DAY
        is WidgetState.Unregistered ->
            "-" to ALPHA_UNREGISTERED
    }
    Text(
        text = text,
        style = TextDefaults.defaultTextStyle.copy(
            color = ColorProvider(Color.White.copy(alpha = alpha)),
            fontSize = if (state is WidgetState.WorkDay) 20.sp else 14.sp,
            fontWeight = if (state is WidgetState.WorkDay) FontWeight.Bold else FontWeight.Normal,
        ),
    )
}

@Composable
private fun WeekGrid(
    today: LocalDate,
    allDays: List<ScheduleDay>,
    timeFmt: DateTimeFormatter,
) {
    val dayFmt = DateTimeFormatter.ofPattern("E")
    Row(modifier = GlanceModifier.fillMaxWidth().fillMaxHeight()) {
        for (offset in GRID_OFFSET_START..GRID_OFFSET_END) {
            val date = today.plusDays(offset.toLong())
            DayCell(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                date = date,
                dayData = allDays.find { it.date == date },
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
        if (isToday) {
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_today_circle))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = date.format(dayFmt),
                    style = TextDefaults.defaultTextStyle.copy(
                        color = ColorProvider(Color.White),
                        fontSize = 10.sp,
                        fontWeight = weight,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        } else {
            Text(
                text = date.format(dayFmt),
                style = TextDefaults.defaultTextStyle.copy(
                    color = accent,
                    fontSize = 10.sp,
                    fontWeight = weight,
                    textAlign = TextAlign.Center,
                ),
            )
        }
        Text(
            text = date.dayOfMonth.toString(),
            style = TextDefaults.defaultTextStyle.copy(
                color = if (isToday) WidgetPrimary else WidgetOnSurface,
                fontSize = 13.sp,
                fontWeight = weight,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(2.dp))
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
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            ),
        )
        is WidgetState.Unregistered -> Text(
            text = "-",
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 12.sp,
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
        fontSize = 12.sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
    )
    Text(text = "${state.startTime.format(timeFmt)}-", style = cellStyle)
    Text(text = state.endTime.format(timeFmt), style = cellStyle)
}

@Suppress("MagicNumber")
private val HeaderDividerColor = Color(0x55FFFFFF)
private const val ALPHA_OFF_DAY = 0.7f
private const val ALPHA_UNREGISTERED = 0.5f
private const val MAX_CODE_CHARS = 4
private const val GRID_OFFSET_START = -2
private const val GRID_OFFSET_END = 2
private const val HEADER_LEFT_WIDTH_DP = 72
private const val HEADER_HEIGHT_DP = 64
private const val HEADER_DIVIDER_HEIGHT_DP = 30

class ShiftWidget4x2WeeklyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x2Weekly()
}
