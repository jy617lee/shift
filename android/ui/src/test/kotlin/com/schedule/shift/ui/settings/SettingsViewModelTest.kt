package com.schedule.shift.ui.settings

import app.cash.turbine.test
import com.schedule.shift.domain.preferences.UserPreferencesRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var preferences: UserPreferencesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        preferences = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `skipConfirm starts as false then loads persisted value`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns true

        val vm = SettingsViewModel(preferences)
        vm.skipConfirm.test {
            assertFalse(awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSkipConfirm true persists and updates state`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns false
        coEvery { preferences.setSkipConfirm(any()) } returns Unit

        val vm = SettingsViewModel(preferences)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.skipConfirm.test {
            assertFalse(awaitItem())
            vm.setSkipConfirm(true)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify { preferences.setSkipConfirm(true) }
    }

    @Test
    fun `setSkipConfirm false updates state to false`() = runTest {
        coEvery { preferences.isSkipConfirm() } returns true
        coEvery { preferences.setSkipConfirm(any()) } returns Unit

        val vm = SettingsViewModel(preferences)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.setSkipConfirm(false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.skipConfirm.value)
        coVerify { preferences.setSkipConfirm(false) }
    }
}
