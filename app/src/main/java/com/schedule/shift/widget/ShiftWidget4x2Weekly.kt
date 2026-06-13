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
                        .background(HeaderBackground)
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

@Suppress("LongMethod")
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
            modifier = GlanceModifier.fillMaxWidth().height(WAVE_HEIGHT_DP.dp)
                .background(HeaderBackground),
        ) {
            Image(
                provider = ImageProvider(R.drawable.widget_wave_divider),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
        }
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight().background(WidgetSurface),
            contentAlignment = Alignment.BottomCenter,
        ) {
            WeekGrid(
                today = today,
                allDays = allDays,
                timeFmt = timeFmt,
                modifier = GlanceModifier.fillMaxWidth()
                    .padding(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 44.dp),
            )
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
                    color = ColorProvider(Color.White.copy(alpha = DAY_LABEL_ALPHA)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = dateLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = ColorProvider(Color.White),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier.width(1.dp).height(HEADER_DIVIDER_HEIGHT_DP.dp)
                .background(DividerColor),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            TodayStateText(state = state, timeFmt = timeFmt)
        }
    }
}

@Composable
private fun TodayStateText(state: WidgetState, timeFmt: DateTimeFormatter) {
    val (text, color) = when (state) {
        is WidgetState.WorkDay ->
            "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}" to WorkTimeColor
        is WidgetState.OffDay ->
            state.codeLabel.ifEmpty { "휴무" } to ColorProvider(Color.White.copy(alpha = ALPHA_OFF_DAY))
        is WidgetState.Unregistered ->
            "-" to ColorProvider(Color.White.copy(alpha = ALPHA_UNREGISTERED))
    }
    Text(
        text = text,
        style = TextDefaults.defaultTextStyle.copy(
            color = color,
            fontSize = if (state is WidgetState.WorkDay) 25.sp else 14.sp,
            fontWeight = if (state is WidgetState.WorkDay) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
        ),
    )
}

@Composable
private fun WeekGrid(
    today: LocalDate,
    allDays: List<ScheduleDay>,
    timeFmt: DateTimeFormatter,
    modifier: GlanceModifier,
) {
    val dayFmt = DateTimeFormatter.ofPattern("E")
    Row(modifier = modifier) {
        for (offset in GRID_OFFSET_START..GRID_OFFSET_END) {
            val date = today.plusDays(offset.toLong())
            DayCell(
                modifier = GlanceModifier.defaultWeight(),
                date = date,
                dayData = allDays.find { it.date == date },
                isToday = offset == 0,
                timeFmt = timeFmt,
                dayFmt = dayFmt,
            )
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
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
    val dateDowText = "${date.dayOfMonth} (${date.format(dayFmt)})"

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isToday) {
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_today_badge))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dateDowText,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = ColorProvider(Color.White),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        } else {
            Box(
                modifier = GlanceModifier.padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dateDowText,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = GridDateColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        DayCellValue(state = state, isToday = isToday, timeFmt = timeFmt)
    }
}

@Composable
private fun DayCellValue(state: WidgetState, isToday: Boolean, timeFmt: DateTimeFormatter) {
    val textColor = if (isToday) WidgetPrimary else GridMutedColor
    val weight = if (isToday) FontWeight.Bold else FontWeight.Normal
    val cellStyle = TextDefaults.defaultTextStyle.copy(
        color = textColor,
        fontSize = 13.sp,
        fontWeight = weight,
        textAlign = TextAlign.Center,
    )
    when (state) {
        is WidgetState.WorkDay -> {
            Text(text = state.startTime.format(timeFmt), style = cellStyle)
            Text(text = state.endTime.format(timeFmt), style = cellStyle)
        }
        is WidgetState.OffDay -> Text(
            text = state.codeLabel.ifEmpty { "휴무" }.take(MAX_CODE_CHARS),
            style = cellStyle,
        )
        is WidgetState.Unregistered -> Text(text = "-", style = cellStyle)
    }
}

@Suppress("MagicNumber")
private val HeaderBackground = ColorProvider(Color(0xFF1E3932))
@Suppress("MagicNumber")
private val DividerColor = ColorProvider(Color(0x26FFFFFF))
@Suppress("MagicNumber")
private val WorkTimeColor = ColorProvider(Color(0xFFD4E9E2))
@Suppress("MagicNumber")
private val GridDateColor = ColorProvider(Color(0xFF1E3932))
@Suppress("MagicNumber")
private val GridMutedColor = ColorProvider(Color(0xFF9A9488))

private const val ALPHA_OFF_DAY = 0.7f
private const val ALPHA_UNREGISTERED = 0.5f
private const val DAY_LABEL_ALPHA = 0.5f
private const val MAX_CODE_CHARS = 4
private const val GRID_OFFSET_START = -2
private const val GRID_OFFSET_END = 2
private const val HEADER_LEFT_WIDTH_DP = 88
private const val HEADER_HEIGHT_DP = 62
private const val HEADER_DIVIDER_HEIGHT_DP = 32
private const val WAVE_HEIGHT_DP = 24

class ShiftWidget4x2WeeklyReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x2Weekly()
}
