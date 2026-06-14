package com.schedule.shift.navigation

import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.widget.WidgetRefresher
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationFlowStateHolderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferences: UserPreferencesRepository
    private lateinit var scheduleRepository: ScheduleRepository
    private lateinit var widgetRefresher: WidgetRefresher
    private lateinit var holder: RegistrationFlowStateHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = mockk(relaxed = true)
        scheduleRepository = mockk(relaxed = true)
        widgetRefresher = mockk(relaxed = true)
        coEvery { preferences.isSkipConfirm() } returns false
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has empty weeks and null imageUri`() {
        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
    }

    @Test
    fun `initial pendingAction is None`() = runTest {
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    @Test
    fun `handleParsed emits GoToConfirmation when skip is disabled`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns false
        val weeks = listOf(buildTestWeek())

        holder.handleParsed(weeks, "content://test/image")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FlowPendingAction.GoToConfirmation, holder.pendingAction.value)
        assertEquals(weeks, holder.pendingWeeks)
        assertEquals("content://test/image", holder.pendingImageUri)
    }

    @Test
    fun `handleParsed emits SavedDirectly when skip is enabled`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns true
        coEvery { scheduleRepository.getWeekByDate(any()) } returns null
        val weeks = listOf(buildTestWeek())

        holder.handleParsed(weeks, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FlowPendingAction.SavedDirectly, holder.pendingAction.value)
    }

    @Test
    fun `clear resets all state`() = runTest {
        val weeks = listOf(buildTestWeek())
        holder.handleParsed(weeks, "content://test/image")
        testDispatcher.scheduler.advanceUntilIdle()

        holder.clear()

        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    @Test
    fun `resetAction resets pendingAction to None`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns false
        holder.handleParsed(listOf(buildTestWeek()), null)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.resetAction()

        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    private fun buildTestWeek(): ScheduleWeek {
        val monday = LocalDate.of(2026, 6, 8)
        return ScheduleWeek(
            weekStartDate = monday,
            days = (0..6).map { offset ->
                ScheduleDay(
                    date = monday.plusDays(offset.toLong()),
                    type = if (offset < 5) DayType.WORK else DayType.OFF,
                    startTime = null,
                    endTime = null,
                    codeLabel = "정상",
                    source = SourceType.PARSED,
                )
            },
        )
    }
}
