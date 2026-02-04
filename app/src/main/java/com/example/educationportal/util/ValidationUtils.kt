package com.example.educationportal.util

object ValidationUtils {

    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(false, "Email cannot be empty")
        }
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        if (!email.matches(emailPattern.toRegex())) {
            return ValidationResult(false, "Invalid email format")
        }
        return ValidationResult(true)
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(false, "Password cannot be empty")
        }
        if (password.length < 8) {
            return ValidationResult(false, "Password must be at least 8 characters")
        }
        if (!password.any { it.isUpperCase() }) {
            return ValidationResult(false, "Password must contain at least one uppercase letter")
        }
        if (!password.any { it.isLowerCase() }) {
            return ValidationResult(false, "Password must contain at least one lowercase letter")
        }
        if (!password.any { it.isDigit() }) {
            return ValidationResult(false, "Password must contain at least one digit")
        }
        return ValidationResult(true)
    }

    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        if (confirmPassword.isBlank()) {
            return ValidationResult(false, "Confirm password cannot be empty")
        }
        if (password != confirmPassword) {
            return ValidationResult(false, "Passwords do not match")
        }
        return ValidationResult(true)
    }

    fun validateFullName(fullName: String): ValidationResult {
        if (fullName.isBlank()) {
            return ValidationResult(false, "Full name cannot be empty")
        }
        if (fullName.length < 2) {
            return ValidationResult(false, "Full name must be at least 2 characters")
        }
        return ValidationResult(true)
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
