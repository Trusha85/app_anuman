package com.appanuman.data.collector

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.appanuman.data.mapper.PermissionCategoryMapper
import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.CategoryResult
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.PermissionCategory
import com.appanuman.data.model.PermissionInfo
import com.appanuman.data.model.PermissionState
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.SpecialAccessResult
import com.appanuman.data.narrative.ThreatNarrativeEngine
import com.appanuman.data.scoring.RiskScoringEngine

class AppInventoryCollector(
    private val specialAccessCollector: SpecialAccessCollector,
    private val usageStatsCollector: UsageStatsCollector,
    private val permissionSnapshotManager: PermissionSnapshotManager,
    private val appContext: Context? = null
) {

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
            return PermissionCategoryMapper.getCategory(perm)
        }
    }

    constructor(context: Context) : this(
        specialAccessCollector = SpecialAccessCollector(context),
        usageStatsCollector = UsageStatsCollector(context),
        permissionSnapshotManager = PermissionSnapshotManager(context),
        appContext = context.applicationContext
    )

    fun scanPackageByName(packageName: String, ctx: Context? = appContext): AppRiskProfile? {
        val currentContext = ctx ?: appContext ?: return null
        val pm = currentContext.packageManager
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA
        return try {
            val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, flags)
            }
            val appInfo = pkgInfo.applicationInfo ?: return null
            val usageMap = usageStatsCollector.queryLastTimeUsedMap()
            buildAppProfile(pm, pkgInfo, appInfo, usageMap)
        } catch (e: Exception) {
            null
        }
    }

    fun collectAllApps(includeSystem: Boolean = false): List<AppRiskProfile> {
        val currentContext = appContext ?: return emptyList()
        return collectAllApps(currentContext, includeSystem)
    }

    fun collectAllApps(context: Context, includeSystem: Boolean): List<AppRiskProfile> {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS or PackageManager.GET_META_DATA

        val installedPackages: List<PackageInfo> = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        val usageMap = usageStatsCollector.queryLastTimeUsedMap()
        val resultList = mutableListOf<AppRiskProfile>()

        for (pkgInfo in installedPackages) {
            val appInfo = pkgInfo.applicationInfo ?: continue
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            // Skip system apps unless includeSystem is true
            if (isSystemApp && !includeSystem) {
                continue
            }

            try {
                val profile = buildAppProfile(pm, pkgInfo, appInfo, usageMap)
                resultList.add(profile)
            } catch (e: Exception) {
                // Defensive: never crash scanning on a single malformed app
            }
        }

        return resultList
    }

    private fun buildAppProfile(
        pm: PackageManager,
        pkgInfo: PackageInfo,
        appInfo: ApplicationInfo,
        usageMap: Map<String, Long>
    ): AppRiskProfile {
        val packageName = pkgInfo.packageName
        val label = pm.getApplicationLabel(appInfo).toString()
        val versionName = pkgInfo.versionName ?: "Unknown"
        val firstInstallTime = pkgInfo.firstInstallTime
        val lastUpdateTime = pkgInfo.lastUpdateTime
        val targetSdkVersion = appInfo.targetSdkVersion

        // Installer trust classification
        val installerPackageName = getInstallerPackage(pm, packageName)
        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val installerTrust = when {
            isSystem -> InstallerTrust.PREINSTALLED
            installerPackageName == "com.android.vending" -> InstallerTrust.VERIFIED
            installerPackageName == "com.google.android.packageinstaller" -> InstallerTrust.SYSTEM_INSTALL
            installerPackageName.isNullOrEmpty() -> InstallerTrust.UNKNOWN
            else -> InstallerTrust.SIDELOADED
        }

        // Permissions parsing
        val declaredPermissions = mutableListOf<PermissionInfo>()
        val grantedPermissions = mutableListOf<PermissionInfo>()
        val requestedPerms = pkgInfo.requestedPermissions ?: emptyArray()
        val requestedFlags = pkgInfo.requestedPermissionsFlags ?: IntArray(0)

        for (i in requestedPerms.indices) {
            val permName = requestedPerms[i]
            val category = PermissionCategoryMapper.getCategory(permName)

            val protectionLevel = try {
                pm.getPermissionInfo(permName, 0).protectionLevel
            } catch (e: Exception) {
                0
            }

            val isGranted = if (i < requestedFlags.size) {
                (requestedFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                false
            }

            val state = if (isGranted) PermissionState.GRANTED else PermissionState.DECLARED_ONLY
            val info = PermissionInfo(
                permissionName = permName,
                category = category,
                state = state,
                protectionLevel = protectionLevel
            )

            declaredPermissions.add(info)
            if (isGranted) {
                grantedPermissions.add(info)
            }
        }

        // 9 Category map
        val allPermissions = declaredPermissions
        val categoryMap = PermissionCategory.values().associateWith { cat ->
            val permsInCat = allPermissions.filter { it.category == cat }
            val catState = when {
                permsInCat.any { it.state == PermissionState.GRANTED } -> PermissionState.GRANTED
                permsInCat.isNotEmpty() -> PermissionState.DECLARED_ONLY
                else -> PermissionState.NOT_REQUESTED
            }
            CategoryResult(
                category = cat,
                state = catState,
                permissions = permsInCat
            )
        }

        // Special Access
        val declaredPermNames = requestedPerms.toList()
        val specialAccess = specialAccessCollector.collectSpecialAccess(
            packageName = packageName,
            uid = appInfo.uid,
            declaredPermissions = declaredPermNames
        )

        // Usage & Dormancy
        val daysUnused = usageStatsCollector.calculateDaysUnused(packageName, usageMap, firstInstallTime)
        val hasDangerousPermGranted = grantedPermissions.any {
            it.category == PermissionCategory.SMS ||
                    it.category == PermissionCategory.CONTACTS_CALLS ||
                    it.category == PermissionCategory.CAMERA_MIC ||
                    it.category == PermissionCategory.LOCATION
        }
        val isDormant = (daysUnused != null && daysUnused > 30 && hasDangerousPermGranted)

        // Permission creep check
        val (creepFlag, addedPerms) = permissionSnapshotManager.checkPermissionCreepAndSave(
            packageName,
            declaredPermNames
        )

        // Remote access app check
        val now = System.currentTimeMillis()
        val installedWithin48h = (now - firstInstallTime) < (48L * 60 * 60 * 1000)
        val overlayActive = specialAccess.overlayState == PermissionState.GRANTED ||
                specialAccess.overlayState == PermissionState.DECLARED_ONLY
        val isRemoteAccess = REMOTE_ACCESS_PACKAGES.contains(packageName) ||
                (specialAccess.accessibilityEnabled == PermissionState.GRANTED &&
                        overlayActive &&
                        grantedPermissions.any { it.permissionName == "android.permission.INTERNET" } &&
                        installedWithin48h)

        // Context mismatch
        val contextMismatch = RiskScoringEngine.checkContextMismatch(label, grantedPermissions)

        // Icon conversion
        val iconBitmap = try {
            val drawable = pm.getApplicationIcon(packageName)
            drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }

        // Build temporary profile to run scoring engine
        val draftProfile = AppRiskProfile(
            label = label,
            packageName = packageName,
            versionName = versionName,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            installerPackageName = installerPackageName,
            targetSdkVersion = targetSdkVersion,
            installerTrust = installerTrust,
            declaredPermissions = declaredPermissions,
            grantedPermissions = grantedPermissions,
            categoryMap = categoryMap,
            specialAccess = specialAccess,
            riskScore = 0,
            riskBand = RiskBand.LOW,
            scoringFindings = emptyList(),
            triggeredChains = emptyList(),
            threatNarratives = emptyList(),
            contextMismatch = contextMismatch,
            permissionCreepFlag = creepFlag,
            addedPermissions = addedPerms,
            daysUnused = daysUnused,
            isDormant = isDormant,
            isRemoteAccessApp = isRemoteAccess,
            whyFlagged = emptyList(),
            icon = iconBitmap
        )

        // Score profile
        val (score, findings) = RiskScoringEngine.score(draftProfile)
        val riskBand = RiskScoringEngine.getRiskBand(score)
        val triggeredChains = RiskScoringEngine.detectAttackChains(draftProfile)
        val threatNarratives = ThreatNarrativeEngine.getNarrativesForChains(
            triggeredChains.filter { it.matched }.map { it.chainId }
        )
        val whyFlagged = RiskScoringEngine.generateWhyFlagged(findings)

        return draftProfile.copy(
            riskScore = score,
            riskBand = riskBand,
            scoringFindings = findings,
            triggeredChains = triggeredChains,
            threatNarratives = threatNarratives,
            whyFlagged = whyFlagged
        )
    }

    private fun getInstallerPackage(pm: PackageManager, packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                return drawable.bitmap
            }
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 72
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 72
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
