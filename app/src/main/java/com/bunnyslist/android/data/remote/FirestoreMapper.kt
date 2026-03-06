package com.bunnyslist.android.data.remote

import android.util.Log
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Centralized Firestore mapping keeps field names aligned with the web app and
 * makes it easier to evolve both clients together.
 */
fun DocumentSnapshot.toGroceryItem(): GroceryItem {
    val lastUpdated = (get("lastUpdated") as? Timestamp)?.toDate() ?: Date()

    return GroceryItem(
        firestoreId = id,
        name = getString("name").orEmpty(),
        description = getString("description").orEmpty(),
        itemCategory = getFlexibleString("itemCategory"),
        lastUpdated = lastUpdated,
        isOwned = getBoolean("isOwned") ?: true,
        need = getBoolean("need") ?: false,
    )
}

fun DocumentSnapshot.toGroceryCategory(): GroceryCategory {
    val lastUpdated = (get("lastUpdated") as? Timestamp)?.toDate() ?: Date()
    val categoryId = when (val rawId = get("id")) {
        // The live Firestore data is not perfectly normalized yet, so Android
        // accepts either a numeric ID or the string form of that same ID.
        is Number -> rawId.toInt()
        is String -> rawId.toIntOrNull() ?: 0
        else -> 0
    }

    return GroceryCategory(
        firestoreId = id,
        id = categoryId,
        itemCategory = getString("itemCategory").orEmpty(),
        lastUpdated = lastUpdated,
    )
}

/**
 * Some legacy Firestore records use numbers where the newer web app expects
 * strings. This helper keeps Android tolerant of both formats.
 */
private fun DocumentSnapshot.getFlexibleString(fieldName: String): String {
    return when (val rawValue = get(fieldName)) {
        is String -> rawValue
        is Number -> rawValue.toString()
        else -> ""
    }
}

/**
 * Converts a Firestore document and logs the issue instead of crashing if the
 * document shape is malformed in production data.
 */
fun DocumentSnapshot.toGroceryItemOrNull(): GroceryItem? {
    return runCatching(::toGroceryItem).getOrElse { error ->
        Log.w("FirestoreMapper", "Skipping invalid item document $id", error)
        null
    }
}

/**
 * Converts category documents defensively because the live backend contains
 * mixed field types from older writes.
 */
fun DocumentSnapshot.toGroceryCategoryOrNull(): GroceryCategory? {
    return runCatching(::toGroceryCategory).getOrElse { error ->
        Log.w("FirestoreMapper", "Skipping invalid category document $id", error)
        null
    }
}
