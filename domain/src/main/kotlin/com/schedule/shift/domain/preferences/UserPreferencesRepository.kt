package com.schedule.shift.domain.preferences

interface UserPreferencesRepository {
    suspend fun isSkipConfirm(): Boolean
    suspend fun setSkipConfirm(value: Boolean)
}
