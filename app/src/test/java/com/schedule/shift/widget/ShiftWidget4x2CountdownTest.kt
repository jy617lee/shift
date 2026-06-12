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
            SOURCE_WIDGET_2X2,
            SOURCE_WIDGET_4X1,
            SOURCE_WIDGET_4X2_COUNTDOWN,
        )
        assertEquals(4, sources.size)
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

    // ── ACTION_MINUTE_UPDATE constant ────────────────────────────────────────

    @Test
    fun `ACTION_MINUTE_UPDATE contains package name prefix`() {
        assertTrue(
            ShiftWidget4x2CountdownReceiver.ACTION_MINUTE_UPDATE.startsWith(
                "com.schedule.shift.widget",
            ),
        )
    }

    @Test
    fun `ACTION_MINUTE_UPDATE ends with ACTION_MINUTE_UPDATE suffix`() {
        assertTrue(
            ShiftWidget4x2CountdownReceiver.ACTION_MINUTE_UPDATE.endsWith("ACTION_MINUTE_UPDATE"),
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

    // ── formatDuration ────────────────────────────────────────────────────────

    @Test
    fun `formatDuration 0 minutes returns 0분`() {
        assertEquals("0분", formatDuration(0))
    }

    @Test
    fun `formatDuration 1 minute returns 1분`() {
        assertEquals("1분", formatDuration(1))
    }

    @Test
    fun `formatDuration 59 minutes returns 59분`() {
        assertEquals("59분", formatDuration(59))
    }

    @Test
    fun `formatDuration 60 minutes returns 1시간 0분`() {
        assertEquals("1시간 0분", formatDuration(60))
    }

    @Test
    fun `formatDuration 61 minutes returns 1시간 1분`() {
        assertEquals("1시간 1분", formatDuration(61))
    }

    @Test
    fun `formatDuration 90 minutes returns 1시간 30분`() {
        assertEquals("1시간 30분", formatDuration(90))
    }

    @Test
    fun `formatDuration 120 minutes returns 2시간 0분`() {
        assertEquals("2시간 0분", formatDuration(120))
    }

    @Test
    fun `formatDuration 480 minutes returns 8시간 0분`() {
        assertEquals("8시간 0분", formatDuration(480))
    }

    @Test
    fun `formatDuration 545 minutes returns 9시간 5분`() {
        assertEquals("9시간 5분", formatDuration(545))
    }

    @Test
    fun `formatDuration omits hours when under 60 minutes`() {
        val result = formatDuration(45)
        assertTrue("Should not contain 시간 for sub-hour duration", !result.contains("시간"))
    }

    @Test
    fun `formatDuration includes 시간 when 60 or more minutes`() {
        val result = formatDuration(60)
        assertTrue("Should contain 시간 for 60+ minute duration", result.contains("시간"))
    }
}
