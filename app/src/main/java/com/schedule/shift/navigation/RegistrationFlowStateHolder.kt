package com.schedule.shift.navigation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
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
}

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
    private val processImage: ProcessScheduleImageUseCase,
) : ViewModel() {

    var pendingWeeks: List<ScheduleWeek> = emptyList()
        private set

    var pendingImageUri: String? = null
        private set

    private val _pendingAction = MutableStateFlow<FlowPendingAction>(FlowPendingAction.None)
    val pendingAction: StateFlow<FlowPendingAction> = _pendingAction.asStateFlow()

    private val _skipConfirm = MutableStateFlow(false)
    val skipConfirm: StateFlow<Boolean> = _skipConfirm.asStateFlow()

    private val _homeRefreshNeeded = MutableStateFlow(false)
    val homeRefreshNeeded: StateFlow<Boolean> = _homeRefreshNeeded.asStateFlow()

    init {
        viewModelScope.launch {
            _skipConfirm.value = preferences.isSkipConfirm()
        }
    }

    fun handleParsed(weeks: List<ScheduleWeek>, imageUri: String?) {
        pendingWeeks = weeks
        pendingImageUri = imageUri
        _pendingAction.value = FlowPendingAction.GoToConfirmation
    }

    @Suppress("UnusedParameter")
    fun startSkipSave(bitmap: Bitmap, uri: String?) {
        viewModelScope.launch {
            val result = processImage(bitmap)
            if (result is ParseResult.Success) {
                result.weeks.forEach { week ->
                    if (scheduleRepository.getWeekByDate(week.weekStartDate) != null) {
                        scheduleRepository.replaceWeek(week)
                    } else {
                        scheduleRepository.saveWeek(week)
                    }
                }
                widgetRefresher.refreshAll()
                _homeRefreshNeeded.value = true
            }
        }
    }

    fun clearHomeRefresh() {
        _homeRefreshNeeded.value = false
    }

    fun resetAction() {
        _pendingAction.value = FlowPendingAction.None
    }

    fun clear() {
        pendingWeeks = emptyList()
        pendingImageUri = null
        _pendingAction.value = FlowPendingAction.None
        _homeRefreshNeeded.value = false
    }
}
