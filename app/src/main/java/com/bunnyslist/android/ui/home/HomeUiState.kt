package com.bunnyslist.android.ui.home

import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem

/**
 * UI state for the first Android parity screen.
 */
data class HomeUiState(
    val items: List<GroceryItem> = emptyList(),
    val categories: List<GroceryCategory> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: HomeFilter = HomeFilter.All,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val errorMessage: String? = null,
) {
    val hasCategories: Boolean
        get() = categories.isNotEmpty()

    val filteredItems: List<GroceryItem>
        get() {
            val query = searchQuery.trim().lowercase()

            return items.filter { item ->
                val matchesFilter = when (selectedFilter) {
                    HomeFilter.All -> true
                    HomeFilter.Needed -> item.need
                }

                val matchesQuery = query.isBlank() ||
                    item.name.lowercase().contains(query) ||
                    item.description.lowercase().contains(query)

                matchesFilter && matchesQuery
            }
        }

    val neededCount: Int
        get() = items.count { it.need }

    val emptyMessage: String
        get() = when {
            searchQuery.isNotBlank() -> "No items found matching your search."
            selectedFilter == HomeFilter.Needed -> "No needed items right now."
            !hasCategories -> "Add a category first, then start building your grocery list."
            else -> "No items yet. Add your first grocery item."
        }

    fun categoryFor(item: GroceryItem): GroceryCategory? {
        // New items store the category document id. Migrated items may still
        // store the old numeric id, which matchesReference also accepts.
        return categories.firstOrNull { category ->
            category.matchesReference(item.itemCategory)
        }
    }
}

enum class HomeFilter {
    All,
    Needed,
}
