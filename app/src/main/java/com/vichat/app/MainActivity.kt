package com.vichat.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
class MainActivity : ComponentActivity() {
    private var openChatContactId by mutableStateOf<Int?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                java.io.File(filesDir, "vichat_crash.log").bufferedWriter().use { w ->
                    w.write("=== CRASH at ${java.util.Date()} ===\n")
                    w.write("Thread: ${thread.name}\n")
                    w.write(android.util.Log.getStackTraceString(throwable))
                    w.write("\n")
                }
            } catch (_: java.lang.Exception) {}
        }

        PrefsManager.init(this)
        NotificationHelper.createChannel(this)

        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                var currentScreen by remember { mutableStateOf("login") }
                var selectedContact by remember { mutableStateOf<Contact?>(null) }
                var initialized by remember { mutableStateOf(false) }
                val mainHandler = remember { Handler(Looper.getMainLooper()) }

                LaunchedEffect(Unit) {
                    val token = PrefsManager.getToken()
                    if (token != null) {
                        ApiClient.token = token
                        ApiClient.getMe { result ->
                            mainHandler.post {
                                if (result.isSuccess) {
                                    SocketManager.connect(token)
                                    currentScreen = "contacts"
                                } else {
                                    PrefsManager.clear()
                                    ApiClient.token = null
                                }
                                initialized = true
                            }
                        }
                    } else {
                        initialized = true
                    }
                }

                LaunchedEffect(Unit) {
                    SocketManager.events.collect { event ->
                        if (event is WsEvent.MessageReceived) {
                            if (event.msg.fromId != openChatContactId) {
                                val username = try {
                                    ApiClient.getContactUsername(event.msg.fromId)
                                } catch (_: Exception) { "ViChat" }
                                NotificationHelper.show(this@MainActivity, username, event.msg.text)
                            }
                        }
                    }
                }

                if (initialized) {
                    when (currentScreen) {
                        "login" -> LoginScreen(onLogin = { token ->
                            SocketManager.connect(token)
                            currentScreen = "contacts"
                        })
                        "contacts" -> ContactsScreen(
                            onChatClick = { contact ->
                                selectedContact = contact; openChatContactId = contact.id; currentScreen = "chat"
                            },
                            onLogout = {
                                SocketManager.disconnect()
                                PrefsManager.clear()
                                ApiClient.token = null
                                currentScreen = "login"
                            }
                        )
                        "chat" -> selectedContact?.let {
                            ChatScreen(contact = it, onBack = { currentScreen = "contacts"; selectedContact = null; openChatContactId = null })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketManager.disconnect()
    }
}
