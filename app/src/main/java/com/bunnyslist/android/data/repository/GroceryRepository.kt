package com.bunnyslist.android.data.repository

import com.bunnyslist.android.data.model.ActionLogEntry
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.bunnyslist.android.data.model.ListActions
import com.bunnyslist.android.data.model.ListFieldLimits
import com.bunnyslist.android.data.model.buildActionData
import com.bunnyslist.android.data.remote.toActionLogEntryOrNull
import com.bunnyslist.android.data.remote.toGroceryCategoryOrNull
import com.bunnyslist.android.data.remote.toGroceryItemOrNull
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Reads and writes the signed-in user's list at users/{uid}/items,
 * users/{uid}/categories, and users/{uid}/actions.
 *
 * Top-level `items` and `categories` are the previous shared collections.
 * They are intentionally not used here. See the migration note in the README
 * before deploying firestore.rules.
 */
@Singleton
class GroceryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    fun observeItems(): Flow<List<GroceryItem>> = callbackFlow {
        val registration = items()
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        val items = snapshot.documents
                            .mapNotNull { it.toGroceryItemOrNull() }
                            .sortedBy { it.name.lowercase() }
                        trySend(items)
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    fun observeCategories(): Flow<List<GroceryCategory>> = callbackFlow {
        // Sort by name. Numeric `id` is legacy and is missing on new categories,
        // so ordering by it would hide every auto-id category.
        val registration = categories()
            .orderBy("itemCategory", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        val categories = snapshot.documents
                            .mapNotNull { it.toGroceryCategoryOrNull() }
                            .filter { it.itemCategory.isNotBlank() }
                            .sortedBy { it.itemCategory.lowercase() }
                        trySend(categories)
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    fun observeActions(): Flow<List<ActionLogEntry>> = callbackFlow {
        val registration = actions()
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(ListFieldLimits.HISTORY_LIMIT)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        val entries = snapshot.documents.mapNotNull { it.toActionLogEntryOrNull() }
                        trySend(entries)
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun addItem(item: GroceryItem) {
        val ref = items().document()
        val fields = item.toFieldMap()
        commitChange(
            apply = { batch ->
                batch.set(ref, fields.withServerTimestamp())
            },
            action = ListActions.ADD,
            targetType = ListActions.ITEM,
            targetId = ref.id,
            before = null,
            after = fields,
        )
    }

    suspend fun updateItem(original: GroceryItem, updated: GroceryItem) {
        require(updated.firestoreId.isNotBlank()) { "Missing item id." }
        val before = original.toFieldMap()
        val after = updated.toFieldMap()
        commitChange(
            apply = { batch ->
                batch.update(items().document(updated.firestoreId), after.withServerTimestamp())
            },
            action = ListActions.EDIT,
            targetType = ListActions.ITEM,
            targetId = updated.firestoreId,
            before = before,
            after = after,
        )
    }

    suspend fun updateNeededStatus(item: GroceryItem, needed: Boolean) {
        require(item.firestoreId.isNotBlank()) { "Missing item id." }
        val before = item.toFieldMap()
        val after = item.copy(need = needed).toFieldMap()
        commitChange(
            apply = { batch ->
                batch.update(
                    items().document(item.firestoreId),
                    mapOf(
                        "need" to needed,
                        "lastUpdated" to FieldValue.serverTimestamp(),
                    ),
                )
            },
            action = ListActions.TOGGLE_NEEDED,
            targetType = ListActions.ITEM,
            targetId = item.firestoreId,
            before = before,
            after = after,
        )
    }

    suspend fun deleteItem(item: GroceryItem) {
        require(item.firestoreId.isNotBlank()) { "Missing item id." }
        commitChange(
            apply = { batch -> batch.delete(items().document(item.firestoreId)) },
            action = ListActions.DELETE,
            targetType = ListActions.ITEM,
            targetId = item.firestoreId,
            before = item.toFieldMap(),
            after = null,
        )
    }

    /**
     * Puts the item back on its original document id. The previous undo path
     * called add(), which minted a new id and left the deleted id gone.
     */
    suspend fun restoreItem(item: GroceryItem) {
        require(item.firestoreId.isNotBlank()) { "Cannot restore an item without its original id." }
        val fields = item.toFieldMap()
        commitChange(
            apply = { batch ->
                batch.set(items().document(item.firestoreId), fields.withServerTimestamp())
            },
            action = ListActions.UNDO,
            targetType = ListActions.ITEM,
            targetId = item.firestoreId,
            before = null,
            after = fields,
        )
    }

    /**
     * Allocates a Firestore auto-id. The old client computed max(id)+1 from
     * the categories it had already loaded, so two devices could create the
     * same numeric id.
     */
    suspend fun addCategory(name: String): GroceryCategory {
        val ref = categories().document()
        val fields = mapOf("itemCategory" to name)
        commitChange(
            apply = { batch ->
                batch.set(ref, fields.withServerTimestamp())
            },
            action = ListActions.ADD_CATEGORY,
            targetType = ListActions.CATEGORY,
            targetId = ref.id,
            before = null,
            after = fields,
        )
        return GroceryCategory(
            firestoreId = ref.id,
            itemCategory = name,
            lastUpdated = Date(),
            legacyId = null,
        )
    }

    /**
     * History reverse writes `before` back onto the original id, or deletes
     * the document when the logged action created it (`before` is absent).
     */
    suspend fun reverseAction(entry: ActionLogEntry) {
        require(entry.targetId.isNotBlank()) { "Missing history target." }
        val ref = when (entry.targetType) {
            ListActions.ITEM -> items().document(entry.targetId)
            ListActions.CATEGORY -> categories().document(entry.targetId)
            else -> error("Unknown history target.")
        }
        val restored = entry.before
        commitChange(
            apply = { batch ->
                if (restored == null) {
                    batch.delete(ref)
                } else {
                    val payload = restored.toMutableMap()
                    payload.remove("lastUpdated")
                    payload["lastUpdated"] = FieldValue.serverTimestamp()
                    // Replace the document so a reverse cannot keep fields the rules reject.
                    batch.set(ref, payload)
                }
            },
            action = ListActions.UNDO,
            targetType = entry.targetType,
            targetId = entry.targetId,
            before = entry.after,
            after = restored,
        )
    }

    private suspend fun commitChange(
        apply: (WriteBatch) -> Unit,
        action: String,
        targetType: String,
        targetId: String,
        before: Map<String, Any>?,
        after: Map<String, Any>?,
    ) {
        val batch = firestore.batch()
        apply(batch)
        batch.set(
            actions().document(),
            buildActionData(
                actorUid = requireUid(),
                actorName = actorName(),
                action = action,
                targetType = targetType,
                targetId = targetId,
                before = before,
                after = after,
                createdAt = FieldValue.serverTimestamp(),
            ),
        )
        batch.commit().await()
    }

    private fun items() = userRoot().collection(ITEMS)

    private fun categories() = userRoot().collection(CATEGORIES)

    private fun actions() = userRoot().collection(ACTIONS)

    private fun userRoot() = firestore.collection(USERS).document(requireUid())

    private fun requireUid(): String {
        return auth.currentUser?.uid ?: error("Sign in required.")
    }

    private fun actorName(): String {
        val raw = auth.currentUser?.displayName?.trim().orEmpty().ifBlank { "You" }
        return raw.take(ListFieldLimits.ACTOR_NAME_MAX)
    }

    private fun Map<String, Any>.withServerTimestamp(): Map<String, Any> {
        return this + ("lastUpdated" to FieldValue.serverTimestamp())
    }

    private companion object {
        const val USERS = "users"
        const val ITEMS = "items"
        const val CATEGORIES = "categories"
        const val ACTIONS = "actions"
    }
}
