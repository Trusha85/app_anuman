package com.appanuman.data.mapper

import com.appanuman.data.model.PermissionCategory

object PermissionCategoryMapper {

    private val SPECIAL_ACCESS_PERMS = setOf(
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.MANAGE_DEVICE_ADMINS"
    )

    private val SMS_PERMS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH",
        "android.permission.BROADCAST_SMS"
    )

    private val CONTACTS_CALLS_PERMS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS"
    )

    private val CAMERA_MIC_PERMS = setOf(
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO"
    )

    private val LOCATION_PERMS = setOf(
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION"
    )

    private val PHONE_IDENTITY_PERMS = setOf(
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.CALL_PHONE",
        "android.permission.USE_SIP",
        "android.permission.ANSWER_PHONE_CALLS",
        "android.permission.ADD_VOICEMAIL",
        "android.permission.USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER"
    )

    private val STORAGE_PERMS = setOf(
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
        "android.permission.MANAGE_EXTERNAL_STORAGE"
    )

    private val BACKGROUND_PERMS = setOf(
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.FOREGROUND_SERVICE",
        "android.permission.FOREGROUND_SERVICE_SPECIAL_USE",
        "android.permission.FOREGROUND_SERVICE_SYSTEM_EXEMPTED",
        "android.permission.FOREGROUND_SERVICE_CAMERA",
        "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        "android.permission.FOREGROUND_SERVICE_LOCATION",
        "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.USE_EXACT_ALARM",
        "android.permission.WAKE_LOCK"
    )

    private val NETWORK_PERMS = setOf(
        "android.permission.INTERNET",
        "android.permission.ACCESS_NETWORK_STATE",
        "android.permission.CHANGE_NETWORK_STATE",
        "android.permission.ACCESS_WIFI_STATE",
        "android.permission.CHANGE_WIFI_STATE",
        "android.permission.CHANGE_WIFI_MULTICAST_STATE"
    )

    /**
     * Maps an Android permission name to its exact PermissionCategory.
     * Unknown permissions default to SPECIAL_ACCESS.
     */
    fun getCategory(permissionName: String): PermissionCategory {
        val perm = permissionName.trim()
        val simpleName = if (perm.startsWith("android.permission.")) {
            perm
        } else if (perm.contains(".")) {
            perm
        } else {
            "android.permission.$perm"
        }

        return when {
            SPECIAL_ACCESS_PERMS.contains(simpleName) || SPECIAL_ACCESS_PERMS.contains(perm) -> PermissionCategory.SPECIAL_ACCESS
            SMS_PERMS.contains(simpleName) || SMS_PERMS.contains(perm) -> PermissionCategory.SMS
            CONTACTS_CALLS_PERMS.contains(simpleName) || CONTACTS_CALLS_PERMS.contains(perm) -> PermissionCategory.CONTACTS_CALLS
            CAMERA_MIC_PERMS.contains(simpleName) || CAMERA_MIC_PERMS.contains(perm) -> PermissionCategory.CAMERA_MIC
            LOCATION_PERMS.contains(simpleName) || LOCATION_PERMS.contains(perm) -> PermissionCategory.LOCATION
            PHONE_IDENTITY_PERMS.contains(simpleName) || PHONE_IDENTITY_PERMS.contains(perm) -> PermissionCategory.PHONE_IDENTITY
            STORAGE_PERMS.contains(simpleName) || STORAGE_PERMS.contains(perm) -> PermissionCategory.STORAGE
            BACKGROUND_PERMS.contains(simpleName) || BACKGROUND_PERMS.contains(perm) -> PermissionCategory.BACKGROUND
            NETWORK_PERMS.contains(simpleName) || NETWORK_PERMS.contains(perm) -> PermissionCategory.NETWORK
            else -> {
                // Secondary heuristic matching if vendor or custom permission
                val upper = perm.uppercase()
                when {
                    upper.contains("SMS") || upper.contains("MMS") -> PermissionCategory.SMS
                    upper.contains("CONTACT") || upper.contains("CALL_LOG") -> PermissionCategory.CONTACTS_CALLS
                    upper.contains("CAMERA") || upper.contains("AUDIO") || upper.contains("RECORD") -> PermissionCategory.CAMERA_MIC
                    upper.contains("LOCATION") -> PermissionCategory.LOCATION
                    upper.contains("STORAGE") || upper.contains("MEDIA") -> PermissionCategory.STORAGE
                    upper.contains("BOOT") || upper.contains("ALARM") -> PermissionCategory.BACKGROUND
                    upper.contains("NETWORK") || upper.contains("WIFI") || upper.contains("INTERNET") -> PermissionCategory.NETWORK
                    else -> PermissionCategory.SPECIAL_ACCESS
                }
            }
        }
    }
}
