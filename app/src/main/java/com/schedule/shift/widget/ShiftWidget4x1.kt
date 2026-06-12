package com.schedule.shift.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShiftWidget4x1 : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_4X1

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget4x1Content(state = state, today = today)
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget4x1Content(state: WidgetState, today: LocalDate) {
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEE"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d"))

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.wrapContentWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = dayLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 11.sp,
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
        Spacer(GlanceModifier.width(16.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            when (state) {
                is WidgetState.WorkDay -> {
                    val start = state.startTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    val end = state.endTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    Text(
                        text = "$start-$end",
                        style = TextDefaults.defaultTextStyle.copy(
                            color = WidgetPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                is WidgetState.OffDay -> Text(
                    text = state.codeLabel,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                is WidgetState.Unregistered -> Text(
                    text = "스케쥴 없음",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 16.sp,
                    ),
                )
            }
        }
    }
}

class ShiftWidget4x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget4x1()
}
