package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.ChatHistoryResponse
import com.example.educationportal.data.model.ChatMessage
import com.example.educationportal.data.remote.ApiService
import com.example.educationportal.data.remote.ConnectionState
import com.example.educationportal.data.remote.SocketManager
import com.example.educationportal.data.remote.TypingEvent
import com.example.educationportal.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class ChatRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val socketManager: SocketManager = SocketManager.getInstance()
) {
    
    val connectionState: StateFlow<ConnectionState> = socketManager.connectionState
    val incomingMessages: Flow<ChatMessage> = socketManager.messages
    val errors: Flow<String> = socketManager.errors
    val typingEvents: Flow<TypingEvent> = socketManager.typingUsers
    
    suspend fun connectSocket() {
        val token = tokenManager.getToken() ?: return
        socketManager.connect(token)
    }
    
    fun disconnectSocket() {
        socketManager.disconnect()
    }
    
    fun joinRoom(classroomId: Int) {
        socketManager.joinRoom(classroomId)
    }
    
    fun leaveRoom(classroomId: Int) {
        socketManager.leaveRoom(classroomId)
    }
    
    fun sendMessage(classroomId: Int, content: String) {
        socketManager.sendMessage(classroomId, content)
    }
    
    fun sendTyping(classroomId: Int) {
        socketManager.sendTyping(classroomId)
    }
    
    fun sendStopTyping(classroomId: Int) {
        socketManager.sendStopTyping(classroomId)
    }
    
    fun isConnected(): Boolean = socketManager.isConnected()
    
    fun isInRoom(classroomId: Int): Boolean = socketManager.isInRoom(classroomId)
    
    suspend fun getChatHistory(
        classroomId: Int,
        page: Int = 1,
        pageSize: Int = 50
    ): Resource<ChatHistoryResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            val response = apiService.getChatHistory("Bearer $token", classroomId, page, pageSize)
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load chat history: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
    
    suspend fun getRecentMessages(
        classroomId: Int,
        limit: Int = 20,
        beforeId: Int? = null
    ): Resource<List<ChatMessage>> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            val response = apiService.getRecentMessages("Bearer $token", classroomId, limit, beforeId)
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response")
            } else {
                Resource.Error("Failed to load messages: ${response.code()}")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}
