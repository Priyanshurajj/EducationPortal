package com.example.educationportal.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.educationportal.data.model.AiChatMessage
import com.example.educationportal.data.model.SourceType
import com.example.educationportal.data.model.SummarizeResponse
import com.example.educationportal.data.repository.AiRepository
import com.example.educationportal.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * UI State for material upload screen
 */
data class MaterialUploadUiState(
    val sourceType: SourceType = SourceType.URL,
    val urlInput: String = "",
    val textInput: String = "",
    val titleInput: String = "",
    val selectedFile: File? = null,
    val selectedFileName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val summarizeResponse: SummarizeResponse? = null
)

/**
 * UI State for AI summary and chat screen
 */
data class AiSummaryUiState(
    val sessionId: String = "",
    val title: String = "",
    val summary: String = "",
    val sourceType: String = "",
    val wordCount: Int = 0,
    val chunkCount: Int = 0,
    val chatMessages: List<AiChatMessage> = emptyList(),
    val chatInput: String = "",
    val isLoading: Boolean = false,
    val isSendingMessage: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Events for material upload
 */
sealed class MaterialUploadEvent {
    data class SourceTypeChanged(val sourceType: SourceType) : MaterialUploadEvent()
    data class UrlInputChanged(val url: String) : MaterialUploadEvent()
    data class TextInputChanged(val text: String) : MaterialUploadEvent()
    data class TitleInputChanged(val title: String) : MaterialUploadEvent()
    data class FileSelected(val file: File, val fileName: String) : MaterialUploadEvent()
    data object ClearFile : MaterialUploadEvent()
    data object Submit : MaterialUploadEvent()
    data object ClearError : MaterialUploadEvent()
    data object Reset : MaterialUploadEvent()
}

/**
 * Events for AI chat
 */
sealed class AiChatEvent {
    data class ChatInputChanged(val input: String) : AiChatEvent()
    data object SendMessage : AiChatEvent()
    data object ClearError : AiChatEvent()
    data object EndSession : AiChatEvent()
}

/**
 * ViewModel for AI Study Assistant feature
 */
class AiSummaryViewModel(
    private val aiRepository: AiRepository
) : ViewModel() {
    private val _uploadState = MutableStateFlow(MaterialUploadUiState())
    val uploadState: StateFlow<MaterialUploadUiState> = _uploadState.asStateFlow()

    private val _summaryState = MutableStateFlow(AiSummaryUiState())
    val summaryState: StateFlow<AiSummaryUiState> = _summaryState.asStateFlow()

    private var messageIdCounter = 0
    init {
        Log.d("AiVM", "AISummary Viewmodel initialized")
    }

    fun onUploadEvent(event: MaterialUploadEvent) {
        when (event) {
            is MaterialUploadEvent.SourceTypeChanged -> {
                _uploadState.update { it.copy(sourceType = event.sourceType, errorMessage = null) }
            }
            is MaterialUploadEvent.UrlInputChanged -> {
                _uploadState.update { it.copy(urlInput = event.url, errorMessage = null) }
            }
            is MaterialUploadEvent.TextInputChanged -> {
                _uploadState.update { it.copy(textInput = event.text, errorMessage = null) }
            }
            is MaterialUploadEvent.TitleInputChanged -> {
                _uploadState.update { it.copy(titleInput = event.title) }
            }
            is MaterialUploadEvent.FileSelected -> {
                _uploadState.update { 
                    it.copy(
                        selectedFile = event.file, 
                        selectedFileName = event.fileName,
                        errorMessage = null
                    ) 
                }
            }
            is MaterialUploadEvent.ClearFile -> {
                _uploadState.update { it.copy(selectedFile = null, selectedFileName = "") }
            }
            is MaterialUploadEvent.Submit -> {
                submitMaterial()
            }
            is MaterialUploadEvent.ClearError -> {
                _uploadState.update { it.copy(errorMessage = null) }
            }
            is MaterialUploadEvent.Reset -> {
                _uploadState.value = MaterialUploadUiState()
            }
        }
    }

    fun onChatEvent(event: AiChatEvent) {
        when (event) {
            is AiChatEvent.ChatInputChanged -> {
                _summaryState.update { it.copy(chatInput = event.input, errorMessage = null) }
            }
            is AiChatEvent.SendMessage -> {
                sendChatMessage()
            }
            is AiChatEvent.ClearError -> {
                _summaryState.update { it.copy(errorMessage = null) }
            }
            is AiChatEvent.EndSession -> {
                endSession()
            }
        }
    }

    /**
     * Initialize summary state from summarize response
     */
    fun initializeSummaryState(response: SummarizeResponse) {
        _summaryState.update {
            it.copy(
                sessionId = response.sessionId,
                title = response.title,
                summary = response.summary,
                sourceType = response.sourceType,
                wordCount = response.wordCount,
                chunkCount = response.chunkCount,
                chatMessages = emptyList(),
                chatInput = "",
                isLoading = false,
                errorMessage = null
            )
        }
        messageIdCounter = 0
    }

    private fun submitMaterial() {
        val currentState = _uploadState.value
        
        // Validate input based on source type
        when (currentState.sourceType) {
            SourceType.URL -> {
                if (currentState.urlInput.isBlank()) {
                    _uploadState.update { it.copy(errorMessage = "Please enter a URL") }
                    return
                }
                if (!currentState.urlInput.startsWith("http://") && !currentState.urlInput.startsWith("https://")) {
                    _uploadState.update { it.copy(errorMessage = "URL must start with http:// or https://") }
                    return
                }
            }
            SourceType.TEXT -> {
                if (currentState.textInput.isBlank()) {
                    _uploadState.update { it.copy(errorMessage = "Please enter some text") }
                    return
                }
                if (currentState.textInput.length < 100) {
                    _uploadState.update { it.copy(errorMessage = "Text must be at least 100 characters") }
                    return
                }
            }
            SourceType.PDF, SourceType.TXT -> {
                if (currentState.selectedFile == null) {
                    _uploadState.update { it.copy(errorMessage = "Please select a file") }
                    return
                }
            }
        }

        viewModelScope.launch {
            _uploadState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = when (currentState.sourceType) {
                SourceType.URL -> aiRepository.summarizeUrl(
                    url = currentState.urlInput,
                    title = currentState.titleInput.ifBlank { null }
                )
                SourceType.TEXT -> aiRepository.summarizeText(
                    text = currentState.textInput,
                    title = currentState.titleInput.ifBlank { null }
                )
                SourceType.PDF -> aiRepository.summarizePdf(
                    file = currentState.selectedFile!!,
                    title = currentState.titleInput.ifBlank { null }
                )
                SourceType.TXT -> aiRepository.summarizeTxtFile(
                    file = currentState.selectedFile!!,
                    title = currentState.titleInput.ifBlank { null }
                )
            }
            Log.d("AI","The result fetched from ai : ${result.data}")
            when (result) {
                is Resource.Success -> {
                    _uploadState.update { 
                        it.copy(
                            isLoading = false, 
                            summarizeResponse = result.data
                        ) 
                    }
                    result.data?.let { initializeSummaryState(it) }
                }
                is Resource.Error -> {
                    _uploadState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun sendChatMessage() {
        val currentState = _summaryState.value
        val message = currentState.chatInput.trim()
        
        if (message.isBlank()) return
        if (currentState.sessionId.isBlank()) {
            _summaryState.update { it.copy(errorMessage = "No active session") }
            return
        }

        // Add user message to chat
        val userMessage = AiChatMessage(
            id = ++messageIdCounter,
            content = message,
            isUser = true
        )
        
        _summaryState.update { 
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                chatInput = "",
                isSendingMessage = true,
                errorMessage = null
            ) 
        }

        viewModelScope.launch {
            when (val result = aiRepository.chat(currentState.sessionId, message)) {
                is Resource.Success -> {
                    result.data?.let { response ->
                        val aiMessage = AiChatMessage(
                            id = ++messageIdCounter,
                            content = response.response,
                            isUser = false,
                            sources = response.sources
                        )
                        _summaryState.update { 
                            it.copy(
                                chatMessages = it.chatMessages + aiMessage,
                                isSendingMessage = false
                            ) 
                        }
                    }
                }
                is Resource.Error -> {
                    _summaryState.update { 
                        it.copy(
                            isSendingMessage = false,
                            errorMessage = result.message
                        ) 
                    }
                }
                is Resource.Loading -> {
                    // Already handled
                }
            }
        }
    }

    private fun endSession() {
        val sessionId = _summaryState.value.sessionId
        if (sessionId.isBlank()) return

        viewModelScope.launch {
            aiRepository.deleteSession(sessionId)
            _summaryState.value = AiSummaryUiState()
            _uploadState.value = MaterialUploadUiState()
        }
    }

    /**
     * Check if there's an active summary session
     */
    fun hasActiveSession(): Boolean {
        return _summaryState.value.sessionId.isNotBlank()
    }

    /**
     * Get the current session ID
     */
    fun getCurrentSessionId(): String {
        return _summaryState.value.sessionId
    }
}
