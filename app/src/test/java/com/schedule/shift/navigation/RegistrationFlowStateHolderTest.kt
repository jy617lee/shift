package com.schedule.shift.navigation

import android.graphics.Bitmap
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import com.schedule.shift.domain.parser.FailureReason
import com.schedule.shift.domain.parser.ParseResult
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import com.schedule.shift.domain.repository.ScheduleRepository
import com.schedule.shift.domain.usecase.ProcessScheduleImageUseCase
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
import org.junit.Assert.assertFalse
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
    private lateinit var processImage: ProcessScheduleImageUseCase
    private lateinit var holder: RegistrationFlowStateHolder

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = mockk(relaxed = true)
        scheduleRepository = mockk(relaxed = true)
        widgetRefresher = mockk(relaxed = true)
        processImage = mockk(relaxed = true)
        coEvery { preferences.isSkipConfirm() } returns false
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher, processImage)
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
    fun `handleParsed stores weeks and emits GoToConfirmation`() = runTest {
        val weeks = listOf(buildTestWeek())
        holder.handleParsed(weeks, "content://test/image")

        assertEquals(FlowPendingAction.GoToConfirmation, holder.pendingAction.value)
        assertEquals(weeks, holder.pendingWeeks)
        assertEquals("content://test/image", holder.pendingImageUri)
    }

    @Test
    fun `startSkipSave sets homeRefreshNeeded on OCR success`() = runTest {
        val bitmap = mockk<Bitmap>()
        val weeks = listOf(buildTestWeek())
        coEvery { processImage(bitmap) } returns ParseResult.Success(weeks)
        coEvery { scheduleRepository.getWeekByDate(any()) } returns null

        holder.startSkipSave(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `startSkipSave does not set homeRefreshNeeded on OCR failure`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)

        holder.startSkipSave(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `clearHomeRefresh resets homeRefreshNeeded`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Success(listOf(buildTestWeek()))
        coEvery { scheduleRepository.getWeekByDate(any()) } returns null

        holder.startSkipSave(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()
        holder.clearHomeRefresh()

        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `clear resets all state`() = runTest {
        val weeks = listOf(buildTestWeek())
        holder.handleParsed(weeks, "content://test/image")
        holder.clear()

        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `resetAction resets pendingAction to None`() = runTest {
        holder.handleParsed(listOf(buildTestWeek()), null)
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
