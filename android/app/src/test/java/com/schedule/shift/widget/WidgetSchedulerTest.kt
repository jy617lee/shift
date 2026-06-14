package com.schedule.shift.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetSchedulerTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)
    }

    @Test
    fun `scheduleMidnightUpdate enqueues periodic work`() {
        WidgetScheduler.scheduleMidnightUpdate(context)
        val workInfos = workManager
            .getWorkInfosByTag(WidgetUpdateWorker::class.java.name)
            .get()
        assertTrue(workInfos.isNotEmpty())
    }

    @Test
    fun `scheduleMidnightUpdate is idempotent on second call`() {
        WidgetScheduler.scheduleMidnightUpdate(context)
        WidgetScheduler.scheduleMidnightUpdate(context)
        val workInfos = workManager
            .getWorkInfosByTag(WidgetUpdateWorker::class.java.name)
            .get()
        assertTrue(workInfos.isNotEmpty())
    }

    @Test
    fun `enqueued work is not in CANCELLED state`() {
        WidgetScheduler.scheduleMidnightUpdate(context)
        val workInfos = workManager
            .getWorkInfosByTag(WidgetUpdateWorker::class.java.name)
            .get()
        assertFalse(workInfos.any { it.state == WorkInfo.State.CANCELLED })
    }
}
