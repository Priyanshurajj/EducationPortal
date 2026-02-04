package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.AuthResponse
import com.example.educationportal.data.model.LoginRequest
import com.example.educationportal.data.model.RegisterRequest
import com.example.educationportal.data.model.User
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

    suspend fun login(email: String, password: String): Resource<AuthResponse> {
        return try {
            val response = apiService.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    tokenManager.saveToken(authResponse.accessToken)
                    authResponse.user?.let { user ->
                        tokenManager.saveUserInfo(user.email, user.fullName)
                    }
                    Resource.Success(authResponse)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, com.example.educationportal.data.model.ApiError::class.java).detail
                } catch (e: Exception) {
                    "Login failed: ${response.code()}"
                }
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun register(email: String, password: String, fullName: String): Resource<AuthResponse> {
        return try {
            val response = apiService.register(RegisterRequest(email, password, fullName))
            if (response.isSuccessful) {
                response.body()?.let { authResponse ->
                    tokenManager.saveToken(authResponse.accessToken)
                    authResponse.user?.let { user ->
                        tokenManager.saveUserInfo(user.email, user.fullName)
                    } ?: tokenManager.saveUserInfo(email, fullName)
                    Resource.Success(authResponse)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    Gson().fromJson(errorBody, com.example.educationportal.data.model.ApiError::class.java).detail
                } catch (e: Exception) {
                    "Registration failed: ${response.code()}"
                }
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
                    tokenManager.saveUserInfo(user.email, user.fullName)
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
            val token = tokenManager.getToken()
            if (token != null) {
                apiService.logout("Bearer $token")
            }
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
}
