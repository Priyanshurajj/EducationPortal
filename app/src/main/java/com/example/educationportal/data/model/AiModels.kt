package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

/**
 * Enum for content source types
 */
enum class SourceType(val value: String) {
    @SerializedName("url")
    URL("url"),
    
    @SerializedName("text")
    TEXT("text"),
    
    @SerializedName("pdf")
    PDF("pdf"),
    
    @SerializedName("txt")
    TXT("txt")
}

/**
 * Response from the summarize endpoint
 */
data class SummarizeResponse(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("summary")
    val summary: String,
    
    @SerializedName("source_type")
    val sourceType: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("chunk_count")
    val chunkCount: Int,
    
    @SerializedName("word_count")
    val wordCount: Int
)

/**
 * Request for RAG chat
 */
data class AiChatRequest(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("message")
    val message: String
)

/**
 * Response from RAG chat
 */
data class AiChatResponse(
    @SerializedName("response")
    val response: String,
    
    @SerializedName("sources")
    val sources: List<String> = emptyList()
)

/**
 * Session information
 */
data class SessionInfo(
    @SerializedName("session_id")
    val sessionId: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("source_type")
    val sourceType: String,
    
    @SerializedName("chunk_count")
    val chunkCount: Int,
    
    @SerializedName("word_count")
    val wordCount: Int
)

/**
 * Response for session deletion
 */
data class SessionDeleteResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String
)

/**
 * Chat message for AI conversation
 */
data class AiChatMessage(
    val id: Int,
    val content: String,
    val isUser: Boolean,
    val sources: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
