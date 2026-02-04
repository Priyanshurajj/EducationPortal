package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.AuthResponse
import com.example.educationportal.data.model.LoginRequest
import com.example.educationportal.data.model.RegisterRequest
import com.example.educationportal.data.model.User
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.data.model.ValidationError
import com.example.educationportal.data.remote.ApiService
import com.example.educationportal.util.Resource
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    val accessToken: Flow<String?> = tokenManager.accessToken
    val userEmail: Flow<String?> = tokenManager.userEmail
    val userName: Flow<String?> = tokenManager.userName
    val userRole: Flow<String?> = tokenManager.userRole

    suspend fun login(email: String, password: String): Resource<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    tokenManager.saveToken(authResponse.accessToken)
                    // After login, fetch user info to get role
                    fetchAndSaveUserInfo(authResponse.accessToken)
                    Resource.Success(authResponse)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun register(email: String, password: String, fullName: String, role: UserRole): Resource<User> {
        return try {
            val response = apiService.register(
                RegisterRequest(
                    email = email,
                    password = password,
                    fullName = fullName,
                    role = role.value
                )
            )
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    // Save user info for display (but not token - user needs to login)
                    tokenManager.saveUserInfo(user.email, user.fullName, user.role)
                    Resource.Success(user)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun getCurrentUser(): Resource<User> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    tokenManager.saveUserInfo(user.email, user.fullName, user.role)
                    Resource.Success(user)
                } ?: Resource.Error("Empty response body")
            } else {
                Resource.Error("Failed to get user: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun logout(): Resource<Unit> {
        return try {
            tokenManager.clearAll()
            Resource.Success(Unit)
        } catch (e: Exception) {
            tokenManager.clearAll()
            Resource.Success(Unit)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    suspend fun getToken(): String? {
        return tokenManager.getToken()
    }

    suspend fun getUserRole(): UserRole {
        return tokenManager.getUserRole()
    }

    private suspend fun fetchAndSaveUserInfo(token: String) {
        try {
            val response = apiService.getCurrentUser("Bearer $token")
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    tokenManager.saveUserInfo(user.email, user.fullName, user.role)
                }
            }
        } catch (e: Exception) {
            // Silently fail - user info will be fetched later
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "Unknown error occurred"

        return try {
            // Try parsing as simple error with detail string
            val simpleError = Gson().fromJson(errorBody, com.example.educationportal.data.model.ApiError::class.java)
            simpleError.detail
        } catch (e: Exception) {
            try {
                // Try parsing as validation error
                val validationError = Gson().fromJson(errorBody, ValidationError::class.java)
                validationError.detail?.firstOrNull()?.msg ?: "Validation error"
            } catch (e: Exception) {
                "Request failed"
            }
        }
    }
}
