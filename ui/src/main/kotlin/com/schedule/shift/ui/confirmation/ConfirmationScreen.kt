package com.schedule.shift.ui.confirmation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationScreen(
    viewModel: ConfirmationViewModel,
    onSaved: () -> Unit,
    onCancelled: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            is ConfirmationUiState.Saved -> onSaved()
            is ConfirmationUiState.Cancelled -> onCancelled()
            is ConfirmationUiState.Reviewing -> Unit
        }
    }

    val reviewing = uiState as? ConfirmationUiState.Reviewing ?: return

    Scaffold(
        topBar = { TopAppBar(title = { Text("스케쥴 확인") }) },
        bottomBar = { ConfirmationBottomBar(onCancel = viewModel::cancel, onConfirm = viewModel::confirm) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            items(reviewing.weeks) { week ->
                WeekCard(week = week)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ConfirmationBottomBar(onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("취소") }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("저장") }
    }
}

@Composable
private fun WeekCard(week: ScheduleWeek) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${week.weekStartDate} 주",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(4.dp))
            week.days.forEach { day -> DaySummaryRow(day = day) }
        }
    }
}

@Composable
private fun DaySummaryRow(day: ScheduleDay) {
    val formatter = DateTimeFormatter.ofPattern("M/d(E)")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = day.date.format(formatter),
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = when (day.type) {
                DayType.WORK -> "${day.startTime}~${day.endTime}  ${day.codeLabel}"
                DayType.OFF, DayType.OTHER -> day.codeLabel
                DayType.UNREGISTERED -> "-"
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
