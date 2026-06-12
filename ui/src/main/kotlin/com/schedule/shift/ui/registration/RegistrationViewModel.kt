package com.schedule.shift.ui.registration

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val processImage: ProcessScheduleImageUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegistrationUiState>(RegistrationUiState.Idle)
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    fun onImageSelected(bitmap: Bitmap, imageUri: String? = null) {
        viewModelScope.launch {
            _uiState.value = RegistrationUiState.Processing
            _uiState.value = when (val result = processImage(bitmap)) {
                is ParseResult.Success -> RegistrationUiState.ParseSuccess(result.weeks, imageUri)
                is ParseResult.Failure -> mapFailure(result.reason)
            }
        }
    }

    fun reset() {
        _uiState.value = RegistrationUiState.Idle
    }

    private fun mapFailure(reason: FailureReason): RegistrationUiState =
        when (reason) {
            FailureReason.NOT_A_SCHEDULE -> RegistrationUiState.NotASchedule
            FailureReason.PARSE_ERROR -> RegistrationUiState.ParseError
        }
}
