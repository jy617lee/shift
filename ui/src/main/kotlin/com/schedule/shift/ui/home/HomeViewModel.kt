package com.schedule.shift.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCurrentWeek()
    }

    fun refresh() {
        loadCurrentWeek()
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun loadCurrentWeek() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            _uiState.value = try {
                HomeUiState.Success(
                    currentWeek = repository.getWeekByDate(today),
                    today = today,
                )
            } catch (e: Exception) {
                HomeUiState.Error
            }
        }
    }
}
