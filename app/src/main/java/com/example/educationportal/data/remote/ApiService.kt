package com.example.educationportal.data.remote

import com.example.educationportal.data.model.AuthResponse
import com.example.educationportal.data.model.ChatHistoryResponse
import com.example.educationportal.data.model.ChatMessage
import com.example.educationportal.data.model.Classroom
import com.example.educationportal.data.model.ClassroomDetail
import com.example.educationportal.data.model.ClassroomListResponse
import com.example.educationportal.data.model.CreateClassroomRequest
import com.example.educationportal.data.model.EnrollClassroomRequest
import com.example.educationportal.data.model.LoginRequest
import com.example.educationportal.data.model.MaterialListResponse
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.model.RegisterRequest
import com.example.educationportal.data.model.User
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ApiService {

    // ==================== Auth Endpoints ====================

    @POST("api/auth/login/json")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<User>

    @GET("api/auth/me")
    suspend fun getCurrentUser(
        @Header("Authorization") token: String
    ): Response<User>

    // ==================== Classroom Endpoints ====================

    @POST("api/classrooms/create")
    suspend fun createClassroom(
        @Header("Authorization") token: String,
        @Body request: CreateClassroomRequest
    ): Response<Classroom>

    @GET("api/classrooms/my-classes")
    suspend fun getMyClassrooms(
        @Header("Authorization") token: String
    ): Response<ClassroomListResponse>

    @GET("api/classrooms/{classroomId}")
    suspend fun getClassroomDetail(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int
    ): Response<ClassroomDetail>

    @POST("api/classrooms/enroll")
    suspend fun enrollInClassroom(
        @Header("Authorization") token: String,
        @Body request: EnrollClassroomRequest
    ): Response<Classroom>

    @DELETE("api/classrooms/{classroomId}/unenroll")
    suspend fun unenrollFromClassroom(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int
    ): Response<Unit>

    @DELETE("api/classrooms/{classroomId}")
    suspend fun deleteClassroom(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int
    ): Response<Unit>

    // ==================== Material Endpoints ====================

    @GET("api/classrooms/{classroomId}/materials")
    suspend fun getClassroomMaterials(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int
    ): Response<MaterialListResponse>

    @Multipart
    @POST("api/classrooms/{classroomId}/materials")
    suspend fun uploadMaterial(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part file: MultipartBody.Part
    ): Response<Material>

    @DELETE("api/classrooms/{classroomId}/materials/{materialId}")
    suspend fun deleteMaterial(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int,
        @Path("materialId") materialId: Int
    ): Response<Unit>

    @Streaming
    @GET("api/classrooms/{classroomId}/materials/{materialId}/download")
    suspend fun downloadMaterial(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int,
        @Path("materialId") materialId: Int
    ): Response<ResponseBody>

    // ==================== Chat Endpoints ====================

    @GET("api/classrooms/{classroomId}/messages")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): Response<ChatHistoryResponse>

    @GET("api/classrooms/{classroomId}/messages/recent")
    suspend fun getRecentMessages(
        @Header("Authorization") token: String,
        @Path("classroomId") classroomId: Int,
        @Query("limit") limit: Int = 20,
        @Query("before_id") beforeId: Int? = null
    ): Response<List<ChatMessage>>

}
