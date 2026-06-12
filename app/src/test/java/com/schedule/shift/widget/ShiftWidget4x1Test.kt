package com.schedule.shift.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShiftWidget4x1Test {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `SOURCE_WIDGET_4X1 constant equals widget_4x1`() {
        assertEquals("widget_4x1", SOURCE_WIDGET_4X1)
    }

    @Test
    fun `SOURCE_WIDGET_4X1 is distinct from SOURCE_WIDGET_2X1`() {
        assertTrue(SOURCE_WIDGET_4X1 != SOURCE_WIDGET_2X1)
    }

    @Test
    fun `ShiftWidget4x1 can be instantiated`() {
        assertNotNull(ShiftWidget4x1())
    }

    @Test
    fun `ShiftWidget4x1Receiver glanceAppWidget is ShiftWidget4x1`() {
        val receiver = ShiftWidget4x1Receiver()
        assertNotNull(receiver.glanceAppWidget)
        assertTrue(receiver.glanceAppWidget is ShiftWidget4x1)
    }

    @Test
    fun `SOURCE_WIDGET_4X1 starts with widget prefix`() {
        assertTrue(SOURCE_WIDGET_4X1.startsWith("widget_"))
    }

    @Test
    fun `widgetIntent for 4x1 has correct source extra`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X1)
        assertEquals(SOURCE_WIDGET_4X1, intent.getStringExtra(EXTRA_WIDGET_SOURCE))
    }

    @Test
    fun `widgetIntent for 4x1 has FLAG_ACTIVITY_NEW_TASK`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X1)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `widgetIntent for 4x1 has FLAG_ACTIVITY_CLEAR_TOP`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X1)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `widgetIntent for 4x1 returns non-null intent`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X1)
        assertNotNull(intent)
    }

    @Test
    fun `SOURCE_WIDGET_4X1 contains 4x1 dimension hint`() {
        assertTrue(SOURCE_WIDGET_4X1.contains("4x1"))
    }

    @Test
    fun `ShiftWidget4x1Receiver is non-null`() {
        assertNotNull(ShiftWidget4x1Receiver())
    }

    @Test
    fun `ACTION_SECOND_UPDATE_4X1 contains package name prefix`() {
        assertTrue(
            ShiftWidget4x1Receiver.ACTION_SECOND_UPDATE_4X1.startsWith("com.schedule.shift.widget"),
        )
    }

    @Test
    fun `ACTION_SECOND_UPDATE_4X1 ends with correct suffix`() {
        assertTrue(
            ShiftWidget4x1Receiver.ACTION_SECOND_UPDATE_4X1.endsWith("ACTION_SECOND_UPDATE_4X1"),
        )
    }

    // ── formatDuration (seconds) ──────────────────────────────────────────────

    @Test
    fun `formatDuration 0 seconds returns 0초`() {
        assertEquals("0초", formatDuration(0))
    }

    @Test
    fun `formatDuration 59 seconds returns 59초`() {
        assertEquals("59초", formatDuration(59))
    }

    @Test
    fun `formatDuration 60 seconds returns 1분 0초`() {
        assertEquals("1분 0초", formatDuration(60))
    }

    @Test
    fun `formatDuration 90 seconds returns 1분 30초`() {
        assertEquals("1분 30초", formatDuration(90))
    }

    @Test
    fun `formatDuration 3600 seconds returns 1시간 0분 0초`() {
        assertEquals("1시간 0분 0초", formatDuration(3_600))
    }

    @Test
    fun `formatDuration 3661 seconds returns 1시간 1분 1초`() {
        assertEquals("1시간 1분 1초", formatDuration(3_661))
    }

    @Test
    fun `formatDuration omits hours when under 3600 seconds`() {
        val result = formatDuration(3_599)
        assertTrue("Should not contain 시간 for sub-hour duration", !result.contains("시간"))
    }

    @Test
    fun `formatDuration omits minutes and hours when under 60 seconds`() {
        val result = formatDuration(45)
        assertTrue("Should not contain 분 for sub-minute duration", !result.contains("분"))
        assertTrue("Should not contain 시간 for sub-minute duration", !result.contains("시간"))
    }
}
