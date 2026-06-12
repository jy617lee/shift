package com.schedule.shift.ui.confirmation

import com.schedule.shift.domain.model.ScheduleWeek

sealed class ConfirmationUiState {
    data class Reviewing(val weeks: List<ScheduleWeek>) : ConfirmationUiState()
    data object Saved : ConfirmationUiState()
    data object Cancelled : ConfirmationUiState()
}
