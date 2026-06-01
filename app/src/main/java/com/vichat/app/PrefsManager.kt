package com.vichat.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PrefsManager {
    private const val PREFS_NAME = "vichat_secure"
    private const val KEY_TOKEN = "token"
    private const val KEY_DARK_THEME = "dark_theme"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, false)
        set(value) { prefs.edit().putBoolean(KEY_DARK_THEME, value).apply() }

    fun toggleTheme(): Boolean {
        val new = !isDarkTheme
        isDarkTheme = new
        return new
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
