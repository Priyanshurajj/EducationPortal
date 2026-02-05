package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.AiChatRequest
import com.example.educationportal.data.model.AiChatResponse
import com.example.educationportal.data.model.ApiError
import com.example.educationportal.data.model.SessionDeleteResponse
import com.example.educationportal.data.model.SessionInfo
import com.example.educationportal.data.model.SourceType
import com.example.educationportal.data.model.SummarizeResponse
import com.example.educationportal.data.remote.ApiService
import com.example.educationportal.util.Resource
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class AiRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    /**
     * Summarize content from a URL
     */
    suspend fun summarizeUrl(url: String, title: String? = null): Resource<SummarizeResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val sourceTypeBody = SourceType.URL.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = url.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.summarizeMaterial(
                token = "Bearer $token",
                sourceType = sourceTypeBody,
                content = contentBody,
                title = titleBody,
                file = null
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Summarize raw text content
     */
    suspend fun summarizeText(text: String, title: String? = null): Resource<SummarizeResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val sourceTypeBody = SourceType.TEXT.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val contentBody = text.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.summarizeMaterial(
                token = "Bearer $token",
                sourceType = sourceTypeBody,
                content = contentBody,
                title = titleBody,
                file = null
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Summarize a PDF file
     */
    suspend fun summarizePdf(file: File, title: String? = null): Resource<SummarizeResponse> {
        return summarizeFile(file, SourceType.PDF, title)
    }

    /**
     * Summarize a text file
     */
    suspend fun summarizeTxtFile(file: File, title: String? = null): Resource<SummarizeResponse> {
        return summarizeFile(file, SourceType.TXT, title)
    }

    /**
     * Generic file summarization
     */
    private suspend fun summarizeFile(
        file: File,
        sourceType: SourceType,
        title: String? = null
    ): Resource<SummarizeResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val sourceTypeBody = sourceType.value.toRequestBody("text/plain".toMediaTypeOrNull())
            val titleBody = title?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val mimeType = when (sourceType) {
                SourceType.PDF -> "application/pdf"
                SourceType.TXT -> "text/plain"
                else -> "application/octet-stream"
            }
            
            val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = apiService.summarizeMaterial(
                token = "Bearer $token",
                sourceType = sourceTypeBody,
                content = null,
                title = titleBody,
                file = filePart
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Send a chat message to the AI
     */
    suspend fun chat(sessionId: String, message: String): Resource<AiChatResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val response = apiService.aiChat(
                token = "Bearer $token",
                request = AiChatRequest(sessionId, message)
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Get session information
     */
    suspend fun getSession(sessionId: String): Resource<SessionInfo> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val response = apiService.getAiSession(
                token = "Bearer $token",
                sessionId = sessionId
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    /**
     * Delete a session
     */
    suspend fun deleteSession(sessionId: String): Resource<SessionDeleteResponse> {
        return try {
            val token = tokenManager.getToken() ?: return Resource.Error("Not authenticated")
            
            val response = apiService.deleteAiSession(
                token = "Bearer $token",
                sessionId = sessionId
            )
            
            if (response.isSuccessful) {
                response.body()?.let {
                    Resource.Success(it)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    private fun parseErrorMessage(errorBody: String?): String {
        if (errorBody == null) return "Unknown error occurred"
        
        return try {
            val error = Gson().fromJson(errorBody, ApiError::class.java)
            error.detail
        } catch (e: Exception) {
            "Request failed"
        }
    }
}
