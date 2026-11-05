package com.ryanshumway.dinorunner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * The app's only screen. `setContent { }` is where Compose takes over, the
 * same role as `WindowGroup { ContentView() }` in a SwiftUI App.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GameScreen() }
    }
}
