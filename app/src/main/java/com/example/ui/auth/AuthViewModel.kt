package com.example.ui.auth

import android.app.Application
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.data.model.UserProfile
import com.example.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Initial : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: FirebaseUser, val profile: UserProfile) : AuthUiState()
    data class Banned(val profile: UserProfile) : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)
    private val auth = FirebaseAuth.getInstance()
    private val credentialManager = CredentialManager.create(application)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Initial)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserProfile(currentUser)
        } else {
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun attemptAutoSignIn() {
        _uiState.value = AuthUiState.Loading
        AuthManager.attemptAutoSignIn(
            context = getApplication(),
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                loadUserProfile(user)
            },
            onUnauthenticated = {
                _uiState.value = AuthUiState.Unauthenticated
            },
            scope = viewModelScope
        )
    }

    fun signInWithGoogle(onCancelled: () -> Unit = {}) {
        _uiState.value = AuthUiState.Loading
        AuthManager.onGoogleSignInClicked(
            context = getApplication(),
            credentialManager = credentialManager,
            onAuthSuccess = { user ->
                loadUserProfile(user)
            },
            onAuthError = { errorMsg ->
                _uiState.value = AuthUiState.Error(errorMsg)
            },
            scope = viewModelScope,
            onAuthCancelled = {
                _uiState.value = AuthUiState.Unauthenticated
                onCancelled()
            }
        )
    }

    private fun loadUserProfile(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                val profile = userRepository.syncUserProfile(user)
                if (profile.isBanned) {
                    _uiState.value = AuthUiState.Banned(profile)
                } else {
                    _uiState.value = AuthUiState.Authenticated(user, profile)
                }

                // Attach real-time profile listener so if admin bans or updates role, UI updates live
                userRepository.observeUserProfile(user.uid)
                    .catch { /* ignore stream err */ }
                    .collect { updatedProfile ->
                        if (updatedProfile != null) {
                            if (updatedProfile.isBanned) {
                                _uiState.value = AuthUiState.Banned(updatedProfile)
                            } else {
                                _uiState.value = AuthUiState.Authenticated(user, updatedProfile)
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "Failed to synchronize profile")
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        _uiState.value = AuthUiState.Loading
        AuthManager.signOut(
            context = getApplication(),
            credentialManager = credentialManager,
            onSignOutComplete = {
                _uiState.value = AuthUiState.Unauthenticated
                onComplete()
            },
            scope = viewModelScope
        )
    }
}
