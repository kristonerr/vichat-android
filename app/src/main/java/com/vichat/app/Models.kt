package com.vichat.app

data class User(val id: Int, val username: String, val color: String?)
data class AuthResponse(val token: String, val user: User)
data class ErrorResponse(val error: String?)
data class Message(val id: Int = 0, val fromId: Int, val text: String, val time: String?, val readAt: String? = null)
data class Contact(val id: Int, val username: String, val color: String?, val unread: Int = 0)
