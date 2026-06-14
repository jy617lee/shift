package com.schedule.shift.ui.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.preferences.UserPreferencesRepository
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

private const val DAYS_IN_WEEK = 7

private data class EditFieldChange(val field: String, val parsed: String, val corrected: String)

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = ConfirmationViewModel.Factory::class)
class ConfirmationViewModel @AssistedInject constructor(
    @Assisted private val initialWeeks: List<ScheduleWeek>,
    @Assisted("imageUri") private val imageUri: String?,
    @Assisted("sessionId") private val sessionId: String,
    private val repository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
    private val preferences: UserPreferencesRepository,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfirmationUiState>(
        ConfirmationUiState.Reviewing(weeks = initialWeeks, imageUri = imageUri),
    )
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    private val confirmStartMs = System.currentTimeMillis()
    private val editedRowIndices = mutableSetOf<Int>()
    private var wasReplace = false

    init {
        tracker.track(AnalyticsEvent.ConfirmShown(sessionId = sessionId, skipped = false))
    }

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
        wasReplace = true
        viewModelScope.launch {
            state.weeks.forEach { week ->
                if (repository.getWeekByDate(week.weekStartDate) != null) {
                    repository.replaceWeek(week)
                } else {
                    repository.saveWeek(week)
                }
            }
            widgetRefresher.refreshAll()
            finishAfterSave()
        }
    }

    fun dismissConflict() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        _uiState.value = state.copy(conflictCount = 0)
    }

    private suspend fun saveAllAndFinish(weeks: List<ScheduleWeek>) {
        weeks.forEach { repository.saveWeek(it) }
        widgetRefresher.refreshAll()
        finishAfterSave()
    }

    private suspend fun finishAfterSave() {
        tracker.track(
            AnalyticsEvent.RegisterComplete(
                sessionId = sessionId,
                editedRows = editedRowIndices.size,
                manualRows = 0,
                replace = wasReplace,
                totalDurationMs = System.currentTimeMillis() - confirmStartMs,
            ),
        )
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        if (!preferences.isSkipConfirmPromptShown()) {
            _uiState.value = state.copy(showSkipPrompt = true)
        } else {
            _uiState.value = ConfirmationUiState.Saved
        }
    }

    fun answerSkipPrompt(skipInFuture: Boolean) {
        viewModelScope.launch {
            preferences.setSkipConfirm(skipInFuture)
            preferences.setSkipConfirmPromptShown(true)
            _uiState.value = ConfirmationUiState.Saved
        }
    }

    fun cancel() {
        tracker.track(AnalyticsEvent.RegisterAbandon(sessionId = sessionId, lastStep = AnalyticsEvent.RegisterAbandon.LAST_STEP_CONFIRM))
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

    @Suppress("ReturnCount")
    fun commitEdit() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        val editing = state.editing ?: return
        val originalDay = state.weeks.getOrNull(editing.weekIndex)?.days?.getOrNull(editing.dayIndex) ?: return
        val draft = editing.draft

        if (originalDay != draft) {
            val rowIndex = editing.weekIndex * DAYS_IN_WEEK + editing.dayIndex
            editedRowIndices.add(rowIndex)
            trackChangedFields(originalDay, draft, rowIndex)
        }

        val updatedWeeks = state.weeks.toMutableList()
        val week = updatedWeeks[editing.weekIndex]
        val updatedDays = week.days.toMutableList()
        updatedDays[editing.dayIndex] = draft
        updatedWeeks[editing.weekIndex] = ScheduleWeek(weekStartDate = week.weekStartDate, days = updatedDays)
        _uiState.value = state.copy(weeks = updatedWeeks, editing = null)
    }

    @Suppress("LongParameterList")
    private fun trackChangedFields(original: ScheduleDay, draft: ScheduleDay, rowIndex: Int) {
        buildChangedFields(original, draft).forEach { change ->
            tracker.track(
                AnalyticsEvent.UserEdit(
                    sessionId = sessionId,
                    rowIndex = rowIndex,
                    field = change.field,
                    parsedValue = change.parsed,
                    correctedValue = change.corrected,
                    wasFailedRow = false,
                    editSource = AnalyticsEvent.UserEdit.EDIT_SOURCE_MANUAL,
                ),
            )
        }
    }

    private fun buildChangedFields(original: ScheduleDay, draft: ScheduleDay): List<EditFieldChange> = buildList {
        if (original.startTime != draft.startTime) {
            add(EditFieldChange(AnalyticsEvent.UserEdit.FIELD_START_TIME, original.startTime?.toString() ?: "", draft.startTime?.toString() ?: ""))
        }
        if (original.endTime != draft.endTime) {
            add(EditFieldChange(AnalyticsEvent.UserEdit.FIELD_END_TIME, original.endTime?.toString() ?: "", draft.endTime?.toString() ?: ""))
        }
        if (original.codeLabel != draft.codeLabel) {
            add(EditFieldChange(AnalyticsEvent.UserEdit.FIELD_CODE_LABEL, original.codeLabel, draft.codeLabel))
        }
    }

    fun dismissEdit() {
        val state = _uiState.value as? ConfirmationUiState.Reviewing ?: return
        _uiState.value = state.copy(editing = null)
    }

    @AssistedFactory
    interface Factory {
        @Suppress("LongParameterList")
        fun create(
            weeks: List<ScheduleWeek>,
            @Assisted("imageUri") imageUri: String?,
            @Assisted("sessionId") sessionId: String,
        ): ConfirmationViewModel
    }
}
