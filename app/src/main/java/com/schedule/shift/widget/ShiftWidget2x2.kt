package com.schedule.shift.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults
import com.schedule.shift.domain.model.WidgetState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ShiftWidget2x2 : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_2X2

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget2x2Content(state = state, today = today)
    }
}

@Suppress("LongMethod")
@Composable
private fun Widget2x2Content(state: WidgetState, today: LocalDate) {
    val dateHeader = today.format(DateTimeFormatter.ofPattern("M/d E"))

    Column(
        modifier = GlanceModifier.fillMaxSize().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dateHeader,
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = GlanceModifier.fillMaxWidth(),
        )
        Spacer(GlanceModifier.defaultWeight())
        when (state) {
            is WidgetState.WorkDay -> {
                val start = state.startTime.format(DateTimeFormatter.ofPattern("H:mm"))
                val end = state.endTime.format(DateTimeFormatter.ofPattern("H:mm"))
                Text(
                    text = start,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
                Text(
                    text = "– $end",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            }
            is WidgetState.OffDay -> Text(
                text = state.codeLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = GlanceModifier.fillMaxWidth(),
            )
            is WidgetState.Unregistered -> Text(
                text = "스케쥴 없음",
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

class ShiftWidget2x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget2x2()
}
