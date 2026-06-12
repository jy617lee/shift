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

@Suppress("LongMethod")
@Composable
private fun Widget2x1Content(state: WidgetState, today: LocalDate) {
    val dayLabel = today.format(DateTimeFormatter.ofPattern("EE"))
    val dateLabel = today.format(DateTimeFormatter.ofPattern("d"))

    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.wrapContentWidth().fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = dateLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayLabel,
                style = TextDefaults.defaultTextStyle.copy(
                    color = WidgetOnSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            when (state) {
                is WidgetState.WorkDay -> {
                    val start = state.startTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    val end = state.endTime.format(DateTimeFormatter.ofPattern("H:mm"))
                    Text(
                        text = "$start-$end",
                        style = TextDefaults.defaultTextStyle.copy(
                            color = WidgetPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                is WidgetState.OffDay -> Text(
                    text = state.codeLabel,
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 13.sp,
                    ),
                )
                is WidgetState.Unregistered -> Text(
                    text = "미등록",
                    style = TextDefaults.defaultTextStyle.copy(
                        color = WidgetOnSurfaceVariant,
                        fontSize = 13.sp,
                    ),
                )
            }
        }
    }
}

class ShiftWidget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ShiftWidget2x1()
}
