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
import com.example.educationportal.ui.viewmodel.CreateClassroomState
import com.example.educationportal.ui.viewmodel.HomeViewModel

// Teacher theme colors - Yellow/Amber with Black gradient
private val TeacherPrimary = Color(0xFFFFB300) // Amber/Yellow
private val TeacherBackground = Color(0xFF000000) // Pure Black
private val TeacherSurface = Color(0xFF1A1A1A) // Dark surface
private val TeacherCardBg = Color(0xFF262626) // Card background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    homeViewModel: HomeViewModel,
    classroomViewModel: ClassroomViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToClassDetail: (Int) -> Unit
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val listState by classroomViewModel.listState.collectAsState()
    val createState by classroomViewModel.createState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

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
                        TeacherBackground,
                        Color(0xFF1A1500), // Dark yellow tint
                        TeacherSurface
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
                TeacherHeader(
                    userName = homeState.userName,
                    onLogout = { homeViewModel.logout() }
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
                        CircularProgressIndicator(color = TeacherPrimary)
                    }
                }
            } else if (listState.classrooms.isEmpty()) {
                item {
                    EmptyClassesCard(
                        message = "No classes yet. Create your first class!",
                        onCreateClick = { showCreateDialog = true }
                    )
                }
            } else {
                items(listState.classrooms) { classroom ->
                    TeacherClassCard(
                        classroom = classroom,
                        onClick = { onNavigateToClassDetail(classroom.id) }
                    )
                }
            }
        }

        // FAB for creating class
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = TeacherPrimary,
            contentColor = Color.Black
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Class")
        }

        // Error Snackbar
        listState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { classroomViewModel.onEvent(ClassroomEvent.ClearError) }) {
                        Text("Dismiss", color = TeacherPrimary)
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Create Class Dialog
    if (showCreateDialog) {
        CreateClassDialog(
            state = createState,
            onNameChange = { classroomViewModel.onEvent(ClassroomEvent.CreateNameChanged(it)) },
            onDescriptionChange = { classroomViewModel.onEvent(ClassroomEvent.CreateDescriptionChanged(it)) },
            onConfirm = { classroomViewModel.onEvent(ClassroomEvent.CreateClassroom) },
            onDismiss = {
                showCreateDialog = false
                classroomViewModel.onEvent(ClassroomEvent.ResetCreateState)
            }
        )
    }

    // Handle create success
    LaunchedEffect(createState.isSuccess) {
        if (createState.isSuccess) {
            showCreateDialog = false
            classroomViewModel.onEvent(ClassroomEvent.ResetCreateState)
        }
    }
}

@Composable
private fun TeacherHeader(
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
                text = "Welcome back,",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = userName.ifEmpty { "Teacher" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Teacher Portal",
                style = MaterialTheme.typography.labelMedium,
                color = TeacherPrimary,
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
private fun TeacherClassCard(
    classroom: Classroom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = TeacherCardBg),
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
                    .background(TeacherPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Class,
                    contentDescription = null,
                    tint = TeacherPrimary,
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
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = TeacherPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = classroom.classCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = TeacherPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${classroom.studentCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
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
private fun EmptyClassesCard(
    message: String,
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = TeacherCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Class,
                contentDescription = null,
                tint = TeacherPrimary.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeacherPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Class", fontSize = 14.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateClassDialog(
    state: CreateClassroomState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TeacherSurface,
        title = {
            Text("Create New Class", color = Color.White, fontSize = 18.sp)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Class Name", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherPrimary,
                        focusedLabelColor = TeacherPrimary,
                        cursorColor = TeacherPrimary,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (Optional)", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TeacherPrimary,
                        focusedLabelColor = TeacherPrimary,
                        cursorColor = TeacherPrimary,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeacherPrimary,
                    contentColor = Color.Black
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create")
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
