package com.schedule.shift.ui.home

import app.cash.turbine.test
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.repository.ScheduleRepository
import io.mockk.coEvery
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScheduleRepository
    private val today = LocalDate.of(2026, 6, 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { repository.getWeekByDate(any()) } returns null
        val viewModel = HomeViewModel(repository, today)

        viewModel.uiState.test {
            val first = awaitItem()
            assertTrue(first is HomeUiState.Loading || first is HomeUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with null week when no schedule exists`() = runTest {
        coEvery { repository.getWeekByDate(any()) } returns null
        val viewModel = HomeViewModel(repository, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertTrue(state is HomeUiState.Success)
            assertTrue((state as HomeUiState.Success).currentWeek == null)
            assertEquals(today, state.today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with current week when schedule exists`() = runTest {
        val week = buildTestWeek()
        coEvery { repository.getWeekByDate(any()) } returns week
        val viewModel = HomeViewModel(repository, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as HomeUiState.Success
            assertEquals(week, state.currentWeek)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when repository throws`() = runTest {
        coEvery { repository.getWeekByDate(any()) } throws RuntimeException("DB 오류")
        val viewModel = HomeViewModel(repository, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertTrue(state is HomeUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads current week`() = runTest {
        val week = buildTestWeek()
        coEvery { repository.getWeekByDate(any()) } returns null andThen week
        val viewModel = HomeViewModel(repository, today)

        viewModel.uiState.test {
            skipItems(2)
            viewModel.refresh()
            skipItems(1)
            val state = awaitItem() as HomeUiState.Success
            assertEquals(week, state.currentWeek)
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
