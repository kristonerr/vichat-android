package com.vichat.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onChatClick: (Contact) -> Unit,
    onLogout: () -> Unit
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var onlineIds by remember { mutableStateOf(emptySet<Int>()) }
    var unreadMap by remember { mutableStateOf(mapOf<Int, Int>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addUsername by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    fun load() {
        ApiClient.getContacts { result ->
            mainHandler.post {
                result.onSuccess { list ->
                    contacts = list
                    unreadMap = list.associate { it.id to it.unread }
                }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    LaunchedEffect(Unit) {
        SocketManager.events.collect { event ->
            when (event) {
                is WsEvent.ContactsOnline -> onlineIds = event.list.toSet()
                is WsEvent.UnreadUpdate -> {
                    unreadMap = unreadMap + (event.fromUserId to event.count)
                }
                else -> {}
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition()
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ViChat", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) { Text("+", color = Color.White, fontSize = 22.sp) }
                    TextButton(onClick = onLogout) { Text("Выйти", color = Color.White, fontSize = 14.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF667eea))
            )
        }
    ) { padding ->
        if (contacts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Нет контактов", color = Color.Gray, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Нажми + чтобы добавить друга", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(contacts) { contact ->
                    val online = contact.id in onlineIds
                    val unread = unreadMap[contact.id] ?: 0
                    val hasUnread = unread > 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { onChatClick(contact) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF667eea)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.username.first().uppercase(), color = Color.White, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.username, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(if (online) "В сети" else "Офлайн", fontSize = 12.sp, color = if (online) Color(0xFF4CAF50) else Color.Gray)
                                    if (hasUnread) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFF6B6B).copy(alpha = blinkAlpha))
                                        )
                                    }
                                }
                            }
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFF6B6B).copy(alpha = blinkAlpha))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (unread > 99) "99+" else unread.toString(),
                                        color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; addUsername = ""; addError = null },
            title = { Text("Добавить контакт") },
            text = {
                Column {
                    OutlinedTextField(
                        value = addUsername,
                        onValueChange = { addUsername = it },
                        label = { Text("Имя пользователя") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    addError?.let { Spacer(Modifier.height(4.dp)); Text(it, color = Color.Red, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (addUsername.isBlank()) { addError = "Введи имя"; return@Button }
                    ApiClient.addContact(addUsername.trim()) { result ->
                        mainHandler.post {
                            result.fold(
                                onSuccess = { showAddDialog = false; addUsername = ""; load() },
                                onFailure = { addError = it.message ?: "Ошибка (${it::class.simpleName})" }
                            )
                        }
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; addUsername = ""; addError = null }) { Text("Отмена") } }
        )
    }
}
