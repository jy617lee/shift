package com.schedule.shift.ui.confirmation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.repository.ScheduleRepository
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
    @Assisted private val weeks: List<ScheduleWeek>,
    private val repository: ScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConfirmationUiState>(ConfirmationUiState.Reviewing(weeks))
    val uiState: StateFlow<ConfirmationUiState> = _uiState.asStateFlow()

    fun confirm() {
        viewModelScope.launch {
            weeks.forEach { repository.saveWeek(it) }
            _uiState.value = ConfirmationUiState.Saved
        }
    }

    fun cancel() {
        _uiState.value = ConfirmationUiState.Cancelled
    }

    @AssistedFactory
    interface Factory {
        fun create(weeks: List<ScheduleWeek>): ConfirmationViewModel
    }
}
