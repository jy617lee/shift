package com.schedule.shift.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataStoreUserPreferencesRepositoryTest {

    private val dataStore: DataStore<Preferences> = mockk()
    private lateinit var repo: DataStoreUserPreferencesRepository

    @Before
    fun setUp() {
        repo = DataStoreUserPreferencesRepository(dataStore)
    }

    @Test
    fun `isSkipConfirm returns false when key absent`() = runTest {
        coEvery { dataStore.data } returns flowOf(mutablePreferencesOf())
        assertFalse(repo.isSkipConfirm())
    }

    @Test
    fun `isSkipConfirm returns true when key set to true`() = runTest {
        val prefs = mutablePreferencesOf(booleanPreferencesKey("skip_confirm") to true)
        coEvery { dataStore.data } returns flowOf(prefs)
        assertTrue(repo.isSkipConfirm())
    }

    @Test
    fun `setSkipConfirm calls updateData on dataStore`() = runTest {
        coEvery { dataStore.updateData(any()) } returns mutablePreferencesOf()
        repo.setSkipConfirm(true)
        coVerify { dataStore.updateData(any()) }
    }
}
