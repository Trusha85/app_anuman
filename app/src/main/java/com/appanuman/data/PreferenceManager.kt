package com.appanuman.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appanuman.model.Language
import com.appanuman.model.UserMode

class PreferenceManager(context: Context) {

    private val sharedPreferences: SharedPreferences by lazy {
        try {
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
        } catch (e: Exception) {
            context.getSharedPreferences("app_anuman_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    var userMode: UserMode
        get() {
            val name = sharedPreferences.getString(KEY_USER_MODE, UserMode.SIMPLE.name)
            return try {
                UserMode.valueOf(name ?: UserMode.SIMPLE.name)
            } catch (e: Exception) {
                UserMode.SIMPLE
            }
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_USER_MODE, value.name).apply()
        }

    var language: Language
        get() {
            val code = sharedPreferences.getString(KEY_LANGUAGE, Language.EN.code)
            return if (code == Language.HI.code) Language.HI else Language.EN
        }
        set(value) {
            sharedPreferences.edit().putString(KEY_LANGUAGE, value.code).apply()
        }

    fun savePermissionSnapshot(packageName: String, permissions: Set<String>) {
        val serialized = permissions.joinToString(",")
        sharedPreferences.edit().putString("snap_$packageName", serialized).apply()
    }

    fun getPermissionSnapshot(packageName: String): Set<String>? {
        val serialized = sharedPreferences.getString("snap_$packageName", null) ?: return null
        return if (serialized.isBlank()) emptySet() else serialized.split(",").toSet()
    }

    companion object {
        private const val KEY_USER_MODE = "pref_user_mode"
        private const val KEY_LANGUAGE = "pref_language"
    }
}
