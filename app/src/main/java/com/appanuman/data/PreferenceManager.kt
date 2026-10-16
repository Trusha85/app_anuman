package com.appanuman.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appanuman.model.Language
import com.appanuman.model.UserMode

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "app_anuman_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        try {
            context.deleteSharedPreferences("app_anuman_secure_prefs")
        } catch (ignored: Throwable) {}
        context.getSharedPreferences("app_anuman_fallback_prefs", Context.MODE_PRIVATE)
    }

    var userMode: UserMode
        get() {
            return try {
                val name = sharedPreferences.getString(KEY_USER_MODE, UserMode.SIMPLE.name)
                UserMode.valueOf(name ?: UserMode.SIMPLE.name)
            } catch (t: Throwable) {
                UserMode.SIMPLE
            }
        }
        set(value) {
            try {
                sharedPreferences.edit().putString(KEY_USER_MODE, value.name).apply()
            } catch (t: Throwable) {
                // Ignore fallback
            }
        }

    var language: Language
        get() {
            return try {
                val code = sharedPreferences.getString(KEY_LANGUAGE, Language.EN.code)
                if (code == Language.HI.code) Language.HI else Language.EN
            } catch (t: Throwable) {
                Language.EN
            }
        }
        set(value) {
            try {
                sharedPreferences.edit().putString(KEY_LANGUAGE, value.code).apply()
            } catch (t: Throwable) {
                // Ignore fallback
            }
        }

    fun savePermissionSnapshot(packageName: String, permissions: Set<String>) {
        try {
            val serialized = permissions.joinToString(",")
            sharedPreferences.edit().putString("snap_$packageName", serialized).apply()
        } catch (t: Throwable) {
            // Ignore fallback
        }
    }

    fun getPermissionSnapshot(packageName: String): Set<String>? {
        return try {
            val serialized = sharedPreferences.getString("snap_$packageName", null) ?: return null
            if (serialized.isBlank()) emptySet() else serialized.split(",").toSet()
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        private const val KEY_USER_MODE = "pref_user_mode"
        private const val KEY_LANGUAGE = "pref_language"
    }
}
