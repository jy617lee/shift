package com.schedule.shift.navigation

import androidx.lifecycle.ViewModel
import com.schedule.shift.domain.model.ScheduleWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor() : ViewModel() {
    var pendingWeeks: List<ScheduleWeek> = emptyList()
        private set

    fun setPendingWeeks(weeks: List<ScheduleWeek>) {
        pendingWeeks = weeks
    }
}
