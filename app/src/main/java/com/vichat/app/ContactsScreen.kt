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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import android.os.Handler
import android.os.Looper
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onChatClick: (Contact) -> Unit,
    onLogout: () -> Unit
) {
    var contacts by remember { mutableStateOf<List<Contact>>(emptyList()) }
    var onlineIds by remember { mutableStateOf(emptySet<Int>()) }
    var statusMap by remember { mutableStateOf(mapOf<Int, UserStatusData>()) }
    var myStatus by remember { mutableStateOf(UserStatus.ONLINE.name) }
    var myCustomText by remember { mutableStateOf("") }
    var unreadMap by remember { mutableStateOf(mapOf<Int, Int>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var addUsername by remember { mutableStateOf("") }
    var addError by remember { mutableStateOf<String?>(null) }
    var updateInfo by remember { mutableStateOf<VersionInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var showStatusDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val isDark = LocalIsDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme

    fun load() {
        ApiClient.getContacts { result ->
            mainHandler.post {
                result.onSuccess { list ->
                    contacts = list.sortedByDescending { it.id in onlineIds }
                        .sortedByDescending { it.id in onlineIds }
                        .also { sorted ->
                            unreadMap = sorted.associate { it.id to it.unread }
                        }
                }
            }
        }
        ApiClient.getIncomingRequests { result ->
            mainHandler.post {
                result.onSuccess { incomingRequests = it }
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    LaunchedEffect(Unit) {
        SocketManager.events.collect { event ->
            when (event) {
                is WsEvent.ContactsOnline -> {
                    onlineIds = event.list.toSet()
                    contacts = contacts.sortedByDescending { it.id in onlineIds }
                }
                is WsEvent.UserStatusChanged -> {
                    statusMap = statusMap + (event.userId to UserStatusData(UserStatus.fromString(event.status), event.customText))
                }
                is WsEvent.MyStatusSet -> {
                    myStatus = event.status
                    myCustomText = event.customText ?: ""
                }
                is WsEvent.UnreadUpdate -> {
                    unreadMap = unreadMap + (event.fromUserId to event.count)
                }
                is WsEvent.Connected -> { load() }
                is WsEvent.FriendRequestReceived -> {
                    load()
                    Toast.makeText(context, "Запрос в друзья от ${event.fromUsername}", Toast.LENGTH_LONG).show()
                }
                is WsEvent.FriendRequestAccepted -> {
                    load()
                    Toast.makeText(context, "${event.username} принял(а) запрос!", Toast.LENGTH_LONG).show()
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

    var showRequestsSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("ViChat", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("v${UpdateManager.getCurrentVersionName(context)}", fontSize = 11.sp, color = colorScheme.onPrimary.copy(alpha = 0.6f))
                            if (myCustomText.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                Text("· ${UserStatus.fromString(myStatus).emoji} $myCustomText", fontSize = 10.sp, color = colorScheme.onPrimary.copy(alpha = 0.7f))
                            }
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { showStatusDialog = true }) {
                        Text(UserStatus.fromString(myStatus).emoji, fontSize = 18.sp)
                    }
                    TextButton(onClick = {
                        PrefsManager.toggleTheme()
                        (context as? android.app.Activity)?.recreate()
                    }) { Text(if (isDark) "☀️" else "🌙", fontSize = 18.sp) }
                    TextButton(onClick = { showAddDialog = true }) { Text("+", color = colorScheme.onPrimary, fontSize = 22.sp) }
                    TextButton(
                        onClick = {
                            checkingUpdate = true
                            UpdateManager.checkUpdate(context) { result ->
                                mainHandler.post {
                                    checkingUpdate = false
                                    result.fold(
                                        onSuccess = { info ->
                                            if (info != null) {
                                                updateInfo = info
                                            } else {
                                                Toast.makeText(context, "У тебя последняя версия!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onFailure = { Toast.makeText(context, "Ошибка: ${it.message}", Toast.LENGTH_LONG).show() }
                                    )
                                }
                            }
                        },
                        enabled = !checkingUpdate
                    ) { Text(if (checkingUpdate) "..." else "☰", color = colorScheme.onPrimary, fontSize = 18.sp) }
                    TextButton(onClick = onLogout) { Text("Выйти", color = colorScheme.onPrimary, fontSize = 14.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.primary)
            )
        },
        floatingActionButton = {
            if (incomingRequests.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showRequestsSheet = !showRequestsSheet },
                    containerColor = colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Text("📩", fontSize = 22.sp)
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.Red),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (incomingRequests.size > 9) "9+" else incomingRequests.size.toString(),
                                color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (incomingRequests.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp)
                    .padding(padding)
                    .clickable { showRequestsSheet = !showRequestsSheet },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Orange100)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📩", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Запросов в друзья: ${incomingRequests.size}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text("→", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }

        if (contacts.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Нет контактов", color = Color.Gray, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Нажми + чтобы добавить друга", color = Color.LightGray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            val sorted = contacts.sortedByDescending { it.id in onlineIds }
                .sortedBy { it.username.lowercase() }
                .sortedByDescending { it.id in onlineIds }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(sorted) { contact ->
                    val online = contact.id in onlineIds
                    val unread = unreadMap[contact.id] ?: 0
                    val hasUnread = unread > 0
                    val cStatus = statusMap[contact.id]
                    val statusObj = cStatus?.status ?: UserStatus.ONLINE
                    val statusLabel = if (online) statusObj.label else "Офлайн"
                    val statusColor = if (online) when (statusObj) {
                        UserStatus.ONLINE -> Green500
                        UserStatus.AWAY -> Color(0xFFFFB300)
                        UserStatus.BUSY -> Color(0xFFFF4444)
                        UserStatus.OFFLINE -> Color.Gray
                    } else Color.Gray
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { onChatClick(contact) },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(contact.username.first().uppercase(), color = colorScheme.onPrimary, fontSize = 20.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.username, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(statusLabel, fontSize = 12.sp, color = statusColor)
                                    if (!cStatus?.customText.isNullOrBlank()) {
                                        Spacer(Modifier.width(4.dp))
                                        Text("· ${cStatus!!.customText}", fontSize = 12.sp, color = statusColor)
                                    }
                                    if (hasUnread) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Red500.copy(alpha = blinkAlpha))
                                        )
                                    }
                                }
                            }
                            if (hasUnread) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Red500.copy(alpha = blinkAlpha))
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

    if (showRequestsSheet && incomingRequests.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showRequestsSheet = false },
            title = { Text("Запросы в друзья (${incomingRequests.size})", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    incomingRequests.forEach { req ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(req.fromUsername.first().uppercase(), color = colorScheme.onPrimary, fontSize = 16.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(req.fromUsername, modifier = Modifier.weight(1f), fontSize = 15.sp)
                        TextButton(onClick = {
                            ApiClient.acceptFriendRequest(req.id) { result ->
                                mainHandler.post {
                                    result.onSuccess { load() }
                                    result.onFailure { Toast.makeText(context, "Ошибка: ${it.message ?: "неизвестная"}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }) { Text("✅", fontSize = 18.sp) }
                        TextButton(onClick = {
                            ApiClient.declineFriendRequest(req.id) { result ->
                                mainHandler.post {
                                    result.onSuccess { load() }
                                    result.onFailure { Toast.makeText(context, "Ошибка: ${it.message ?: "неизвестная"}", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }) { Text("❌", fontSize = 18.sp) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showRequestsSheet = false }) { Text("Закрыть") } }
        )
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("Доступно обновление!") },
            text = {
                Column {
                    Text("v${updateInfo!!.versionName}")
                    Spacer(Modifier.height(4.dp))
                    Text(updateInfo!!.changelog, fontSize = 14.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    UpdateManager.downloadAndInstall(context, updateInfo!!.apkUrl)
                    updateInfo = null
                }) { Text("Обновить") }
            },
            dismissButton = { TextButton(onClick = { updateInfo = null }) { Text("Позже") } }
        )
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
                    ApiClient.sendFriendRequest(addUsername.trim()) { result ->
                        mainHandler.post {
                            result.fold(
                                onSuccess = {
                                    showAddDialog = false; addUsername = ""
                                    Toast.makeText(context, "Запрос отправлен!", Toast.LENGTH_SHORT).show()
                                    load()
                                },
                                onFailure = { addError = it.message ?: "Ошибка" }
                            )
                        }
                    }
                }) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false; addUsername = ""; addError = null }) { Text("Отмена") } }
        )
    }

    if (showStatusDialog) {
        var statusText by remember { mutableStateOf(myCustomText) }
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Мой статус", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    UserStatus.entries.forEach { s ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    myStatus = s.name
                                    SocketManager.setStatus(s.name, statusText)
                                    showStatusDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${s.emoji}  ${s.label}", fontSize = 16.sp,
                                fontWeight = if (myStatus == s.name) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        label = { Text("Текст статуса (необязательно)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            myCustomText = statusText
                            SocketManager.setStatus(UserStatus.fromString(myStatus).name, statusText)
                            showStatusDialog = false
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Сохранить текст") }
                }
            },
            confirmButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Закрыть") } }
        )
    }
}
