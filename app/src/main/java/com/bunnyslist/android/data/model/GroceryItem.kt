package com.bunnyslist.android.data.model

import java.util.Date

/**
 * Item stored at users/{uid}/items/{firestoreId}.
 *
 * [itemCategory] is the category document id for categories created on
 * Android. Migrated web documents may still store the old numeric category
 * id as a string; [GroceryCategory.matchesReference] accepts both.
 */
data class GroceryItem(
    val firestoreId: String,
    val name: String,
    val description: String,
    val itemCategory: String,
    val lastUpdated: Date,
    val isOwned: Boolean,
    val need: Boolean,
) {
    /** Snapshot stored on the action log. lastUpdated is applied at write time. */
    fun toFieldMap(): Map<String, Any> = mapOf(
        "name" to name,
        "description" to description,
        "itemCategory" to itemCategory,
        "isOwned" to isOwned,
        "need" to need,
    )
}
