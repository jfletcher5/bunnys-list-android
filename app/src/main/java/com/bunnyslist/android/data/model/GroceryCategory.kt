package com.bunnyslist.android.data.model

import java.util.Date

/**
 * Native category model that matches the web app's category collection.
 */
data class GroceryCategory(
    val firestoreId: String,
    val id: Int,
    val itemCategory: String,
    val lastUpdated: Date,
)
