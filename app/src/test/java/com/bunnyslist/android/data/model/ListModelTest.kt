package com.bunnyslist.android.data.model

import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListModelTest {
    @Test
    fun categoryMatchesDocumentIdAndLegacyNumericId() {
        val category = GroceryCategory(
            firestoreId = "autoId123",
            itemCategory = "Dairy",
            lastUpdated = Date(0),
            legacyId = 4,
        )

        assertTrue(category.matchesReference("autoId123"))
        assertTrue(category.matchesReference("4"))
        assertFalse(category.matchesReference("5"))
        assertFalse(category.matchesReference(""))
    }

    @Test
    fun homeStateResolvesLegacyCategoryReference() {
        val category = GroceryCategory(
            firestoreId = "autoId123",
            itemCategory = "Dairy",
            lastUpdated = Date(0),
            legacyId = 4,
        )
        val item = GroceryItem(
            firestoreId = "item1",
            name = "Milk",
            description = "",
            itemCategory = "4",
            lastUpdated = Date(0),
            isOwned = true,
            need = true,
        )

        val state = com.bunnyslist.android.ui.home.HomeUiState(
            items = listOf(item),
            categories = listOf(category),
            isLoading = false,
        )

        assertEquals(category, state.categoryFor(item))
    }

    @Test
    fun actionPayloadOmitsNullSnapshots() {
        val created = buildActionData(
            actorUid = "uid",
            actorName = "Ada",
            action = ListActions.ADD,
            targetType = ListActions.ITEM,
            targetId = "item1",
            before = null,
            after = mapOf("name" to "Milk"),
            createdAt = "server-time",
        )
        val deleted = buildActionData(
            actorUid = "uid",
            actorName = "Ada",
            action = ListActions.DELETE,
            targetType = ListActions.ITEM,
            targetId = "item1",
            before = mapOf("name" to "Milk"),
            after = null,
            createdAt = "server-time",
        )

        assertFalse(created.containsKey("before"))
        assertEquals(mapOf("name" to "Milk"), created["after"])
        assertFalse(deleted.containsKey("after"))
        assertNull(deleted["after"])
    }

    @Test
    fun restoreKeepsOriginalItemIdInTheFieldMap() {
        val deleted = GroceryItem(
            firestoreId = "original-id",
            name = "Milk",
            description = "2%",
            itemCategory = "autoId123",
            lastUpdated = Date(0),
            isOwned = true,
            need = true,
        )

        // The undo bug wrote firestoreId = "" and then add() minted a new id.
        // The snapshot used for restore must still describe the same item,
        // and the caller writes it to deleted.firestoreId.
        assertEquals("original-id", deleted.firestoreId)
        assertEquals("Milk", deleted.toFieldMap()["name"])
        assertFalse(deleted.toFieldMap().containsKey("firestoreId"))
    }
}
