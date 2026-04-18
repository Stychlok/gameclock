package com.gameclock.referee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gameclock.referee.ui.GameClockScreen
import com.gameclock.referee.ui.theme.GameClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameClockTheme {
                GameClockScreen()
            }
        }
    }
}
