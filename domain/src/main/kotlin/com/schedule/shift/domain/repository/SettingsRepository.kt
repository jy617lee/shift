package com.schedule.shift.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun skipConfirm(): Flow<Boolean>
    suspend fun setSkipConfirm(skip: Boolean)
}
