package com.bunnyslist.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bunnyslist.android.ui.home.BunnysListApp
import com.bunnyslist.android.ui.theme.BunnysListTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity entry point for the first Android MVP.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BunnysListTheme {
                BunnysListApp()
            }
        }
    }
}
