package com.example.educationportal.data.repository

import com.example.educationportal.data.local.TokenManager
import com.example.educationportal.data.model.ApiError
import com.example.educationportal.data.model.Classroom
import com.example.educationportal.data.model.ClassroomDetail
import com.example.educationportal.data.model.CreateClassroomRequest
import com.example.educationportal.data.model.EnrollClassroomRequest
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.model.ValidationError
import com.example.educationportal.data.remote.ApiService
import com.example.educationportal.util.Resource
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class ClassroomRepository(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {

    private suspend fun getAuthHeader(): String? {
        val token = tokenManager.getToken() ?: return null
        return "Bearer $token"
    }

    // ==================== Classroom Operations ====================

    suspend fun createClassroom(name: String, description: String?): Resource<Classroom> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.createClassroom(
                token,
                CreateClassroomRequest(name, description)
            )
            if (response.isSuccessful) {
                response.body()?.let { classroom ->
                    Resource.Success(classroom)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun getMyClassrooms(): Resource<List<Classroom>> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getMyClassrooms(token)
            if (response.isSuccessful) {
                response.body()?.let { listResponse ->
                    Resource.Success(listResponse.classrooms)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun getClassroomDetail(classroomId: Int): Resource<ClassroomDetail> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getClassroomDetail(token, classroomId)
            if (response.isSuccessful) {
                response.body()?.let { detail ->
                    Resource.Success(detail)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun enrollInClassroom(classCode: String): Resource<Classroom> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.enrollInClassroom(
                token,
                EnrollClassroomRequest(classCode.uppercase())
            )
            if (response.isSuccessful) {
                response.body()?.let { classroom ->
                    Resource.Success(classroom)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun unenrollFromClassroom(classroomId: Int): Resource<Unit> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.unenrollFromClassroom(token, classroomId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun deleteClassroom(classroomId: Int): Resource<Unit> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.deleteClassroom(token, classroomId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    // ==================== Material Operations ====================

    suspend fun getClassroomMaterials(classroomId: Int): Resource<List<Material>> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.getClassroomMaterials(token, classroomId)
            if (response.isSuccessful) {
                response.body()?.let { listResponse ->
                    Resource.Success(listResponse.materials)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun uploadMaterial(
        classroomId: Int,
        title: String,
        description: String?,
        file: File
    ): Resource<Material> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")

            val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
            val descriptionBody = description?.toRequestBody("text/plain".toMediaTypeOrNull())

            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val response = apiService.uploadMaterial(
                token,
                classroomId,
                titleBody,
                descriptionBody,
                filePart
            )

            if (response.isSuccessful) {
                response.body()?.let { material ->
                    Resource.Success(material)
                } ?: Resource.Error("Empty response body")
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun deleteMaterial(classroomId: Int, materialId: Int): Resource<Unit> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.deleteMaterial(token, classroomId, materialId)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Resource.Error(errorMessage)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun downloadMaterial(
        classroomId: Int,
        materialId: Int,
        file: File
    ): Resource<File> {
        return try {
            val token = getAuthHeader() ?: return Resource.Error("Not authenticated")
            val response = apiService.downloadMaterial(token, classroomId, materialId)

            if (response.isSuccessful) {
                response.body()?.let { body ->
                    body.byteStream().use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Resource.Success(file)
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
            val simpleError = Gson().fromJson(errorBody, ApiError::class.java)
            simpleError.detail
        } catch (e: Exception) {
            try {
                val validationError = Gson().fromJson(errorBody, ValidationError::class.java)
                validationError.detail?.firstOrNull()?.msg ?: "Validation error"
            } catch (e: Exception) {
                "Request failed"
            }
        }
    }
}
