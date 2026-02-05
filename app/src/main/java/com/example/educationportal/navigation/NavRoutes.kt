package com.example.educationportal.navigation

sealed class NavRoutes(val route: String) {
    data object Splash : NavRoutes("splash")
    data object Login : NavRoutes("login")
    data object Register : NavRoutes("register")
    data object TeacherHome : NavRoutes("teacher_home")
    data object StudentHome : NavRoutes("student_home")
    data object ClassDetail : NavRoutes("class_detail/{classroomId}") {
        fun createRoute(classroomId: Int) = "class_detail/$classroomId"
    }
    data object ClassChat : NavRoutes("class_chat/{classroomId}/{classroomName}") {
        fun createRoute(classroomId: Int, classroomName: String) = 
            "class_chat/$classroomId/${java.net.URLEncoder.encode(classroomName, "UTF-8")}"
    }
    data object AiUpload : NavRoutes("ai_upload")
    data object AiSummary : NavRoutes("ai_summary")
}
