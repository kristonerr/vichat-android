# ViChat Android

Android client for the ViChat messaging platform. Built with Kotlin and Jetpack Compose.

## Architecture

```
┌─────────────────────────────────────┐
│            MainActivity             │
│    (Navigation: login/contacts/chat)│
├─────────────────────────────────────┤
│  LoginScreen    ContactsScreen      │
│  (Auth UI)      (Contact List +     │
│                  Add Contact)       │
├─────────────────────────────────────┤
│            ChatScreen               │
│    (Messages + Context Menu:        │
│     copy/edit/delete + Typing)      │
├─────────────────────────────────────┤
│  ApiClient (OkHttp REST)            │
│  SocketManager (OkHttp WebSocket)   │
├─────────────────────────────────────┤
│  Models (User, Message, Contact)    │
│  PrefsManager (SharedPreferences)   │
│  NotificationHelper (Push notifs)   │
└─────────────────────────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.21 |
| UI | Jetpack Compose (BOM 2024.01.00) + Material3 |
| HTTP | OkHttp 4.12.0 |
| WebSocket | OkHttp (native Socket.IO protocol) |
| JSON | Gson 2.10.1 |
| Async | Kotlin Coroutines 1.7.3 |
| Android SDK | compileSdk 34, minSdk 24 |

## Features

- **Authentication**: Login/Register with gradient UI, password visibility toggle
- **Contacts**: List with online/offline status, unread counter (animated dot), add contact dialog
- **Chat**: Message bubbles, real-time delivery, read receipts, typing indicator
- **Context Menu** (long press): Copy text, edit message, delete message
- **Notifications**: High priority channel with custom sound, permission request on Android 13+
- **Auto-login**: Token persisted in SharedPreferences
- **Crash Handler**: Writes stack traces to `vichat_crash.log`

## API Configuration

Server URL is configured in `ApiClient.kt`:
```kotlin
private const val BASE_URL = "http://157.22.206.163:3001"
```

WebSocket URL is configured in `SocketManager.kt`:
```kotlin
private const val WS_BASE = "ws://157.22.206.163:3001/socket.io/?EIO=4&transport=websocket"
```

## Project Structure

```
vichat-android/
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # JVM & AndroidX settings
├── gradlew / gradlew.bat        # Gradle wrapper
├── app/
│   ├── build.gradle.kts          # App module config
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/vichat/app/
│       │   ├── MainActivity.kt        # Navigation host
│       │   ├── LoginScreen.kt         # Auth screen
│       │   ├── ContactsScreen.kt      # Contact list
│       │   ├── ChatScreen.kt          # Chat messages
│       │   ├── ApiClient.kt           # REST API client
│       │   ├── SocketManager.kt       # WebSocket client
│       │   ├── Models.kt              # Data classes
│       │   ├── PrefsManager.kt        # Token storage
│       │   └── NotificationHelper.kt  # Notifications
│       └── res/
│           ├── raw/icq_uhoh.mp3       # Notification sound
│           ├── values/colors.xml
│           ├── values/strings.xml
│           └── values/themes.xml
```

## Building

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/`
