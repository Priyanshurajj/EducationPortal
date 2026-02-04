package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.ApiError
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

    suspend fun login(email: String, password: String): Resource<UserRole> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    // Save the token first
                    tokenManager.saveToken(authResponse.accessToken)
                    
                    // Fetch user details to get role
                    val userResponse = apiService.getCurrentUser("Bearer ${authResponse.accessToken}")
                    if (userResponse.isSuccessful) {
                        userResponse.body()?.let { user ->
                            tokenManager.saveUserInfo(user.email, user.fullName, user.role.name.lowercase())
                            Resource.Success(user.role)
                        } ?: Resource.Error("Failed to get user details")
                    } else {
                        Resource.Error("Failed to get user details: ${userResponse.code()}")
                    }
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun register(email: String, password: String, fullName: String, role: String): Resource<User> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, fullName, role))
            if (response.isSuccessful) {
                response.body()?.let { user ->
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
                    tokenManager.saveUserInfo(user.email, user.fullName, user.role.name.lowercase())
                    Resource.Success(user)
                } ?: Resource.Error("Empty response body")
            } else {
                Resource.Error("Failed to get user: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun getUserRole(): UserRole? {
        return tokenManager.getUserRole()?.let { roleStr ->
            try {
                UserRole.valueOf(roleStr.uppercase())
            } catch (e: Exception) {
                null
            }
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

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "Unknown error occurred"

        return try {
            val simpleError = Gson().fromJson(errorBody, ApiError::class.java)
            simpleError.detail
        } catch (e: Exception) {
            try {
                val validationError = Gson().fromJson(errorBody, ValidationError::class.java)
                validationError.detail?.firstOrNull()?.msg ?: "Validation error"
            } catch (e: Exception) {
                "Request failed"
            }
        }
    }
}
