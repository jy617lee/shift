package com.schedule.shift

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.analytics.AppOpenSource
import com.schedule.shift.navigation.ShiftNavGraph
import com.schedule.shift.ui.theme.ShiftTheme
import com.schedule.shift.widget.EXTRA_WIDGET_SOURCE
import com.schedule.shift.widget.SOURCE_WIDGET_2X1
import com.schedule.shift.widget.SOURCE_WIDGET_2X2
import com.schedule.shift.widget.SOURCE_WIDGET_4X1
import com.schedule.shift.widget.SOURCE_WIDGET_4X2_COUNTDOWN
import com.schedule.shift.widget.SOURCE_WIDGET_4X2_WEEKLY
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        trackAppOpen()
        setContent {
            ShiftTheme {
                ShiftNavGraph()
            }
        }
    }

    private fun trackAppOpen() {
        val source = when (intent.getStringExtra(EXTRA_WIDGET_SOURCE)) {
            SOURCE_WIDGET_2X1 -> AppOpenSource.WIDGET_2X1
            SOURCE_WIDGET_2X2 -> AppOpenSource.WIDGET_2X2
            SOURCE_WIDGET_4X1 -> AppOpenSource.WIDGET_4X1
            SOURCE_WIDGET_4X2_COUNTDOWN -> AppOpenSource.WIDGET_4X2_COUNTDOWN
            SOURCE_WIDGET_4X2_WEEKLY -> AppOpenSource.WIDGET_4X2_WEEKLY
            else -> AppOpenSource.ICON
        }
        analyticsTracker.track(AnalyticsEvent.AppOpen(source))
        trackWidgetActive()
    }

    private fun trackWidgetActive() {
        val manager = AppWidgetManager.getInstance(applicationContext)
        val providers = manager.getInstalledProviders()
        val packageName = applicationContext.packageName
        val activeTypes = providers
            .filter { it.provider.packageName == packageName }
            .mapNotNull { it.provider.className.toWidgetSourceOrNull() }
            .distinct()
        analyticsTracker.track(AnalyticsEvent.WidgetActive(activeTypes))
    }
}

private fun String.toWidgetSourceOrNull(): String? = when {
    endsWith("ShiftWidget2x1Receiver") -> SOURCE_WIDGET_2X1
    endsWith("ShiftWidget2x2Receiver") -> SOURCE_WIDGET_2X2
    endsWith("ShiftWidget4x1Receiver") -> SOURCE_WIDGET_4X1
    endsWith("ShiftWidget4x2CountdownReceiver") -> SOURCE_WIDGET_4X2_COUNTDOWN
    endsWith("ShiftWidget4x2WeeklyReceiver") -> SOURCE_WIDGET_4X2_WEEKLY
    else -> null
}
