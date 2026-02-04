package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ChatMessage(
    @SerializedName("id")
    val id: Int,
    @SerializedName("classroom_id")
    val classroomId: Int,
    @SerializedName("sender_id")
    val senderId: Int,
    @SerializedName("sender_name")
    val senderName: String,
    @SerializedName("sender_role")
    val senderRole: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("sent_at")
    val sentAt: String
) {
    fun getFormattedTime(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(sentAt.substringBefore(".").substringBefore("+"))
            
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            date?.let { outputFormat.format(it) } ?: sentAt
        } catch (e: Exception) {
            sentAt.substringAfter("T").substringBefore(".")
        }
    }
    
    fun getFormattedDate(): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(sentAt.substringBefore(".").substringBefore("+"))
            
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            outputFormat.timeZone = TimeZone.getDefault()
            date?.let { outputFormat.format(it) } ?: ""
        } catch (e: Exception) {
            sentAt.substringBefore("T")
        }
    }
    
    fun isTeacher(): Boolean = senderRole == "teacher"
}

data class ChatHistoryResponse(
    @SerializedName("messages")
    val messages: List<ChatMessage>,
    @SerializedName("total")
    val total: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("page_size")
    val pageSize: Int,
    @SerializedName("has_more")
    val hasMore: Boolean
)

data class SendMessageRequest(
    @SerializedName("classroom_id")
    val classroomId: Int,
    @SerializedName("content")
    val content: String
)
