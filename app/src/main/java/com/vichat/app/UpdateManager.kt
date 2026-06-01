package com.vichat.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import okhttp3.*
import java.io.File

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
    private var currentDownloadId: Long = -1

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
                mainHandler.post { callback(Result.failure(e)) }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful) {
                        val info = gson.fromJson(body, VersionInfo::class.java)
                        mainHandler.post {
                            if (info.versionCode > currentVer) {
                                callback(Result.success(info))
                            } else {
                                callback(Result.success(null))
                            }
                        }
                    } else {
                        mainHandler.post { callback(Result.failure(Exception("HTTP ${response.code}"))) }
                    }
                } catch (e: Exception) {
                    mainHandler.post { callback(Result.failure(e)) }
                }
            }
        })
    }

    fun downloadAndInstall(context: Context, apkUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(context, "Разреши установку из неизвестных источников", Toast.LENGTH_LONG).show()
                return
            }
        }

        val fullUrl = if (apkUrl.startsWith("http")) apkUrl else "http://157.22.206.163:3001$apkUrl"
        val fileName = "vichat-update.apk"

        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir?.let { File(it, fileName).delete() }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(fullUrl)).apply {
            setTitle("ViChat обновление")
            setDescription("Скачиваю...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }
        currentDownloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != currentDownloadId) return
                ctx.unregisterReceiver(this)
                val file = File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                if (!file.exists()) return
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                ctx.startActivity(installIntent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        Toast.makeText(context, "Скачивание...", Toast.LENGTH_SHORT).show()
    }
}
