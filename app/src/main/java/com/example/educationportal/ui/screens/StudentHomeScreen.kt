package com.example.educationportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationportal.data.model.Classroom
import com.example.educationportal.ui.viewmodel.ClassroomEvent
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.EnrollClassroomState
import com.example.educationportal.ui.viewmodel.HomeViewModel

// Student theme colors - Blue with Black gradient
private val StudentPrimary = Color(0xFF2196F3) // Blue
private val StudentBackground = Color(0xFF000000) // Pure Black
private val StudentSurface = Color(0xFF0A1929) // Dark blue tint
private val StudentCardBg = Color(0xFF1A2744) // Card background with blue tint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    homeViewModel: HomeViewModel,
    classroomViewModel: ClassroomViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToClassDetail: (Int) -> Unit,
    onNavigateToAiUpload: () -> Unit = {}
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val listState by classroomViewModel.listState.collectAsState()
    val enrollState by classroomViewModel.enrollState.collectAsState()

    var showEnrollDialog by remember { mutableStateOf(false) }

    // Handle logout
    LaunchedEffect(homeState.isLoggedOut) {
        if (homeState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        StudentBackground,
                        Color(0xFF001529), // Dark blue tint
                        StudentSurface
                    )
                )
            )
            .systemBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Header
            item {
                StudentHeader(
                    userName = homeState.userName,
                    onLogout = { homeViewModel.logout() }
                )
            }

            // AI Study Assistant Card
            item {
                AiStudyAssistantCard(
                    onClick = onNavigateToAiUpload,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // My Classes Section Title
            item {
                Text(
                    text = "My Classes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (listState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = StudentPrimary)
                    }
                }
            } else if (listState.classrooms.isEmpty()) {
                item {
                    EmptyEnrolledClassesCard(
                        onEnrollClick = { showEnrollDialog = true }
                    )
                }
            } else {
                items(listState.classrooms) { classroom ->
                    StudentClassCard(
                        classroom = classroom,
                        onClick = { onNavigateToClassDetail(classroom.id) }
                    )
                }
            }
        }

        // FAB for enrolling
        FloatingActionButton(
            onClick = { showEnrollDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = StudentPrimary,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Join Class")
        }

        // Error Snackbar
        listState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { classroomViewModel.onEvent(ClassroomEvent.ClearError) }) {
                        Text("Dismiss", color = StudentPrimary)
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Enroll Dialog
    if (showEnrollDialog) {
        EnrollClassDialog(
            state = enrollState,
            onCodeChange = { classroomViewModel.onEvent(ClassroomEvent.EnrollCodeChanged(it)) },
            onConfirm = { classroomViewModel.onEvent(ClassroomEvent.EnrollInClassroom) },
            onDismiss = {
                showEnrollDialog = false
                classroomViewModel.onEvent(ClassroomEvent.ResetEnrollState)
            }
        )
    }

    // Handle enroll success
    LaunchedEffect(enrollState.isSuccess) {
        if (enrollState.isSuccess) {
            showEnrollDialog = false
            classroomViewModel.onEvent(ClassroomEvent.ResetEnrollState)
        }
    }
}

@Composable
private fun StudentHeader(
    userName: String,
    onLogout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = userName.ifEmpty { "Student" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Student Portal",
                style = MaterialTheme.typography.labelMedium,
                color = StudentPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(onClick = onLogout) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun StudentClassCard(
    classroom: Classroom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudentPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Class,
                    contentDescription = null,
                    tint = StudentPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classroom.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                classroom.teacher?.let { teacher ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = teacher.fullName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyEnrolledClassesCard(
    onEnrollClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = StudentPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No classes yet. Join a class with a code!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onEnrollClick,
                colors = ButtonDefaults.buttonColors(containerColor = StudentPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Join Class", fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnrollClassDialog(
    state: EnrollClassroomState,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudentSurface,
        title = {
            Text("Join a Class", color = Color.White, fontSize = 18.sp)
        },
        text = {
            Column {
                Text(
                    text = "Enter the class code from your teacher",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.classCode,
                    onValueChange = { onCodeChange(it.uppercase()) },
                    label = { Text("Class Code", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudentPrimary,
                        focusedLabelColor = StudentPrimary,
                        cursorColor = StudentPrimary,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
                state.errorMessage?.let { error ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = StudentPrimary)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Join")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}

@Composable
private fun AiStudyAssistantCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0).copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF9C27B0).copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF9C27B0),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column {
                    Text(
                        text = "AI Study Assistant",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Get summaries and ask questions",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
