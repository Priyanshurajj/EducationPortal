package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.repository.AuthRepository
import com.example.educationportal.util.Resource
import com.example.educationportal.util.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val role: String = "student",
    val fullNameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false
)

sealed class RegisterEvent {
    data class FullNameChanged(val fullName: String) : RegisterEvent()
    data class EmailChanged(val email: String) : RegisterEvent()
    data class PasswordChanged(val password: String) : RegisterEvent()
    data class ConfirmPasswordChanged(val confirmPassword: String) : RegisterEvent()
    data class RoleChanged(val role: String) : RegisterEvent()
    data object TogglePasswordVisibility : RegisterEvent()
    data object ToggleConfirmPasswordVisibility : RegisterEvent()
    data object Register : RegisterEvent()
    data object ClearError : RegisterEvent()
}

class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.FullNameChanged -> {
                _uiState.update { it.copy(fullName = event.fullName, fullNameError = null) }
            }
            is RegisterEvent.EmailChanged -> {
                _uiState.update { it.copy(email = event.email, emailError = null) }
            }
            is RegisterEvent.PasswordChanged -> {
                _uiState.update { it.copy(password = event.password, passwordError = null) }
            }
            is RegisterEvent.ConfirmPasswordChanged -> {
                _uiState.update { it.copy(confirmPassword = event.confirmPassword, confirmPasswordError = null) }
            }
            is RegisterEvent.RoleChanged -> {
                _uiState.update { it.copy(role = event.role) }
            }
            is RegisterEvent.TogglePasswordVisibility -> {
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            }
            is RegisterEvent.ToggleConfirmPasswordVisibility -> {
                _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
            }
            is RegisterEvent.Register -> {
                register()
            }
            is RegisterEvent.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }

    private fun register() {
        val currentState = _uiState.value

        // Validate full name
        val fullNameValidation = ValidationUtils.validateFullName(currentState.fullName)
        if (!fullNameValidation.isValid) {
            _uiState.update { it.copy(fullNameError = fullNameValidation.errorMessage) }
            return
        }

        // Validate email
        val emailValidation = ValidationUtils.validateEmail(currentState.email)
        if (!emailValidation.isValid) {
            _uiState.update { it.copy(emailError = emailValidation.errorMessage) }
            return
        }

        // Validate password
        val passwordValidation = ValidationUtils.validatePassword(currentState.password)
        if (!passwordValidation.isValid) {
            _uiState.update { it.copy(passwordError = passwordValidation.errorMessage) }
            return
        }

        // Validate confirm password
        val confirmPasswordValidation = ValidationUtils.validateConfirmPassword(
            currentState.password,
            currentState.confirmPassword
        )
        if (!confirmPasswordValidation.isValid) {
            _uiState.update { it.copy(confirmPasswordError = confirmPasswordValidation.errorMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = authRepository.register(
                currentState.email,
                currentState.password,
                currentState.fullName,
                currentState.role
            )) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
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
        _uiState.value = RegisterUiState()
    }
}
