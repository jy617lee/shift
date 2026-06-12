package com.schedule.shift.ui.registration

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val processImage: ProcessScheduleImageUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private var sessionId: String = ""
    private var registerStartMs: Long = 0L
    private var currentStep: String = STEP_IDLE

    fun onRegisterStart() {
        sessionId = UUID.randomUUID().toString()
        registerStartMs = System.currentTimeMillis()
        currentStep = STEP_IMAGE_SELECT
        analyticsTracker.track(AnalyticsEvent.RegisterStart(sessionId))
    }

    fun onImageSelected(bitmap: Bitmap, imageUri: String? = null) {
        analyticsTracker.track(
            AnalyticsEvent.ImageSelected(sessionId, bitmap.width, bitmap.height),
        )
        currentStep = STEP_PROCESSING
        viewModelScope.launch {
            _uiState.value = RegistrationUiState.Processing
            val parseMs = System.currentTimeMillis()
            val result = processImage(bitmap)
            val durationMs = System.currentTimeMillis() - parseMs
            trackParseResult(result, durationMs)
            _uiState.value = when (result) {
                is ParseResult.Success -> {
                    currentStep = STEP_CONFIRM
                    RegistrationUiState.ParseSuccess(
                        weeks = result.weeks,
                        imageUri = imageUri,
                        sessionId = sessionId,
                        sessionStartMs = registerStartMs,
                    )
                }
                is ParseResult.Failure -> {
                    currentStep = STEP_IDLE
                    mapFailure(result.reason)
                }
            }
        }
    }

    fun reset() {
        if (sessionId.isNotEmpty() && currentStep != STEP_IDLE) {
            analyticsTracker.track(AnalyticsEvent.RegisterAbandon(sessionId, currentStep))
        }
        sessionId = ""
        currentStep = STEP_IDLE
        _uiState.value = RegistrationUiState.Idle
    }

    private fun trackParseResult(result: ParseResult, durationMs: Long) {
        when (result) {
            is ParseResult.Success -> {
                analyticsTracker.track(
                    AnalyticsEvent.Stage1Result(sessionId, pass = true, failReason = null),
                )
                analyticsTracker.track(
                    AnalyticsEvent.ParseResult(
                        sessionId = sessionId,
                        failedRows = 0,
                        durationMs = durationMs,
                        ocrConfidenceAvg = DEFAULT_CONFIDENCE,
                    ),
                )
            }
            is ParseResult.Failure -> {
                val failReason = result.reason.name.lowercase()
                analyticsTracker.track(
                    AnalyticsEvent.Stage1Result(sessionId, pass = false, failReason = failReason),
                )
            }
        }
    }

    private fun mapFailure(reason: FailureReason): RegistrationUiState =
        when (reason) {
            FailureReason.NOT_A_SCHEDULE -> RegistrationUiState.NotASchedule
            FailureReason.PARSE_ERROR -> RegistrationUiState.ParseError
        }

    companion object {
        private const val STEP_IDLE = "idle"
        private const val STEP_IMAGE_SELECT = "image_select"
        private const val STEP_PROCESSING = "processing"
        const val STEP_CONFIRM = "confirm"
        private const val DEFAULT_CONFIDENCE = 0f
    }
}
