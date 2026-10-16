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
    val accessibilityEnabled: PermissionState,
    val deviceAdminActive: PermissionState,
    val notificationListenerEnabled: PermissionState,
    val overlayState: PermissionState,
    val installPackagesState: PermissionState,
    val enabledAccessibilityServices: List<String>,
    val activeDeviceAdmins: List<String>
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
    val chainId: AttackChainId,
    val title_en: String,
    val title_hi: String,
    val body_en: String,
    val body_hi: String,
    val analogy_en: String,
    val analogy_hi: String,
    val mitre_ref: String,
    val severity: RiskBand
)

data class AppRiskProfile(
    val label: String,
    val packageName: String,
    val versionName: String,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val installerPackageName: String?,
    val targetSdkVersion: Int,
    val installerTrust: InstallerTrust,
    val declaredPermissions: List<PermissionInfo>,
    val grantedPermissions: List<PermissionInfo>,
    val categoryMap: Map<PermissionCategory, CategoryResult>,
    val specialAccess: SpecialAccessResult,
    val riskScore: Int,
    val riskBand: RiskBand,
    val scoringFindings: List<ScoringFinding>,
    val triggeredChains: List<AttackChain>,
    val threatNarratives: List<ThreatNarrative>,
    val contextMismatch: Boolean,
    val permissionCreepFlag: Boolean,
    val addedPermissions: List<String>,
    val daysUnused: Int?,
    val isDormant: Boolean,
    val isRemoteAccessApp: Boolean,
    val whyFlagged: List<String>,
    val icon: Bitmap?
)

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
