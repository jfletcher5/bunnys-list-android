package com.bunnyslist.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bunnyslist.android.data.model.GroceryCategory
import com.bunnyslist.android.data.model.GroceryItem
import com.bunnyslist.android.data.model.ListFieldLimits
import com.bunnyslist.android.data.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel that translates the signed-in user's Firestore data into
 * screen-ready state. Listeners start only after the auth gate creates this
 * ViewModel, so we never subscribe anonymously.
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

    suspend fun setNeeded(item: GroceryItem, needed: Boolean): Boolean {
        return runCatching {
            repository.updateNeededStatus(item, needed)
        }.onFailure {
            emitMessage("Failed to update item status.")
        }.isSuccess
    }

    fun addItem(
        name: String,
        description: String,
        categoryId: String,
        needed: Boolean,
    ) {
        val trimmedName = name.trim()
        val trimmedDescription = description.trim()
        if (!validateItem(trimmedName, trimmedDescription, categoryId)) return

        viewModelScope.launch {
            runCatching {
                repository.addItem(
                    GroceryItem(
                        firestoreId = "",
                        name = trimmedName,
                        description = trimmedDescription,
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
        val trimmedDescription = description.trim()
        if (!validateItem(trimmedName, trimmedDescription, categoryId)) return

        viewModelScope.launch {
            runCatching {
                repository.updateItem(
                    original = original,
                    updated = original.copy(
                        name = trimmedName,
                        description = trimmedDescription,
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
            repository.deleteItem(item)
            item
        }.onFailure {
            emitMessage("Failed to delete item.")
        }.getOrNull()
    }

    fun undoDelete(item: GroceryItem) {
        viewModelScope.launch {
            runCatching {
                // Restore the deleted document id. Do not add() a new one.
                repository.restoreItem(item)
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
        if (trimmedName.length > ListFieldLimits.CATEGORY_NAME_MAX) {
            emitMessage("Category name must be ${ListFieldLimits.CATEGORY_NAME_MAX} characters or fewer.")
            return null
        }

        if (_uiState.value.categories.any { it.itemCategory.equals(trimmedName, ignoreCase = true) }) {
            emitMessage("That category already exists.")
            return null
        }

        return runCatching {
            repository.addCategory(trimmedName)
        }.onSuccess {
            emitMessage("Category added.")
        }.onFailure {
            emitMessage("Failed to add category.")
        }.getOrNull()
    }

    private fun validateItem(name: String, description: String, categoryId: String): Boolean {
        if (name.isBlank()) {
            emitMessage("Item name is required.")
            return false
        }
        if (name.length > ListFieldLimits.NAME_MAX) {
            emitMessage("Item name must be ${ListFieldLimits.NAME_MAX} characters or fewer.")
            return false
        }
        if (description.length > ListFieldLimits.DESCRIPTION_MAX) {
            emitMessage("Description must be ${ListFieldLimits.DESCRIPTION_MAX} characters or fewer.")
            return false
        }
        if (categoryId.isBlank()) {
            emitMessage("Choose a category before saving the item.")
            return false
        }
        if (categoryId.length > ListFieldLimits.CATEGORY_REF_MAX) {
            emitMessage("Choose a category before saving the item.")
            return false
        }
        return true
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
