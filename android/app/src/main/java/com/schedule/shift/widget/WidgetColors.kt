package com.schedule.shift.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

// ColorProvider(day, night) needs glance-material3 dynamic colors.
// Using static providers here for simplicity; GlanceTheme applies system dark/light at runtime.
internal val WidgetSurface = ColorProvider(Color(0xFFFFFFFF))
internal val WidgetSurfaceDark = ColorProvider(Color(0xFF1C1B1F))
internal val WidgetOnSurface = ColorProvider(Color(0xFF1C1B1F))
internal val WidgetOnSurfaceVariant = ColorProvider(Color(0xFF49454F))
internal val WidgetPrimary = ColorProvider(Color(0xFF00704A))
internal val WidgetBackground = ColorProvider(Color(0xFFF7F4EF))
@Suppress("MagicNumber")
internal val WidgetDivider = ColorProvider(Color(0xFFDDDDDD))
