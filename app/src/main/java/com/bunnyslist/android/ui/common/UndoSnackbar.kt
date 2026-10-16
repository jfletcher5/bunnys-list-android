package com.bunnyslist.android.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

private const val UNDO_VISIBLE_MS = 5_000L

/**
 * Shows an Undo snackbar for about five seconds. Returning true means the
 * user tapped Undo before it timed out.
 */
suspend fun showTimedUndoSnackbar(
    snackbarHostState: SnackbarHostState,
    message: String,
): Boolean {
    // Don't spend the five seconds waiting behind another snackbar.
    while (snackbarHostState.currentSnackbarData != null) {
        delay(50)
    }
    val result = withTimeoutOrNull(UNDO_VISIBLE_MS) {
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = "Undo",
            duration = SnackbarDuration.Indefinite,
        )
    }
    return result == SnackbarResult.ActionPerformed
}
