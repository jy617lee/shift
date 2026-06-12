package com.schedule.shift.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.analytics.SettingKey
import com.schedule.shift.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    val skipConfirm: StateFlow<Boolean> = settingsRepository
        .skipConfirm()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MS), false)

    fun setSkipConfirm(skip: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSkipConfirm(skip)
            analyticsTracker.track(
                AnalyticsEvent.SettingChanged(
                    key = SettingKey.SKIP_CONFIRM,
                    value = skip.toString(),
                ),
            )
        }
    }

    companion object {
        private const val SUBSCRIBE_TIMEOUT_MS = 5_000L
    }
}
