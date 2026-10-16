package com.bunnyslist.android.data.model

import java.util.Date

/**
 * Category stored at users/{uid}/categories/{firestoreId}.
 *
 * New categories use the Firestore document id as their identity. [legacyId]
 * is only present on documents copied from the old numeric `id` field, so
 * items that still point at that number can be matched after migration.
 */
data class GroceryCategory(
    val firestoreId: String,
    val itemCategory: String,
    val lastUpdated: Date,
    val legacyId: Int? = null,
) {
    fun matchesReference(reference: String): Boolean {
        if (reference.isBlank()) return false
        return firestoreId == reference || legacyId?.toString() == reference
    }

    /** Snapshot stored on the action log. lastUpdated is applied at write time. */
    fun toFieldMap(): Map<String, Any> = buildMap {
        put("itemCategory", itemCategory)
        legacyId?.let { put("id", it) }
    }
}
