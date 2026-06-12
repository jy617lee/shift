package com.schedule.shift.ui.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.DayType
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
@Suppress("LongParameterList")
class ConfirmationViewModel @AssistedInject constructor(
    @Assisted private val initialWeeks: List<ScheduleWeek>,
    @Assisted("imageUri") private val imageUri: String?,
    @Assisted("sessionId") private val sessionId: String,
    @Assisted private val sessionStartMs: Long,
    @Assisted private val replace: Boolean,
    private val repository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfirmationUiState>(
        ConfirmationUiState.Reviewing(weeks = initialWeeks, imageUri = imageUri),
    )
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    private var editedRows = 0

    init {
        analyticsTracker.track(AnalyticsEvent.ConfirmShown(sessionId, skipped = false))
    }

    fun confirm() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        viewModelScope.launch {
            if (replace) {
                state.weeks.forEach { repository.replaceWeek(it) }
            } else {
                state.weeks.forEach { repository.saveWeek(it) }
            }
            widgetRefresher.refreshAll()
            analyticsTracker.track(
                AnalyticsEvent.RegisterComplete(
                    sessionId = sessionId,
                    editedRows = editedRows,
                    manualRows = state.weeks.flatMap { it.days }
                        .count { it.type == DayType.UNREGISTERED },
                    replace = replace,
                    totalDurationMs = System.currentTimeMillis() - sessionStartMs,
                ),
            )
            _uiState.value = ConfirmationUiState.Saved
        }
    }

    fun cancel() {
        analyticsTracker.track(AnalyticsEvent.RegisterAbandon(sessionId, lastStep = "confirm"))
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
        val original = updatedDays[editing.dayIndex]
        updatedDays[editing.dayIndex] = editing.draft
        updatedWeeks[editing.weekIndex] = ScheduleWeek(
            weekStartDate = week.weekStartDate,
            days = updatedDays,
        )
        trackUserEdit(editing, original)
        editedRows++
        _uiState.value = state.copy(weeks = updatedWeeks, editing = null)
    }

    fun dismissEdit() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        _uiState.value = state.copy(editing = null)
    }

    private fun trackUserEdit(editing: EditingState, original: ScheduleDay) {
        val (field, parsed, corrected) = resolveEditedField(original, editing.draft)
        analyticsTracker.track(
            AnalyticsEvent.UserEdit(
                sessionId = sessionId,
                rowIndex = editing.weekIndex * DAYS_IN_WEEK + editing.dayIndex,
                field = field,
                parsedValue = parsed,
                correctedValue = corrected,
                wasFailedRow = false,
                editSource = "manual",
            ),
        )
    }

    private fun resolveEditedField(
        original: ScheduleDay,
        updated: ScheduleDay,
    ): Triple<String, String, String> = when {
        original.startTime != updated.startTime ->
            Triple("start_time", original.startTime?.toString() ?: "", updated.startTime?.toString() ?: "")
        original.endTime != updated.endTime ->
            Triple("end_time", original.endTime?.toString() ?: "", updated.endTime?.toString() ?: "")
        else ->
            Triple("code_label", original.codeLabel, updated.codeLabel)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            weeks: List<ScheduleWeek>,
            @Assisted("imageUri") imageUri: String?,
            @Assisted("sessionId") sessionId: String,
            sessionStartMs: Long,
            replace: Boolean,
        ): ConfirmationViewModel
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
    }
}
