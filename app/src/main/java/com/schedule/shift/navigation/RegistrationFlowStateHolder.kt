package com.schedule.shift.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.repository.SettingsRepository
import com.schedule.shift.domain.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor(
    settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    val skipConfirm: StateFlow<Boolean> = settingsRepository
        .skipConfirm()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    fun autoSave(weeks: List<ScheduleWeek>) {
        val sessionId = pendingSessionId
        val sessionStartMs = pendingSessionStartMs
        viewModelScope.launch {
            weeks.forEach { scheduleRepository.saveWeek(it) }
            widgetRefresher.refreshAll()
            analyticsTracker.track(
                AnalyticsEvent.RegisterComplete(
                    sessionId = sessionId,
                    editedRows = 0,
                    manualRows = 0,
                    replace = false,
                    totalDurationMs = System.currentTimeMillis() - sessionStartMs,
                ),
            )
        }
    }

    fun clear() {
        pendingWeeks = emptyList()
        pendingImageUri = null
        pendingSessionId = ""
        pendingSessionStartMs = 0L
        pendingReplace = false
    }
}
