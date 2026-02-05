package com.example.educationportal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationportal.data.model.AiChatMessage
import com.example.educationportal.ui.viewmodel.AiChatEvent
import com.example.educationportal.ui.viewmodel.AiSummaryViewModel
import kotlinx.coroutines.launch

// AI Assistant theme colors
private val AiPrimary = Color(0xFF9C27B0) // Purple
private val AiBackground = Color(0xFF000000) // Pure Black
private val AiSurface = Color(0xFF1A1A2E) // Dark purple tint
private val UserMessageBg = Color(0xFF7B1FA2) // Darker purple for user messages
private val AiMessageBg = Color(0xFF2D1B3D) // Dark purple for AI messages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSummaryScreen(
    viewModel: AiSummaryViewModel,
    onNavigateBack: () -> Unit
) {
    val summaryState by viewModel.summaryState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(summaryState.chatMessages.size) {
        if (summaryState.chatMessages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(summaryState.chatMessages.size - 1)
            }
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
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = summaryState.title.ifBlank { "AI Summary" },
                        color = Color.White,
                        fontSize = 18.sp,
                        maxLines = 1
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
                    IconButton(
                        onClick = { viewModel.onChatEvent(AiChatEvent.EndSession) }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "End Session",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Main Content
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Summary Card
                item {
                    SummaryCard(
                        title = summaryState.title,
                        summary = summaryState.summary,
                        sourceType = summaryState.sourceType,
                        wordCount = summaryState.wordCount,
                        chunkCount = summaryState.chunkCount
                    )
                }

                // Chat Messages
                if (summaryState.chatMessages.isEmpty()) {
                    item {
                        EmptyChatPlaceholder()
                    }
                } else {
                    items(summaryState.chatMessages) { message ->
                        ChatMessageBubble(message = message)
                    }
                }

                // Loading indicator when sending message
                if (summaryState.isSendingMessage) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AiPrimary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }

            // Chat Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AiSurface)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = summaryState.chatInput,
                    onValueChange = { viewModel.onChatEvent(AiChatEvent.ChatInputChanged(it)) },
                    placeholder = {
                        Text(
                            "Ask a question...",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AiPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        cursorColor = AiPrimary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = { viewModel.onChatEvent(AiChatEvent.SendMessage) },
                    containerColor = AiPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message"
                    )
                }
            }
        }

        // Error Snackbar
        summaryState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(
                        onClick = { viewModel.onChatEvent(AiChatEvent.ClearError) }
                    ) {
                        Text("Dismiss", color = AiPrimary)
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    summary: String,
    sourceType: String,
    wordCount: Int,
    chunkCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AiSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and Source Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = AiPrimary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = sourceType.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = AiPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Summary Text
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Info Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.TextFields,
                    text = "$wordCount words"
                )
                InfoChip(
                    icon = Icons.Default.Article,
                    text = "$chunkCount chunks"
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = AiPrimary,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (message.isUser) 12.dp else 0.dp,
                        bottomEnd = if (message.isUser) 0.dp else 12.dp
                    )
                )
                .background(if (message.isUser) UserMessageBg else AiMessageBg)
                .padding(12.dp)
        ) {
            Column {
                // Message Content
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )
                
                // Sources (only for AI messages)
                if (message.sources.isNotEmpty() && !message.isUser) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sources:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    message.sources.take(2).forEach { source ->
                        Text(
                            text = "• ${if (source.length > 100) source.take(100) + "..." else source}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            tint = AiPrimary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Ask questions about the material",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "The AI will answer based on the uploaded content",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}
