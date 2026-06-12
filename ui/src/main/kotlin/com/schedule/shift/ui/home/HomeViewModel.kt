package com.schedule.shift.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.ui.di.TodayDate
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
    @TodayDate private val today: LocalDate,
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
                val week = repository.getWeekByDate(today) ?: repository.getNextWeekFrom(today)
                HomeUiState.Success(currentWeek = week, today = today)
            } catch (e: Exception) {
                HomeUiState.Error
            }
        }
    }
}
