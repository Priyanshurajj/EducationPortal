package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

data class Material(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("file_name")
    val fileName: String,

    @SerializedName("file_type")
    val fileType: String,

    @SerializedName("file_size")
    val fileSize: Int,

    @SerializedName("classroom_id")
    val classroomId: Int,

    @SerializedName("uploaded_by")
    val uploadedBy: Int,

    @SerializedName("uploaded_at")
    val uploadedAt: String,

    @SerializedName("uploader_name")
    val uploaderName: String?
) {
    fun getFormattedFileSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }
    }

    fun getFileIcon(): String {
        return when (fileType.lowercase()) {
            "pdf" -> "📄"
            "txt" -> "📝"
            "doc", "docx" -> "📃"
            "ppt", "pptx" -> "📊"
            "xls", "xlsx" -> "📈"
            else -> "📁"
        }
    }
}

data class MaterialListResponse(
    @SerializedName("materials")
    val materials: List<Material>,

    @SerializedName("total")
    val total: Int
)
