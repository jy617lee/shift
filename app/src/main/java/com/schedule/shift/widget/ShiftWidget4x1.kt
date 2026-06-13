package com.schedule.shift.widget

import android.app.AlarmManager
import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ShiftWidget4x1 : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_4X1

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        ShiftWidget4x1Receiver.scheduleSecondUpdate(context)

        val repo = EntryPointAccessors
            .fromApplication<ShiftWidgetEntryPoint>(context.applicationContext)
            .scheduleRepository()
        val today = LocalDate.now()
        val now = LocalTime.now()
        val state = repo.getWeekByDate(today)?.days?.find { it.date == today }
            ?.toWidgetState() ?: WidgetState.Unregistered

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetSurface)
                        .clickable(actionStartActivity(widgetIntent(context, widgetSource))),
                    contentAlignment = Alignment.Center,
                ) {
                    Widget4x1Content(state = state, today = today, now = now)
                }
            }
        }
    }

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x1Content(state = state, today = today, now = LocalTime.now())
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget4x1Content(state: WidgetState, today: LocalDate, now: LocalTime) {
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
                is WidgetState.WorkDay -> WorkDayCountdown(state = state, now = now)
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

@Composable
private fun WorkDayCountdown(state: WidgetState.WorkDay, now: LocalTime) {
    val timeFmt = DateTimeFormatter.ofPattern("H:mm")
    val workTimeText = "${state.startTime.format(timeFmt)}-${state.endTime.format(timeFmt)}"
    val countdownText = when {
        now.isBefore(state.startTime) ->
            "근무까지 ${formatDuration(ChronoUnit.SECONDS.between(now, state.startTime))}"
        now.isAfter(state.endTime) ->
            "근무 종료"
        else ->
            "퇴근까지 ${formatDuration(ChronoUnit.SECONDS.between(now, state.endTime))}"
    }
    val countdownColor = if (now.isAfter(state.endTime)) WidgetOnSurfaceVariant else WidgetPrimary

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
        Text(
            text = countdownText,
            style = TextDefaults.defaultTextStyle.copy(
                color = countdownColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
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
        scheduleSecondUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java)
            .cancel(secondUpdatePendingIntent(context))
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleSecondUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SECOND_UPDATE_4X1 -> {
                val pendingResult = goAsync()
                MainScope().launch {
                    try {
                        glanceAppWidget.updateAll(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
                scheduleSecondUpdate(context)
            }
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                scheduleSecondUpdate(context)
            }
        }
    }

    companion object {
        const val ACTION_SECOND_UPDATE_4X1 = "com.schedule.shift.widget.ACTION_SECOND_UPDATE_4X1"
        private const val REQUEST_CODE_SECOND = 1002
        private const val SECOND_MS = 1_000L

        fun scheduleSecondUpdate(context: Context) {
            val am = context.getSystemService(AlarmManager::class.java)
            val pi = secondUpdatePendingIntent(context)
            val triggerAt = System.currentTimeMillis() + SECOND_MS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.set(AlarmManager.RTC, triggerAt, pi)
            } else {
                am.setExact(AlarmManager.RTC, triggerAt, pi)
            }
        }

        private fun secondUpdatePendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SECOND,
                Intent(context, ShiftWidget4x1Receiver::class.java)
                    .apply { action = ACTION_SECOND_UPDATE_4X1 },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
