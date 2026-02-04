package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SplashUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean? = null,
    val userRole: UserRole? = null
)

class SplashViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState: StateFlow<SplashUiState> = _uiState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            delay(1500) // Minimum splash display time
            val isLoggedIn = authRepository.isLoggedIn()
            val userRole = if (isLoggedIn) authRepository.getUserRole() else null
            _uiState.value = SplashUiState(
                isLoading = false,
                isLoggedIn = isLoggedIn,
                userRole = userRole
            )
        }
    }
}
