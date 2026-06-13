package com.schedule.shift.ui.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ConfirmationViewModel.Factory::class)
class ConfirmationViewModel @AssistedInject constructor(
    @Assisted private val initialWeeks: List<ScheduleWeek>,
    @Assisted private val imageUri: String?,
    private val repository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfirmationUiState>(
        ConfirmationUiState.Reviewing(weeks = initialWeeks, imageUri = imageUri),
    )
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    fun confirm() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        viewModelScope.launch {
            val conflicts = state.weeks.count { repository.getWeekByDate(it.weekStartDate) != null }
            if (conflicts > 0) {
                _uiState.value = state.copy(conflictCount = conflicts)
                return@launch
            }
            saveAllAndFinish(state.weeks)
        }
    }

    fun proceedWithReplace() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        viewModelScope.launch {
            state.weeks.forEach { week ->
                if (repository.getWeekByDate(week.weekStartDate) != null) {
                    repository.replaceWeek(week)
                } else {
                    repository.saveWeek(week)
                }
            }
            widgetRefresher.refreshAll()
            _uiState.value = ConfirmationUiState.Saved
        }
    }

    fun dismissConflict() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        _uiState.value = state.copy(conflictCount = 0)
    }

    private suspend fun saveAllAndFinish(weeks: List<ScheduleWeek>) {
        weeks.forEach { repository.saveWeek(it) }
        widgetRefresher.refreshAll()
        _uiState.value = ConfirmationUiState.Saved
    }

    fun cancel() {
        _uiState.value = ConfirmationUiState.Cancelled
    }

    fun startEdit(weekIndex: Int, dayIndex: Int) {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        val day = state.weeks.getOrNull(weekIndex)?.days?.getOrNull(dayIndex) ?: return
        _uiState.value = state.copy(editing = EditingState(weekIndex, dayIndex, day))
    }

    fun updateDraft(draft: ScheduleDay) {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        val editing = state.editing ?: return
        _uiState.value = state.copy(editing = editing.copy(draft = draft))
    }

    fun commitEdit() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        val editing = state.editing ?: return
        val updatedWeeks = state.weeks.toMutableList()
        val week = updatedWeeks[editing.weekIndex]
        val updatedDays = week.days.toMutableList()
        updatedDays[editing.dayIndex] = editing.draft
        updatedWeeks[editing.weekIndex] = ScheduleWeek(
            weekStartDate = week.weekStartDate,
            days = updatedDays,
        )
        _uiState.value = state.copy(weeks = updatedWeeks, editing = null)
    }

    fun dismissEdit() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        _uiState.value = state.copy(editing = null)
    }

    @AssistedFactory
    interface Factory {
        fun create(weeks: List<ScheduleWeek>, imageUri: String?): ConfirmationViewModel
    }
}
