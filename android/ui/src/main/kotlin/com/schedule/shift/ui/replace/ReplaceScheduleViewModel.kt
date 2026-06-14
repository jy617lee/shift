package com.schedule.shift.ui.replace

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

@HiltViewModel(assistedFactory = ReplaceScheduleViewModel.Factory::class)
class ReplaceScheduleViewModel @AssistedInject constructor(
    @Assisted("incoming") private val incoming: ScheduleWeek,
    @Assisted("existing") private val existing: ScheduleWeek,
    private val repository: ScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReplaceDialogUiState>(
        ReplaceDialogUiState.ShowingDialog(incoming, existing),
    )
    val uiState: StateFlow<ReplaceDialogUiState> = _uiState.asStateFlow()

    fun confirmReplace() {
        viewModelScope.launch {
            repository.replaceWeek(incoming)
            _uiState.value = ReplaceDialogUiState.Replaced
        }
    }

    fun dismiss() {
        _uiState.value = ReplaceDialogUiState.Dismissed
    }

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("incoming") incoming: ScheduleWeek,
            @Assisted("existing") existing: ScheduleWeek,
        ): ReplaceScheduleViewModel
    }
}
