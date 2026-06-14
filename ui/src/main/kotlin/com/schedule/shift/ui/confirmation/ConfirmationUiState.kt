package com.schedule.shift.ui.confirmation

import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek

data class EditingState(
    val weekIndex: Int,
    val dayIndex: Int,
    val draft: ScheduleDay,
)

sealed class ConfirmationUiState {
    data class Reviewing(
        val weeks: List<ScheduleWeek>,
        val imageUri: String? = null,
        val editing: EditingState? = null,
        val conflictCount: Int = 0,
        val showSkipPrompt: Boolean = false,
    ) : ConfirmationUiState()

    data object Saved : ConfirmationUiState()
    data object Cancelled : ConfirmationUiState()
}
