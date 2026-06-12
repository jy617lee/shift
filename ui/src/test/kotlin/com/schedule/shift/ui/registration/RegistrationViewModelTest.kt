package com.schedule.shift.ui.registration

import android.graphics.Bitmap
import app.cash.turbine.test
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var useCase: ProcessScheduleImageUseCase
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var viewModel: RegistrationViewModel
    private lateinit var bitmap: Bitmap

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mockk()
        analyticsTracker = mockk(relaxed = true)
        bitmap = mockk(relaxed = true)
        viewModel = RegistrationViewModel(useCase, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uiState.test {
            assertEquals(RegistrationUiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage transitions through Loading to ParseSuccess`() = runTest {
        val week = buildTestWeek()
        coEvery { useCase(bitmap) } returns ParseResult.Success(listOf(week))

        viewModel.uiState.test {
            assertEquals(RegistrationUiState.Idle, awaitItem())
            viewModel.onImageSelected(bitmap)
            assertEquals(RegistrationUiState.Processing, awaitItem())
            val result = awaitItem()
            assertTrue(result is RegistrationUiState.ParseSuccess)
            assertEquals(listOf(week), (result as RegistrationUiState.ParseSuccess).weeks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits NotASchedule when parse fails with NOT_A_SCHEDULE`() = runTest {
        coEvery { useCase(bitmap) } returns ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onImageSelected(bitmap)
            skipItems(1)
            val result = awaitItem()
            assertTrue(result is RegistrationUiState.NotASchedule)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits ParseError when parse fails with PARSE_ERROR`() = runTest {
        coEvery { useCase(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onImageSelected(bitmap)
            skipItems(1)
            val result = awaitItem()
            assertTrue(result is RegistrationUiState.ParseError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset returns to Idle`() = runTest {
        coEvery { useCase(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onImageSelected(bitmap)
            skipItems(2)
            viewModel.reset()
            assertEquals(RegistrationUiState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onRegisterStart tracks RegisterStart event`() {
        viewModel.onRegisterStart()
        verify { analyticsTracker.track(match { it is AnalyticsEvent.RegisterStart }) }
    }

    @Test
    fun `onImageSelected tracks ImageSelected event`() = runTest {
        coEvery { useCase(bitmap) } returns ParseResult.Success(listOf(buildTestWeek()))
        viewModel.onRegisterStart()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.onImageSelected(bitmap)
            skipItems(2)
            cancelAndIgnoreRemainingEvents()
        }
        verify { analyticsTracker.track(match { it is AnalyticsEvent.ImageSelected }) }
    }

    @Test
    fun `reset after start tracks RegisterAbandon event`() {
        viewModel.onRegisterStart()
        viewModel.reset()
        verify { analyticsTracker.track(match { it is AnalyticsEvent.RegisterAbandon }) }
    }

    @Test
    fun `reset without start does not track RegisterAbandon`() {
        viewModel.reset()
        verify(exactly = 0) { analyticsTracker.track(match { it is AnalyticsEvent.RegisterAbandon }) }
    }

    private fun buildTestWeek() = ScheduleWeek(
        weekStartDate = LocalDate.of(2026, 6, 8),
        days = (0..6).map { offset ->
            ScheduleDay(
                date = LocalDate.of(2026, 6, 8).plusDays(offset.toLong()),
                type = if (offset < 5) DayType.WORK else DayType.OFF,
                startTime = if (offset < 5) LocalTime.of(9, 0) else null,
                endTime = if (offset < 5) LocalTime.of(18, 0) else null,
                codeLabel = if (offset < 5) "정상" else "정규휴일",
                source = SourceType.PARSED,
            )
        },
    )
}
