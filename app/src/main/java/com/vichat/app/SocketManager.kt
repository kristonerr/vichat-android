package com.vichat.app

import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import okhttp3.*

sealed class WsEvent {
    data class MessageReceived(val msg: Message) : WsEvent()
    data class MessageEdited(val id: Int, val text: String) : WsEvent()
    data class MessageDeleted(val id: Int) : WsEvent()
    data class Typing(val fromUsername: String) : WsEvent()
    object StopTyping : WsEvent()
    data class ContactsOnline(val list: List<Int>) : WsEvent()
    data class UnreadUpdate(val fromUserId: Int, val count: Int) : WsEvent()
    data class FriendRequestReceived(val fromUserId: Int, val fromUsername: String) : WsEvent()
    data class FriendRequestAccepted(val userId: Int, val username: String) : WsEvent()
    data class UserStatusChanged(val userId: Int, val username: String, val status: String, val customText: String? = null) : WsEvent()
    data class MyStatusSet(val status: String, val customText: String? = null) : WsEvent()
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class Error(val text: String) : WsEvent()
}

object SocketManager {
    private const val WS_BASE = "ws://157.22.206.163:3001/socket.io/?EIO=4&transport=websocket"
    private val client = OkHttpClient.Builder().build()
    private var ws: WebSocket? = null
    private val gson = Gson()
    private val _events = MutableSharedFlow<WsEvent>(extraBufferCapacity = 64)
    val events: Flow<WsEvent> = _events

    fun connect(token: String) {
        val request = Request.Builder()
            .url(WS_BASE)
            .addHeader("Authorization", token)
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleFrame(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.tryEmit(WsEvent.Error(t.message ?: "Connection failed"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.tryEmit(WsEvent.Disconnected)
            }
        })
    }

    private fun handleFrame(text: String) {
        when {
            text.startsWith("0") -> {
                ws?.send("40")
            }
            text == "2" -> {
                ws?.send("3")
            }
            text == "40" -> {
                _events.tryEmit(WsEvent.Connected)
            }
            text.startsWith("40") && text.length > 2 -> {
                _events.tryEmit(WsEvent.Connected)
            }
            text.startsWith("42") -> {
                try {
                    val jsonArr = JsonParser.parseString(text.substring(2)).asJsonArray
                    val event = jsonArr[0].asString
                    val data = jsonArr[1]
                    when (event) {
                        "private-message" -> {
                            val msg = gson.fromJson(data, Message::class.java)
                            _events.tryEmit(WsEvent.MessageReceived(msg))
                        }
                        "contacts-online" -> {
                            val onlineArr = data.asJsonArray
                            val ids = mutableListOf<Int>()
                            for (el in onlineArr) {
                                val elObj = el.asJsonObject
                                ids.add(elObj.get("id").asInt)
                                val status = elObj.get("status")?.asString
                                if (status != null) {
                                    val username = elObj.get("username")?.asString ?: ""
                                    val customText = if (elObj.has("customText")) elObj.get("customText")?.asString else null
                                    _events.tryEmit(WsEvent.UserStatusChanged(elObj.get("id").asInt, username, status, customText))
                                }
                            }
                            _events.tryEmit(WsEvent.ContactsOnline(ids))
                        }
                        "typing" -> {
                            val from = data.asJsonObject.get("fromUsername").asString
                            _events.tryEmit(WsEvent.Typing(from))
                        }
                        "stop-typing" -> {
                            _events.tryEmit(WsEvent.StopTyping)
                        }
                        "unread-update" -> {
                            val obj = data.asJsonObject
                            val fromUserId = obj.get("fromUserId").asInt
                            val count = obj.get("count").asInt
                            _events.tryEmit(WsEvent.UnreadUpdate(fromUserId, count))
                        }
                        "message-edited" -> {
                            val obj = data.asJsonObject
                            _events.tryEmit(WsEvent.MessageEdited(obj.get("id").asInt, obj.get("text").asString))
                        }
                        "message-deleted" -> {
                            _events.tryEmit(WsEvent.MessageDeleted(data.asJsonObject.get("id").asInt))
                        }
                        "friend-request" -> {
                            val obj = data.asJsonObject
                            _events.tryEmit(WsEvent.FriendRequestReceived(obj.get("fromUserId").asInt, obj.get("fromUsername").asString))
                        }
                        "friend-request-accepted" -> {
                            val obj = data.asJsonObject
                            _events.tryEmit(WsEvent.FriendRequestAccepted(obj.get("userId").asInt, obj.get("username").asString))
                        }
                        "user-status" -> {
                            val obj = data.asJsonObject
                            val userId = obj.get("userId").asInt
                            val username = obj.get("username").asString
                            val status = obj.get("status").asString
                            val customText = if (obj.has("customText")) obj.get("customText")?.asString else null
                            _events.tryEmit(WsEvent.UserStatusChanged(userId, username, status, customText))
                        }
                        "my-status" -> {
                            val obj = data.asJsonObject
                            val s = obj.get("status").asString
                            val customText = if (obj.has("customText")) obj.get("customText")?.asString else null
                            _events.tryEmit(WsEvent.MyStatusSet(s, customText))
                        }
                        else -> {}
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun sendMessage(toUserId: Int, text: String) {
        val data = gson.toJson(mapOf("toUserId" to toUserId, "text" to text))
        ws?.send("42" + "[\"private-message\",$data]")
    }

    fun sendTyping(toUserId: Int) {
        ws?.send("42" + "[\"typing\",{\"toUserId\":$toUserId}]")
    }

    fun sendStopTyping(toUserId: Int) {
        ws?.send("42" + "[\"stop-typing\",{\"toUserId\":$toUserId}]")
    }

    fun sendMarkRead(fromUserId: Int) {
        ws?.send("42" + "[\"mark-read\",{\"fromUserId\":$fromUserId}]")
    }

    fun setStatus(status: String, customText: String? = null) {
        val obj = mutableMapOf("status" to status)
        if (!customText.isNullOrBlank()) obj["customText"] = customText
        val json = gson.toJson(obj)
        ws?.send("42" + "[\"set-status\",$json]")
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }
}
