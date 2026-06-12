package com.schedule.shift.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.ui.di.TodayDate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ScheduleRepository,
    private val analyticsTracker: AnalyticsTracker,
    @TodayDate private val today: LocalDate,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var lastTrackedOffset: Int? = null

    init {
        loadCurrentWeek()
    }

    fun refresh() {
        loadCurrentWeek()
    }

    fun onWeekViewed(weekStartDate: LocalDate) {
        val offset = weekOffsetFromToday(weekStartDate)
        if (offset == lastTrackedOffset) return
        lastTrackedOffset = offset
        analyticsTracker.track(AnalyticsEvent.HomeWeekViewed(offset))
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun loadCurrentWeek() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            _uiState.value = try {
                val weeks = repository.getAllWeeks().sortedBy { it.weekStartDate }
                HomeUiState.Success(weeks = weeks, today = today)
            } catch (e: Exception) {
                HomeUiState.Error
            }
        }
    }

    private fun weekOffsetFromToday(weekStart: LocalDate): Int {
        val todayWeekStart = today.with(DayOfWeek.MONDAY)
            .let { if (it.isAfter(today)) it.minusWeeks(1) else it }
        return (weekStart.toEpochDay() - todayWeekStart.toEpochDay()).toInt() / DAYS_IN_WEEK
    }

    companion object {
        private const val DAYS_IN_WEEK = 7
    }
}
