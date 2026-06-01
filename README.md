# ViChat Android

Android client for the ViChat messaging platform. Built with Kotlin and Jetpack Compose.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 1.9.21 |
| UI | Jetpack Compose (BOM 2024.01.00) + Material3 |
| HTTP | OkHttp 4.12.0 |
| WebSocket | OkHttp (Socket.IO protocol) |
| JSON | Gson 2.10.1 |
| Async | Kotlin Coroutines 1.7.3 |
| Storage | EncryptedSharedPreferences (AES256) |
| Android SDK | compileSdk 34, minSdk 24 |

## Features

- **Authentication**: Login/Register with gradient UI, password visibility toggle
- **Contacts**: List with online/offline status, unread counter, add contact dialog, incoming requests
- **Chat**: Message bubbles, real-time delivery, read receipts, typing indicator, emoji picker
- **Context Menu** (long press): Copy text, edit message, delete message
- **Themes**: Light/Dark mode toggle
- **Notifications**: High priority channel with custom sound
- **In-app Updates**: Check for updates via ☰ menu button
- **Auto-login**: Encrypted token persistence
- **Encrypted Storage**: Token protected with AES256 via EncryptedSharedPreferences
- **Security**: Token never in URL, Network Security Config ready for HTTPS

## Project Structure

```
vichat-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/vichat/app/
│       │   ├── MainActivity.kt        # Navigation host
│       │   ├── LoginScreen.kt         # Auth screen
│       │   ├── ContactsScreen.kt      # Contact list + updates
│       │   ├── ChatScreen.kt          # Chat messages
│       │   ├── ApiClient.kt           # REST API client
│       │   ├── SocketManager.kt       # WebSocket client
│       │   ├── UpdateManager.kt       # In-app update checker
│       │   ├── Models.kt              # Data classes
│       │   ├── Theme.kt               # Dark/Light theme
│       │   ├── EmojiPicker.kt         # Emoji selector
│       │   ├── PrefsManager.kt        # Encrypted storage
│       │   └── NotificationHelper.kt  # Notifications
│       └── res/
│           ├── xml/provider_paths.xml
│           ├── xml/network_security_config.xml
│           └── raw/icq_uhoh.mp3
├── build.gradle.kts
└── settings.gradle.kts
```

## Building

```bash
./gradlew assembleDebug
```
