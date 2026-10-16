package com.appanuman.data.collector

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.appanuman.BuildConfig
import com.appanuman.data.engine.RiskScoringEngine
import com.appanuman.data.model.*

class AppInventoryCollector(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val appOpsManager: AppOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    private val accessibilityManager: AccessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    private val devicePolicyManager: DevicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val prefs by lazy {
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

    companion object {
        val REMOTE_ACCESS_PACKAGES = setOf(
            "com.anydesk.anydeskandroid",
            "com.teamviewer.teamviewer",
            "com.zoho.assist.agent",
            "com.rustdesk.rustdesk",
            "com.supremo.supremocontrol",
            "com.logmein.rescue",
            "com.alfredcamera.android",
            "com.hmdm.launcher",
            "com.manageengine.mdm.android"
        )

        fun mapPermissionToCategory(perm: String): PermissionCategory {
            val upper = perm.uppercase()
            return when {
                upper.contains("ACCESSIBILITY") || upper.contains("SYSTEM_ALERT_WINDOW") ||
                        upper.contains("BIND_DEVICE_ADMIN") || upper.contains("NOTIFICATION_LISTENER") ||
                        upper.contains("REQUEST_INSTALL_PACKAGES") || upper.contains("PACKAGE_USAGE_STATS") ->
                    PermissionCategory.SPECIAL_ACCESS

                upper.contains("SMS") -> PermissionCategory.SMS

                upper.contains("CONTACTS") || upper.contains("CALL_LOG") -> PermissionCategory.CONTACTS_CALLS

                upper.contains("CAMERA") || upper.contains("RECORD_AUDIO") -> PermissionCategory.CAMERA_MIC

                upper.contains("LOCATION") -> PermissionCategory.LOCATION

                upper.contains("READ_PHONE_STATE") || upper.contains("READ_PHONE_NUMBERS") ||
                        upper.contains("CALL_PHONE") -> PermissionCategory.PHONE_IDENTITY

                upper.contains("STORAGE") || upper.contains("MEDIA") -> PermissionCategory.STORAGE

                upper.contains("BOOT_COMPLETED") || upper.contains("FOREGROUND_SERVICE") ||
                        upper.contains("REQUEST_IGNORE_BATTERY_OPTIMIZATIONS") -> PermissionCategory.BACKGROUND

                upper.contains("INTERNET") || upper.contains("NETWORK") -> PermissionCategory.NETWORK

                else -> PermissionCategory.SPECIAL_ACCESS
            }
        }
    }

    fun collectAllApps(includeSystemApps: Boolean = false): List<AppRiskProfile> {
        val packages = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        val usageMap = getUsageStatsMap()
        val enabledAccessibilities = getEnabledAccessibilityPackages()
        val activeAdmins = getActiveAdminPackages()
        val enabledNotifListeners = getNotificationListenerPackages()

        val results = mutableListOf<AppRiskProfile>()

        for (pkg in packages) {
            val isSystemApp = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystemApp && !includeSystemApps && pkg.packageName != context.packageName) {
                continue
            }

            val profile = scanSinglePackage(
                pkg = pkg,
                usageMap = usageMap,
                enabledAccessibilities = enabledAccessibilities,
                activeAdmins = activeAdmins,
                enabledNotifListeners = enabledNotifListeners
            )
            results.add(profile)
        }

        return results
    }

    fun scanPackageByName(packageName: String): AppRiskProfile? {
        return try {
            val pkg = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val usageMap = getUsageStatsMap()
            val enabledAccessibilities = getEnabledAccessibilityPackages()
            val activeAdmins = getActiveAdminPackages()
            val enabledNotifListeners = getNotificationListenerPackages()
            scanSinglePackage(pkg, usageMap, enabledAccessibilities, activeAdmins, enabledNotifListeners)
        } catch (e: Exception) {
            null
        }
    }

    private fun scanSinglePackage(
        pkg: PackageInfo,
        usageMap: Map<String, Long>,
        enabledAccessibilities: Set<String>,
        activeAdmins: Set<String>,
        enabledNotifListeners: Set<String>
    ): AppRiskProfile {
        val label = packageManager.getApplicationLabel(pkg.applicationInfo).toString()
        val isSystem = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // Installer trust classification
        val installerPkg = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(pkg.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(pkg.packageName)
            }
        } catch (e: Exception) {
            null
        }

        val trust = when {
            isSystem -> InstallerTrust.PREINSTALLED
            installerPkg == "com.android.vending" -> InstallerTrust.VERIFIED
            installerPkg == "com.google.android.packageinstaller" -> InstallerTrust.SYSTEM_INSTALL
            installerPkg == null -> InstallerTrust.UNKNOWN
            else -> InstallerTrust.SIDELOADED
        }

        // Permissions extraction
        val declared = pkg.requestedPermissions?.toList() ?: emptyList()
        val granted = mutableListOf<String>()
        val permDetails = mutableListOf<PermissionDetail>()

        if (pkg.requestedPermissions != null && pkg.requestedPermissionsFlags != null) {
            for (i in pkg.requestedPermissions.indices) {
                val permName = pkg.requestedPermissions[i]
                val flags = pkg.requestedPermissionsFlags[i]
                val isGranted = (flags and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                if (isGranted) {
                    granted.add(permName)
                }

                val protLevel = try {
                    packageManager.getPermissionInfo(permName, 0).protectionLevel
                } catch (e: Exception) {
                    0
                }

                permDetails.add(
                    PermissionDetail(
                        permission = permName,
                        category = mapPermissionToCategory(permName),
                        isGranted = isGranted,
                        isDeclared = true,
                        protectionLevel = protLevel
                    )
                )
            }
        }

        // Special access extraction
        val specialAccess = checkSpecialAccess(pkg, enabledAccessibilities, activeAdmins, enabledNotifListeners)

        // Usage stats
        val lastUsedTime = usageMap[pkg.packageName] ?: pkg.lastUpdateTime
        val now = System.currentTimeMillis()
        val daysUnused = if (lastUsedTime > 0) ((now - lastUsedTime) / (1000L * 60 * 60 * 24)).coerceAtLeast(0) else 0L

        // Permission creep diff detection
        val creepResult = checkPermissionDiff(pkg.packageName, declared)

        // Base profile scoring
        val baseProfile = RiskScoringEngine.scoreApp(
            label = label,
            packageName = pkg.packageName,
            targetSdkVersion = pkg.applicationInfo.targetSdkVersion,
            installerTrust = trust,
            declaredPermissions = declared,
            grantedPermissions = granted,
            specialAccess = specialAccess,
            daysUnused = daysUnused,
            permissionCreepFlag = creepResult.first,
            addedPermissions = creepResult.second
        )

        // Remote access check
        val installedWithin48h = (now - pkg.firstInstallTime) < 48 * 60 * 60 * 1000
        val isRemoteAccess = pkg.packageName in REMOTE_ACCESS_PACKAGES ||
                (specialAccess.accessibilityEnabled == SpecialAccessState.ENABLED &&
                        specialAccess.overlayState == SpecialAccessState.GRANTED &&
                        granted.contains("android.permission.INTERNET") &&
                        installedWithin48h)

        val appIcon = try {
            packageManager.getApplicationIcon(pkg.applicationInfo)
        } catch (e: Exception) {
            null
        }

        return baseProfile.copy(
            icon = appIcon,
            versionName = pkg.versionName ?: "1.0",
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) pkg.longVersionCode else pkg.versionCode.toLong(),
            firstInstallTime = pkg.firstInstallTime,
            lastUpdateTime = pkg.lastUpdateTime,
            installerPackageName = installerPkg,
            isSystemApp = isSystem,
            permissionDetails = permDetails,
            isRemoteAccessApp = isRemoteAccess
        )
    }

    private fun checkSpecialAccess(
        pkg: PackageInfo,
        enabledAccessibilities: Set<String>,
        activeAdmins: Set<String>,
        enabledNotifListeners: Set<String>
    ): SpecialAccessInfo {
        val packageName = pkg.packageName
        val uid = pkg.applicationInfo.uid

        // Accessibility
        val accessibilityState = if (enabledAccessibilities.contains(packageName)) {
            SpecialAccessState.ENABLED
        } else {
            SpecialAccessState.NOT_ENABLED
        }

        // Device Admin
        val adminState = if (activeAdmins.contains(packageName)) {
            SpecialAccessState.ACTIVE
        } else {
            SpecialAccessState.NOT_ACTIVE
        }

        // Notification Listener
        val notifState = if (enabledNotifListeners.contains(packageName)) {
            SpecialAccessState.ENABLED
        } else {
            SpecialAccessState.NOT_ENABLED
        }

        // Overlay (SYSTEM_ALERT_WINDOW)
        val declaredOverlay = pkg.requestedPermissions?.contains("android.permission.SYSTEM_ALERT_WINDOW") == true
        val overlayState = if (!declaredOverlay) {
            SpecialAccessState.NOT_GRANTED
        } else {
            try {
                val mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW,
                    uid,
                    packageName
                )
                when (mode) {
                    AppOpsManager.MODE_ALLOWED -> SpecialAccessState.GRANTED
                    AppOpsManager.MODE_IGNORED -> SpecialAccessState.NOT_GRANTED
                    AppOpsManager.MODE_ERRORED -> SpecialAccessState.DECLARED_ONLY
                    else -> SpecialAccessState.UNABLE_TO_CHECK
                }
            } catch (e: Exception) {
                SpecialAccessState.UNABLE_TO_CHECK
            }
        }

        // Request Install Packages
        val declaredInstall = pkg.requestedPermissions?.contains("android.permission.REQUEST_INSTALL_PACKAGES") == true
        val installState = if (!declaredInstall) {
            SpecialAccessState.NOT_GRANTED
        } else {
            try {
                val mode = appOpsManager.checkOpNoThrow(
                    "android:request_install_packages",
                    uid,
                    packageName
                )
                when (mode) {
                    AppOpsManager.MODE_ALLOWED -> SpecialAccessState.GRANTED
                    AppOpsManager.MODE_IGNORED -> SpecialAccessState.NOT_GRANTED
                    else -> SpecialAccessState.DECLARED_ONLY
                }
            } catch (e: Exception) {
                SpecialAccessState.UNABLE_TO_CHECK
            }
        }

        return SpecialAccessInfo(
            accessibilityEnabled = accessibilityState,
            deviceAdminActive = adminState,
            notificationListenerEnabled = notifState,
            overlayState = overlayState,
            installPackagesState = installState
        )
    }

    private fun getUsageStatsMap(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        if (usageStatsManager == null) return map

        try {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_MONTHLY,
                thirtyDaysAgo,
                now
            )
            if (stats != null) {
                for (stat in stats) {
                    val prev = map[stat.packageName] ?: 0L
                    if (stat.lastTimeUsed > prev) {
                        map[stat.packageName] = stat.lastTimeUsed
                    }
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("AppInventoryCollector", "Usage stats access denied: ${e.message}")
            }
        }
        return map
    }

    private fun getEnabledAccessibilityPackages(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            for (service in list) {
                val comp = ComponentName.unflattenFromString(service.id)
                comp?.packageName?.let { set.add(it) }
            }
        } catch (e: Exception) {
            // honest fallthrough
        }
        return set
    }

    private fun getActiveAdminPackages(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val admins = devicePolicyManager.activeAdmins
            if (admins != null) {
                for (admin in admins) {
                    set.add(admin.packageName)
                }
            }
        } catch (e: Exception) {
            // honest fallthrough
        }
        return set
    }

    private fun getNotificationListenerPackages(): Set<String> {
        val set = mutableSetOf<String>()
        try {
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    val comp = ComponentName.unflattenFromString(name)
                    comp?.packageName?.let { set.add(it) }
                }
            }
        } catch (e: Exception) {
            // honest fallthrough
        }
        return set
    }

    private fun checkPermissionDiff(packageName: String, currentDeclared: List<String>): Pair<Boolean, List<String>> {
        val key = "perms_$packageName"
        val previousStored = prefs.getStringSet(key, null)
        val currentSet = currentDeclared.toSet()

        if (previousStored == null) {
            // First scan ever: store snapshot, no diff shown
            prefs.edit().putStringSet(key, currentSet).apply()
            return Pair(false, emptyList())
        }

        val added = currentSet - previousStored
        prefs.edit().putStringSet(key, currentSet).apply()

        return if (added.isNotEmpty()) {
            Pair(true, added.toList())
        } else {
            Pair(false, emptyList())
        }
    }
}
