package com.schedule.shift.ui.settings

import app.cash.turbine.test
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.repository.SettingsRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: SettingsRepository
    private lateinit var analyticsTracker: AnalyticsTracker
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        analyticsTracker = mockk(relaxed = true)
        every { repository.skipConfirm() } returns flowOf(false)
        viewModel = SettingsViewModel(repository, analyticsTracker)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `skipConfirm defaults to false`() = runTest {
        viewModel.skipConfirm.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSkipConfirm persists value`() = runTest {
        viewModel.setSkipConfirm(true)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { repository.setSkipConfirm(true) }
    }

    @Test
    fun `setSkipConfirm tracks SettingChanged event`() = runTest {
        viewModel.setSkipConfirm(true)
        testDispatcher.scheduler.advanceUntilIdle()
        verify { analyticsTracker.track(match { it is AnalyticsEvent.SettingChanged }) }
    }

    @Test
    fun `setSkipConfirm false tracks event with false value`() = runTest {
        viewModel.setSkipConfirm(false)
        testDispatcher.scheduler.advanceUntilIdle()
        verify {
            analyticsTracker.track(
                match { it is AnalyticsEvent.SettingChanged && it.value == "false" },
            )
        }
    }

    @Test
    fun `skipConfirm emits true when repository returns true`() = runTest {
        every { repository.skipConfirm() } returns flowOf(true)
        val vm = SettingsViewModel(repository, analyticsTracker)
        vm.skipConfirm.test {
            // stateIn(WhileSubscribed) emits initial false, then upstream value
            val first = awaitItem()
            val settled = if (!first) awaitItem() else first
            assertTrue(settled)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
