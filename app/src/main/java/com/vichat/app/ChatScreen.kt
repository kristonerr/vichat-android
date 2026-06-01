package com.vichat.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun formatTime(time: String?): String {
    if (time == null) return ""
    return try {
        val dt = if (time.contains('-')) {
            LocalDateTime.parse(time, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        } else {
            LocalDateTime.parse(time, DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss"))
        }
        val now = LocalDateTime.now()
        if (dt.toLocalDate() == now.toLocalDate()) {
            dt.format(DateTimeFormatter.ofPattern("HH:mm", Locale("ru")))
        } else if (dt.year == now.year) {
            dt.format(DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru")))
        } else {
            dt.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("ru")))
        }
    } catch (_: Exception) { "" }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(contact: Contact, onBack: () -> Unit) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var inputText by remember { mutableStateOf("") }
    var typing by remember { mutableStateOf(false) }
    var editMsgId by remember { mutableStateOf<Int?>(null) }
    var editText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }
    var contextMenuMsg by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var typingJob by remember { mutableStateOf<Job?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    fun loadMessages() {
        ApiClient.getMessages(contact.id) { result ->
            mainHandler.post {
                result.onSuccess {
                    messages = it
                    SocketManager.sendMarkRead(contact.id)
                    if (it.isNotEmpty()) {
                        scope.launch { listState.animateScrollToItem(it.size - 1) }
                    }
                }
            }
        }
    }

    LaunchedEffect(contact.id) { loadMessages() }

    LaunchedEffect(Unit) {
        SocketManager.events.collect { event ->
            when (event) {
                is WsEvent.MessageReceived -> {
                    if (event.msg.fromId == contact.id) {
                        messages = messages + event.msg
                        SocketManager.sendMarkRead(contact.id)
                        scope.launch { listState.animateScrollToItem(messages.size - 1) }
                    } else if (event.msg.fromId == ApiClient.currentUserId) {
                        val idx = messages.indexOfLast { it.id == 0 }
                        if (idx >= 0) messages = messages.toMutableList().also { it[idx] = event.msg }
                    }
                }
                is WsEvent.MessageEdited -> {
                    messages = messages.map { if (it.id == event.id) it.copy(text = event.text) else it }
                }
                is WsEvent.MessageDeleted -> {
                    messages = messages.filter { it.id != event.id }
                }
                is WsEvent.Typing -> typing = event.fromUsername == contact.username
                is WsEvent.StopTyping -> typing = false
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.username, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("←", color = Color.White, fontSize = 22.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF667eea))
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.padding(8.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it
                            if (it.isNotBlank()) {
                                SocketManager.sendTyping(contact.id)
                                typingJob?.cancel()
                                typingJob = scope.launch {
                                    delay(2000)
                                    SocketManager.sendStopTyping(contact.id)
                                }
                            } else {
                                SocketManager.sendStopTyping(contact.id)
                            }
                        },
                        placeholder = { Text("Сообщение...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (inputText.isBlank()) return@Button
                            SocketManager.sendMessage(contact.id, inputText.trim())
                            val now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss"))
                            messages = messages + Message(fromId = 0, text = inputText.trim(), time = now)
                            inputText = ""
                            SocketManager.sendStopTyping(contact.id)
                            scope.launch {
                                delay(100)
                                if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("→", fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (typing) {
                Text("печатает...", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = Color.Gray, fontSize = 12.sp)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                items(messages) { msg ->
                    val isMe = msg.fromId == ApiClient.currentUserId || msg.fromId == 0
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 12.dp, topEnd = 12.dp,
                                bottomStart = if (isMe) 12.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 12.dp
                            ),
                            color = if (isMe) Color(0xFF667eea) else Color(0xFFE8E8E8),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        contextMenuMsg = msg
                                    }
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(
                                    msg.text,
                                    color = if (isMe) Color.White else Color(0xFF333333),
                                    fontSize = 15.sp
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val timeText = formatTime(msg.time)
                                    if (timeText.isNotEmpty()) {
                                        Text(
                                            timeText,
                                            color = if (isMe) Color.White.copy(alpha = 0.6f) else Color.Gray,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    if (isMe && msg.id > 0 && msg.readAt != null) {
                                        Spacer(Modifier.width(4.dp))
                                        Text("✓", color = if (isMe) Color.White.copy(alpha = 0.6f) else Color.Gray, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val clipboard = LocalClipboardManager.current
    if (contextMenuMsg != null) {
        val msg = contextMenuMsg!!
        val isOwn = msg.fromId == ApiClient.currentUserId || msg.fromId == 0
        AlertDialog(
            onDismissRequest = { contextMenuMsg = null },
            title = { Text("Действие") },
            text = { Text("Что сделать с сообщением?") },
            confirmButton = {
                Column {
                    Button(onClick = {
                        clipboard.setText(AnnotatedString(msg.text))
                        contextMenuMsg = null
                    }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Text("Копировать") }
                    if (isOwn && msg.id > 0) {
                        Button(onClick = {
                            editMsgId = msg.id; editText = msg.text; showEditDialog = true; contextMenuMsg = null
                        }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) { Text("Редактировать") }
                        Button(onClick = {
                            showDeleteConfirm = msg.id; contextMenuMsg = null
                        }, modifier = Modifier.fillMaxWidth()) { Text("Удалить") }
                    }
                }
            },
            dismissButton = { TextButton(onClick = { contextMenuMsg = null }) { Text("Отмена") } }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Редактировать") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (editText.isBlank()) return@Button
                    val id = editMsgId ?: return@Button
                    ApiClient.editMessage(id, editText.trim()) { result ->
                        mainHandler.post {
                            result.onSuccess {
                                messages = messages.map { if (it.id == id) it.copy(text = editText.trim()) else it }
                            }
                        }
                    }
                    showEditDialog = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Отмена") } }
        )
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Удалить?") },
            text = { Text("Сообщение исчезнет у обоих") },
            confirmButton = {
                Button(onClick = {
                    val id = showDeleteConfirm!!; showDeleteConfirm = null
                    if (id > 0) {
                        ApiClient.deleteMessage(id) { result ->
                            mainHandler.post {
                                result.onSuccess { messages = messages.filter { it.id != id } }
                            }
                        }
                    } else {
                        messages = messages.filter { it.id != id }
                    }
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = null }) { Text("Отмена") } }
        )
    }
}
