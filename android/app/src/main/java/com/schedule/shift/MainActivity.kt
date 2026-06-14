package com.schedule.shift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schedule.shift.domain.analytics.AnalyticsEvent
import com.schedule.shift.domain.analytics.AnalyticsTracker
import com.schedule.shift.domain.analytics.AppOpenSource
import com.schedule.shift.navigation.ShiftNavGraph
import com.schedule.shift.ui.theme.ShiftTheme
import com.schedule.shift.widget.EXTRA_OPEN_REGISTRATION
import com.schedule.shift.widget.EXTRA_WIDGET_SOURCE
import com.schedule.shift.widget.SOURCE_WIDGET_2X1
import com.schedule.shift.widget.SOURCE_WIDGET_4X1
import com.schedule.shift.widget.SOURCE_WIDGET_4X2_WEEKLY
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tracker: AnalyticsTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openGallery = intent.getBooleanExtra(EXTRA_OPEN_REGISTRATION, false)

        val source = when (intent.getStringExtra(EXTRA_WIDGET_SOURCE)) {
            SOURCE_WIDGET_2X1 -> AppOpenSource.WIDGET_2X1
            SOURCE_WIDGET_4X1 -> AppOpenSource.WIDGET_4X1
            SOURCE_WIDGET_4X2_WEEKLY -> AppOpenSource.WIDGET_4X2_WEEKLY
            else -> AppOpenSource.ICON
        }
        tracker.track(AnalyticsEvent.AppOpen(source = source))

        setContent {
            ShiftTheme {
                ShiftNavGraph(openGallery = openGallery)
            }
        }
    }
}
