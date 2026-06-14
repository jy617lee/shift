package com.schedule.shift.analytics

import android.content.Context
import com.schedule.shift.domain.analytics.AnonymousIdProvider
import java.util.UUID

class SharedPrefsAnonymousIdProvider(context: Context) : AnonymousIdProvider {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getAnonymousId(): String {
        val existing = prefs.getString(KEY_ANONYMOUS_ID, null)
        if (existing != null) return existing
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_ANONYMOUS_ID, newId).apply()
        return newId
    }

    companion object {
        private const val PREFS_NAME = "shift_analytics"
        private const val KEY_ANONYMOUS_ID = "anonymous_id"
    }
}
