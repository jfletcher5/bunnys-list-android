package com.bunnyslist.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.bunnyslist.android.ui.home.BunnysListApp
import com.bunnyslist.android.ui.theme.BunnysListTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity entry point for the first Android MVP.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // This counter acts as a simple one-shot signal to open the Add Item dialog from external intents.
    private var openAddDialogSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            BunnysListTheme {
                BunnysListApp(openAddDialogSignal = openAddDialogSignal)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ADD_ITEM, false) == true) {
            openAddDialogSignal += 1
        }
    }

    companion object {
        const val EXTRA_OPEN_ADD_ITEM = "com.bunnyslist.android.extra.OPEN_ADD_ITEM"
    }
}
