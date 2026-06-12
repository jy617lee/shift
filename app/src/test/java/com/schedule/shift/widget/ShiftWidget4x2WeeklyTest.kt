package com.schedule.shift.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.schedule.shift.domain.model.DayType
import com.schedule.shift.domain.model.ScheduleDay
import com.schedule.shift.domain.model.ScheduleWeek
import com.schedule.shift.domain.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ShiftWidget4x2WeeklyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // ── Source constant ───────────────────────────────────────────────────────

    @Test
    fun `SOURCE_WIDGET_4X2_WEEKLY equals widget_4x2_weekly`() {
        assertEquals("widget_4x2_weekly", SOURCE_WIDGET_4X2_WEEKLY)
    }

    @Test
    fun `SOURCE_WIDGET_4X2_WEEKLY starts with widget prefix`() {
        assertTrue(SOURCE_WIDGET_4X2_WEEKLY.startsWith("widget_"))
    }

    @Test
    fun `SOURCE_WIDGET_4X2_WEEKLY is distinct from all other widget sources`() {
        val sources = setOf(
            SOURCE_WIDGET_2X1,
            SOURCE_WIDGET_4X1,
            SOURCE_WIDGET_4X2_COUNTDOWN,
            SOURCE_WIDGET_4X2_WEEKLY,
        )
        assertEquals(4, sources.size)
    }

    // ── Widget instantiation ──────────────────────────────────────────────────

    @Test
    fun `ShiftWidget4x2Weekly can be instantiated`() {
        assertNotNull(ShiftWidget4x2Weekly())
    }

    @Test
    fun `ShiftWidget4x2WeeklyReceiver can be instantiated`() {
        assertNotNull(ShiftWidget4x2WeeklyReceiver())
    }

    @Test
    fun `ShiftWidget4x2WeeklyReceiver glanceAppWidget is ShiftWidget4x2Weekly`() {
        val receiver = ShiftWidget4x2WeeklyReceiver()
        assertNotNull(receiver.glanceAppWidget)
        assertTrue(receiver.glanceAppWidget is ShiftWidget4x2Weekly)
    }

    // ── widgetIntent ──────────────────────────────────────────────────────────

    @Test
    fun `widgetIntent for weekly has correct source extra`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_WEEKLY)
        assertEquals(SOURCE_WIDGET_4X2_WEEKLY, intent.getStringExtra(EXTRA_WIDGET_SOURCE))
    }

    @Test
    fun `widgetIntent for weekly has FLAG_ACTIVITY_NEW_TASK`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_WEEKLY)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `widgetIntent for weekly has FLAG_ACTIVITY_CLEAR_TOP`() {
        val intent = widgetIntent(context, SOURCE_WIDGET_4X2_WEEKLY)
        assertTrue(intent.flags and android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    // ── Week data helpers ─────────────────────────────────────────────────────

    @Test
    fun `buildTestWeek returns 7 days starting Monday`() {
        val monday = LocalDate.of(2026, 6, 8)
        val week = buildTestWeek(monday)
        assertEquals(7, week.days.size)
        assertEquals(monday, week.weekStartDate)
        assertEquals(monday, week.days.first().date)
    }

    @Test
    fun `buildTestWeek days are sequential`() {
        val monday = LocalDate.of(2026, 6, 8)
        val week = buildTestWeek(monday)
        week.days.forEachIndexed { index, day ->
            assertEquals(monday.plusDays(index.toLong()), day.date)
        }
    }

    @Test
    fun `today day found correctly in week`() {
        val today = LocalDate.of(2026, 6, 10)
        val week = buildTestWeek(LocalDate.of(2026, 6, 8))
        val found = week.days.find { it.date == today }
        assertNotNull(found)
        assertEquals(today, found!!.date)
    }

    @Test
    fun `offset minus2 date is two days before today`() {
        val today = LocalDate.of(2026, 6, 10)
        val minus2 = today.plusDays(-2)
        assertEquals(LocalDate.of(2026, 6, 8), minus2)
    }

    @Test
    fun `offset plus2 date is two days after today`() {
        val today = LocalDate.of(2026, 6, 10)
        val plus2 = today.plusDays(2)
        assertEquals(LocalDate.of(2026, 6, 12), plus2)
    }

    @Test
    fun `five days grid covers today minus2 through today plus2`() {
        val today = LocalDate.of(2026, 6, 12)
        val dates = (-2..2).map { today.plusDays(it.toLong()) }
        assertEquals(5, dates.size)
        assertEquals(LocalDate.of(2026, 6, 10), dates.first())
        assertEquals(LocalDate.of(2026, 6, 14), dates.last())
        assertEquals(today, dates[2])
    }

    @Test
    fun `week with null returns Unregistered for all days`() {
        val today = LocalDate.of(2026, 6, 12)
        val week: ScheduleWeek? = null
        val todayDay = week?.days?.find { it.date == today }
        assertTrue(todayDay == null)
    }

    @Test
    fun `work day data is retrievable from week`() {
        val monday = LocalDate.of(2026, 6, 8)
        val week = buildTestWeek(monday)
        val wednesday = week.days.find { it.date == LocalDate.of(2026, 6, 10) }
        assertNotNull(wednesday)
        assertEquals(DayType.WORK, wednesday!!.type)
        assertEquals(LocalTime.of(9, 0), wednesday.startTime)
        assertEquals(LocalTime.of(18, 0), wednesday.endTime)
    }

    @Test
    fun `off day code label is accessible from week`() {
        val monday = LocalDate.of(2026, 6, 8)
        val week = buildTestWeek(monday)
        val sunday = week.days.find { it.date == LocalDate.of(2026, 6, 14) }
        assertNotNull(sunday)
        assertEquals(DayType.OTHER, sunday!!.type)
        assertEquals("정규휴일", sunday.codeLabel)
    }

    private fun buildTestWeek(weekStart: LocalDate): ScheduleWeek {
        val days = (0 until 7).map { i ->
            val date = weekStart.plusDays(i.toLong())
            val isWeekend = i >= 5
            if (isWeekend) {
                ScheduleDay(
                    date = date,
                    type = DayType.OTHER,
                    startTime = null,
                    endTime = null,
                    codeLabel = "정규휴일",
                    source = SourceType.PARSED,
                )
            } else {
                ScheduleDay(
                    date = date,
                    type = DayType.WORK,
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(18, 0),
                    codeLabel = "정상",
                    source = SourceType.PARSED,
                )
            }
        }
        return ScheduleWeek(weekStartDate = weekStart, days = days)
    }
}
