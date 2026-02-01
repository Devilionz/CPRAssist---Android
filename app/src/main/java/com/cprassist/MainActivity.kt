package com.cprassist

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.cprassist.ui.navigation.CPRAssistNavGraph
import com.cprassist.ui.theme.CPRAssistTheme

/**
 * Main Activity for CPR Assist application.
 *
 * Configures the app for emergency use:
 * - Keeps screen on during active events
 * - Locks to portrait orientation
 * - Uses high-contrast dark theme
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on - critical for emergency use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ensure bright screen
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Show over lock screen for emergencies
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

        // Full screen edge-to-edge
        enableEdgeToEdge()

        // Configure system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            CPRAssistTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CPRAssistNavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure screen stays on when app is visible
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
