package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

enum class UserRole(val value: String) {
    @SerializedName("teacher")
    TEACHER("teacher"),

    @SerializedName("student")
    STUDENT("student");

    companion object {
        fun fromString(value: String): UserRole {
            return entries.find { it.value == value.lowercase() } ?: STUDENT
        }
    }
}

data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

data class RegisterRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("role")
    val role: String // "teacher" or "student"
)

data class AuthResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("token_type")
    val tokenType: String = "bearer"
)

data class ApiError(
    @SerializedName("detail")
    val detail: String
)

// For validation error responses from FastAPI
data class ValidationError(
    @SerializedName("detail")
    val detail: List<ValidationErrorDetail>?
)

data class ValidationErrorDetail(
    @SerializedName("type")
    val type: String,

    @SerializedName("loc")
    val loc: List<String>,

    @SerializedName("msg")
    val msg: String
)
