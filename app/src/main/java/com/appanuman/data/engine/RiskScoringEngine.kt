package com.appanuman.data.engine

import com.appanuman.data.model.*
import kotlin.math.min

object RiskScoringEngine {

    val CONTEXT_MISMATCH_KEYWORDS = listOf(
        "torch", "flashlight", "calculator", "wallpaper", "clock", "lantern",
        "battery", "compass", "ruler", "magnifier", "qr", "barcode", "scanner"
    )

    fun scoreApp(
        label: String,
        packageName: String,
        targetSdkVersion: Int,
        installerTrust: InstallerTrust,
        declaredPermissions: List<String>,
        grantedPermissions: List<String>,
        specialAccess: SpecialAccessInfo,
        daysUnused: Long,
        permissionCreepFlag: Boolean,
        addedPermissions: List<String>
    ): AppRiskProfile {
        val waterfall = mutableListOf<ScoringFinding>()
        val narratives = mutableListOf<ThreatNarrative>()

        // Helper indicators
        val accessibilityEnabled = specialAccess.accessibilityEnabled == SpecialAccessState.ENABLED ||
                specialAccess.accessibilityEnabled == SpecialAccessState.GRANTED
        val notificationListenerEnabled = specialAccess.notificationListenerEnabled == SpecialAccessState.ENABLED ||
                specialAccess.notificationListenerEnabled == SpecialAccessState.GRANTED
        val deviceAdminActive = specialAccess.deviceAdminActive == SpecialAccessState.ACTIVE ||
                specialAccess.deviceAdminActive == SpecialAccessState.GRANTED
        val overlayDeclaredOrGranted = specialAccess.overlayState == SpecialAccessState.GRANTED ||
                specialAccess.overlayState == SpecialAccessState.DECLARED_ONLY ||
                declaredPermissions.contains("android.permission.SYSTEM_ALERT_WINDOW")

        val smsAnyGranted = grantedPermissions.any {
            it.contains("SMS", ignoreCase = true)
        }
        val backgroundLocationGranted = grantedPermissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION")
        val broadStorageGranted = grantedPermissions.any {
            it == "android.permission.MANAGE_EXTERNAL_STORAGE" ||
                    it == "android.permission.READ_EXTERNAL_STORAGE" ||
                    it == "android.permission.READ_MEDIA_IMAGES" ||
                    it == "android.permission.READ_MEDIA_VIDEO"
        }
        val contactsOrCallLogGranted = grantedPermissions.any {
            it.contains("CONTACTS", ignoreCase = true) || it.contains("CALL_LOG", ignoreCase = true)
        }
        val cameraOrMicGranted = grantedPermissions.any {
            it.contains("CAMERA", ignoreCase = true) || it.contains("RECORD_AUDIO", ignoreCase = true)
        }
        val bootPersistenceGranted = grantedPermissions.contains("android.permission.RECEIVE_BOOT_COMPLETED") ||
                declaredPermissions.contains("android.permission.RECEIVE_BOOT_COMPLETED")
        val internetGranted = grantedPermissions.contains("android.permission.INTERNET") ||
                declaredPermissions.contains("android.permission.INTERNET")

        // BASE WEIGHTS
        if (accessibilityEnabled) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_ACCESSIBILITY",
                    label_en = "Accessibility service enabled",
                    label_hi = "एक्सेसिबिलिटी सेवा सक्रिय है",
                    points = 25,
                    evidence = "App has active accessibility service"
                )
            )
        }
        if (notificationListenerEnabled) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_NOTIF_LISTENER",
                    label_en = "Notification listener enabled",
                    label_hi = "नोटिफिकेशन लिसनर सक्रिय है",
                    points = 20,
                    evidence = "App can read all incoming notifications"
                )
            )
        }
        if (deviceAdminActive) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_DEVICE_ADMIN",
                    label_en = "Device administrator active",
                    label_hi = "डिवाइस एडमिन सक्रिय है",
                    points = 15,
                    evidence = "App has device administration privileges"
                )
            )
        }
        if (overlayDeclaredOrGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_OVERLAY",
                    label_en = "Overlay access declared or granted",
                    label_hi = "ओवरले अनुमति घोषित या प्रदान की गई",
                    points = 15,
                    evidence = "App can draw over other apps"
                )
            )
        }
        if (smsAnyGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_SMS",
                    label_en = "SMS permissions granted",
                    label_hi = "SMS अनुमतियां प्रदान की गईं",
                    points = 10,
                    evidence = "App can read/receive sensitive SMS"
                )
            )
        }
        if (backgroundLocationGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_BG_LOCATION",
                    label_en = "Background location granted",
                    label_hi = "पृष्ठभूमि स्थान अनुमति प्रदान की गई",
                    points = 8,
                    evidence = "App tracks location even when closed"
                )
            )
        }
        if (broadStorageGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_STORAGE",
                    label_en = "Broad storage access granted",
                    label_hi = "व्यापक स्टोरेज अनुमति प्रदान की गई",
                    points = 8,
                    evidence = "App can read user photos and files"
                )
            )
        }
        if (contactsOrCallLogGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_CONTACTS_CALLLOG",
                    label_en = "Contacts or call log granted",
                    label_hi = "संपर्क या कॉल लॉग अनुमति दी गई",
                    points = 6,
                    evidence = "App has access to contact book / call logs"
                )
            )
        }
        if (cameraOrMicGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_CAMERA_MIC",
                    label_en = "Camera or microphone granted",
                    label_hi = "कैमरा या माइक्रोफोन अनुमति दी गई",
                    points = 5,
                    evidence = "App can record audio or capture images"
                )
            )
        }
        if (bootPersistenceGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_BOOT",
                    label_en = "Boot persistence declared/granted",
                    label_hi = "बूट पर्सिस्टेंस अनुमति",
                    points = 5,
                    evidence = "App starts automatically on device boot"
                )
            )
        }
        if (internetGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_INTERNET",
                    label_en = "Internet access available",
                    label_hi = "इंटरनेट पहुंच उपलब्ध",
                    points = 2,
                    evidence = "App can transmit data over the network"
                )
            )
        }
        if (targetSdkVersion < 28) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_OLD_TARGET_SDK",
                    label_en = "Old target SDK (<28)",
                    label_hi = "पुराना टारगेट SDK (<28)",
                    points = 5,
                    evidence = "Target SDK $targetSdkVersion bypasses modern runtime protections"
                )
            )
        }
        if (installerTrust == InstallerTrust.SIDELOADED) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_SIDELOADED",
                    label_en = "Sideloaded from unknown source",
                    label_hi = "अज्ञात स्रोत से साइडलोड किया गया",
                    points = 5,
                    evidence = "App was not installed from official Play Store"
                )
            )
        }
        if (permissionCreepFlag && addedPermissions.isNotEmpty()) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "BASE_PERM_CREEP",
                    label_en = "New sensitive permissions added in update",
                    label_hi = "अपडेट में नई संवेदनशील अनुमतियां जोड़ी गईं",
                    points = 8,
                    evidence = "Added: ${addedPermissions.joinToString()}"
                )
            )
        }

        // ATTACK CHAINS
        if (smsAnyGranted && internetGranted && bootPersistenceGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "CHAIN_OTP_STEALER",
                    label_en = "Attack Chain: OTP Stealer pattern",
                    label_hi = "हमला श्रृंखला: OTP चोरी पैटर्न",
                    points = 15,
                    evidence = "SMS + Internet + Boot persistence detected"
                )
            )
            narratives.add(ThreatNarrativeEngine.OTP_STEALER)
        }

        if (accessibilityEnabled && overlayDeclaredOrGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "CHAIN_OVERLAY_TRAP",
                    label_en = "Attack Chain: Overlay Trap pattern",
                    label_hi = "हमला श्रृंखला: ओवरले ट्रैप पैटर्न",
                    points = 15,
                    evidence = "Accessibility + Screen Overlay detected"
                )
            )
            narratives.add(ThreatNarrativeEngine.OVERLAY_TRAP)
        }

        if (notificationListenerEnabled && accessibilityEnabled && internetGranted) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "CHAIN_SILENT_SPY",
                    label_en = "Attack Chain: Silent Background Spy",
                    label_hi = "हमला श्रृंखला: शांत पृष्ठभूमि जासूस",
                    points = 15,
                    evidence = "Notification Listener + Accessibility + Internet"
                )
            )
            narratives.add(ThreatNarrativeEngine.SILENT_SPY)
        }

        if (deviceAdminActive && internetGranted &&
            (installerTrust == InstallerTrust.SIDELOADED || installerTrust == InstallerTrust.UNKNOWN)
        ) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "CHAIN_PERSIST_ADMIN",
                    label_en = "Attack Chain: Persistent Device Admin",
                    label_hi = "हमला श्रृंखला: लगातार डिवाइस एडमिन",
                    points = 15,
                    evidence = "Device Admin + Internet on non-store app"
                )
            )
            narratives.add(ThreatNarrativeEngine.PERSIST_ADMIN)
        }

        val hasDangerousPerm = smsAnyGranted || broadStorageGranted || cameraOrMicGranted || backgroundLocationGranted
        val isDormant = daysUnused > 30 && (hasDangerousPerm || accessibilityEnabled || overlayDeclaredOrGranted)
        if (daysUnused > 30 && (smsAnyGranted || accessibilityEnabled || overlayDeclaredOrGranted)) {
            waterfall.add(
                ScoringFinding(
                    ruleId = "CHAIN_DORMANT_THREAT",
                    label_en = "Attack Chain: Dormant Threat with sensitive access",
                    label_hi = "हमला श्रृंखला: संवेदनशील पहुंच वाला निष्क्रिय खतरा",
                    points = 10,
                    evidence = "Unused for $daysUnused days while retaining high privileges"
                )
            )
            narratives.add(ThreatNarrativeEngine.DORMANT_THREAT)
        }

        // Context mismatch rule
        val lowerLabel = label.lowercase()
        val keywordMatch = CONTEXT_MISMATCH_KEYWORDS.any { lowerLabel.contains(it) }
        val dangerousMismatch = smsAnyGranted || contactsOrCallLogGranted || cameraOrMicGranted || backgroundLocationGranted
        val contextMismatch = keywordMatch && dangerousMismatch
        val mismatchSeverity = if (contextMismatch) "HIGH" else null

        // Calculate score
        val rawScore = waterfall.sumOf { it.points }
        val finalScore = min(100, rawScore)

        val riskBand = when {
            finalScore >= 75 -> RiskBand.CRITICAL
            finalScore >= 50 -> RiskBand.HIGH
            finalScore >= 25 -> RiskBand.MEDIUM
            else -> RiskBand.LOW
        }

        return AppRiskProfile(
            packageName = packageName,
            label = label,
            targetSdkVersion = targetSdkVersion,
            installerTrust = installerTrust,
            declaredPermissions = declaredPermissions,
            grantedPermissions = grantedPermissions,
            specialAccess = specialAccess,
            daysUnused = daysUnused,
            isDormant = isDormant,
            permissionCreepFlag = permissionCreepFlag,
            addedPermissions = addedPermissions,
            score = finalScore,
            riskBand = riskBand,
            waterfall = waterfall,
            threatNarratives = narratives,
            contextMismatch = contextMismatch,
            mismatchSeverity = mismatchSeverity
        )
    }

    fun calculateDeviceRisk(apps: List<AppRiskProfile>, panicTriggers: Int = 0): Pair<Int, String> {
        val criticalCount = apps.count { it.riskBand == RiskBand.CRITICAL }
        val highCount = apps.count { it.riskBand == RiskBand.HIGH }
        val mediumCount = apps.count { it.riskBand == RiskBand.MEDIUM }
        val dormantCount = apps.count { it.isDormant }

        val rawScore = (criticalCount * 20) + (highCount * 10) + (mediumCount * 3) +
                (dormantCount * 5) + (panicTriggers * 25)
        val score = min(100, rawScore)

        val grade = when {
            score <= 20 -> "A"
            score <= 40 -> "B"
            score <= 60 -> "C"
            score <= 80 -> "D"
            else -> "F"
        }

        return Pair(score, grade)
    }
}
