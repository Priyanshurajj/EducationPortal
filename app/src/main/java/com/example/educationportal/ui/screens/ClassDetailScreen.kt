package com.example.educationportal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.ui.theme.Primary
import com.example.educationportal.ui.theme.Secondary
import com.example.educationportal.ui.viewmodel.ClassroomEvent
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.HomeViewModel
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classroomId: Int,
    classroomViewModel: ClassroomViewModel,
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val detailState by classroomViewModel.detailState.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadTitle by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val isTeacher = homeState.userRole == UserRole.TEACHER
    val themeColor = if (isTeacher) Secondary else Primary

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { output ->
                    inputStream?.copyTo(output)
                }
                selectedFile = tempFile
                selectedFileName = fileName
                if (uploadTitle.isBlank()) {
                    uploadTitle = fileName.substringBeforeLast('.')
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to select file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(classroomId) {
        classroomViewModel.onEvent(ClassroomEvent.LoadClassroomDetail(classroomId))
    }

    LaunchedEffect(detailState.uploadSuccess) {
        if (detailState.uploadSuccess) {
            showUploadDialog = false
            uploadTitle = ""
            uploadDescription = ""
            selectedFile = null
            selectedFileName = ""
            Toast.makeText(context, "Material uploaded successfully!", Toast.LENGTH_SHORT).show()
            classroomViewModel.onEvent(ClassroomEvent.ResetUploadSuccess)
        }
    }

    LaunchedEffect(detailState.errorMessage) {
        detailState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            classroomViewModel.onEvent(ClassroomEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detailState.classroom?.name ?: "Class Details",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isTeacher) Color.Black else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isTeacher) Color.Black else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = if (isTeacher) {
                            listOf(Secondary.copy(alpha = 0.8f), Secondary)
                        } else {
                            listOf(Primary.copy(alpha = 0.8f), Primary)
                        }
                    )
                )
            )
        },
        floatingActionButton = {
            if (isTeacher) {
                FloatingActionButton(
                    onClick = { showUploadDialog = true },
                    containerColor = themeColor,
                    contentColor = if (isTeacher) Color.Black else Color.White
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Upload Material")
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) { paddingValues ->
        if (detailState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = themeColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Class Info Card
                item {
                    detailState.classroom?.let { classroom ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(themeColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Class,
                                            contentDescription = null,
                                            tint = if (isTeacher) Color.Black else Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = classroom.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        classroom.teacher?.let { teacher ->
                                            Text(
                                                text = "by ${teacher.fullName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                classroom.description?.let { desc ->
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Class Code (visible to teacher)
                                    if (isTeacher) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(themeColor.copy(alpha = 0.2f))
                                                .clickable {
                                                    clipboardManager.setText(AnnotatedString(classroom.classCode))
                                                    Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = "Code: ${classroom.classCode}",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                modifier = Modifier.size(16.dp),
                                                tint = themeColor
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${classroom.studentCount} students",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Materials Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Materials (${detailState.materials.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (detailState.isMaterialsLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = themeColor
                            )
                        }
                    }
                }

                // Materials List
                if (detailState.materials.isEmpty() && !detailState.isMaterialsLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No materials yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isTeacher) {
                                    Text(
                                        text = "Tap + to upload materials",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(detailState.materials) { material ->
                        MaterialCard(
                            material = material,
                            isTeacher = isTeacher,
                            themeColor = themeColor,
                            onDelete = {
                                classroomViewModel.onEvent(
                                    ClassroomEvent.DeleteMaterial(classroomId, material.id)
                                )
                            }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // Upload Material Dialog
    if (showUploadDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!detailState.isUploading) {
                    showUploadDialog = false
                    uploadTitle = ""
                    uploadDescription = ""
                    selectedFile = null
                    selectedFileName = ""
                }
            },
            title = { Text("Upload Material") },
            text = {
                Column {
                    OutlinedTextField(
                        value = uploadTitle,
                        onValueChange = { uploadTitle = it },
                        label = { Text("Title *") },
                        placeholder = { Text("Material title") },
                        singleLine = true,
                        enabled = !detailState.isUploading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uploadDescription,
                        onValueChange = { uploadDescription = it },
                        label = { Text("Description (optional)") },
                        maxLines = 3,
                        enabled = !detailState.isUploading,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // File Selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !detailState.isUploading) {
                                filePickerLauncher.launch("*/*")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = themeColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (selectedFileName.isNotEmpty()) selectedFileName else "Select file (PDF, TXT, DOC...)",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedFile?.let { file ->
                            classroomViewModel.onEvent(
                                ClassroomEvent.UploadMaterial(
                                    classroomId = classroomId,
                                    title = uploadTitle.trim(),
                                    description = uploadDescription.trim().ifBlank { null },
                                    file = file
                                )
                            )
                        }
                    },
                    enabled = !detailState.isUploading && uploadTitle.isNotBlank() && selectedFile != null,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    if (detailState.isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = if (isTeacher) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Upload",
                            color = if (isTeacher) Color.Black else Color.White
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUploadDialog = false
                        uploadTitle = ""
                        uploadDescription = ""
                        selectedFile = null
                        selectedFileName = ""
                    },
                    enabled = !detailState.isUploading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MaterialCard(
    material: Material,
    isTeacher: Boolean,
    themeColor: Color,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = material.getFileIcon(),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${material.fileName} • ${material.getFormattedFileSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                material.uploaderName?.let { name ->
                    Text(
                        text = "by $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isTeacher) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Material") },
            text = { Text("Are you sure you want to delete \"${material.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
