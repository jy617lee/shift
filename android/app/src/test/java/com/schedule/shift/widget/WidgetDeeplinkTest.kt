package com.schedule.shift.widget

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetDeeplinkTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `widgetIntent for 2x1 has correct source extra`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_2X1)
        assertEquals(SOURCE_WIDGET_2X1, intent.getStringExtra(EXTRA_WIDGET_SOURCE))
    }

    @Test
    fun `widgetIntent has FLAG_ACTIVITY_NEW_TASK`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_2X1)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `widgetIntent has FLAG_ACTIVITY_CLEAR_TOP`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_2X1)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `EXTRA_WIDGET_SOURCE constant is widget_source`() {
        assertEquals("widget_source", EXTRA_WIDGET_SOURCE)
    }

    @Test
    fun `SOURCE_WIDGET_2X1 constant is widget_2x1`() {
        assertEquals("widget_2x1", SOURCE_WIDGET_2X1)
    }

    @Test
    fun `widgetIntent returns non-null intent`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_2X1)
        assertNotNull(intent)
    }

    @Test
    fun `widgetIntent for custom source stores extra correctly`() {
        val customSource = "widget_custom"
        val intent = widgetIntent(context, customSource)
        assertEquals(customSource, intent.getStringExtra(EXTRA_WIDGET_SOURCE))
    }

    @Test
    fun `all widget source constants start with widget prefix`() {
        listOf(SOURCE_WIDGET_2X1, SOURCE_WIDGET_4X1, SOURCE_WIDGET_4X2_WEEKLY).forEach { source ->
            assertTrue("$source should start with widget_", source.startsWith("widget_"))
        }
    }

    @Test
    fun `all widget source constants are distinct`() {
        val sources = setOf(
            SOURCE_WIDGET_2X1,
            SOURCE_WIDGET_4X1,
            SOURCE_WIDGET_4X2_WEEKLY,
        )
        assertEquals(3, sources.size)
    }
}
