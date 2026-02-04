package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

enum class UserRole {
    @SerializedName("student")
    STUDENT,

    @SerializedName("teacher")
    TEACHER
}

data class User(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("role")
    val role: UserRole = UserRole.STUDENT,

    @SerializedName("is_active")
    val isActive: Boolean = true,

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    fun isTeacher(): Boolean = role == UserRole.TEACHER
    fun isStudent(): Boolean = role == UserRole.STUDENT
}
