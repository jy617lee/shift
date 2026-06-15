package com.schedule.shift.navigation

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
import com.schedule.shift.domain.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FlowPendingAction {
    data object None : FlowPendingAction()
    data object GoToConfirmation : FlowPendingAction()
}

@HiltViewModel
class RegistrationFlowStateHolder @Inject constructor(
    private val preferences: UserPreferencesRepository,
    private val scheduleRepository: ScheduleRepository,
    private val widgetRefresher: WidgetRefresher,
    private val processImage: ProcessScheduleImageUseCase,
    private val tracker: AnalyticsTracker,
) : ViewModel() {

    var pendingWeeks: List<ScheduleWeek> = emptyList()
        private set
    var pendingImageUri: String? = null
        private set
    var pendingSessionId: String = ""
        private set

    private val _pendingAction = MutableStateFlow<FlowPendingAction>(FlowPendingAction.None)
    val pendingAction: StateFlow<FlowPendingAction> = _pendingAction.asStateFlow()

    private val _isProcessingImage = MutableStateFlow(false)
    val isProcessingImage: StateFlow<Boolean> = _isProcessingImage.asStateFlow()

    private val _imageErrorMessage = MutableStateFlow<String?>(null)
    val imageErrorMessage: StateFlow<String?> = _imageErrorMessage.asStateFlow()

    private val _homeRefreshNeeded = MutableStateFlow(false)
    val homeRefreshNeeded: StateFlow<Boolean> = _homeRefreshNeeded.asStateFlow()

    fun handleImageSelected(bitmap: Bitmap, uri: String?) {
        viewModelScope.launch {
            _isProcessingImage.value = true
            _imageErrorMessage.value = null

            val sessionId = UUID.randomUUID().toString()
            val startMs = System.currentTimeMillis()

            tracker.track(AnalyticsEvent.RegisterStart(sessionId = sessionId))
            tracker.track(
                AnalyticsEvent.ImageSelected(
                    sessionId = sessionId,
                    imageWidth = bitmap.width,
                    imageHeight = bitmap.height,
                ),
            )

            val result = processImage(bitmap)
            val durationMs = System.currentTimeMillis() - startMs

            trackParseAnalytics(result, sessionId, durationMs)

            when (result) {
                is ParseResult.Success -> handleSuccess(result.weeks, uri, sessionId)
                is ParseResult.Failure -> handleFailure(result.reason)
            }
            _isProcessingImage.value = false
        }
    }

    @Suppress("LongParameterList")
    private fun trackParseAnalytics(result: ParseResult, sessionId: String, durationMs: Long) {
        when (result) {
            is ParseResult.Success -> {
                tracker.track(
                    AnalyticsEvent.Stage1Result(sessionId = sessionId, pass = true, failReason = null),
                )
                tracker.track(
                    AnalyticsEvent.ParseResult(
                        sessionId = sessionId,
                        failedRows = 0,
                        durationMs = durationMs,
                        ocrConfidenceAvg = 0f,
                    ),
                )
            }
            is ParseResult.Failure -> when (result.reason) {
                FailureReason.NOT_A_SCHEDULE ->
                    tracker.track(
                        AnalyticsEvent.Stage1Result(
                            sessionId = sessionId,
                            pass = false,
                            failReason = AnalyticsEvent.Stage1Result.FAIL_NOT_A_SCHEDULE,
                        ),
                    )
                FailureReason.PARSE_ERROR -> {
                    tracker.track(
                        AnalyticsEvent.Stage1Result(sessionId = sessionId, pass = true, failReason = null),
                    )
                }
            }
        }
    }

    @Suppress("LongParameterList")
    private suspend fun handleSuccess(weeks: List<ScheduleWeek>, uri: String?, sessionId: String) {
        if (preferences.isSkipConfirm()) {
            tracker.track(AnalyticsEvent.ConfirmShown(sessionId = sessionId, skipped = true))
            val saveStartMs = System.currentTimeMillis()
            var didReplace = false
            weeks.forEach { week ->
                if (scheduleRepository.getWeekByDate(week.weekStartDate) != null) {
                    scheduleRepository.replaceWeek(week)
                    didReplace = true
                } else {
                    scheduleRepository.saveWeek(week)
                }
            }
            widgetRefresher.refreshAll()
            tracker.track(
                AnalyticsEvent.RegisterComplete(
                    sessionId = sessionId,
                    editedRows = 0,
                    manualRows = 0,
                    replace = didReplace,
                    totalDurationMs = System.currentTimeMillis() - saveStartMs,
                ),
            )
            _homeRefreshNeeded.value = true
        } else {
            pendingWeeks = weeks
            pendingImageUri = uri
            pendingSessionId = sessionId
            _pendingAction.value = FlowPendingAction.GoToConfirmation
        }
    }

    private fun handleFailure(reason: FailureReason) {
        _imageErrorMessage.value = when (reason) {
            FailureReason.NOT_A_SCHEDULE -> "스케쥴 이미지가 아닌 것 같아요\n주간 스케쥴표 이미지인지 확인해 주세요"
            FailureReason.PARSE_ERROR -> "이미지를 읽지 못했어요\n다시 시도해 주세요"
        }
    }

    fun clearImageError() {
        _imageErrorMessage.value = null
    }

    fun clearHomeRefresh() {
        _homeRefreshNeeded.value = false
    }

    fun resetAction() {
        _pendingAction.value = FlowPendingAction.None
    }

    fun clear() {
        pendingWeeks = emptyList()
        pendingImageUri = null
        pendingSessionId = ""
        _pendingAction.value = FlowPendingAction.None
        _homeRefreshNeeded.value = false
        _imageErrorMessage.value = null
    }
}
