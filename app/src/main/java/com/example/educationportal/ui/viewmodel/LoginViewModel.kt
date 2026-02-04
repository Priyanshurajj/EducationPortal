package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.data.repository.AuthRepository
import com.example.educationportal.util.Resource
import com.example.educationportal.util.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val isNavigated: Boolean = false,
    val userRole: UserRole? = null,
    val errorMessage: String? = null,
    val isPasswordVisible: Boolean = false
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class PasswordChanged(val password: String) : LoginEvent()
    data object TogglePasswordVisibility : LoginEvent()
    data object Login : LoginEvent()
    data object ClearError : LoginEvent()
    data object NavigationHandled : LoginEvent()
}

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email, emailError = null) }
            }
            is LoginEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.password, passwordError = null) }
            }
            is LoginEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is LoginEvent.Login -> {
                login()
            }
            is LoginEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            is LoginEvent.NavigationHandled -> {
                _uiState.update { it.copy(isNavigated = true) }
            }
        }
    }

    private fun login() {
        val currentState = _uiState.value

        // Prevent multiple submissions
        if (currentState.isLoading || currentState.isSuccess) {
            return
        }

        // Validate email
        val emailValidation = ValidationUtils.validateEmail(currentState.email)
        if (!emailValidation.isValid) {
            _uiState.update { it.copy(emailError = emailValidation.errorMessage) }
            return
        }

        // Basic password check (not full validation for login)
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.login(currentState.email, currentState.password)) {
                is Resource.Success -> {
                    // Get user role after successful login
                    val userRole = authRepository.getUserRole()
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            isSuccess = true,
                            userRole = userRole
                        ) 
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.message)
                    }
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = LoginUiState()
    }
}
