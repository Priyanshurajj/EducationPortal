package com.example.educationportal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.remote.RetrofitClient
import com.example.educationportal.data.repository.AiRepository
import com.example.educationportal.data.repository.AuthRepository
import com.example.educationportal.data.repository.ChatRepository
import com.example.educationportal.data.repository.ClassroomRepository

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {

    private val tokenManager by lazy { TokenManager(context) }
    private val authRepository by lazy {
        AuthRepository(RetrofitClient.apiService, tokenManager)
    }
    private val classroomRepository by lazy {
        ClassroomRepository(RetrofitClient.apiService, tokenManager)
    }
    private val chatRepository by lazy {
        ChatRepository(RetrofitClient.apiService, tokenManager)
    }
    private val aiRepository by lazy {
        AiRepository(RetrofitClient.apiService, tokenManager)
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
            modelClass.isAssignableFrom(ClassroomViewModel::class.java) -> {
                ClassroomViewModel(classroomRepository) as T
            }
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(chatRepository) as T
            }
            modelClass.isAssignableFrom(AiSummaryViewModel::class.java) -> {
                AiSummaryViewModel(aiRepository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
