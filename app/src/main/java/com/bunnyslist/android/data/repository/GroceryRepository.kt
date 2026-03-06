package com.bunnyslist.android.data.repository

import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.bunnyslist.android.data.remote.toGroceryCategoryOrNull
import com.bunnyslist.android.data.remote.toGroceryItemOrNull
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that keeps Android parity with the web app's Firestore collections.
 */
@Singleton
class GroceryRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun observeItems(): Flow<List<GroceryItem>> = callbackFlow {
        val registration = firestore.collection(ITEMS_COLLECTION)
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
        val registration = firestore.collection(CATEGORIES_COLLECTION)
            .orderBy("id", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        val categories = snapshot.documents
                            .mapNotNull { it.toGroceryCategoryOrNull() }
                            .filter { it.itemCategory.isNotBlank() }
                            .sortedBy { it.id }
                        trySend(categories)
                    }
                }
            }

        awaitClose { registration.remove() }
    }

    suspend fun addItem(item: GroceryItem) {
        firestore.collection(ITEMS_COLLECTION)
            .add(
                mapOf(
                    "name" to item.name,
                    "description" to item.description,
                    "itemCategory" to item.itemCategory,
                    "isOwned" to item.isOwned,
                    "need" to item.need,
                    "lastUpdated" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    suspend fun updateItem(item: GroceryItem) {
        firestore.collection(ITEMS_COLLECTION)
            .document(item.firestoreId)
            .update(
                mapOf(
                    "name" to item.name,
                    "description" to item.description,
                    "itemCategory" to item.itemCategory,
                    "isOwned" to item.isOwned,
                    "need" to item.need,
                    "lastUpdated" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    suspend fun updateNeededStatus(id: String, needed: Boolean) {
        firestore.collection(ITEMS_COLLECTION)
            .document(id)
            .update(
                mapOf(
                    "need" to needed,
                    "lastUpdated" to FieldValue.serverTimestamp(),
                ),
            )
            .await()
    }

    suspend fun deleteItem(id: String) {
        firestore.collection(ITEMS_COLLECTION)
            .document(id)
            .delete()
            .await()
    }

    suspend fun addCategory(name: String, nextId: Int): GroceryCategory {
        val payload = mapOf(
            "id" to nextId,
            "itemCategory" to name,
            "lastUpdated" to FieldValue.serverTimestamp(),
        )

        val document = firestore.collection(CATEGORIES_COLLECTION).add(payload).await()

        return GroceryCategory(
            firestoreId = document.id,
            id = nextId,
            itemCategory = name,
            lastUpdated = Date(),
        )
    }

    private companion object {
        const val ITEMS_COLLECTION = "items"
        const val CATEGORIES_COLLECTION = "categories"
    }
}
