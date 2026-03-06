package com.bunnyslist.android.data.model

import java.util.Date

/**
 * Native model that mirrors the current web app Firestore document shape.
 *
 * Note:
 * `itemCategory` intentionally stores the category ID as a string because the
 * web app persists it that way today.
 */
data class GroceryItem(
    val firestoreId: String,
    val name: String,
    val description: String,
    val itemCategory: String,
    val lastUpdated: Date,
    val isOwned: Boolean,
    val need: Boolean,
)
