package com.schedule.shift.widget

import android.content.Context
import android.content.Intent
import com.schedule.shift.MainActivity

internal const val EXTRA_WIDGET_SOURCE = "widget_source"
internal const val SOURCE_WIDGET_2X1 = "widget_2x1"
internal const val SOURCE_WIDGET_4X1 = "widget_4x1"
internal const val SOURCE_WIDGET_4X2_WEEKLY = "widget_4x2_weekly"

internal fun widgetIntent(context: Context, source: String): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_WIDGET_SOURCE, source)
    }
