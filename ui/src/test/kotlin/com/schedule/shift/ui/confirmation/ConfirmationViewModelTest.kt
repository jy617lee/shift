package com.schedule.shift.ui.confirmation

import app.cash.turbine.test
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
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
class ConfirmationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScheduleRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows weeks from constructor`() = runTest {
        val weeks = listOf(buildTestWeek())
        val viewModel = ConfirmationViewModel(weeks, repository)

        viewModel.uiState.test {
            val state = awaitItem() as ConfirmationUiState.Reviewing
            assertEquals(weeks, state.weeks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirm saves all weeks and emits Saved`() = runTest {
        val weeks = listOf(buildTestWeek())
        coEvery { repository.saveWeek(any()) } returns Unit
        val viewModel = ConfirmationViewModel(weeks, repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirm()
            val state = awaitItem()
            assertTrue(state is ConfirmationUiState.Saved)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = weeks.size) { repository.saveWeek(any()) }
    }

    @Test
    fun `confirm with empty weeks list emits Saved without saving`() = runTest {
        val viewModel = ConfirmationViewModel(emptyList(), repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirm()
            val state = awaitItem()
            assertTrue(state is ConfirmationUiState.Saved)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { repository.saveWeek(any()) }
    }

    @Test
    fun `cancel emits Cancelled`() = runTest {
        val viewModel = ConfirmationViewModel(listOf(buildTestWeek()), repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.cancel()
            assertTrue(awaitItem() is ConfirmationUiState.Cancelled)
            cancelAndIgnoreRemainingEvents()
        }
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
