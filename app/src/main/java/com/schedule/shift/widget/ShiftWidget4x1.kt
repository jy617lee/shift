package com.schedule.shift.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import com.schedule.shift.R
import com.schedule.shift.domain.model.WidgetState
import com.schedule.shift.domain.model.toWidgetState
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ShiftWidget4x1 : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_4X1

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = EntryPointAccessors
            .fromApplication<ShiftWidgetEntryPoint>(context.applicationContext)
            .scheduleRepository()
        val today = LocalDate.now()
        val now = LocalTime.now()
        val elapsedNow = SystemClock.elapsedRealtime()
        val state = repo.getWeekByDate(today)?.days?.find { it.date == today }
            ?.toWidgetState() ?: WidgetState.Unregistered

        ShiftWidget4x1Receiver.scheduleNextStateAlarm(context, state, now, today)

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.Center,
                ) {
                    Widget4x1Content(
                        state = state,
                        today = today,
                        now = now,
                        elapsedNow = elapsedNow,
                    )
                }
            }
        }
    }

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x1Content(
            state = state,
            today = today,
            now = LocalTime.now(),
            elapsedNow = SystemClock.elapsedRealtime(),
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget4x1Content(
    state: WidgetState,
    today: LocalDate,
    now: LocalTime,
    elapsedNow: Long,
) {
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEE"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d"))
    val context = LocalContext.current

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.width(LEFT_COL_WIDTH_DP.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dayLabel,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                Text(
                    text = dateLabel,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.width(10.dp))
        Box(
            modifier = GlanceModifier.fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier.width(1.dp).height(DIVIDER_HEIGHT_DP.dp)
                    .background(WidgetDivider),
            ) {}
        }
        Spacer(GlanceModifier.width(DIVIDER_RIGHT_MARGIN_DP.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            when (state) {
                is WidgetState.WorkDay ->
                    WorkDayContent(state = state, now = now, elapsedNow = elapsedNow, context = context)
                is WidgetState.OffDay -> Text(
                    text = state.codeLabel.ifEmpty { "휴무" },
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                is WidgetState.Unregistered -> Text(
                    text = "스케쥴 없음",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 21.sp,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.width(ADD_BUTTON_MARGIN_DP.dp))
        Box(
            modifier = GlanceModifier.width(ADD_BUTTON_SIZE_DP.dp).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "스케쥴 추가",
                modifier = GlanceModifier
                    .width(ADD_ICON_SIZE_DP.dp)
                    .height(ADD_ICON_SIZE_DP.dp)
                    .clickable(actionStartActivity(addScheduleIntent(context))),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun WorkDayContent(
    state: WidgetState.WorkDay,
    now: LocalTime,
    elapsedNow: Long,
    context: Context,
) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val workTimeText = "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}"

    Column(
        modifier = GlanceModifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = workTimeText,
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurface,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        when {
            now.isBefore(state.startTime) -> {
                val remainingMs = ChronoUnit.MILLIS.between(now, state.startTime)
                ChronometerCountdown(
                    context = context,
                    base = elapsedNow + remainingMs,
                    format = "근무까지 %s",
                )
            }
            now.isAfter(state.endTime) -> Text(
                text = "근무 종료",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 15.sp,
                ),
            )
            else -> {
                val remainingMs = ChronoUnit.MILLIS.between(now, state.endTime)
                ChronometerCountdown(
                    context = context,
                    base = elapsedNow + remainingMs,
                    format = "퇴근까지 %s",
                )
            }
        }
    }
}

@Composable
private fun ChronometerCountdown(context: Context, base: Long, format: String) {
    AndroidRemoteViews(
        remoteViews = RemoteViews(context.packageName, R.layout.widget_4x1_countdown).apply {
            setChronometer(R.id.countdown_text, base, format, true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setChronometerCountDown(R.id.countdown_text, true)
            }
        },
    )
}

private const val LEFT_COL_WIDTH_DP = 52
private const val DIVIDER_HEIGHT_DP = 42
private const val DIVIDER_RIGHT_MARGIN_DP = 18
private const val ADD_BUTTON_MARGIN_DP = 8
private const val ADD_BUTTON_SIZE_DP = 32
private const val ADD_ICON_SIZE_DP = 20

class ShiftWidget4x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x1()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextStateAlarm(context, null, LocalTime.now(), LocalDate.now())
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)
            .cancel(stateUpdatePendingIntent(context))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleNextStateAlarm(context, null, LocalTime.now(), LocalDate.now())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_STATE_UPDATE_4X1 -> {
                val pendingResult = goAsync()
                MainScope().launch {
                    try { glanceAppWidget.updateAll(context) }
                    finally { pendingResult.finish() }
                }
            }
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                val pendingResult = goAsync()
                MainScope().launch {
                    try { glanceAppWidget.updateAll(context) }
                    finally { pendingResult.finish() }
                }
            }
        }
    }

    companion object {
        const val ACTION_STATE_UPDATE_4X1 = "com.schedule.shift.widget.ACTION_STATE_UPDATE_4X1"
        private const val REQUEST_CODE_STATE = 1002
        private const val MINUTES_PAST_MIDNIGHT = 1L

        @Suppress("LongParameterList")
        fun scheduleNextStateAlarm(
            context: Context,
            state: WidgetState?,
            now: LocalTime,
            today: LocalDate,
        ) {
            val am = context.getSystemService(AlarmManager::class.java)
            val pi = stateUpdatePendingIntent(context)

            val nextEvent: LocalDateTime = when (state) {
                is WidgetState.WorkDay -> when {
                    now.isBefore(state.startTime) -> LocalDateTime.of(today, state.startTime)
                    now.isBefore(state.endTime) -> LocalDateTime.of(today, state.endTime)
                    else -> midnight(today)
                }
                else -> midnight(today)
            }

            val triggerMs = nextEvent.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pi)
            }
        }

        private fun midnight(today: LocalDate): LocalDateTime =
            LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT.plusMinutes(MINUTES_PAST_MIDNIGHT))

        private fun stateUpdatePendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_STATE,
                Intent(context, ShiftWidget4x1Receiver::class.java)
                    .apply { action = ACTION_STATE_UPDATE_4X1 },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
