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
class ShiftWidget4x2CountdownTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ── Source constant ───────────────────────────────────────────────────────

    @Test
    fun `SOURCE_WIDGET_4X2_COUNTDOWN equals widget_4x2_countdown`() {
        assertEquals("widget_4x2_countdown", SOURCE_WIDGET_4X2_COUNTDOWN)
    }

    @Test
    fun `SOURCE_WIDGET_4X2_COUNTDOWN starts with widget prefix`() {
        assertTrue(SOURCE_WIDGET_4X2_COUNTDOWN.startsWith("widget_"))
    }

    @Test
    fun `SOURCE_WIDGET_4X2_COUNTDOWN is distinct from other widget sources`() {
        val sources = setOf(
            SOURCE_WIDGET_2X1,
            SOURCE_WIDGET_4X1,
            SOURCE_WIDGET_4X2_COUNTDOWN,
        )
        assertEquals(3, sources.size)
    }

    // ── Widget instantiation ──────────────────────────────────────────────────

    @Test
    fun `ShiftWidget4x2Countdown can be instantiated`() {
        assertNotNull(ShiftWidget4x2Countdown())
    }

    @Test
    fun `ShiftWidget4x2CountdownReceiver can be instantiated`() {
        assertNotNull(ShiftWidget4x2CountdownReceiver())
    }

    @Test
    fun `ShiftWidget4x2CountdownReceiver glanceAppWidget is ShiftWidget4x2Countdown`() {
        val receiver = ShiftWidget4x2CountdownReceiver()
        assertNotNull(receiver.glanceAppWidget)
        assertTrue(receiver.glanceAppWidget is ShiftWidget4x2Countdown)
    }

    // ── ACTION_SECOND_UPDATE constant ────────────────────────────────────────

    @Test
    fun `ACTION_SECOND_UPDATE contains package name prefix`() {
        assertTrue(
            ShiftWidget4x2CountdownReceiver.ACTION_SECOND_UPDATE.startsWith(
                "com.schedule.shift.widget",
            ),
        )
    }

    @Test
    fun `ACTION_SECOND_UPDATE ends with ACTION_SECOND_UPDATE suffix`() {
        assertTrue(
            ShiftWidget4x2CountdownReceiver.ACTION_SECOND_UPDATE.endsWith("ACTION_SECOND_UPDATE"),
        )
    }

    // ── widgetIntent ──────────────────────────────────────────────────────────

    @Test
    fun `widgetIntent for countdown has correct source extra`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_COUNTDOWN)
        assertEquals(SOURCE_WIDGET_4X2_COUNTDOWN, intent.getStringExtra(EXTRA_WIDGET_SOURCE))
    }

    @Test
    fun `widgetIntent for countdown has FLAG_ACTIVITY_NEW_TASK`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_COUNTDOWN)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `widgetIntent for countdown has FLAG_ACTIVITY_CLEAR_TOP`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_COUNTDOWN)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    // ── formatDuration (seconds) ──────────────────────────────────────────────

    @Test
    fun `formatDuration 0 seconds returns 0초`() {
        assertEquals("0초", formatDuration(0))
    }

    @Test
    fun `formatDuration 1 second returns 1초`() {
        assertEquals("1초", formatDuration(1))
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
    fun `formatDuration 61 seconds returns 1분 1초`() {
        assertEquals("1분 1초", formatDuration(61))
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
    fun `formatDuration 5400 seconds returns 1시간 30분 0초`() {
        assertEquals("1시간 30분 0초", formatDuration(5_400))
    }

    @Test
    fun `formatDuration 28800 seconds returns 8시간 0분 0초`() {
        assertEquals("8시간 0분 0초", formatDuration(28_800))
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

    @Test
    fun `formatDuration includes 시간 when 3600 or more seconds`() {
        val result = formatDuration(3_600)
        assertTrue("Should contain 시간 for 3600+ second duration", result.contains("시간"))
    }
}
