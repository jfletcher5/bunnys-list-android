package com.bunnyslist.android.data.model

import java.util.Date

/**
 * One changelog row written beside the grocery mutation it describes.
 * `before` and `after` are omitted (not stored as null) when that side does
 * not exist, because the security rules reject a null snapshot.
 */
data class ActionLogEntry(
    val id: String,
    val actorUid: String,
    val actorName: String,
    val action: String,
    val targetType: String,
    val targetId: String,
    val before: Map<String, Any>?,
    val after: Map<String, Any>?,
    val createdAt: Date,
) {
    fun subjectName(): String {
        val afterName = after.displayName()
        val beforeName = before.displayName()
        return afterName ?: beforeName ?: targetId
    }

    fun title(): String {
        val subject = subjectName()
        val kind = if (targetType == ListActions.CATEGORY) "category" else "item"
        return when (action) {
            ListActions.ADD, ListActions.ADD_CATEGORY -> "Added $kind $subject"
            ListActions.EDIT -> "Edited $kind $subject"
            ListActions.DELETE -> "Deleted $kind $subject"
            ListActions.TOGGLE_NEEDED -> "Updated needed on $subject"
            ListActions.UNDO -> "Reversed $kind $subject"
            else -> "$action $subject"
        }
    }

    fun detailLine(): String {
        val parts = mutableListOf<String>()
        val beforeName = before.displayName()
        val afterName = after.displayName()
        if (beforeName != null || afterName != null) {
            parts += "${beforeName ?: "—"} → ${afterName ?: "—"}"
        }
        val beforeNeed = before?.get("need")
        val afterNeed = after?.get("need")
        if (beforeNeed != null || afterNeed != null) {
            parts += "needed ${beforeNeed ?: "—"} → ${afterNeed ?: "—"}"
        }
        return parts.joinToString(" · ").ifBlank { targetId }
    }
}

object ListActions {
    const val ADD = "add"
    const val EDIT = "edit"
    const val DELETE = "delete"
    const val TOGGLE_NEEDED = "toggle_needed"
    const val ADD_CATEGORY = "add_category"
    const val UNDO = "undo"
    const val ITEM = "item"
    const val CATEGORY = "category"
}

/**
 * Builds the Firestore action document. Null snapshots are left out so the
 * rules see an absent field rather than a null map.
 */
fun buildActionData(
    actorUid: String,
    actorName: String,
    action: String,
    targetType: String,
    targetId: String,
    before: Map<String, Any>?,
    after: Map<String, Any>?,
    createdAt: Any,
): Map<String, Any> = buildMap {
    put("actorUid", actorUid)
    put("actorName", actorName)
    put("action", action)
    put("targetType", targetType)
    put("targetId", targetId)
    put("createdAt", createdAt)
    if (before != null) put("before", before)
    if (after != null) put("after", after)
}

private fun Map<String, Any>?.displayName(): String? {
    val name = this?.get("name") as? String
    if (!name.isNullOrBlank()) return name
    val category = this?.get("itemCategory") as? String
    return category?.takeIf { it.isNotBlank() }
}
