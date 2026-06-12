@file:Suppress("MaxLineLength")

package com.schedule.shift.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val COLOR_SUNDAY = Color(0xFFEF4444)
private val COLOR_SATURDAY = Color(0xFF3B82F6)
private const val TODAY_BAR_ALPHA = 0.06f
private const val WEEK_HEADER_ALPHA = 0.6f
private const val TODAY_BAR_WIDTH = 3
private const val TODAY_BAR_HEIGHT = 48
private const val WEEK_LAST_DAY_OFFSET = 6L

private val DAY_LABEL = DateTimeFormatter.ofPattern("EE")
private val DATE_LABEL = DateTimeFormatter.ofPattern("d")
private val WEEK_RANGE_FMT = DateTimeFormatter.ofPattern("M월 d일")

@Composable
fun HomeScreen(
    onAddSchedule: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(uiState = uiState, onAddSchedule = onAddSchedule, onRefresh = viewModel::refresh)
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onAddSchedule: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState is HomeUiState.Success) ShiftFab(onClick = onAddSchedule)
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ShiftAppBar()
            Box(modifier = Modifier.fillMaxSize()) {
                when (uiState) {
                    is HomeUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    is HomeUiState.Success -> HomeSuccessContent(
                        currentWeek = uiState.currentWeek,
                        today = uiState.today,
                        onAddSchedule = onAddSchedule,
                    )
                    is HomeUiState.Error -> HomeErrorContent(onRefresh = onRefresh)
                }
            }
        }
    }
}

@Composable
private fun ShiftAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Shift", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun ShiftFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = "스케쥴 추가", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

@Composable
private fun HomeSuccessContent(currentWeek: ScheduleWeek?, today: LocalDate, onAddSchedule: () -> Unit) {
    if (currentWeek == null) {
        ShiftEmptyState(onAddSchedule = onAddSchedule)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
        ) {
            WeekCard(week = currentWeek, today = today)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun WeekCard(week: ScheduleWeek, today: LocalDate) {
    val start = week.weekStartDate
    val end = start.plusDays(WEEK_LAST_DAY_OFFSET)
    val header = buildWeekHeader(start, end)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = WEEK_HEADER_ALPHA))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "내 스케쥴",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
            )
            Text(text = header, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        week.days.forEachIndexed { index, day ->
            DayRow(day = day, isToday = day.date == today)
            if (index < week.days.size - 1) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

private fun buildWeekHeader(start: LocalDate, end: LocalDate): String =
    if (start.month == end.month) {
        "${start.year}년 ${start.format(WEEK_RANGE_FMT)}–${end.dayOfMonth}일"
    } else {
        "${start.year}년 ${start.format(WEEK_RANGE_FMT)} – ${end.format(WEEK_RANGE_FMT)}"
    }

@Suppress("LongMethod")
@Composable
private fun DayRow(day: ScheduleDay, isToday: Boolean) {
    val dayOfWeek = day.date.dayOfWeek
    val dayColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        dayOfWeek == DayOfWeek.SUNDAY -> COLOR_SUNDAY
        dayOfWeek == DayOfWeek.SATURDAY -> COLOR_SATURDAY
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = TODAY_BAR_ALPHA))
                else Modifier,
            ),
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .width(TODAY_BAR_WIDTH.dp)
                    .height(TODAY_BAR_HEIGHT.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        } else {
            Spacer(modifier = Modifier.width(TODAY_BAR_WIDTH.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DayDateColumn(day = day, isToday = isToday, dayColor = dayColor)
            DayShiftRow(day = day, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DayDateColumn(day: ScheduleDay, isToday: Boolean, dayColor: Color) {
    Column(modifier = Modifier.width(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = day.date.format(DAY_LABEL),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = dayColor,
            textAlign = TextAlign.Center,
        )
        Text(
            text = day.date.format(DATE_LABEL),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DayShiftRow(day: ScheduleDay, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (day.type) {
            DayType.WORK -> {
                ShiftTypeBadge(label = "근무", type = ShiftBadgeType.WORK)
                Text(text = "${day.startTime}–${day.endTime}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            }
            DayType.OFF -> ShiftTypeBadge(label = day.codeLabel.ifEmpty { "휴무" }, type = ShiftBadgeType.OFF)
            DayType.OTHER -> ShiftTypeBadge(label = day.codeLabel, type = ShiftBadgeType.OFF)
            DayType.UNREGISTERED -> Text(text = "—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

enum class ShiftBadgeType { WORK, OFF, VACATION }

@Composable
fun ShiftTypeBadge(label: String, type: ShiftBadgeType) {
    val (bg, fg) = when (type) {
        ShiftBadgeType.WORK -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        ShiftBadgeType.OFF -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        ShiftBadgeType.VACATION -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(text = label, fontSize = 11.sp, fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, color = fg, letterSpacing = 0.3.sp)
    }
}

@Suppress("LongMethod")
@Composable
private fun ShiftEmptyState(onAddSchedule: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "📅", fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "등록된 스케쥴이 없어요",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "아직 교대 근무 일정이 없습니다.\n스케쥴 추가 버튼을 눌러 첫 번째 일정을 등록해 보세요.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Default,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onAddSchedule,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("지금 추가하기", style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}

@Composable
private fun HomeErrorContent(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "스케쥴을 불러오는 중\n오류가 발생했습니다",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRefresh,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("다시 시도", color = Color.White)
        }
    }
}
