package com.example.educationportal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.remote.RetrofitClient
import com.example.educationportal.data.repository.AuthRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val tokenManager by lazy { TokenManager(context) }
    private val authRepository by lazy {
        AuthRepository(RetrofitClient.apiService, tokenManager)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(authRepository) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(authRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
