package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id")
    val id: Int,

    @SerializedName("email")
    val email: String,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("role")
    val role: String, // "teacher" or "student"

    @SerializedName("created_at")
    val createdAt: String? = null
) {
    fun getUserRole(): UserRole = UserRole.fromString(role)
    
    fun isTeacher(): Boolean = role.lowercase() == "teacher"
    
    fun isStudent(): Boolean = role.lowercase() == "student"
}
