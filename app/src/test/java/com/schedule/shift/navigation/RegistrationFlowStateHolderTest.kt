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
import org.junit.Assert.assertNotNull
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
    fun `initial pendingAction is None`() {
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    @Test
    fun `handleImageSelected emits GoToConfirmation on success when skip is false`() = runTest {
        val bitmap = mockk<Bitmap>()
        val weeks = listOf(buildTestWeek())
        coEvery { preferences.isSkipConfirm() } returns false
        coEvery { processImage(bitmap) } returns ParseResult.Success(weeks)
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher, processImage)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.handleImageSelected(bitmap, "content://test/image")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FlowPendingAction.GoToConfirmation, holder.pendingAction.value)
        assertEquals(weeks, holder.pendingWeeks)
        assertEquals("content://test/image", holder.pendingImageUri)
        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `handleImageSelected saves directly when skip is true and no conflict`() = runTest {
        val bitmap = mockk<Bitmap>()
        val weeks = listOf(buildTestWeek())
        coEvery { preferences.isSkipConfirm() } returns true
        coEvery { processImage(bitmap) } returns ParseResult.Success(weeks)
        coEvery { scheduleRepository.getWeekByDate(any()) } returns null
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher, processImage)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(holder.homeRefreshNeeded.value)
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    @Test
    fun `handleImageSelected goes to confirmation when skip is true but conflict exists`() = runTest {
        val bitmap = mockk<Bitmap>()
        val week = buildTestWeek()
        coEvery { preferences.isSkipConfirm() } returns true
        coEvery { processImage(bitmap) } returns ParseResult.Success(listOf(week))
        coEvery { scheduleRepository.getWeekByDate(week.weekStartDate) } returns week
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher, processImage)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(FlowPendingAction.GoToConfirmation, holder.pendingAction.value)
        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `handleImageSelected sets imageErrorMessage on NOT_A_SCHEDULE failure`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Failure(FailureReason.NOT_A_SCHEDULE)

        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(holder.imageErrorMessage.value)
        assertFalse(holder.homeRefreshNeeded.value)
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
    }

    @Test
    fun `handleImageSelected sets imageErrorMessage on PARSE_ERROR failure`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)

        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(holder.imageErrorMessage.value)
    }

    @Test
    fun `clearImageError clears imageErrorMessage`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)
        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.clearImageError()

        assertNull(holder.imageErrorMessage.value)
    }

    @Test
    fun `clearHomeRefresh resets homeRefreshNeeded`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { preferences.isSkipConfirm() } returns true
        coEvery { processImage(bitmap) } returns ParseResult.Success(listOf(buildTestWeek()))
        coEvery { scheduleRepository.getWeekByDate(any()) } returns null
        holder = RegistrationFlowStateHolder(preferences, scheduleRepository, widgetRefresher, processImage)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()
        holder.clearHomeRefresh()

        assertFalse(holder.homeRefreshNeeded.value)
    }

    @Test
    fun `clear resets all state`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Failure(FailureReason.PARSE_ERROR)
        holder.handleImageSelected(bitmap, null)
        testDispatcher.scheduler.advanceUntilIdle()

        holder.clear()

        assertTrue(holder.pendingWeeks.isEmpty())
        assertNull(holder.pendingImageUri)
        assertEquals(FlowPendingAction.None, holder.pendingAction.value)
        assertFalse(holder.homeRefreshNeeded.value)
        assertNull(holder.imageErrorMessage.value)
    }

    @Test
    fun `resetAction resets pendingAction to None`() = runTest {
        val bitmap = mockk<Bitmap>()
        coEvery { processImage(bitmap) } returns ParseResult.Success(listOf(buildTestWeek()))
        holder.handleImageSelected(bitmap, null)
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
