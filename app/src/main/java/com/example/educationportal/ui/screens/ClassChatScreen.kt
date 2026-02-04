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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.educationportal.data.model.ChatMessage
import com.example.educationportal.data.remote.ConnectionState
import com.example.educationportal.ui.viewmodel.ChatEvent
import com.example.educationportal.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch

// Theme colors
private val PrimaryColor = Color(0xFF6366F1) // Indigo
private val BackgroundDark = Color(0xFF000000)
private val SurfaceDark = Color(0xFF0D1117)
private val CardBg = Color(0xFF1C1C2E)
private val OwnMessageBg = Color(0xFF6366F1)
private val OtherMessageBg = Color(0xFF2D2D44)
private val TeacherBadgeColor = Color(0xFFFFB300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassChatScreen(
    classroomId: Int,
    classroomName: String,
    currentUserId: Int,
    chatViewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Initialize chat when screen loads
    LaunchedEffect(classroomId, currentUserId) {
        chatViewModel.onEvent(ChatEvent.Initialize(classroomId, currentUserId))
    }
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
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
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = classroomName,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        ConnectionStatusText(uiState.connectionState)
                    }
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
            
            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryColor
                    )
                } else if (uiState.messages.isEmpty()) {
                    EmptyChat()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Load more indicator
                        if (uiState.hasMoreMessages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingMore) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = PrimaryColor,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        TextButton(
                                            onClick = { chatViewModel.onEvent(ChatEvent.LoadMoreMessages) }
                                        ) {
                                            Text("Load older messages", color = PrimaryColor)
                                        }
                                    }
                                }
                            }
                        }
                        
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId
                            )
                        }
                    }
                }
                
                // Typing indicator
                if (uiState.typingUsers.isNotEmpty()) {
                    TypingIndicator(
                        users = uiState.typingUsers,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    )
                }
            }
            
            // Message Input
            MessageInput(
                value = messageText,
                onValueChange = { 
                    messageText = it
                    chatViewModel.onEvent(ChatEvent.OnTyping(it.isNotEmpty()))
                },
                onSend = {
                    if (messageText.isNotBlank()) {
                        chatViewModel.onEvent(ChatEvent.SendMessage(messageText))
                        messageText = ""
                    }
                },
                isConnected = uiState.connectionState is ConnectionState.Connected
            )
        }
        
        // Error Snackbar
        uiState.errorMessage?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 72.dp),
                action = {
                    TextButton(onClick = { chatViewModel.onEvent(ChatEvent.ClearError) }) {
                        Text("Dismiss", color = PrimaryColor)
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}

@Composable
private fun ConnectionStatusText(state: ConnectionState) {
    val (text, color) = when (state) {
        is ConnectionState.Connected -> "Online" to Color(0xFF4CAF50)
        is ConnectionState.Connecting -> "Connecting..." to Color(0xFFFFB300)
        is ConnectionState.Disconnected -> "Offline" to Color(0xFFEF4444)
        is ConnectionState.Error -> "Connection error" to Color(0xFFEF4444)
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = text,
            color = color,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun EmptyChat() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Chat,
            contentDescription = null,
            tint = PrimaryColor.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = "Be the first to start the conversation!",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        // Sender name (only for other's messages)
        if (!isOwnMessage) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (message.isTeacher()) TeacherBadgeColor else PrimaryColor,
                    fontWeight = FontWeight.SemiBold
                )
                if (message.isTeacher()) {
                    Surface(
                        color = TeacherBadgeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Teacher",
                            style = MaterialTheme.typography.labelSmall,
                            color = TeacherBadgeColor,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
        
        // Message bubble
        Surface(
            color = if (isOwnMessage) OwnMessageBg else OtherMessageBg,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwnMessage) 16.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = message.getFormattedTime(),
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun TypingIndicator(
    users: List<String>,
    modifier: Modifier = Modifier
) {
    val text = when {
        users.size == 1 -> "${users[0]} is typing..."
        users.size == 2 -> "${users[0]} and ${users[1]} are typing..."
        users.size > 2 -> "${users[0]} and ${users.size - 1} others are typing..."
        else -> ""
    }
    
    if (text.isNotEmpty()) {
        Surface(
            color = CardBg,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    isConnected: Boolean
) {
    Surface(
        color = CardBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { 
                    Text(
                        if (isConnected) "Type a message..." else "Connecting...",
                        color = Color.White.copy(alpha = 0.5f)
                    ) 
                },
                modifier = Modifier.weight(1f),
                enabled = isConnected,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrimaryColor,
                    disabledBorderColor = Color.White.copy(alpha = 0.1f),
                    disabledTextColor = Color.White.copy(alpha = 0.5f)
                )
            )
            
            FilledIconButton(
                onClick = onSend,
                enabled = isConnected && value.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = PrimaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = PrimaryColor.copy(alpha = 0.3f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
