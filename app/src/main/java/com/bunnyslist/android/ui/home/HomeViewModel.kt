package com.bunnyslist.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.bunnyslist.android.data.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel that translates Firestore data into screen-ready state.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: GroceryRepository,
    private val connectivityObserver: ConnectivityObserver,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var initialItemsLoaded = false
    private var initialCategoriesLoaded = false

    init {
        observeConnectivity()
        observeItems()
        observeCategories()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateFilter(filter: HomeFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun toggleNeeded(item: GroceryItem) {
        viewModelScope.launch {
            runCatching {
                repository.updateNeededStatus(item.firestoreId, !item.need)
            }.onFailure {
                emitMessage("Failed to update item status.")
            }
        }
    }

    fun addItem(
        name: String,
        description: String,
        categoryId: String,
        needed: Boolean,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            emitMessage("Item name is required.")
            return
        }

        if (categoryId.isBlank()) {
            emitMessage("Choose a category before saving the item.")
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.addItem(
                    GroceryItem(
                        firestoreId = "",
                        name = trimmedName,
                        description = description.trim(),
                        itemCategory = categoryId,
                        lastUpdated = Date(),
                        isOwned = true,
                        need = needed,
                    ),
                )
            }.onSuccess {
                emitMessage("$trimmedName added.")
            }.onFailure {
                emitMessage("Failed to add item.")
            }
        }
    }

    fun updateItem(
        original: GroceryItem,
        name: String,
        description: String,
        categoryId: String,
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            emitMessage("Item name is required.")
            return
        }

        if (categoryId.isBlank()) {
            emitMessage("Choose a category before saving the item.")
            return
        }

        viewModelScope.launch {
            runCatching {
                repository.updateItem(
                    original.copy(
                        name = trimmedName,
                        description = description.trim(),
                        itemCategory = categoryId,
                        lastUpdated = Date(),
                    ),
                )
            }.onSuccess {
                emitMessage("Item updated.")
            }.onFailure {
                emitMessage("Failed to update item.")
            }
        }
    }

    suspend fun deleteItem(item: GroceryItem): GroceryItem? {
        return runCatching {
            repository.deleteItem(item.firestoreId)
            item
        }.onFailure {
            emitMessage("Failed to delete item.")
        }.getOrNull()
    }

    fun undoDelete(item: GroceryItem) {
        viewModelScope.launch {
            runCatching {
                repository.addItem(item.copy(firestoreId = "", lastUpdated = Date()))
            }.onSuccess {
                emitMessage("${item.name} restored.")
            }.onFailure {
                emitMessage("Failed to restore item.")
            }
        }
    }

    suspend fun addCategory(name: String): GroceryCategory? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            emitMessage("Category name is required.")
            return null
        }

        if (_uiState.value.categories.any { it.itemCategory.equals(trimmedName, ignoreCase = true) }) {
            emitMessage("That category already exists.")
            return null
        }

        val nextId = (_uiState.value.categories.maxOfOrNull(GroceryCategory::id) ?: 0) + 1

        return runCatching {
            repository.addCategory(trimmedName, nextId)
        }.onSuccess {
            emitMessage("Category added.")
        }.onFailure {
            emitMessage("Failed to add category.")
        }.getOrNull()
    }

    private fun observeItems() {
        viewModelScope.launch {
            repository.observeItems()
                .catch { throwable ->
                    handleLoadFailure(throwable.message ?: "Failed to load grocery items.")
                }
                .collect { items ->
                    initialItemsLoaded = true
                    _uiState.value = _uiState.value.copy(
                        items = items,
                        isLoading = !initialCategoriesLoaded,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun observeCategories() {
        viewModelScope.launch {
            repository.observeCategories()
                .catch { throwable ->
                    handleLoadFailure(throwable.message ?: "Failed to load categories.")
                }
                .collect { categories ->
                    initialCategoriesLoaded = true
                    _uiState.value = _uiState.value.copy(
                        categories = categories,
                        isLoading = !initialItemsLoaded,
                        errorMessage = null,
                    )
                }
        }
    }

    private fun observeConnectivity() {
        viewModelScope.launch {
            connectivityObserver.observe().collect { isOnline ->
                _uiState.value = _uiState.value.copy(isOnline = isOnline)
            }
        }
    }

    private fun handleLoadFailure(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = message,
        )
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch {
            _messages.emit(message)
        }
    }
}
