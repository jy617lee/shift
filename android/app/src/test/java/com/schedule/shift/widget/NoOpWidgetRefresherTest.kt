package com.schedule.shift.widget

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NoOpWidgetRefresherTest {

    private val refresher = NoOpWidgetRefresher()

    @Test
    fun `refreshAll completes without error`() = runTest {
        refresher.refreshAll()
    }

    @Test
    fun `refreshAll can be called multiple times`() = runTest {
        refresher.refreshAll()
        refresher.refreshAll()
        refresher.refreshAll()
    }

    @Test
    fun `NoOpWidgetRefresher instance is non-null`() {
        assertNotNull(refresher)
    }

    @Test
    fun `NoOpWidgetRefresher implements WidgetRefresher`() {
        assertNotNull(refresher as com.schedule.shift.domain.widget.WidgetRefresher)
    }

    @Test
    fun `refreshAll is a suspend function that returns Unit`() = runTest {
        val result = refresher.refreshAll()
        assertEquals(Unit, result)
    }

    @Test
    fun `concurrent refreshAll calls do not throw`() = runTest {
        repeat(5) { refresher.refreshAll() }
    }
}
