package com.bunnyslist.android.data.remote

import android.util.Log
import com.bunnyslist.android.data.model.ActionLogEntry
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

/**
 * Centralized Firestore mapping keeps field names aligned with the rules file
 * and makes malformed production documents skippable instead of fatal.
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
    val legacyId = when (val rawId = get("id")) {
        // Older web writes used a numeric id. New Android categories omit it.
        is Number -> rawId.toInt()
        is String -> rawId.toIntOrNull()
        else -> null
    }

    return GroceryCategory(
        firestoreId = id,
        itemCategory = getString("itemCategory").orEmpty(),
        lastUpdated = lastUpdated,
        legacyId = legacyId,
    )
}

fun DocumentSnapshot.toActionLogEntry(): ActionLogEntry {
    val createdAt = (get("createdAt") as? Timestamp)?.toDate() ?: Date()
    return ActionLogEntry(
        id = id,
        actorUid = getString("actorUid").orEmpty(),
        actorName = getString("actorName").orEmpty().ifBlank { "You" },
        action = getString("action").orEmpty(),
        targetType = getString("targetType").orEmpty(),
        targetId = getString("targetId").orEmpty(),
        before = snapshotMap("before"),
        after = snapshotMap("after"),
        createdAt = createdAt,
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

private fun DocumentSnapshot.snapshotMap(field: String): Map<String, Any>? {
    val raw = get(field) as? Map<*, *> ?: return null
    return raw.entries.mapNotNull { (key, value) ->
        val name = key as? String ?: return@mapNotNull null
        if (value == null) null else name to value
    }.toMap().ifEmpty { null }
}

fun DocumentSnapshot.toGroceryItemOrNull(): GroceryItem? {
    return runCatching(::toGroceryItem).getOrElse { error ->
        Log.w("FirestoreMapper", "Skipping invalid item document $id", error)
        null
    }
}

fun DocumentSnapshot.toGroceryCategoryOrNull(): GroceryCategory? {
    return runCatching(::toGroceryCategory).getOrElse { error ->
        Log.w("FirestoreMapper", "Skipping invalid category document $id", error)
        null
    }
}

fun DocumentSnapshot.toActionLogEntryOrNull(): ActionLogEntry? {
    return runCatching(::toActionLogEntry).getOrElse { error ->
        Log.w("FirestoreMapper", "Skipping invalid action document $id", error)
        null
    }
}
