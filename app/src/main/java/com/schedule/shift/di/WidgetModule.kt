package com.schedule.shift.di

import com.schedule.shift.domain.widget.WidgetRefresher
import com.schedule.shift.widget.NoOpWidgetRefresher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WidgetModule {

    @Provides
    @Singleton
    fun provideWidgetRefresher(): WidgetRefresher =
        NoOpWidgetRefresher()
}
