package com.vichat.app

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*

sealed class WsEvent {
    data class MessageReceived(val msg: Message) : WsEvent()
    data class MessageEdited(val id: Int, val text: String) : WsEvent()
    data class MessageDeleted(val id: Int) : WsEvent()
    data class Typing(val fromUsername: String) : WsEvent()
    object StopTyping : WsEvent()
    data class ContactsOnline(val list: List<Int>) : WsEvent()
    data class UnreadUpdate(val fromUserId: Int, val count: Int) : WsEvent()
    object Connected : WsEvent()
    object Disconnected : WsEvent()
    data class Error(val text: String) : WsEvent()
}

object SocketManager {
    private const val WS_BASE = "ws://157.22.206.163:3001/socket.io/?EIO=4&transport=websocket"
    private val client = OkHttpClient.Builder().build()
    private var ws: WebSocket? = null
    private val gson = Gson()
    private val _events = Channel<WsEvent>(Channel.UNLIMITED)
    val events: Flow<WsEvent> = _events.receiveAsFlow()

    fun connect(token: String) {
        val request = Request.Builder()
            .url("$WS_BASE&token=$token")
            .addHeader("Authorization", token)
            .build()
        ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Wait for Engine.IO open packet 0{...}, then send 40
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleFrame(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _events.trySend(WsEvent.Error(t.message ?: "Connection failed"))
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _events.trySend(WsEvent.Disconnected)
            }
        })
    }

    private fun handleFrame(text: String) {
        when {
            text.startsWith("0") -> {
                // Engine.IO open packet, send Socket.IO connect
                ws?.send("40")
            }
            text == "2" -> {
                // Engine.IO ping, send pong
                ws?.send("3")
            }
            text == "40" -> {
                // Socket.IO connected (empty sid)
                _events.trySend(WsEvent.Connected)
            }
            text.startsWith("40") && text.length > 2 -> {
                // Socket.IO connected with sid
                _events.trySend(WsEvent.Connected)
            }
            text.startsWith("42") -> {
                try {
                    val jsonArr = JsonParser.parseString(text.substring(2)).asJsonArray
                    val event = jsonArr[0].asString
                    val data = jsonArr[1]
                    when (event) {
                        "private-message" -> {
                            val msg = gson.fromJson(data, Message::class.java)
                            _events.trySend(WsEvent.MessageReceived(msg))
                        }
                        "contacts-online" -> {
                            val onlineArr = data.asJsonArray
                            val ids = mutableListOf<Int>()
                            for (el in onlineArr) ids.add(el.asJsonObject.get("id").asInt)
                            _events.trySend(WsEvent.ContactsOnline(ids))
                        }
                        "typing" -> {
                            val from = data.asJsonObject.get("fromUsername").asString
                            _events.trySend(WsEvent.Typing(from))
                        }
                        "stop-typing" -> {
                            _events.trySend(WsEvent.StopTyping)
                        }
                        "unread-update" -> {
                            val obj = data.asJsonObject
                            val fromUserId = obj.get("fromUserId").asInt
                            val count = obj.get("count").asInt
                            _events.trySend(WsEvent.UnreadUpdate(fromUserId, count))
                        }
                        "message-edited" -> {
                            val obj = data.asJsonObject
                            _events.trySend(WsEvent.MessageEdited(obj.get("id").asInt, obj.get("text").asString))
                        }
                        "message-deleted" -> {
                            _events.trySend(WsEvent.MessageDeleted(data.asJsonObject.get("id").asInt))
                        }
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

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
    }
}
