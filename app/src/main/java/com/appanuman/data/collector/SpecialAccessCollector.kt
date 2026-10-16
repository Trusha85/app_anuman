package com.appanuman.data.collector

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.appanuman.data.model.PermissionState
import com.appanuman.data.model.SpecialAccessResult

class SpecialAccessCollector(private val context: Context) {

    fun collectSpecialAccess(
        packageName: String,
        uid: Int,
        declaredPermissions: List<String>
    ): SpecialAccessResult {
        // 1. Accessibility Service check
        val (accessibilityState, enabledServices) = checkAccessibility(packageName)

        // 2. Device Admin check
        val (adminState, activeAdmins) = checkDeviceAdmin(packageName)

        // 3. Notification Listener check
        val notificationListenerState = checkNotificationListener(packageName)

        // 4. Overlay check (SYSTEM_ALERT_WINDOW)
        val overlayState = checkOverlay(packageName, uid, declaredPermissions)

        // 5. Install Packages check (REQUEST_INSTALL_PACKAGES)
        val installPackagesState = checkInstallPackages(packageName, uid, declaredPermissions)

        return SpecialAccessResult(
            accessibilityEnabled = accessibilityState,
            deviceAdminActive = adminState,
            notificationListenerEnabled = notificationListenerState,
            overlayState = overlayState,
            installPackagesState = installPackagesState,
            enabledAccessibilityServices = enabledServices,
            activeDeviceAdmins = activeAdmins
        )
    }

    private fun checkAccessibility(packageName: String): Pair<PermissionState, List<String>> {
        return try {
            val am = context.getSystemService(AccessibilityManager::class.java)
                ?: return Pair(PermissionState.UNABLE_TO_CHECK, emptyList())

            val services = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                ?: emptyList()

            val matchingServices = services.mapNotNull { serviceInfo ->
                val id = serviceInfo.id
                val component = ComponentName.unflattenFromString(id)
                if (component?.packageName == packageName || id.startsWith("$packageName/")) {
                    id
                } else null
            }

            if (matchingServices.isNotEmpty()) {
                Pair(PermissionState.GRANTED, matchingServices)
            } else {
                Pair(PermissionState.NOT_REQUESTED, emptyList())
            }
        } catch (e: Exception) {
            Pair(PermissionState.UNABLE_TO_CHECK, emptyList())
        }
    }

    private fun checkDeviceAdmin(packageName: String): Pair<PermissionState, List<String>> {
        return try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
                ?: return Pair(PermissionState.UNABLE_TO_CHECK, emptyList())

            val admins = dpm.activeAdmins ?: emptyList()
            val matchingAdmins = admins.filter { it.packageName == packageName }.map { it.flattenToString() }

            if (matchingAdmins.isNotEmpty()) {
                Pair(PermissionState.GRANTED, matchingAdmins)
            } else {
                Pair(PermissionState.NOT_REQUESTED, emptyList())
            }
        } catch (e: Exception) {
            Pair(PermissionState.UNABLE_TO_CHECK, emptyList())
        }
    }

    private fun checkNotificationListener(packageName: String): PermissionState {
        return try {
            val setting = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return PermissionState.NOT_REQUESTED

            val flatComponents = setting.split(":").map { it.trim() }.filter { it.isNotEmpty() }
            val isEnabled = flatComponents.any { flatStr ->
                val cn = ComponentName.unflattenFromString(flatStr)
                cn?.packageName == packageName || flatStr.startsWith("$packageName/")
            }

            if (isEnabled) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
        } catch (e: Exception) {
            PermissionState.UNABLE_TO_CHECK
        }
    }

    private fun checkOverlay(
        packageName: String,
        uid: Int,
        declaredPermissions: List<String>
    ): PermissionState {
        val isDeclared = declaredPermissions.any {
            it.equals("android.permission.SYSTEM_ALERT_WINDOW", ignoreCase = true)
        }
        if (!isDeclared) {
            return PermissionState.NOT_REQUESTED
        }

        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
                ?: return PermissionState.DECLARED_ONLY

            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                uid,
                packageName
            )

            if (mode == AppOpsManager.MODE_ALLOWED) {
                PermissionState.GRANTED
            } else {
                PermissionState.DECLARED_ONLY
            }
        } catch (e: Exception) {
            // NEVER show GRANTED if AppOpsManager throws
            PermissionState.DECLARED_ONLY
        }
    }

    companion object {
        private const val OPSTR_REQUEST_INSTALL_PACKAGES = "android:request_install_packages"
    }

    private fun checkInstallPackages(
        packageName: String,
        uid: Int,
        declaredPermissions: List<String>
    ): PermissionState {
        val isDeclared = declaredPermissions.any {
            it.equals("android.permission.REQUEST_INSTALL_PACKAGES", ignoreCase = true)
        }
        if (!isDeclared) {
            return PermissionState.NOT_REQUESTED
        }

        return try {
            val appOps = context.getSystemService(AppOpsManager::class.java)
                ?: return PermissionState.DECLARED_ONLY

            val mode = appOps.checkOpNoThrow(
                OPSTR_REQUEST_INSTALL_PACKAGES,
                uid,
                packageName
            )

            if (mode == AppOpsManager.MODE_ALLOWED) {
                PermissionState.GRANTED
            } else {
                PermissionState.DECLARED_ONLY
            }
        } catch (e: Exception) {
            PermissionState.DECLARED_ONLY
        }
    }
}
