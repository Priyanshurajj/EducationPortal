package com.example.educationportal.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.educationportal.data.model.SourceType
import com.example.educationportal.ui.viewmodel.AiSummaryViewModel
import com.example.educationportal.ui.viewmodel.MaterialUploadEvent
import java.io.File
import java.io.FileOutputStream

// AI Assistant theme colors
private val AiPrimary = Color(0xFF9C27B0) // Purple
private val AiBackground = Color(0xFF000000) // Pure Black
private val AiSurface = Color(0xFF1A1A2E) // Dark purple tint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialUploadScreen(
    viewModel: AiSummaryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSummary: () -> Unit
) {
    val context = LocalContext.current
    val uploadState by viewModel.uploadState.collectAsState()
    
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedFileUri = it
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                var fileName = "file"
                
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex) ?: "file"
                    }
                }
                
                val tempFile = File(context.cacheDir, fileName)
                FileOutputStream(tempFile).use { output ->
                    inputStream?.copyTo(output)
                }
                selectedFile = tempFile
                viewModel.onUploadEvent(MaterialUploadEvent.FileSelected(tempFile, fileName))
                Log.d("MaterialUpload", "Selected file: $fileName")
            } catch (e: Exception) {
                Log.e("MaterialUpload", "Error selecting file", e)
            }
        }
    }
    
    // Navigate to summary when response is ready
    LaunchedEffect(uploadState.summarizeResponse) {
        uploadState.summarizeResponse?.let {
            onNavigateToSummary()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AiBackground,
                        Color(0xFF0A0A1A),
                        AiSurface
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
                        text = "AI Study Assistant",
                        color = Color.White,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Source Type Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AiSurface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Source Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SourceTypeChip(
                                label = "URL",
                                selected = uploadState.sourceType == SourceType.URL,
                                onClick = {
                                    viewModel.onUploadEvent(MaterialUploadEvent.SourceTypeChanged(SourceType.URL))
                                }
                            )
                            SourceTypeChip(
                                label = "Text",
                                selected = uploadState.sourceType == SourceType.TEXT,
                                onClick = {
                                    viewModel.onUploadEvent(MaterialUploadEvent.SourceTypeChanged(SourceType.TEXT))
                                }
                            )
                            SourceTypeChip(
                                label = "PDF",
                                selected = uploadState.sourceType == SourceType.PDF,
                                onClick = {
                                    viewModel.onUploadEvent(MaterialUploadEvent.SourceTypeChanged(SourceType.PDF))
                                }
                            )
                            SourceTypeChip(
                                label = "TXT",
                                selected = uploadState.sourceType == SourceType.TXT,
                                onClick = {
                                    viewModel.onUploadEvent(MaterialUploadEvent.SourceTypeChanged(SourceType.TXT))
                                }
                            )
                        }
                    }
                }
                
                // Content Input based on source type
                when (uploadState.sourceType) {
                    SourceType.URL -> {
                        OutlinedTextField(
                            value = uploadState.urlInput,
                            onValueChange = { viewModel.onUploadEvent(MaterialUploadEvent.UrlInputChanged(it)) },
                            label = { Text("Website URL", color = Color.White.copy(alpha = 0.7f)) },
                            placeholder = { Text("https://example.com/article", color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AiPrimary,
                                focusedLabelColor = AiPrimary,
                                cursorColor = AiPrimary,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                    SourceType.TEXT -> {
                        OutlinedTextField(
                            value = uploadState.textInput,
                            onValueChange = { viewModel.onUploadEvent(MaterialUploadEvent.TextInputChanged(it)) },
                            label = { Text("Paste your text here", color = Color.White.copy(alpha = 0.7f)) },
                            placeholder = { Text("Enter or paste text content...", color = Color.White.copy(alpha = 0.5f)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            minLines = 8,
                            maxLines = 15,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AiPrimary,
                                focusedLabelColor = AiPrimary,
                                cursorColor = AiPrimary,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                    SourceType.PDF, SourceType.TXT -> {
                        OutlinedButton(
                            onClick = { filePickerLauncher.launch("*/*") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AiPrimary
                            )
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uploadState.selectedFileName.isNotEmpty()) {
                                    uploadState.selectedFileName
                                } else {
                                    "Select ${uploadState.sourceType.value.uppercase()} File"
                                }
                            )
                        }
                        
                        if (uploadState.selectedFileName.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Selected: ${uploadState.selectedFileName}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                                TextButton(onClick = {
                                    viewModel.onUploadEvent(MaterialUploadEvent.ClearFile)
                                }) {
                                    Text("Clear", color = AiPrimary)
                                }
                            }
                        }
                    }
                }
                
                // Optional Title Input
                OutlinedTextField(
                    value = uploadState.titleInput,
                    onValueChange = { viewModel.onUploadEvent(MaterialUploadEvent.TitleInputChanged(it)) },
                    label = { Text("Title (Optional)", color = Color.White.copy(alpha = 0.7f)) },
                    placeholder = { Text("Enter a title for this material", color = Color.White.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AiPrimary,
                        focusedLabelColor = AiPrimary,
                        cursorColor = AiPrimary,
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                
                // Submit Button
                Button(
                    onClick = { viewModel.onUploadEvent(MaterialUploadEvent.Submit) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uploadState.isLoading && isInputValid(uploadState),
                    colors = ButtonDefaults.buttonColors(containerColor = AiPrimary)
                ) {
                    if (uploadState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Summary")
                    }
                }
                
                // Error Message
                uploadState.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB00020)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(12.dp),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AiPrimary,
            selectedLabelColor = Color.White,
            containerColor = AiSurface,
            labelColor = Color.White.copy(alpha = 0.7f)
        )
    )
}

private fun isInputValid(state: com.example.educationportal.ui.viewmodel.MaterialUploadUiState): Boolean {
    return when (state.sourceType) {
        SourceType.URL -> state.urlInput.isNotBlank()
        SourceType.TEXT -> state.textInput.isNotBlank() && state.textInput.length >= 100
        SourceType.PDF, SourceType.TXT -> state.selectedFile != null
    }
}
