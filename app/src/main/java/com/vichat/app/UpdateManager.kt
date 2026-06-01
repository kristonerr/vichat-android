package com.vichat.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Environment
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
        try {
            val fullUrl = if (apkUrl.startsWith("http")) apkUrl else "http://157.22.206.163:3001$apkUrl"
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(fullUrl)).apply {
                setTitle("ViChat")
                setDescription("Скачиваю обновление...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "vichat-v0.5.7.apk")
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Скачивание в фоне...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
