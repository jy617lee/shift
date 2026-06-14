package com.schedule.shift.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FlowPendingAction {
    data object None : FlowPendingAction()
    data object GoToConfirmation : FlowPendingAction()
    data object SavedDirectly : FlowPendingAction()
}

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    var pendingWeeks: List<ScheduleWeek> = emptyList()
        private set

    var pendingImageUri: String? = null
        private set

    private val _pendingAction = MutableStateFlow<FlowPendingAction>(FlowPendingAction.None)
    val pendingAction: StateFlow<FlowPendingAction> = _pendingAction.asStateFlow()

    fun handleParsed(weeks: List<ScheduleWeek>, imageUri: String?) {
        viewModelScope.launch {
            if (preferences.isSkipConfirm()) {
                autoSave(weeks)
            } else {
                pendingWeeks = weeks
                pendingImageUri = imageUri
                _pendingAction.value = FlowPendingAction.GoToConfirmation
            }
        }
    }

    private suspend fun autoSave(weeks: List<ScheduleWeek>) {
        weeks.forEach { week ->
            if (scheduleRepository.getWeekByDate(week.weekStartDate) != null) {
                scheduleRepository.replaceWeek(week)
            } else {
                scheduleRepository.saveWeek(week)
            }
        }
        widgetRefresher.refreshAll()
        _pendingAction.value = FlowPendingAction.SavedDirectly
    }

    fun resetAction() {
        _pendingAction.value = FlowPendingAction.None
    }

    fun clear() {
        pendingWeeks = emptyList()
        pendingImageUri = null
        _pendingAction.value = FlowPendingAction.None
    }
}
