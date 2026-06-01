package com.vichat.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*

data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String
)

object UpdateManager {
    private const val VERSION_URL = "http://157.22.206.163:3001/api/version"
    private const val APK_BASE = "http://157.22.206.163:3001"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = Gson()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getCurrentVersionCode(context: Context): Int {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (_: Exception) { 0 }
    }

    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (_: Exception) { "?" }
    }

    fun checkUpdate(context: Context, callback: (Result<VersionInfo?>) -> Unit) {
        val currentVer = getCurrentVersionCode(context)
        val req = Request.Builder().url(VERSION_URL).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                callback(Result.failure(e))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val info = gson.fromJson(body, VersionInfo::class.java)
                        if (info.versionCode > currentVer) {
                            callback(Result.success(info))
                        } else {
                            callback(Result.success(null))
                        }
                    } else {
                        callback(Result.failure(Exception("HTTP ${response.code}")))
                    }
                } catch (e: Exception) {
                    callback(Result.failure(e))
                }
            }
        })
    }

    fun downloadAndInstall(context: Context, apkUrl: String) {
        val fullUrl = if (apkUrl.startsWith("http")) apkUrl else "$APK_BASE$apkUrl"
        mainHandler.post {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Не могу открыть браузер: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
