package com.vichat.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

object ApiClient {
    private const val BASE_URL = "http://157.22.206.163:3001"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val JSON_MEDIA = "application/json".toMediaType()
    var token: String? = null
    var currentUserId: Int = 0
    val contactNames = mutableMapOf<Int, String>()

    fun getContactUsername(userId: Int): String = contactNames[userId] ?: "ViChat"

    private fun buildRequest(method: String, path: String, body: Any? = null): Request {
        val jsonBody = body?.let { gson.toJson(it) }
        return Request.Builder()
            .url("$BASE_URL$path")
            .method(method, jsonBody?.toRequestBody(JSON_MEDIA))
            .also { token?.let { t -> it.addHeader("Authorization", t) } }
            .build()
    }

    fun register(username: String, password: String, callback: (Result<AuthResponse>) -> Unit) {
        val req = buildRequest("POST", "/api/register", mapOf("username" to username, "password" to password))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        callback(Result.success(gson.fromJson(body, AuthResponse::class.java)))
                    } else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    fun login(username: String, password: String, callback: (Result<AuthResponse>) -> Unit) {
        val req = buildRequest("POST", "/api/login", mapOf("username" to username, "password" to password))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        callback(Result.success(gson.fromJson(body, AuthResponse::class.java)))
                    } else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    fun getContacts(callback: (Result<List<Contact>>) -> Unit) {
        val req = buildRequest("GET", "/api/contacts")
        val type = object : TypeToken<List<Contact>>() {}.type
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val list: List<Contact> = gson.fromJson(body, type)
                        contactNames.clear()
                        list.forEach { contactNames[it.id] = it.username }
                        callback(Result.success(list))
                    } else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun addContact(username: String, callback: (Result<String>) -> Unit) {
        val req = buildRequest("POST", "/api/contacts/add", mapOf("username" to username))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun getMessages(contactId: Int, callback: (Result<List<Message>>) -> Unit) {
        val req = buildRequest("GET", "/api/messages/$contactId")
        val type = object : TypeToken<List<Message>>() {}.type
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(gson.fromJson(body, type)))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun editMessage(msgId: Int, text: String, callback: (Result<String>) -> Unit) {
        val req = buildRequest("PUT", "/api/messages/$msgId/edit", mapOf("text" to text))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun deleteMessage(msgId: Int, callback: (Result<String>) -> Unit) {
        val req = buildRequest("DELETE", "/api/messages/$msgId")
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun getMe(callback: (Result<User>) -> Unit) {
        val req = buildRequest("GET", "/api/me")
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val user = gson.fromJson(body, User::class.java)
                        currentUserId = user.id
                        callback(Result.success(user))
                    } else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception("HTTP ${response.code}: $err")))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun sendFriendRequest(username: String, callback: (Result<String>) -> Unit) {
        val req = buildRequest("POST", "/api/friend-request", mapOf("username" to username))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception(err)))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun getIncomingRequests(callback: (Result<List<FriendRequest>>) -> Unit) {
        val req = buildRequest("GET", "/api/friend-requests/incoming")
        val type = object : TypeToken<List<FriendRequest>>() {}.type
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(gson.fromJson(body, type)))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception(err)))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun acceptFriendRequest(requestId: Int, callback: (Result<String>) -> Unit) {
        val req = buildRequest("POST", "/api/friend-request/$requestId/accept", mapOf("_dummy" to true))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception(err)))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }

    fun declineFriendRequest(requestId: Int, callback: (Result<String>) -> Unit) {
        val req = buildRequest("POST", "/api/friend-request/$requestId/decline", mapOf("_dummy" to true))
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) { callback(Result.failure(e)) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) callback(Result.success(body))
                    else {
                        val err = try { gson.fromJson(body, ErrorResponse::class.java)?.error ?: body } catch (_: Exception) { body }
                        callback(Result.failure(Exception(err)))
                    }
                } catch (e: Exception) { callback(Result.failure(e)) }
            }
        })
    }
}
