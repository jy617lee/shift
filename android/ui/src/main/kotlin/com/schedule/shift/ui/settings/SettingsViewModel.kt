package com.schedule.shift.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.analytics.SettingKey
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    private val _skipConfirm = MutableStateFlow(false)
    val skipConfirm: StateFlow<Boolean> = _skipConfirm.asStateFlow()

    init {
        viewModelScope.launch {
            _skipConfirm.value = preferences.isSkipConfirm()
        }
    }

    fun setSkipConfirm(value: Boolean) {
        viewModelScope.launch {
            preferences.setSkipConfirm(value)
            _skipConfirm.value = value
            tracker.track(AnalyticsEvent.SettingChanged(key = SettingKey.SKIP_CONFIRM, value = value.toString()))
        }
    }
}
