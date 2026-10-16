package com.appanuman.data.collector

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appanuman.data.mapper.PermissionCategoryMapper
import com.appanuman.data.model.PermissionCategory
import org.json.JSONArray

class PermissionSnapshotManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "appanuman_perm_snapshots",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("appanuman_perm_snapshots_fallback", Context.MODE_PRIVATE)
        }
    }

    /**
     * Diffs current declared permissions against previous snapshot.
     * Returns Pair(permissionCreepFlag, addedDangerousPermissions).
     * Saves new snapshot to EncryptedSharedPreferences.
     */
    fun checkPermissionCreepAndSave(
        packageName: String,
        currentDeclaredPermissions: List<String>
    ): Pair<Boolean, List<String>> {
        val key = "snap_$packageName"
        val previousJson = prefs.getString(key, null)

        val currentSet = currentDeclaredPermissions.toSet()

        if (previousJson == null) {
            // First time seeing this app, store snapshot and do not flag creep
            saveSnapshot(key, currentSet)
            return Pair(false, emptyList())
        }

        val previousSet = parseJsonArrayToSet(previousJson)
        val addedPermissions = (currentSet - previousSet).filter { perm ->
            isDangerousOrSensitive(perm)
        }

        saveSnapshot(key, currentSet)
        val hasCreep = addedPermissions.isNotEmpty()
        return Pair(hasCreep, addedPermissions)
    }

    private fun isDangerousOrSensitive(permissionName: String): Boolean {
        val category = PermissionCategoryMapper.getCategory(permissionName)
        return when (category) {
            PermissionCategory.SMS,
            PermissionCategory.CONTACTS_CALLS,
            PermissionCategory.CAMERA_MIC,
            PermissionCategory.LOCATION,
            PermissionCategory.PHONE_IDENTITY,
            PermissionCategory.STORAGE,
            PermissionCategory.SPECIAL_ACCESS -> true
            else -> false
        }
    }

    private fun saveSnapshot(key: String, permissions: Set<String>) {
        val jsonArray = JSONArray()
        for (perm in permissions) {
            jsonArray.put(perm)
        }
        prefs.edit().putString(key, jsonArray.toString()).apply()
    }

    private fun parseJsonArrayToSet(jsonStr: String): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                set.add(array.getString(i))
            }
        } catch (e: Exception) {
            // ignore
        }
        return set
    }
}
