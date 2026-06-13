package com.schedule.shift.ui.confirmation

import app.cash.turbine.test
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.widget.WidgetRefresher
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
    private lateinit var widgetRefresher: WidgetRefresher

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        widgetRefresher = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun buildViewModel(weeks: List<ScheduleWeek> = listOf(buildTestWeek())) =
        ConfirmationViewModel(weeks, null, repository, widgetRefresher)

    @Test
    fun `initial state shows weeks from constructor`() = runTest {
        val weeks = listOf(buildTestWeek())
        val viewModel = buildViewModel(weeks)

        viewModel.uiState.test {
            val state = awaitItem() as ConfirmationUiState.Reviewing
            assertEquals(weeks, state.weeks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `confirm saves all weeks and emits Saved when no conflict`() = runTest {
        val weeks = listOf(buildTestWeek())
        coEvery { repository.getWeekByDate(any()) } returns null
        coEvery { repository.saveWeek(any()) } returns Unit
        val viewModel = buildViewModel(weeks)

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
    fun `confirm sets conflictCount when week already exists`() = runTest {
        val weeks = listOf(buildTestWeek())
        coEvery { repository.getWeekByDate(any()) } returns buildTestWeek()
        val viewModel = buildViewModel(weeks)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirm()
            val state = awaitItem() as ConfirmationUiState.Reviewing
            assertEquals(1, state.conflictCount)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { repository.saveWeek(any()) }
    }

    @Test
    fun `confirm refreshes widget after saving when no conflict`() = runTest {
        coEvery { repository.getWeekByDate(any()) } returns null
        val viewModel = buildViewModel()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.confirm()
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { widgetRefresher.refreshAll() }
    }

    @Test
    fun `confirm with empty weeks list emits Saved without saving`() = runTest {
        coEvery { repository.getWeekByDate(any()) } returns null
        val viewModel = buildViewModel(emptyList())

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
        val viewModel = buildViewModel()

        viewModel.uiState.test {
            skipItems(1)
            viewModel.cancel()
            assertTrue(awaitItem() is ConfirmationUiState.Cancelled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `startEdit opens editing state for correct day`() = runTest {
        val week = buildTestWeek()
        val viewModel = buildViewModel(listOf(week))

        viewModel.startEdit(weekIndex = 0, dayIndex = 2)

        viewModel.uiState.test {
            val state = awaitItem() as ConfirmationUiState.Reviewing
            assertEquals(2, state.editing?.dayIndex)
            assertEquals(week.days[2], state.editing?.draft)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `commitEdit updates day and clears editing`() = runTest {
        val week = buildTestWeek()
        val viewModel = buildViewModel(listOf(week))
        viewModel.startEdit(weekIndex = 0, dayIndex = 0)
        val updatedDay = week.days[0].copy(codeLabel = "수정됨")
        viewModel.updateDraft(updatedDay)
        viewModel.commitEdit()

        viewModel.uiState.test {
            val state = awaitItem() as ConfirmationUiState.Reviewing
            assertEquals("수정됨", state.weeks[0].days[0].codeLabel)
            assertEquals(null, state.editing)
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
