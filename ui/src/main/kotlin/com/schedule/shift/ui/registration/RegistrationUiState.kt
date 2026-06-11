package com.schedule.shift.ui.registration

import com.schedule.shift.domain.model.ScheduleWeek

sealed class RegistrationUiState {
    data object Idle : RegistrationUiState()
    data object Processing : RegistrationUiState()
    data class ParseSuccess(val weeks: List<ScheduleWeek>) : RegistrationUiState()
    data object NotASchedule : RegistrationUiState()
    data object ParseError : RegistrationUiState()
}
