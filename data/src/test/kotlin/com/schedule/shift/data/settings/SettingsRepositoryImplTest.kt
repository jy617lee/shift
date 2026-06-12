package com.schedule.shift.data.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryImplTest {

    @get:Rule
    val tmpFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `skipConfirm defaults to false`() = runTest(testDispatcher) {
        val repo = SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmpFolder.newFile("prefs_1.preferences_pb") },
            ),
        )
        repo.skipConfirm().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSkipConfirm true updates flow`() = runTest(testDispatcher) {
        val repo = SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmpFolder.newFile("prefs_2.preferences_pb") },
            ),
        )
        repo.setSkipConfirm(true)
        repo.skipConfirm().test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setSkipConfirm false after true updates flow`() = runTest(testDispatcher) {
        val repo = SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmpFolder.newFile("prefs_3.preferences_pb") },
            ),
        )
        repo.setSkipConfirm(true)
        repo.setSkipConfirm(false)
        repo.skipConfirm().test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple toggles emit correct values`() = runTest(testDispatcher) {
        val repo = SettingsRepositoryImpl(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { tmpFolder.newFile("prefs_4.preferences_pb") },
            ),
        )
        repo.skipConfirm().test {
            assertFalse(awaitItem())
            repo.setSkipConfirm(true)
            assertTrue(awaitItem())
            repo.setSkipConfirm(false)
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
