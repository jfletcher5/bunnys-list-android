package com.bunnyslist.android.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.bunnyslist.android.ui.theme.BunnysListTheme
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Renders the real home screen with one needed item and taps the Needed chip
 * so the Undo snackbar can be captured. No Firebase call is made.
 */
@RunWith(AndroidJUnit4::class)
class NeededUndoScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun neededChipShowsUndoSnackbar() {
        val category = GroceryCategory(
            firestoreId = "cat-dairy",
            itemCategory = "Dairy",
            lastUpdated = Date(0),
        )
        val item = GroceryItem(
            firestoreId = "item-milk",
            name = "Milk",
            description = "2%",
            itemCategory = category.firestoreId,
            lastUpdated = Date(0),
            isOwned = true,
            need = true,
        )
        val snackbarHostState = SnackbarHostState()

        composeRule.setContent {
            BunnysListTheme {
                HomeScreen(
                    uiState = HomeUiState(
                        items = listOf(item),
                        categories = listOf(category),
                        isLoading = false,
                    ),
                    snackbarHostState = snackbarHostState,
                    openAddDialogSignal = 0,
                    onSearchQueryChange = {},
                    onFilterSelected = {},
                    onSetNeeded = { _, _ -> true },
                    onAddItem = { _, _, _, _ -> },
                    onUpdateItem = { _, _, _, _ -> },
                    onDeleteItem = { null },
                    onUndoDelete = {},
                    onAddCategory = { null },
                    onOpenHistory = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithText("Needed").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Undo").assertExists()

        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        val out = File(
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
            "undo-snackbar.png",
        )
        FileOutputStream(out).use { stream ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}
