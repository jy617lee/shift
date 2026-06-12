package com.schedule.shift.ui.home

import com.schedule.shift.domain.model.ScheduleWeek
import java.time.LocalDate

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val weeks: List<ScheduleWeek>,
        val today: LocalDate,
    ) : HomeUiState()
    data object Error : HomeUiState()
}
