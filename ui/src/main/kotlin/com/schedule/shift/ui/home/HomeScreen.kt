package com.schedule.shift.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddSchedule: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        uiState = uiState,
        onAddSchedule = onAddSchedule,
        onRefresh = viewModel::refresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onAddSchedule: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("스케쥴") })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (uiState) {
                is HomeUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                )
                is HomeUiState.Success -> HomeSuccessContent(
                    currentWeek = uiState.currentWeek,
                    today = uiState.today,
                    onAddSchedule = onAddSchedule,
                )
                is HomeUiState.Error -> ErrorContent(onRefresh = onRefresh)
            }
        }
    }
}

@Composable
private fun HomeSuccessContent(
    currentWeek: ScheduleWeek?,
    today: LocalDate,
    onAddSchedule: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        if (currentWeek == null) {
            EmptyState(onAddSchedule = onAddSchedule)
        } else {
            WeekScheduleCard(week = currentWeek, today = today)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddSchedule,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("스케쥴 업데이트")
            }
        }
    }
}

@Composable
private fun EmptyState(onAddSchedule: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "등록된 스케쥴이 없습니다",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddSchedule) {
            Text("스케쥴 추가")
        }
    }
}

@Composable
private fun WeekScheduleCard(week: ScheduleWeek, today: LocalDate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "이번 주 스케쥴",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(week.days) { day ->
                    DayRow(day = day, isToday = day.date == today)
                }
            }
        }
    }
}

@Composable
private fun DayRow(day: ScheduleDay, isToday: Boolean) {
    val dateFormatter = DateTimeFormatter.ofPattern("M/d(E)")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = day.date.format(dateFormatter),
            style = if (isToday) MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
            ) else MaterialTheme.typography.bodyMedium,
        )
        when (day.type) {
            DayType.WORK -> Text(
                text = "${day.startTime}~${day.endTime}  ${day.codeLabel}",
                style = MaterialTheme.typography.bodySmall,
            )
            DayType.OFF, DayType.OTHER -> Text(
                text = day.codeLabel,
                style = MaterialTheme.typography.bodySmall,
            )
            DayType.UNREGISTERED -> Text(
                text = "-",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ErrorContent(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "스케쥴을 불러오는 중 오류가 발생했습니다")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRefresh) { Text("다시 시도") }
    }
}
