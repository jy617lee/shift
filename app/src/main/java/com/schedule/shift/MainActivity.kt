package com.schedule.shift

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.schedule.shift.navigation.ShiftNavGraph
import com.schedule.shift.ui.theme.ShiftTheme
import com.schedule.shift.widget.EXTRA_OPEN_REGISTRATION
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val openGallery = intent.getBooleanExtra(EXTRA_OPEN_REGISTRATION, false)
        setContent {
            ShiftTheme {
                ShiftNavGraph(openGallery = openGallery)
            }
        }
    }
}
