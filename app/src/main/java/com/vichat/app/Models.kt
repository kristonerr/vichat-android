package com.vichat.app

data class User(val id: Int, val username: String, val color: String?, val avatarUrl: String? = null)
data class AuthResponse(val accessToken: String, val refreshToken: String, val user: User)
data class ErrorResponse(val error: String?)
data class Message(val id: Int = 0, val fromId: Int, val text: String, val time: String?, val readAt: String? = null, val replyToId: Int? = null, val replyTo: ReplyPreview? = null)
data class ReplyPreview(val id: Int, val text: String, val fromId: Int)
data class Contact(val id: Int, val username: String, val color: String?, val avatarUrl: String? = null, val unread: Int = 0)
data class FriendRequest(val id: Int, val fromUserId: Int, val fromUsername: String, val fromColor: String?, val createdAt: String?)

data class UserStatusData(val status: UserStatus, val customText: String? = null)

enum class UserStatus(val label: String, val emoji: String) {
    ONLINE("В сети", "🟢"),
    AWAY("Нет на месте", "🟡"),
    BUSY("Не беспокоить", "🔴"),
    OFFLINE("Офлайн", "⚪");

    companion object {
        fun fromString(s: String?): UserStatus =
            entries.find { it.name == s } ?: ONLINE
    }
}
