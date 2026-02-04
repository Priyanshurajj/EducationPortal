package com.example.educationportal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.model.ChatMessage
import com.example.educationportal.data.remote.ConnectionState
import com.example.educationportal.data.repository.ChatRepository
import com.example.educationportal.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val errorMessage: String? = null,
    val hasMoreMessages: Boolean = true,
    val typingUsers: List<String> = emptyList(),
    val currentUserId: Int? = null
)

sealed class ChatEvent {
    data class Initialize(val classroomId: Int, val currentUserId: Int) : ChatEvent()
    data class SendMessage(val content: String) : ChatEvent()
    data object LoadMoreMessages : ChatEvent()
    data object Reconnect : ChatEvent()
    data object ClearError : ChatEvent()
    data class OnTyping(val isTyping: Boolean) : ChatEvent()
}

class ChatViewModel(
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var classroomId: Int = 0
    private var typingJob: Job? = null
    
    init {
        observeConnectionState()
        observeIncomingMessages()
        observeErrors()
        observeTypingEvents()
    }
    
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Initialize -> initialize(event.classroomId, event.currentUserId)
            is ChatEvent.SendMessage -> sendMessage(event.content)
            is ChatEvent.LoadMoreMessages -> loadMoreMessages()
            is ChatEvent.Reconnect -> reconnect()
            is ChatEvent.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            is ChatEvent.OnTyping -> handleTyping(event.isTyping)
        }
    }
    
    private fun initialize(classroomId: Int, currentUserId: Int) {
        this.classroomId = classroomId
        _uiState.update { it.copy(currentUserId = currentUserId) }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            // Connect to socket
            chatRepository.connectSocket()
            
            // Wait a bit for connection
            delay(500)
            
            // Join the room
            chatRepository.joinRoom(classroomId)
            
            // Load chat history
            loadChatHistory()
        }
    }
    
    private fun loadChatHistory() {
        viewModelScope.launch {
            when (val result = chatRepository.getChatHistory(classroomId)) {
                is Resource.Success -> {
                    result.data?.let { history ->
                        _uiState.update {
                            it.copy(
                                messages = history.messages,
                                isLoading = false,
                                hasMoreMessages = history.hasMore
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> { }
            }
        }
    }
    
    private fun loadMoreMessages() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreMessages) return
        
        val oldestMessage = _uiState.value.messages.firstOrNull() ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            
            when (val result = chatRepository.getRecentMessages(
                classroomId = classroomId,
                limit = 20,
                beforeId = oldestMessage.id
            )) {
                is Resource.Success -> {
                    result.data?.let { olderMessages ->
                        _uiState.update {
                            it.copy(
                                messages = olderMessages + it.messages,
                                isLoadingMore = false,
                                hasMoreMessages = olderMessages.isNotEmpty()
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            errorMessage = result.message
                        )
                    }
                }
                is Resource.Loading -> { }
            }
        }
    }
    
    private fun sendMessage(content: String) {
        val trimmedContent = content.trim()
        if (trimmedContent.isEmpty()) return
        
        chatRepository.sendMessage(classroomId, trimmedContent)
        chatRepository.sendStopTyping(classroomId)
    }
    
    private fun handleTyping(isTyping: Boolean) {
        typingJob?.cancel()
        
        if (isTyping) {
            chatRepository.sendTyping(classroomId)
            // Auto stop typing after 3 seconds of no input
            typingJob = viewModelScope.launch {
                delay(3000)
                chatRepository.sendStopTyping(classroomId)
            }
        } else {
            chatRepository.sendStopTyping(classroomId)
        }
    }
    
    private fun reconnect() {
        viewModelScope.launch {
            chatRepository.connectSocket()
            delay(500)
            chatRepository.joinRoom(classroomId)
        }
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            chatRepository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }
    }
    
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            chatRepository.incomingMessages.collect { message ->
                // Only add if it's for our classroom and not a duplicate
                if (message.classroomId == classroomId) {
                    _uiState.update { state ->
                        if (state.messages.none { it.id == message.id }) {
                            state.copy(messages = state.messages + message)
                        } else {
                            state
                        }
                    }
                }
            }
        }
    }
    
    private fun observeErrors() {
        viewModelScope.launch {
            chatRepository.errors.collect { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }
    
    private fun observeTypingEvents() {
        viewModelScope.launch {
            chatRepository.typingEvents.collect { event ->
                if (event.classroomId == classroomId && event.userId != _uiState.value.currentUserId) {
                    _uiState.update { state ->
                        val currentTyping = state.typingUsers.toMutableList()
                        if (event.isTyping) {
                            if (!currentTyping.contains(event.userName)) {
                                currentTyping.add(event.userName)
                            }
                        } else {
                            currentTyping.removeAll { it == event.userName }
                        }
                        state.copy(typingUsers = currentTyping)
                    }
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        if (classroomId > 0) {
            chatRepository.leaveRoom(classroomId)
        }
    }
}
