package com.appanuman.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

enum class RiskBand {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class SpecialAccessState {
    ENABLED,
    NOT_ENABLED,
    ACTIVE,
    NOT_ACTIVE,
    GRANTED,
    NOT_GRANTED,
    DECLARED_ONLY,
    UNABLE_TO_CHECK,
    UNKNOWN
}

enum class InstallerTrust {
    VERIFIED,        // com.android.vending (Play Store)
    SYSTEM_INSTALL,  // com.google.android.packageinstaller
    UNKNOWN,         // null
    SIDELOADED,      // anything else
    PREINSTALLED     // FLAG_SYSTEM
}

enum class PermissionCategory {
    SPECIAL_ACCESS,
    SMS,
    CONTACTS_CALLS,
    CAMERA_MIC,
    LOCATION,
    PHONE_IDENTITY,
    STORAGE,
    BACKGROUND,
    NETWORK
}

enum class UserMode {
    SIMPLE,
    DETAILED,
    GRANDPARENT
}

enum class Language {
    EN,
    HI
}

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

data class ThreatNarrative(
    val chainId: String,
    val title_en: String,
    val title_hi: String,
    val body_en: String,
    val body_hi: String,
    val analogy_en: String,
    val analogy_hi: String,
    val mitre_ref: String,
    val severity: String
)

data class PermissionDetail(
    val permission: String,
    val category: PermissionCategory,
    val isGranted: Boolean,
    val isDeclared: Boolean,
    val protectionLevel: Int = 0
)

data class AppRiskProfile(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null,
    val iconBitmap: ImageBitmap? = null,
    val versionName: String = "1.0",
    val versionCode: Long = 1L,
    val firstInstallTime: Long = 0L,
    val lastUpdateTime: Long = 0L,
    val installerPackageName: String? = null,
    val installerTrust: InstallerTrust = InstallerTrust.UNKNOWN,
    val targetSdkVersion: Int = 34,
    val isSystemApp: Boolean = false,
    val declaredPermissions: List<String> = emptyList(),
    val grantedPermissions: List<String> = emptyList(),
    val permissionDetails: List<PermissionDetail> = emptyList(),
    val specialAccess: SpecialAccessInfo = SpecialAccessInfo(),
    val daysUnused: Long = 0L,
    val isDormant: Boolean = false,
    val permissionCreepFlag: Boolean = false,
    val addedPermissions: List<String> = emptyList(),
    val score: Int = 0,
    val riskBand: RiskBand = RiskBand.LOW,
    val waterfall: List<ScoringFinding> = emptyList(),
    val threatNarratives: List<ThreatNarrative> = emptyList(),
    val contextMismatch: Boolean = false,
    val mismatchSeverity: String? = null,
    val isRemoteAccessApp: Boolean = false
)
