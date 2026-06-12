package com.schedule.shift.navigation

import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.repository.SettingsRepository
import com.schedule.shift.domain.widget.WidgetRefresher
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationFlowStateHolderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scheduleRepo: ScheduleRepository
    private lateinit var widgetRefresher: WidgetRefresher
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var holder: RegistrationFlowStateHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val settingsRepo = mockk<SettingsRepository>()
        every { settingsRepo.skipConfirm() } returns flowOf(false)
        scheduleRepo = mockk(relaxed = true)
        widgetRefresher = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
        holder = RegistrationFlowStateHolder(
            settingsRepository = settingsRepo,
            scheduleRepository = scheduleRepo,
            widgetRefresher = widgetRefresher,
            analyticsTracker = analyticsTracker,
        )
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
    fun `initial session state is blank`() {
        assertEquals("", holder.pendingSessionId)
        assertEquals(0L, holder.pendingSessionStartMs)
        assertFalse(holder.pendingReplace)
    }

    @Test
    fun `setSession stores sessionId and startMs`() {
        holder.setSession("abc-123", 1_000L)
        assertEquals("abc-123", holder.pendingSessionId)
        assertEquals(1_000L, holder.pendingSessionStartMs)
    }

    @Test
    fun `setReplace stores replace flag`() {
        holder.setReplace(true)
        assertTrue(holder.pendingReplace)
        holder.setReplace(false)
        assertFalse(holder.pendingReplace)
    }

    @Test
    fun `clear resets session fields`() {
        holder.setSession("abc-123", 9_999L)
        holder.setReplace(true)
        holder.clear()
        assertEquals("", holder.pendingSessionId)
        assertEquals(0L, holder.pendingSessionStartMs)
        assertFalse(holder.pendingReplace)
    }

    @Test
    fun `setSession overwrites previous session`() {
        holder.setSession("first", 100L)
        holder.setSession("second", 200L)
        assertEquals("second", holder.pendingSessionId)
        assertEquals(200L, holder.pendingSessionStartMs)
    }

    @Test
    fun `setPendingWeeks stores weeks`() {
        val weeks = listOf(buildTestWeek())
        holder.setPendingWeeks(weeks)
        assertEquals(weeks, holder.pendingWeeks)
    }

    @Test
    fun `setPendingImageUri stores uri`() {
        holder.setPendingImageUri("content://test/image")
        assertEquals("content://test/image", holder.pendingImageUri)
    }

    @Test
    fun `clear resets all fields`() {
        holder.setPendingWeeks(listOf(buildTestWeek()))
        holder.setPendingImageUri("content://test/image")
        holder.setSession("sid", 100L)
        holder.setReplace(true)
        holder.clear()
        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
        assertEquals("", holder.pendingSessionId)
        assertEquals(0L, holder.pendingSessionStartMs)
        assertFalse(holder.pendingReplace)
    }

    @Test
    fun `setPendingImageUri with null clears uri`() {
        holder.setPendingImageUri("content://test/image")
        holder.setPendingImageUri(null)
        assertNull(holder.pendingImageUri)
    }

    @Test
    fun `setPendingWeeks with empty list clears weeks`() {
        holder.setPendingWeeks(listOf(buildTestWeek()))
        holder.setPendingWeeks(emptyList())
        assertTrue(holder.pendingWeeks.isEmpty())
    }

    @Test
    fun `setPendingWeeks replaces previous value`() {
        val week1 = buildTestWeek()
        val week2 = buildTestWeek()
        holder.setPendingWeeks(listOf(week1))
        holder.setPendingWeeks(listOf(week2))
        assertEquals(listOf(week2), holder.pendingWeeks)
    }

    @Test
    fun `multiple clears are idempotent`() {
        holder.setPendingWeeks(listOf(buildTestWeek()))
        holder.clear()
        holder.clear()
        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
    }

    @Test
    fun `autoSave saves each week to repository`() = runTest {
        val weeks = listOf(buildTestWeek(), buildTestWeek())
        holder.setSession("sid", 0L)
        holder.autoSave(weeks)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = weeks.size) { scheduleRepo.saveWeek(any()) }
    }

    @Test
    fun `autoSave refreshes widgets`() = runTest {
        holder.setSession("sid", 0L)
        holder.autoSave(listOf(buildTestWeek()))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { widgetRefresher.refreshAll() }
    }

    @Test
    fun `autoSave tracks RegisterComplete event`() = runTest {
        holder.setSession("test-session", 0L)
        holder.autoSave(listOf(buildTestWeek()))
        testDispatcher.scheduler.advanceUntilIdle()
        verify { analyticsTracker.track(match { it is AnalyticsEvent.RegisterComplete }) }
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
