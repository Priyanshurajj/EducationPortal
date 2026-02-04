package com.example.educationportal.data.remote

import android.util.Log
import com.example.educationportal.BuildConfig
import com.example.educationportal.data.model.ChatMessage
import com.google.gson.Gson
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONObject
import java.net.URI

class SocketManager {
    
    companion object {
        private const val TAG = "SocketManager"
        
        @Volatile
        private var INSTANCE: SocketManager? = null
        
        fun getInstance(): SocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SocketManager().also { INSTANCE = it }
            }
        }
    }
    
    private var socket: Socket? = null
    private val gson = Gson()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _messages = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 100)
    val messages: Flow<ChatMessage> = _messages.asSharedFlow()
    
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val errors: Flow<String> = _errors.asSharedFlow()
    
    private val _typingUsers = MutableSharedFlow<TypingEvent>(extraBufferCapacity = 10)
    val typingUsers: Flow<TypingEvent> = _typingUsers.asSharedFlow()
    
    private var currentToken: String? = null
    private val joinedRooms = mutableSetOf<Int>()
    
    fun connect(token: String) {
        if (socket?.connected() == true && currentToken == token) {
            Log.d(TAG, "Already connected with same token")
            return
        }
        
        disconnect()
        currentToken = token
        
        try {
            val baseUrl = BuildConfig.BASE_URL.removeSuffix("/")
            
            Log.d(TAG, "Connecting to Socket.IO at: $baseUrl")

            val options = IO.Options().apply {
                auth = mapOf("token" to token)
                path = "/socket.io/"  // Default Socket.IO path (no /ws prefix now)
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000
            }
            
            socket = IO.socket(URI.create(baseUrl), options).apply {
                on(Socket.EVENT_CONNECT, onConnect)
                on(Socket.EVENT_DISCONNECT, onDisconnect)
                on(Socket.EVENT_CONNECT_ERROR, onConnectError)
                on("connected", onConnected)
                on("message_received", onMessageReceived)
                on("room_joined", onRoomJoined)
                on("room_left", onRoomLeft)
                on("error", onError)
                on("user_typing", onUserTyping)
                on("user_stop_typing", onUserStopTyping)
            }
            
            _connectionState.value = ConnectionState.Connecting
            socket?.connect()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket", e)
            _connectionState.value = ConnectionState.Error(e.message ?: "Connection error")
        }
    }
    
    fun disconnect() {
        socket?.apply {
            off()
            disconnect()
        }
        socket = null
        currentToken = null
        joinedRooms.clear()
        _connectionState.value = ConnectionState.Disconnected
    }
    
    fun joinRoom(classroomId: Int) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot join room - not connected")
            return
        }
        
        val data = JSONObject().apply {
            put("classroom_id", classroomId)
        }
        socket?.emit("join_room", data)
        Log.d(TAG, "Joining room: $classroomId")
    }
    
    fun leaveRoom(classroomId: Int) {
        if (socket?.connected() != true) return
        
        val data = JSONObject().apply {
            put("classroom_id", classroomId)
        }
        socket?.emit("leave_room", data)
        joinedRooms.remove(classroomId)
        Log.d(TAG, "Leaving room: $classroomId")
    }
    
    fun sendMessage(classroomId: Int, content: String) {
        if (socket?.connected() != true) {
            Log.w(TAG, "Cannot send message - not connected")
            return
        }
        
        val data = JSONObject().apply {
            put("classroom_id", classroomId)
            put("content", content)
        }
        socket?.emit("send_message", data)
        Log.d(TAG, "Sending message to room $classroomId: ${content.take(50)}...")
    }
    
    fun sendTyping(classroomId: Int) {
        if (socket?.connected() != true) return
        
        val data = JSONObject().apply {
            put("classroom_id", classroomId)
        }
        socket?.emit("typing", data)
    }
    
    fun sendStopTyping(classroomId: Int) {
        if (socket?.connected() != true) return
        
        val data = JSONObject().apply {
            put("classroom_id", classroomId)
        }
        socket?.emit("stop_typing", data)
    }
    
    fun isConnected(): Boolean = socket?.connected() == true
    
    fun isInRoom(classroomId: Int): Boolean = joinedRooms.contains(classroomId)
    
    // Event listeners
    private val onConnect = Emitter.Listener {
        Log.d(TAG, "Socket connected")
        _connectionState.value = ConnectionState.Connected
    }
    
    private val onDisconnect = Emitter.Listener {
        Log.d(TAG, "Socket disconnected")
        _connectionState.value = ConnectionState.Disconnected
        joinedRooms.clear()
    }
    
    private val onConnectError = Emitter.Listener { args ->
        val error = args.firstOrNull()?.toString() ?: "Unknown error"
        Log.e(TAG, "Connection error: $error")
        _connectionState.value = ConnectionState.Error(error)
    }
    
    private val onConnected = Emitter.Listener { args ->
        Log.d(TAG, "Received connected event: ${args.firstOrNull()}")
    }
    
    private val onMessageReceived = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull()
            Log.d(TAG, "Message received: $data")
            
            val jsonString = when (data) {
                is JSONObject -> data.toString()
                is String -> data
                else -> return@Listener
            }
            
            val message = gson.fromJson(jsonString, ChatMessage::class.java)
            _messages.tryEmit(message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }
    
    private val onRoomJoined = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            val classroomId = data?.optInt("classroom_id") ?: return@Listener
            joinedRooms.add(classroomId)
            Log.d(TAG, "Joined room: $classroomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling room_joined", e)
        }
    }
    
    private val onRoomLeft = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            val classroomId = data?.optInt("classroom_id") ?: return@Listener
            joinedRooms.remove(classroomId)
            Log.d(TAG, "Left room: $classroomId")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling room_left", e)
        }
    }
    
    private val onError = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject
            val message = data?.optString("message") ?: "Unknown error"
            Log.e(TAG, "Socket error: $message")
            _errors.tryEmit(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling error event", e)
        }
    }
    
    private val onUserTyping = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            val event = TypingEvent(
                userId = data.optInt("user_id"),
                userName = data.optString("user_name"),
                classroomId = data.optInt("classroom_id"),
                isTyping = true
            )
            _typingUsers.tryEmit(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling typing event", e)
        }
    }
    
    private val onUserStopTyping = Emitter.Listener { args ->
        try {
            val data = args.firstOrNull() as? JSONObject ?: return@Listener
            val event = TypingEvent(
                userId = data.optInt("user_id"),
                userName = "",
                classroomId = data.optInt("classroom_id"),
                isTyping = false
            )
            _typingUsers.tryEmit(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling stop_typing event", e)
        }
    }
}

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Connected : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

data class TypingEvent(
    val userId: Int,
    val userName: String,
    val classroomId: Int,
    val isTyping: Boolean
)
