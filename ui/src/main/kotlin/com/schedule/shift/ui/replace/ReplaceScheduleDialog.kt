package com.schedule.shift.ui.replace

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.schedule.shift.domain.model.ScheduleWeek
import java.time.format.DateTimeFormatter

@Composable
fun ReplaceScheduleDialog(
    viewModel: ReplaceScheduleViewModel,
    onReplaced: () -> Unit,
    onDismissed: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        when (uiState) {
            is ReplaceDialogUiState.Replaced -> onReplaced()
            is ReplaceDialogUiState.Dismissed -> onDismissed()
            is ReplaceDialogUiState.ShowingDialog -> Unit
        }
    }

    val showing = uiState as? ReplaceDialogUiState.ShowingDialog ?: return

    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("스케쥴 교체") },
        text = { ReplaceDialogBody(incoming = showing.incoming, existing = showing.existing) },
        confirmButton = {
            Button(onClick = viewModel::confirmReplace) { Text("교체") }
        },
        dismissButton = {
            OutlinedButton(onClick = viewModel::dismiss) { Text("취소") }
        },
    )
}

@Composable
private fun ReplaceDialogBody(incoming: ScheduleWeek, existing: ScheduleWeek) {
    val formatter = DateTimeFormatter.ofPattern("M/d")
    val existingStart = existing.weekStartDate.format(formatter)
    val existingEnd = existing.days.last().date.format(formatter)
    val incomingStart = incoming.weekStartDate.format(formatter)
    val incomingEnd = incoming.days.last().date.format(formatter)
    Text(
        text = "${existingStart}~${existingEnd} 스케쥴이 이미 등록되어 있습니다.\n" +
            "${incomingStart}~${incomingEnd} 스케쥴로 교체하시겠습니까?",
    )
}
