package com.bunnyslist.android.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bunnyslist.android.data.auth.AuthRepository
import com.bunnyslist.android.data.auth.SignInCancelledException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val displayName: String? = null,
    val email: String? = null,
    val isSigningIn: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.observeUser().collect { user ->
                _uiState.update { current ->
                    current.copy(
                        isSignedIn = user != null,
                        displayName = user?.displayName,
                        email = user?.email,
                        isSigningIn = false,
                    )
                }
            }
        }
    }

    fun signIn(activity: Activity) {
        if (_uiState.value.isSigningIn) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, errorMessage = null) }
            runCatching { authRepository.signIn(activity) }
                .onFailure { error ->
                    if (error is SignInCancelledException) {
                        _uiState.update { it.copy(isSigningIn = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isSigningIn = false,
                                errorMessage = "Google sign-in failed. Confirm the Google provider is enabled and this app's SHA-1 is registered.",
                            )
                        }
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching { authRepository.signOut() }
                .onFailure {
                    _uiState.update { it.copy(errorMessage = "Couldn't sign out. Try again.") }
                }
        }
    }
}
