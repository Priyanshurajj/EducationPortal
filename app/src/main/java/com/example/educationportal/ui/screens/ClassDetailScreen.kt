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
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.educationportal.data.model.Material
import com.example.educationportal.data.model.UserRole
import com.example.educationportal.ui.viewmodel.ClassroomEvent
import com.example.educationportal.ui.viewmodel.ClassroomViewModel
import com.example.educationportal.ui.viewmodel.HomeViewModel
import java.io.File
import java.io.FileOutputStream

// Theme colors - Purple/Indigo with Black gradient
private val PrimaryColor = Color(0xFF7C4DFF) // Purple accent
private val SecondaryColor = Color(0xFF536DFE)
private val BackgroundDark = Color(0xFF000000)
private val SurfaceDark = Color(0xFF0D1117)
private val CardBg = Color(0xFF1C1C2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classroomId: Int,
    classroomViewModel: ClassroomViewModel,
    homeViewModel: HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (Int, String) -> Unit = { _, _ -> }
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

                var fileName = "file"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "file"
                    }
                }

                if (fileName == "file" || fileName.isEmpty()) {
                    fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "file"
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
            classroomViewModel.onEvent(ClassroomEvent.ResetUploadSuccess)
            Toast.makeText(context, "Material uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

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
                    colors = listOf(
                        BackgroundDark,
                        Color(0xFF0A0A1A),
                        SurfaceDark
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Text(
                        text = detailState.classroom?.name ?: "Class Details",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp
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
                actions = {
                    // Chat button
                    IconButton(
                        onClick = {
                            detailState.classroom?.let { classroom ->
                                onNavigateToChat(classroom.id, classroom.name)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat",
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
                    contentPadding = PaddingValues(bottom = if (isTeacher) 88.dp else 16.dp)
                ) {
                    item {
                        ClassInfoCard(
                            classroom = detailState.classroom,
                            isTeacher = isTeacher
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Materials (${detailState.materials.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (detailState.isMaterialsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
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

        if (isTeacher && !detailState.isLoading) {
            FloatingActionButton(
                onClick = { showUploadDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload Material")
            }
        }

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
                Text("Upload Material", color = Color.White, fontSize = 18.sp)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = uploadTitle,
                        onValueChange = { uploadTitle = it },
                        label = { Text("Title", fontSize = 14.sp) },
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
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = uploadDescription,
                        onValueChange = { uploadDescription = it },
                        label = { Text("Description (Optional)", fontSize = 14.sp) },
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
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFileName.isNotEmpty()) selectedFileName else "Select File",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 14.sp
                        )
                    }

                    if (selectedFileName.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Selected: $selectedFileName",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "Supported: PDF, TXT, DOC, DOCX, PPT, PPTX, XLS, XLSX",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 6.dp)
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
                            modifier = Modifier.size(18.dp),
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
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Class,
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = classroom?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isTeacher) {
                        Surface(
                            color = PrimaryColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Code: ${classroom?.classCode ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            classroom?.description?.let { desc ->
                if (desc.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${classroom?.studentCount ?: 0}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Students",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }

                if (!isTeacher) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = classroom?.teacher?.fullName ?: "Unknown",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Teacher",
                            style = MaterialTheme.typography.labelSmall,
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
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = !isDownloading) { onClick() },
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = material.getFileIcon(),
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${material.fileType.uppercase()} • ${material.getFormattedFileSize()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PrimaryColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = PrimaryColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isTeacher) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
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
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Folder,
                contentDescription = null,
                tint = PrimaryColor.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isTeacher) "No materials yet. Upload your first material!" else "No materials available yet.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
