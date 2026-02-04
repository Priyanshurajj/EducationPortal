package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.repository.AuthRepository
import com.example.educationportal.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val userName: String = "",
    val userEmail: String = "",
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)

sealed class HomeEvent {
    data object LoadUserInfo : HomeEvent()
    data object Logout : HomeEvent()
    data object ClearError : HomeEvent()
}

class HomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserInfo()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadUserInfo -> loadUserInfo()
            is HomeEvent.Logout -> logout()
            is HomeEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // First try to get from local storage
            authRepository.userName.collect { name ->
                if (name != null) {
                    _uiState.update { it.copy(userName = name, isLoading = false) }
                }
            }
        }

        viewModelScope.launch {
            authRepository.userEmail.collect { email ->
                if (email != null) {
                    _uiState.update { it.copy(userEmail = email) }
                }
            }
        }

        // Also try to refresh from API
        viewModelScope.launch {
            when (val result = authRepository.getCurrentUser()) {
                is Resource.Success -> {
                    result.data?.let { user ->
                        _uiState.update {
                            it.copy(
                                userName = user.fullName,
                                userEmail = user.email,
                                isLoading = false
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (authRepository.logout()) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, isLoggedOut = true) }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, isLoggedOut = true) }
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }
}
