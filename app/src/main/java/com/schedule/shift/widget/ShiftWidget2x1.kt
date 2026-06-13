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

class ShiftWidget2x1 : BaseShiftWidget() {
    override val widgetSource = SOURCE_WIDGET_2X1

    @Composable
    override fun WidgetBody(state: WidgetState, today: LocalDate) {
        Widget2x1Content(state = state, today = today)
    }
}

@Composable
private fun Widget2x1Content(state: WidgetState, today: LocalDate) {
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EEE"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d"))

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DateColumn(dayLabel = dayLabel, dateLabel = dateLabel)
        Spacer(GlanceModifier.width(10.dp))
        Box(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Widget2x1StateText(state = state)
        }
    }
}

@Composable
private fun DateColumn(dayLabel: String, dateLabel: String) {
    Column(
        modifier = GlanceModifier.wrapContentWidth().fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = dayLabel,
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = dateLabel,
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun Widget2x1StateText(state: WidgetState) {
    when (state) {
        is WidgetState.WorkDay -> {
            val start = state.startTime.format(DateTimeFormatter.ofPattern("H:mm"))
            val end = state.endTime.format(DateTimeFormatter.ofPattern("H:mm"))
            Text(
                text = "$start-$end",
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        is WidgetState.OffDay -> Text(
            text = state.codeLabel.ifEmpty { "휴무" },
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
        )
        is WidgetState.Unregistered -> Text(
            text = "미등록",
            style = TextDefaults.defaultTextStyle.copy(
                color = WidgetOnSurfaceVariant,
                fontSize = 17.sp,
            ),
        )
    }
}

class ShiftWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget2x1()
}
