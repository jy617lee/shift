package com.schedule.shift.navigation

import androidx.lifecycle.ViewModel
import com.schedule.shift.domain.model.ScheduleWeek
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor() : ViewModel() {

    var pendingWeeks: List<ScheduleWeek> = emptyList()
        private set

    var pendingImageUri: String? = null
        private set

    var pendingSessionId: String = ""
        private set

    var pendingSessionStartMs: Long = 0L
        private set

    var pendingReplace: Boolean = false
        private set

    fun setPendingWeeks(weeks: List<ScheduleWeek>) {
        pendingWeeks = weeks
    }

    fun setPendingImageUri(uri: String?) {
        pendingImageUri = uri
    }

    fun setSession(sessionId: String, sessionStartMs: Long) {
        pendingSessionId = sessionId
        pendingSessionStartMs = sessionStartMs
    }

    fun setReplace(replace: Boolean) {
        pendingReplace = replace
    }

    fun clear() {
        pendingWeeks = emptyList()
        pendingImageUri = null
        pendingSessionId = ""
        pendingSessionStartMs = 0L
        pendingReplace = false
    }
}
