package com.example.educationportal.data.model

import com.google.gson.annotations.SerializedName

data class Classroom(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("class_code")
    val classCode: String,

    @SerializedName("teacher_id")
    val teacherId: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("teacher")
    val teacher: TeacherInfo?,

    @SerializedName("student_count")
    val studentCount: Int = 0
)

data class TeacherInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String
)

data class StudentInfo(
    @SerializedName("id")
    val id: Int,

    @SerializedName("full_name")
    val fullName: String,

    @SerializedName("email")
    val email: String
)

data class ClassroomDetail(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("class_code")
    val classCode: String,

    @SerializedName("teacher_id")
    val teacherId: Int,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("teacher")
    val teacher: TeacherInfo?,

    @SerializedName("students")
    val students: List<StudentInfo> = emptyList(),

    @SerializedName("student_count")
    val studentCount: Int = 0
)

data class ClassroomListResponse(
    @SerializedName("classrooms")
    val classrooms: List<Classroom>,

    @SerializedName("total")
    val total: Int
)

data class CreateClassroomRequest(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?
)

data class EnrollClassroomRequest(
    @SerializedName("class_code")
    val classCode: String
)
