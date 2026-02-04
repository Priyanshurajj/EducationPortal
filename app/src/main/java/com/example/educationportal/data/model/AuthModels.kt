package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

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
    val role: String = "student"
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

data class ValidationError(
    @SerializedName("detail")
    val detail: List<ValidationErrorDetail>?
)

data class ValidationErrorDetail(
    @SerializedName("loc")
    val loc: List<String>?,

    @SerializedName("msg")
    val msg: String?,

    @SerializedName("type")
    val type: String?
)
