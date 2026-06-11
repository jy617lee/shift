package com.schedule.shift.ui.replace

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class ReplaceScheduleViewModelTest {

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
    fun `initial state is ShowingDialog`() = runTest {
        val incoming = buildTestWeek(LocalDate.of(2026, 6, 8))
        val existing = buildTestWeek(LocalDate.of(2026, 6, 8))
        val viewModel = ReplaceScheduleViewModel(incoming, existing, repository)

        viewModel.uiState.test {
            assertTrue(awaitItem() is ReplaceDialogUiState.ShowingDialog)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirmReplace saves incoming week and emits Replaced`() = runTest {
        val incoming = buildTestWeek(LocalDate.of(2026, 6, 8))
        val existing = buildTestWeek(LocalDate.of(2026, 6, 8))
        coEvery { repository.replaceWeek(incoming) } returns Unit
        val viewModel = ReplaceScheduleViewModel(incoming, existing, repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirmReplace()
            assertTrue(awaitItem() is ReplaceDialogUiState.Replaced)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { repository.replaceWeek(incoming) }
    }

    @Test
    fun `dismiss emits Dismissed without saving`() = runTest {
        val incoming = buildTestWeek(LocalDate.of(2026, 6, 8))
        val existing = buildTestWeek(LocalDate.of(2026, 6, 8))
        val viewModel = ReplaceScheduleViewModel(incoming, existing, repository)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.dismiss()
            assertTrue(awaitItem() is ReplaceDialogUiState.Dismissed)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { repository.replaceWeek(any()) }
    }

    private fun buildTestWeek(monday: LocalDate) = ScheduleWeek(
        weekStartDate = monday,
        days = (0..6).map { offset ->
            ScheduleDay(
                date = monday.plusDays(offset.toLong()),
                type = if (offset < 5) DayType.WORK else DayType.OFF,
                startTime = if (offset < 5) LocalTime.of(9, 0) else null,
                endTime = if (offset < 5) LocalTime.of(18, 0) else null,
                codeLabel = if (offset < 5) "정상" else "정규휴일",
                source = SourceType.PARSED,
            )
        },
    )
}
