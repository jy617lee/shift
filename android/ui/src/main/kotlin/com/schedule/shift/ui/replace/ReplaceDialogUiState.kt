package com.schedule.shift.ui.replace

import com.schedule.shift.domain.model.ScheduleWeek

sealed class ReplaceDialogUiState {
    data class ShowingDialog(
        val incoming: ScheduleWeek,
        val existing: ScheduleWeek,
    ) : ReplaceDialogUiState()
    data object Replaced : ReplaceDialogUiState()
    data object Dismissed : ReplaceDialogUiState()
}
