package com.schedule.shift.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.schedule.shift.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl(private val dataStore: DataStore<Preferences>) : SettingsRepository {

    private val skipConfirmKey = booleanPreferencesKey(KEY_SKIP_CONFIRM)

    override fun skipConfirm(): Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[skipConfirmKey] ?: false }

    override suspend fun setSkipConfirm(skip: Boolean) {
        dataStore.edit { prefs -> prefs[skipConfirmKey] = skip }
    }

    companion object {
        private const val KEY_SKIP_CONFIRM = "skip_confirm"
    }
}
