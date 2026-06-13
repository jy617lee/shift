package com.schedule.shift.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class DataStoreUserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override suspend fun isSkipConfirm(): Boolean =
        dataStore.data.map { it[SKIP_CONFIRM_KEY] ?: false }.first()

    override suspend fun setSkipConfirm(value: Boolean) {
        dataStore.edit { it[SKIP_CONFIRM_KEY] = value }
    }

    companion object {
        private val SKIP_CONFIRM_KEY = booleanPreferencesKey("skip_confirm")
    }
}
