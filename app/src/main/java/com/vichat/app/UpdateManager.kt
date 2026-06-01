package com.vichat.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import okhttp3.*
import java.io.File
import java.io.FileOutputStream

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
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
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
        Toast.makeText(context, "Скачиваю обновление...", Toast.LENGTH_SHORT).show()
        val req = Request.Builder().url(apkUrl).build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {
                mainHandler.post {
                    Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        mainHandler.post { Toast.makeText(context, "Ошибка сервера ${response.code}", Toast.LENGTH_LONG).show() }
                        return
                    }
                    val bytes = response.body?.bytes() ?: run {
                        mainHandler.post { Toast.makeText(context, "Пустой ответ", Toast.LENGTH_LONG).show() }; return
                    }
                    if (bytes.size < 1000000) {
                        mainHandler.post { Toast.makeText(context, "Файл слишком мал (${bytes.size} байт)", Toast.LENGTH_LONG).show() }; return
                    }
                    val file = File(context.cacheDir, "update.apk")
                    FileOutputStream(file).write(bytes)
                    if (!file.exists() || file.length() < 1000000) {
                        mainHandler.post { Toast.makeText(context, "Ошибка записи файла", Toast.LENGTH_LONG).show() }; return
                    }
                    val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    mainHandler.post {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(apkUri, "application/vnd.android.package-archive")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                                    data = apkUri
                                    putExtra(Intent.EXTRA_RETURN_RESULT, true)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Не могу открыть установщик: ${e2.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}
