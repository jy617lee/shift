package com.schedule.shift.ui.home

import app.cash.turbine.test
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.repository.ScheduleRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScheduleRepository
    private lateinit var tracker: AnalyticsTracker
    private val today = LocalDate.of(2026, 6, 12)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        tracker = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.uiState.test {
            val first = awaitItem()
            assertTrue(first is HomeUiState.Loading || first is HomeUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with empty list when no schedule exists`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertTrue(state is HomeUiState.Success)
            assertTrue((state as HomeUiState.Success).weeks.isEmpty())
            assertEquals(today, state.today)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Success with all weeks sorted by date`() = runTest {
        val week1 = buildTestWeek(LocalDate.of(2026, 6, 8))
        val week2 = buildTestWeek(LocalDate.of(2026, 6, 22))
        coEvery { repository.getAllWeeks() } returns listOf(week2, week1)
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as HomeUiState.Success
            assertEquals(2, state.weeks.size)
            assertEquals(week1, state.weeks[0])
            assertEquals(week2, state.weeks[1])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits Error when repository throws`() = runTest {
        coEvery { repository.getAllWeeks() } throws RuntimeException("DB 오류")
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem()
            assertTrue(state is HomeUiState.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads all weeks`() = runTest {
        val week = buildTestWeek()
        coEvery { repository.getAllWeeks() } returns emptyList() andThen listOf(week)
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.uiState.test {
            skipItems(2)
            viewModel.refresh()
            skipItems(1)
            val state = awaitItem() as HomeUiState.Success
            assertEquals(listOf(week), state.weeks)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onWeekViewed fires HomeWeekViewed event with correct offset`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)
        val eventSlot = slot<AnalyticsEvent>()

        val currentWeekStart = LocalDate.of(2026, 6, 8)
        viewModel.onWeekViewed(currentWeekStart)

        verify { tracker.track(capture(eventSlot)) }
        val event = eventSlot.captured as AnalyticsEvent.HomeWeekViewed
        assertEquals(0, event.offset)
    }

    @Test
    fun `onWeekViewed offset minus1 for previous week`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)

        val prevWeekStart = LocalDate.of(2026, 6, 1)
        viewModel.onWeekViewed(prevWeekStart)

        verify { tracker.track(AnalyticsEvent.HomeWeekViewed(-1)) }
    }

    @Test
    fun `onWeekViewed same offset not fired twice consecutively`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)

        val weekStart = LocalDate.of(2026, 6, 8)
        viewModel.onWeekViewed(weekStart)
        viewModel.onWeekViewed(weekStart)

        verify(exactly = 1) { tracker.track(AnalyticsEvent.HomeWeekViewed(0)) }
    }

    @Test
    fun `onWeekViewed different offsets each fire once`() = runTest {
        coEvery { repository.getAllWeeks() } returns emptyList()
        val viewModel = HomeViewModel(repository, tracker, today)

        viewModel.onWeekViewed(LocalDate.of(2026, 6, 8))
        viewModel.onWeekViewed(LocalDate.of(2026, 6, 15))

        verify(exactly = 1) { tracker.track(AnalyticsEvent.HomeWeekViewed(0)) }
        verify(exactly = 1) { tracker.track(AnalyticsEvent.HomeWeekViewed(1)) }
    }

    private fun buildTestWeek(monday: LocalDate = LocalDate.of(2026, 6, 8)) = ScheduleWeek(
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
