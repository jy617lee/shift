package com.schedule.shift.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.schedule.shift.data.preferences.DataStoreUserPreferencesRepository
import com.schedule.shift.domain.preferences.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun provideUserPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.userPrefsDataStore

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(
        dataStore: DataStore<Preferences>,
    ): UserPreferencesRepository =
        DataStoreUserPreferencesRepository(dataStore)
}
