package com.bunnyslist.android.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bunnyslist.android.data.model.ActionLogEntry
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val entries: List<ActionLogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val reversingIds: Set<String> = emptySet(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GroceryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.observeActions()
                .catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = throwable.message ?: "Couldn't load history.",
                        )
                    }
                }
                .collect { entries ->
                    _uiState.update {
                        it.copy(entries = entries, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    fun reverse(entry: ActionLogEntry) {
        if (entry.id in _uiState.value.reversingIds) return
        viewModelScope.launch {
            _uiState.update { it.copy(reversingIds = it.reversingIds + entry.id) }
            runCatching { repository.reverseAction(entry) }
                .onSuccess { emitMessage("Action reversed.") }
                .onFailure { emitMessage("Couldn't reverse that action.") }
            _uiState.update { it.copy(reversingIds = it.reversingIds - entry.id) }
        }
    }

    private fun emitMessage(message: String) {
        viewModelScope.launch { _messages.emit(message) }
    }
}
