package com.example.educationportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educationportal.data.model.Classroom
import com.example.educationportal.ui.viewmodel.ClassroomEvent
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.EnrollClassroomState
import com.example.educationportal.ui.viewmodel.HomeViewModel

// Student theme colors
private val StudentPrimary = Color(0xFF14B8A6) // Teal
private val StudentPrimaryDark = Color(0xFF0D9488)
private val StudentBackground = Color(0xFF0F172A)
private val StudentSurface = Color(0xFF1E293B)
private val StudentCardBg = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    homeViewModel: HomeViewModel,
    classroomViewModel: ClassroomViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToClassDetail: (Int) -> Unit
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
                    colors = listOf(StudentBackground, StudentSurface)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                StudentHeader(
                    userName = homeState.userName,
                    onLogout = { homeViewModel.logout() }
                )
            }

            // Stats Cards
            item {
                StudentStatsSection(
                    classCount = listState.classrooms.size
                )
            }

            // Quick Actions
            item {
                StudentQuickActions(
                    onEnrollClick = { showEnrollDialog = true }
                )
            }

            // My Classes Section
            item {
                Text(
                    text = "My Classes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            if (listState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
                .padding(24.dp),
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
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = userName.ifEmpty { "Student" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Student Portal",
                style = MaterialTheme.typography.bodySmall,
                color = StudentPrimary
            )
        }

        Row {
            IconButton(onClick = { /* Notifications */ }) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color.White
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
}

@Composable
private fun StudentStatsSection(
    classCount: Int
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudentStatCard(
                icon = Icons.Default.Class,
                label = "Classes",
                value = classCount.toString(),
                color = StudentPrimary
            )
        }
        item {
            StudentStatCard(
                icon = Icons.Default.TrendingUp,
                label = "Progress",
                value = "0%",
                color = Color(0xFF8B5CF6)
            )
        }
        item {
            StudentStatCard(
                icon = Icons.Default.EmojiEvents,
                label = "Badges",
                value = "0",
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
private fun StudentStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.size(width = 120.dp, height = 100.dp),
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StudentQuickActions(
    onEnrollClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(20.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudentQuickActionButton(
                icon = Icons.Default.Add,
                label = "Join Class",
                color = StudentPrimary,
                modifier = Modifier.weight(1f),
                onClick = onEnrollClick
            )
            StudentQuickActionButton(
                icon = Icons.Default.Assignment,
                label = "Assignments",
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f),
                onClick = { }
            )
            StudentQuickActionButton(
                icon = Icons.Default.MenuBook,
                label = "Materials",
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f),
                onClick = { }
            )
        }
    }
}

@Composable
private fun StudentQuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
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
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudentPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Class,
                    contentDescription = null,
                    tint = StudentPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = classroom.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                classroom.teacher?.let { teacher ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = teacher.fullName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
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

@Composable
private fun EmptyEnrolledClassesCard(
    onEnrollClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudentCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.School,
                contentDescription = null,
                tint = StudentPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No classes yet. Join a class with a code!",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onEnrollClick,
                colors = ButtonDefaults.buttonColors(containerColor = StudentPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Join Class")
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
            Text("Join a Class", color = Color.White)
        },
        text = {
            Column {
                Text(
                    text = "Enter the class code provided by your teacher",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = state.classCode,
                    onValueChange = { onCodeChange(it.uppercase()) },
                    label = { Text("Class Code") },
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
                    Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier.size(20.dp),
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
