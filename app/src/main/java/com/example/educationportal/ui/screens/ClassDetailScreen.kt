package com.example.educationportal.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.ui.viewmodel.ClassroomEvent
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.HomeViewModel
import java.io.File
import java.io.FileOutputStream

// Theme colors (matching teacher/student themes)
private val PrimaryColor = Color(0xFF6366F1) // Indigo
private val BackgroundDark = Color(0xFF0F172A)
private val SurfaceDark = Color(0xFF1E293B)
private val CardBg = Color(0xFF334155)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classroomId: Int,
    classroomViewModel: ClassroomViewModel,
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val homeState by homeViewModel.uiState.collectAsState()
    val detailState by classroomViewModel.detailState.collectAsState()
    
    val userRole = homeState.userRole
    val isTeacher = userRole == UserRole.TEACHER

    var showUploadDialog by remember { mutableStateOf(false) }
    var uploadTitle by remember { mutableStateOf("") }
    var uploadDescription by remember { mutableStateOf("") }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)

                // Get filename from ContentResolver (works for content URIs)
                var fileName = "file"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "file"
                    }
                }

                // Fallback: try to extract from URI path if ContentResolver didn't work
                if (fileName == "file" || fileName.isEmpty()) {
                    fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
                    // If still no valid filename, generate one with extension from MIME type
                    if (fileName == "file" || !fileName.contains('.')) {
                        val mimeType = context.contentResolver.getType(uri)
                        val extension = when {
                            mimeType?.contains("pdf") == true -> ".pdf"
                            mimeType?.contains("text") == true -> ".txt"
                            mimeType?.contains("word") == true -> ".docx"
                            mimeType?.contains("excel") == true -> ".xlsx"
                            mimeType?.contains("powerpoint") == true -> ".pptx"
                            else -> ".bin"
                        }
                        fileName = "document_${System.currentTimeMillis()}$extension"
                    }
                }

                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { output ->
                    inputStream?.copyTo(output)
                }
                selectedFile = tempFile
                selectedFileName = fileName
                Log.d("ClassDetails", "Selected FileName : $selectedFileName")
                if (uploadTitle.isBlank()) {
                    uploadTitle = fileName.substringBeforeLast('.')
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to select file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper function to open file
    fun openFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = when {
                file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                file.name.endsWith(".txt", ignoreCase = true) -> "text/plain"
                file.name.endsWith(".doc", ignoreCase = true) -> "application/msword"
                file.name.endsWith(".docx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                file.name.endsWith(".xls", ignoreCase = true) -> "application/vnd.ms-excel"
                file.name.endsWith(".xlsx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                file.name.endsWith(".ppt", ignoreCase = true) -> "application/vnd.ms-powerpoint"
                file.name.endsWith(".pptx", ignoreCase = true) -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                else -> "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    // Load classroom detail on launch
    LaunchedEffect(classroomId) {
        classroomViewModel.onEvent(ClassroomEvent.LoadClassroomDetail(classroomId))
    }

    // Handle upload success
    LaunchedEffect(detailState.uploadSuccess) {
        if (detailState.uploadSuccess) {
            showUploadDialog = false
            uploadTitle = ""
            uploadDescription = ""
            selectedFile = null
            selectedFileName = ""
            classroomViewModel.onEvent(ClassroomEvent.ResetUploadSuccess)
            Toast.makeText(context, "Material uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle downloaded file
    LaunchedEffect(detailState.downloadedFile) {
        detailState.downloadedFile?.let { file ->
            openFile(file)
            classroomViewModel.onEvent(ClassroomEvent.ClearDownloadedFile)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundDark, SurfaceDark)
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = detailState.classroom?.name ?: "Class Details",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            if (detailState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = if (isTeacher) 100.dp else 20.dp)
                ) {
                    // Class Info Card
                    item {
                        ClassInfoCard(
                            classroom = detailState.classroom,
                            isTeacher = isTeacher
                        )
                    }

                    // Materials Section
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Materials (${detailState.materials.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (detailState.isMaterialsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryColor,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    if (detailState.materials.isEmpty() && !detailState.isMaterialsLoading) {
                        item {
                            EmptyMaterialsCard(isTeacher = isTeacher)
                        }
                    } else {
                        items(detailState.materials) { material ->
                            MaterialCard(
                                material = material,
                                isTeacher = isTeacher,
                                isDownloading = detailState.isDownloading,
                                onDelete = {
                                    classroomViewModel.onEvent(ClassroomEvent.DeleteMaterial(classroomId, material.id))
                                },
                                onClick = {
                                    // Download and open the material
                                    classroomViewModel.onEvent(
                                        ClassroomEvent.DownloadMaterial(
                                            classroomId = classroomId,
                                            materialId = material.id,
                                            fileName = material.fileName,
                                            cacheDir = context.cacheDir
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // FAB for upload (Teacher only)
        if (isTeacher && !detailState.isLoading) {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload Material")
            }
        }

        // Error Snackbar
        detailState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { classroomViewModel.onEvent(ClassroomEvent.ClearError) }) {
                        Text("Dismiss", color = PrimaryColor)
                    }
                }
            ) {
                Text(error)
            }
        }
    }

    // Upload Dialog
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
            containerColor = SurfaceDark,
            title = {
                Text("Upload Material", color = Color.White)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uploadTitle,
                        onValueChange = { uploadTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = uploadDescription,
                        onValueChange = { uploadDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryColor,
                            focusedLabelColor = PrimaryColor,
                            cursorColor = PrimaryColor,
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // File selection
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryColor
                        )
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedFileName.isNotEmpty()) selectedFileName else "Select File",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (selectedFileName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selected: $selectedFileName",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "Supported: PDF, TXT, DOC, DOCX, PPT, PPTX, XLS, XLSX",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    if (detailState.isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Upload")
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
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            }
        )
    }
}

@Composable
private fun ClassInfoCard(
    classroom: com.example.educationportal.data.model.ClassroomDetail?,
    isTeacher: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Class,
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = classroom?.name ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isTeacher) {
                        // Show class code for teachers
                        Surface(
                            color = PrimaryColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Code: ${classroom?.classCode ?: ""}",
                                style = MaterialTheme.typography.labelMedium,
                                color = PrimaryColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            classroom?.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${classroom?.studentCount ?: 0}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Students",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (!isTeacher) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f)
                        )
                        Text(
                            text = classroom?.teacher?.fullName ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Teacher",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCard(
    material: Material,
    isTeacher: Boolean,
    isDownloading: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(enabled = !isDownloading) { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = material.getFileIcon(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${material.fileType.uppercase()} • ${material.getFormattedFileSize()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
                material.uploaderName?.let { name ->
                    Text(
                        text = "by $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = PrimaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isTeacher) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyMaterialsCard(isTeacher: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = PrimaryColor.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isTeacher) "No materials yet. Upload your first material!" else "No materials available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
