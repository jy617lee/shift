package com.schedule.shift.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.schedule.shift.analytics.FirebaseAnalyticsTracker
import com.schedule.shift.analytics.SharedPrefsAnonymousIdProvider
import com.schedule.shift.domain.analytics.AnonymousIdProvider
import com.schedule.shift.domain.analytics.AnalyticsTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        @ApplicationContext context: Context,
        anonymousIdProvider: AnonymousIdProvider,
    ): AnalyticsTracker =
        FirebaseAnalyticsTracker(
            FirebaseAnalytics.getInstance(context),
            anonymousIdProvider,
        )

    @Provides
    @Singleton
    fun provideAnonymousIdProvider(@ApplicationContext context: Context): AnonymousIdProvider =
        SharedPrefsAnonymousIdProvider(context)
}
