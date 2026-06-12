package com.schedule.shift.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import androidx.glance.unit.ColorProvider
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.WidgetState
import com.schedule.shift.domain.model.toWidgetState
import com.schedule.shift.domain.repository.ScheduleRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ShiftWidget4x2Countdown : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_4X2_COUNTDOWN

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication<ShiftWidgetEntryPoint>(context.applicationContext)
            .scheduleRepository()
        val today = LocalDate.now()
        val now = LocalTime.now()
        val todayDay = repo.getWeekByDate(today)?.days?.find { it.date == today }
        val state = todayDay?.toWidgetState() ?: WidgetState.Unregistered
        val nextWorkDay = if (state is WidgetState.WorkDay && now.isAfter(state.endTime)) {
            findNextWorkDay(repo, today)
        } else {
            null
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.Center,
                ) {
                    Widget4x2CountdownContent(state, today, now, nextWorkDay)
                }
            }
        }
    }

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x2CountdownContent(state, today, LocalTime.now(), null)
    }

    private suspend fun findNextWorkDay(repo: ScheduleRepository, from: LocalDate): ScheduleDay? {
        val thisWeekDays = repo.getWeekByDate(from)?.days.orEmpty()
        val futureWeekDays = repo.getWeeksInRange(
            from.plusDays(1),
            from.plusDays(NEXT_WORK_SEARCH_DAYS),
        ).flatMap { it.days }
        return (thisWeekDays + futureWeekDays)
            .filter { it.date.isAfter(from) && it.type == DayType.WORK }
            .minByOrNull { it.date }
    }

    companion object {
        private const val NEXT_WORK_SEARCH_DAYS = 14L
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget4x2CountdownContent(
    state: WidgetState,
    today: LocalDate,
    now: LocalTime,
    nextWorkDay: ScheduleDay?,
) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = today.format(DateTimeFormatter.ofPattern("M/d (E)")),
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(GlanceModifier.defaultWeight())
        when (state) {
            is WidgetState.WorkDay -> WorkDaySection(state, now, nextWorkDay)
            is WidgetState.OffDay -> Text(
                text = state.codeLabel.ifEmpty { "휴무" },
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            is WidgetState.Unregistered -> Text(
                text = "스케줄 없음",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 16.sp,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
    }
}

@Suppress("LongMethod")
@Composable
private fun WorkDaySection(state: WidgetState.WorkDay, now: LocalTime, nextWorkDay: ScheduleDay?) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    Text(
        text = "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}",
        style = TextDefaults.defaultTextStyle.copy(
            color = WidgetOnSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        ),
        modifier = GlanceModifier.fillMaxWidth(),
    )
    Spacer(GlanceModifier.height(8.dp))
    val totalSeconds = ChronoUnit.SECONDS.between(state.startTime, state.endTime).coerceAtLeast(1)
    when {
        now.isBefore(state.startTime) -> {
            Text(
                text = "근무까지 ${formatDuration(ChronoUnit.SECONDS.between(now, state.startTime))}",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        now.isAfter(state.endTime) -> {
            val text = if (nextWorkDay != null) {
                val d = nextWorkDay.date.format(DateTimeFormatter.ofPattern("M/d(E)"))
                val start = nextWorkDay.startTime?.format(timeFmt) ?: ""
                val end = nextWorkDay.endTime?.format(timeFmt) ?: ""
                val timeRange = if (start.isNotEmpty() && end.isNotEmpty()) "$start-$end" else start
                "다음 근무 $d $timeRange"
            } else {
                "오늘 근무 종료"
            }
            Text(
                text = text,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
        }
        else -> {
            val elapsed = ChronoUnit.SECONDS.between(state.startTime, now)
            val fraction = (elapsed.toFloat() / totalSeconds).coerceIn(0f, 1f)
            Text(
                text = "퇴근까지 ${formatDuration(ChronoUnit.SECONDS.between(now, state.endTime))}",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = fraction,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = WidgetPrimary,
                backgroundColor = ColorProvider(ProgressBackgroundColor),
            )
        }
    }
}

@Suppress("MagicNumber")
private val ProgressBackgroundColor = Color(0xFFE0E0E0)

internal fun formatDuration(totalSeconds: Long): String {
    val h = totalSeconds / SECONDS_PER_HOUR
    val m = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val s = totalSeconds % SECONDS_PER_MINUTE
    return when {
        h > 0 -> "${h}시간 ${m}분 ${s}초"
        m > 0 -> "${m}분 ${s}초"
        else -> "${s}초"
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L

class ShiftWidget4x2CountdownReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x2Countdown()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleSecondUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)
            .cancel(secondUpdatePendingIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SECOND_UPDATE) {
            MainScope().launch { glanceAppWidget.updateAll(context) }
            scheduleSecondUpdate(context)
        }
    }

    private fun scheduleSecondUpdate(context: Context) {
        context.getSystemService(AlarmManager::class.java).set(
            AlarmManager.RTC,
            System.currentTimeMillis() + SECOND_MS,
            secondUpdatePendingIntent(context),
        )
    }

    private fun secondUpdatePendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SECOND,
            Intent(context, ShiftWidget4x2CountdownReceiver::class.java)
                .apply { action = ACTION_SECOND_UPDATE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_SECOND_UPDATE = "com.schedule.shift.widget.ACTION_SECOND_UPDATE"
        private const val REQUEST_CODE_SECOND = 1001
        private const val SECOND_MS = 1_000L
    }
}
