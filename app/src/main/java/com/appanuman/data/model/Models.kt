package com.appanuman.data.model

import android.graphics.Bitmap

enum class InstallerTrust {
    VERIFIED, SYSTEM_INSTALL, UNKNOWN, SIDELOADED, PREINSTALLED
}

enum class RiskBand {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class PermissionCategory {
    SPECIAL_ACCESS, SMS, CONTACTS_CALLS, CAMERA_MIC,
    LOCATION, PHONE_IDENTITY, STORAGE, BACKGROUND, NETWORK
}

enum class PermissionState {
    GRANTED, DECLARED_ONLY, NOT_REQUESTED, UNABLE_TO_CHECK
}

enum class SpecialAccessState {
    ENABLED, NOT_ENABLED, ACTIVE, NOT_ACTIVE, GRANTED, NOT_GRANTED, DECLARED_ONLY, UNABLE_TO_CHECK, UNKNOWN
}

data class PermissionInfo(
    val permissionName: String,
    val category: PermissionCategory,
    val state: PermissionState,
    val protectionLevel: Int
)

data class CategoryResult(
    val category: PermissionCategory,
    val state: PermissionState,
    val permissions: List<PermissionInfo>
)

data class SpecialAccessResult(
    val accessibilityEnabled: PermissionState = PermissionState.NOT_REQUESTED,
    val deviceAdminActive: PermissionState = PermissionState.NOT_REQUESTED,
    val notificationListenerEnabled: PermissionState = PermissionState.NOT_REQUESTED,
    val overlayState: PermissionState = PermissionState.NOT_REQUESTED,
    val installPackagesState: PermissionState = PermissionState.NOT_REQUESTED,
    val enabledAccessibilityServices: List<String> = emptyList(),
    val activeDeviceAdmins: List<String> = emptyList()
)

data class SpecialAccessInfo(
    val accessibilityEnabled: SpecialAccessState = SpecialAccessState.NOT_ENABLED,
    val deviceAdminActive: SpecialAccessState = SpecialAccessState.NOT_ACTIVE,
    val notificationListenerEnabled: SpecialAccessState = SpecialAccessState.NOT_ENABLED,
    val overlayState: SpecialAccessState = SpecialAccessState.NOT_GRANTED,
    val installPackagesState: SpecialAccessState = SpecialAccessState.NOT_GRANTED
)

data class ScoringFinding(
    val ruleId: String,
    val label_en: String,
    val label_hi: String,
    val points: Int,
    val evidence: String
)

enum class AttackChainId {
    OTP_STEALER, OVERLAY_TRAP, SILENT_SPY,
    PERSIST_ADMIN, DORMANT_THREAT
}

data class AttackChain(
    val chainId: AttackChainId,
    val matched: Boolean,
    val evidence: List<String>
)

data class ThreatNarrative(
    val chainId: AttackChainId = AttackChainId.OTP_STEALER,
    val chainIdStr: String = chainId.name,
    val title_en: String,
    val title_hi: String,
    val body_en: String,
    val body_hi: String,
    val analogy_en: String,
    val analogy_hi: String,
    val mitre_ref: String,
    val severity: RiskBand = RiskBand.LOW
) {
    constructor(
        chainId: String,
        title_en: String,
        title_hi: String,
        body_en: String,
        body_hi: String,
        analogy_en: String,
        analogy_hi: String,
        mitre_ref: String,
        severity: String
    ) : this(
        chainId = try { AttackChainId.valueOf(chainId) } catch (e: Exception) { AttackChainId.OTP_STEALER },
        chainIdStr = chainId,
        title_en = title_en,
        title_hi = title_hi,
        body_en = body_en,
        body_hi = body_hi,
        analogy_en = analogy_en,
        analogy_hi = analogy_hi,
        mitre_ref = mitre_ref,
        severity = try { RiskBand.valueOf(severity) } catch (e: Exception) { RiskBand.LOW }
    )
}

data class AppRiskProfile(
    val label: String,
    val packageName: String,
    val versionName: String = "1.0.0",
    val versionCode: Long = 1L,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val installerPackageName: String? = null,
    val targetSdkVersion: Int = 34,
    val installerTrust: InstallerTrust = InstallerTrust.UNKNOWN,
    val declaredPermissions: List<PermissionInfo> = emptyList(),
    val grantedPermissions: List<PermissionInfo> = emptyList(),
    val categoryMap: Map<PermissionCategory, CategoryResult> = emptyMap(),
    val specialAccess: SpecialAccessResult = SpecialAccessResult(),
    val riskScore: Int = 0,
    val riskBand: RiskBand = RiskBand.LOW,
    val scoringFindings: List<ScoringFinding> = emptyList(),
    val triggeredChains: List<AttackChain> = emptyList(),
    val threatNarratives: List<ThreatNarrative> = emptyList(),
    val contextMismatch: Boolean = false,
    val mismatchSeverity: String? = null,
    val permissionCreepFlag: Boolean = false,
    val addedPermissions: List<String> = emptyList(),
    val daysUnused: Int? = 0,
    val isDormant: Boolean = false,
    val isRemoteAccessApp: Boolean = false,
    val whyFlagged: List<String> = emptyList(),
    val icon: Bitmap? = null
) {
    val score: Int get() = riskScore
    val waterfall: List<ScoringFinding> get() = scoringFindings

    constructor(
        packageName: String,
        label: String,
        firstInstallTime: Long = 0L,
        installerTrust: InstallerTrust = InstallerTrust.UNKNOWN,
        grantedPermissions: List<String> = emptyList(),
        specialAccess: SpecialAccessInfo = SpecialAccessInfo(),
        threatNarratives: List<ThreatNarrative> = emptyList()
    ) : this(
        label = label,
        packageName = packageName,
        firstInstallTime = firstInstallTime,
        installerTrust = installerTrust,
        grantedPermissions = grantedPermissions.map {
            PermissionInfo(
                permissionName = it,
                category = PermissionCategory.SPECIAL_ACCESS,
                state = PermissionState.GRANTED,
                protectionLevel = 1
            )
        },
        specialAccess = SpecialAccessResult(
            accessibilityEnabled = if (specialAccess.accessibilityEnabled == SpecialAccessState.ENABLED || specialAccess.accessibilityEnabled == SpecialAccessState.GRANTED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED,
            deviceAdminActive = if (specialAccess.deviceAdminActive == SpecialAccessState.ACTIVE || specialAccess.deviceAdminActive == SpecialAccessState.GRANTED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED,
            notificationListenerEnabled = if (specialAccess.notificationListenerEnabled == SpecialAccessState.ENABLED || specialAccess.notificationListenerEnabled == SpecialAccessState.GRANTED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED,
            overlayState = if (specialAccess.overlayState == SpecialAccessState.GRANTED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED,
            installPackagesState = if (specialAccess.installPackagesState == SpecialAccessState.GRANTED) PermissionState.GRANTED else PermissionState.NOT_REQUESTED
        ),
        threatNarratives = threatNarratives
    )
}

data class DeviceRiskSummary(
    val deviceScore: Int,
    val grade: String,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val dormantCount: Int,
    val sideloadedCount: Int,
    val scanDurationMs: Long,
    val scannedAppCount: Int,
    val lastScanTime: Long
)

enum class PanicVerdict {
    SAFE, WARNING, DANGER
}

data class PanicCheckResult(
    val remoteAccessAppsFound: List<String>,
    val recentSpecialAccessApps: List<String>,
    val recentSideloads: List<String>,
    val activeOverlayRisk: Boolean,
    val upiRiskApps: List<String>,
    val overallVerdict: PanicVerdict
)

enum class AppFilter {
    ALL, CRITICAL, HIGH, DORMANT, SIDELOADED, MISMATCH
}
