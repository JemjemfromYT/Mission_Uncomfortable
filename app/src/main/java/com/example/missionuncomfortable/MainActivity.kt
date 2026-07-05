package com.example.missionuncomfortable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.example.missionuncomfortable.ui.dashboard.DashboardScreen

private val MissionDarkColors = darkColorScheme(
    primary            = Color(0xFFFFD700),
    onPrimary          = Color(0xFF000000),
    background         = Color(0xFF0A0A0A),
    onBackground       = Color(0xFFFFFFFF),
    surface            = Color(0xFF1A1A1A),
    onSurface          = Color(0xFFFFFFFF),
    surfaceVariant     = Color(0xFF2A2A2A),
    onSurfaceVariant   = Color(0xFFAAAAAA),
    error              = Color(0xFFCF6679),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = MissionDarkColors) {
                DashboardScreen()
            }
        }
    }
}