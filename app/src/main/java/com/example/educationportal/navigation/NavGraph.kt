package com.example.educationportal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.ui.screens.ClassDetailScreen
import com.example.educationportal.ui.screens.LoginScreen
import com.example.educationportal.ui.screens.RegisterScreen
import com.example.educationportal.ui.screens.SplashScreen
import com.example.educationportal.ui.screens.StudentHomeScreen
import com.example.educationportal.ui.screens.TeacherHomeScreen
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.HomeViewModel
import com.example.educationportal.ui.viewmodel.LoginViewModel
import com.example.educationportal.ui.viewmodel.RegisterViewModel
import com.example.educationportal.ui.viewmodel.SplashViewModel
import com.example.educationportal.ui.viewmodel.ViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = NavRoutes.Splash.route
) {
    val context = LocalContext.current
    val viewModelFactory = remember { ViewModelFactory(context) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Splash.route) {
            val splashViewModel: SplashViewModel = viewModel(factory = viewModelFactory)
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = { userRole ->
                    val destination = when (userRole) {
                        UserRole.TEACHER -> NavRoutes.TeacherHome.route
                        UserRole.STUDENT -> NavRoutes.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Login.route) {
            val loginViewModel: LoginViewModel = viewModel(factory = viewModelFactory)
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToRegister = {
                    navController.navigate(NavRoutes.Register.route)
                },
                onNavigateToHome = { userRole ->
                    val destination = when (userRole) {
                        UserRole.TEACHER -> NavRoutes.TeacherHome.route
                        UserRole.STUDENT -> NavRoutes.StudentHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Register.route) {
            val registerViewModel: RegisterViewModel = viewModel(factory = viewModelFactory)
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToLogin = {
                    // Navigate to login, clearing register from back stack
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // This won't be used since registration redirects to login
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(NavRoutes.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.TeacherHome.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val classroomViewModel: ClassroomViewModel = viewModel(factory = viewModelFactory)
            TeacherHomeScreen(
                homeViewModel = homeViewModel,
                classroomViewModel = classroomViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToClassDetail = { classroomId ->
                    navController.navigate(NavRoutes.ClassDetail.createRoute(classroomId))
                }
            )
        }

        composable(NavRoutes.StudentHome.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val classroomViewModel: ClassroomViewModel = viewModel(factory = viewModelFactory)
            StudentHomeScreen(
                homeViewModel = homeViewModel,
                classroomViewModel = classroomViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToClassDetail = { classroomId ->
                    navController.navigate(NavRoutes.ClassDetail.createRoute(classroomId))
                }
            )
        }

        composable(
            route = NavRoutes.ClassDetail.route,
            arguments = listOf(
                navArgument("classroomId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val classroomId = backStackEntry.arguments?.getInt("classroomId") ?: return@composable
            val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
            val classroomViewModel: ClassroomViewModel = viewModel(factory = viewModelFactory)
            ClassDetailScreen(
                classroomId = classroomId,
                classroomViewModel = classroomViewModel,
                homeViewModel = homeViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
