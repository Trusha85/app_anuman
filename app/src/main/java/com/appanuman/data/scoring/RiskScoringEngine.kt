package com.appanuman.data.scoring

import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.AttackChain
import com.appanuman.data.model.AttackChainId
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.PermissionCategory
import com.appanuman.data.model.PermissionState
import com.appanuman.data.model.RiskBand
import com.appanuman.data.model.ScoringFinding

object RiskScoringEngine {

    private val MISMATCH_KEYWORDS = listOf(
        "torch", "flashlight", "calculator", "wallpaper", "clock", "lantern",
        "battery", "compass", "ruler", "magnifier", "qr", "barcode", "scanner"
    )

    private val DANGEROUS_CATEGORIES = setOf(
        PermissionCategory.SMS,
        PermissionCategory.CONTACTS_CALLS,
        PermissionCategory.CAMERA_MIC,
        PermissionCategory.LOCATION
    )

    fun score(app: AppRiskProfile): Pair<Int, List<ScoringFinding>> {
        val findings = mutableListOf<ScoringFinding>()

        // Helper to check if a specific permission is granted
        fun isPermGranted(permName: String): Boolean {
            return app.grantedPermissions.any {
                it.permissionName.equals(permName, ignoreCase = true) && it.state == PermissionState.GRANTED
            }
        }

        // Helper to check if any permission in category is granted
        fun isCategoryGranted(category: PermissionCategory): Boolean {
            val catResult = app.categoryMap[category]
            if (catResult != null && catResult.state == PermissionState.GRANTED) {
                return true
            }
            return app.grantedPermissions.any {
                it.category == category && it.state == PermissionState.GRANTED
            }
        }

        // 1. accessibility_enabled: 25
        val accessibilityEnabled = app.specialAccess.accessibilityEnabled == PermissionState.GRANTED
        if (accessibilityEnabled) {
            findings.add(
                ScoringFinding(
                    ruleId = "accessibility_enabled",
                    label_en = "Accessibility Service Active",
                    label_hi = "एक्सेसिबिलिटी सेवा सक्रिय",
                    points = 25,
                    evidence = "App has active accessibility service enabled: ${app.specialAccess.enabledAccessibilityServices.joinToString()}"
                )
            )
        }

        // 2. notification_listener_enabled: 20
        val notificationListenerEnabled = app.specialAccess.notificationListenerEnabled == PermissionState.GRANTED
        if (notificationListenerEnabled) {
            findings.add(
                ScoringFinding(
                    ruleId = "notification_listener_enabled",
                    label_en = "Notification Listener Active",
                    label_hi = "सूचना श्रोता सक्रिय",
                    points = 20,
                    evidence = "App can inspect all incoming notifications and verification codes"
                )
            )
        }

        // 3. device_admin_active: 15
        val deviceAdminActive = app.specialAccess.deviceAdminActive == PermissionState.GRANTED
        if (deviceAdminActive) {
            findings.add(
                ScoringFinding(
                    ruleId = "device_admin_active",
                    label_en = "Device Administrator Active",
                    label_hi = "डिवाइस एडमिन सक्रिय",
                    points = 15,
                    evidence = "App holds device admin authority: ${app.specialAccess.activeDeviceAdmins.joinToString()}"
                )
            )
        }

        // 4. overlay_declared_or_granted: 15
        val overlayDeclaredOrGranted = app.specialAccess.overlayState == PermissionState.GRANTED ||
                app.specialAccess.overlayState == PermissionState.DECLARED_ONLY
        if (overlayDeclaredOrGranted) {
            val isGranted = app.specialAccess.overlayState == PermissionState.GRANTED
            findings.add(
                ScoringFinding(
                    ruleId = "overlay_declared_or_granted",
                    label_en = if (isGranted) "Overlay Permission Granted" else "Overlay Permission Declared",
                    label_hi = if (isGranted) "ओवरले अनुमति स्वीकृत" else "ओवरले अनुमति घोषित",
                    points = 15,
                    evidence = if (isGranted) "Can draw floating views over other applications" else "Declared in manifest (live state restricted)"
                )
            )
        }

        // 5. sms_any_granted: 10
        val smsAnyGranted = isCategoryGranted(PermissionCategory.SMS)
        if (smsAnyGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "sms_any_granted",
                    label_en = "SMS Access Granted",
                    label_hi = "SMS एक्सेस स्वीकृत",
                    points = 10,
                    evidence = "Can read, send, or receive SMS messages"
                )
            )
        }

        // 6. background_location_granted: 8
        val bgLocationGranted = isPermGranted("android.permission.ACCESS_BACKGROUND_LOCATION")
        if (bgLocationGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "background_location_granted",
                    label_en = "Background Location Granted",
                    label_hi = "पृष्ठभूमि स्थान स्वीकृत",
                    points = 8,
                    evidence = "Has ACCESS_BACKGROUND_LOCATION granted"
                )
            )
        }

        // 7. broad_storage_granted: 8
        val broadStorageGranted = isPermGranted("android.permission.MANAGE_EXTERNAL_STORAGE") ||
                isPermGranted("android.permission.READ_EXTERNAL_STORAGE") ||
                isPermGranted("android.permission.WRITE_EXTERNAL_STORAGE")
        if (broadStorageGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "broad_storage_granted",
                    label_en = "Broad Storage Access Granted",
                    label_hi = "व्यापक स्टोरेज एक्सेस स्वीकृत",
                    points = 8,
                    evidence = "Can access device files and external storage"
                )
            )
        }

        // 8. contacts_or_calllog_granted: 6
        val contactsOrCallLogGranted = isCategoryGranted(PermissionCategory.CONTACTS_CALLS)
        if (contactsOrCallLogGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "contacts_or_calllog_granted",
                    label_en = "Contacts or Call Log Granted",
                    label_hi = "संपर्क या कॉल लॉग स्वीकृत",
                    points = 6,
                    evidence = "Can read or modify contacts or call history"
                )
            )
        }

        // 9. camera_or_mic_granted: 5
        val cameraOrMicGranted = isCategoryGranted(PermissionCategory.CAMERA_MIC)
        if (cameraOrMicGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "camera_or_mic_granted",
                    label_en = "Camera or Microphone Granted",
                    label_hi = "कैमरा या माइक्रोफ़ोन स्वीकृत",
                    points = 5,
                    evidence = "Can access hardware camera or record audio"
                )
            )
        }

        // 10. boot_persistence_granted: 5
        val bootPersistenceGranted = isPermGranted("android.permission.RECEIVE_BOOT_COMPLETED")
        if (bootPersistenceGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "boot_persistence_granted",
                    label_en = "Boot Persistence Granted",
                    label_hi = "बूट पर शुरू होने की अनुमति स्वीकृत",
                    points = 5,
                    evidence = "Can start automatically upon device reboot"
                )
            )
        }

        // 11. internet_granted: 2
        val internetGranted = isPermGranted("android.permission.INTERNET")
        if (internetGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "internet_granted",
                    label_en = "Internet Access Granted",
                    label_hi = "इंटरनेट एक्सेस स्वीकृत",
                    points = 2,
                    evidence = "Can communicate over network"
                )
            )
        }

        // 12. targetSdkVersion < 28: 5
        val outdatedTargetSdk = app.targetSdkVersion in 1..27
        if (outdatedTargetSdk) {
            findings.add(
                ScoringFinding(
                    ruleId = "targetSdkVersion_below_28",
                    label_en = "Outdated Target SDK (< 28)",
                    label_hi = "पुराना टारगेट SDK (< 28)",
                    points = 5,
                    evidence = "Targets API ${app.targetSdkVersion}, bypassing modern privacy protections"
                )
            )
        }

        // 13. SIDELOADED installer: 5
        val isSideloaded = app.installerTrust == InstallerTrust.SIDELOADED
        if (isSideloaded) {
            findings.add(
                ScoringFinding(
                    ruleId = "sideloaded_installer",
                    label_en = "Sideloaded Application",
                    label_hi = "साइडलोडेड ऐप",
                    points = 5,
                    evidence = "Installed from outside official app store"
                )
            )
        }

        // 14. permissionCreepFlag = true: 8
        if (app.permissionCreepFlag) {
            findings.add(
                ScoringFinding(
                    ruleId = "permission_creep",
                    label_en = "Permission Creep Detected",
                    label_hi = "अनुमति क्रिप पहचानी गई",
                    points = 8,
                    evidence = "App update added new sensitive permissions: ${app.addedPermissions.joinToString()}"
                )
            )
        }

        // Attack Chain Bonuses (+15 each, dormant +10)
        // Chain 1: OTP_STEALER
        if (smsAnyGranted && internetGranted && bootPersistenceGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "CHAIN_OTP_STEALER",
                    label_en = "Attack Chain: OTP Stealer",
                    label_hi = "हमला श्रृंखला: OTP चोरी",
                    points = 15,
                    evidence = "SMS access + Internet transmission + Boot auto-start"
                )
            )
        }

        // Chain 2: OVERLAY_TRAP
        if (accessibilityEnabled && overlayDeclaredOrGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "CHAIN_OVERLAY_TRAP",
                    label_en = "Attack Chain: Overlay Trap",
                    label_hi = "हमला श्रृंखला: नकली स्क्रीन ओवरले",
                    points = 15,
                    evidence = "Accessibility service + Floating screen overlay"
                )
            )
        }

        // Chain 3: SILENT_SPY
        if (notificationListenerEnabled && accessibilityEnabled && internetGranted) {
            findings.add(
                ScoringFinding(
                    ruleId = "CHAIN_SILENT_SPY",
                    label_en = "Attack Chain: Silent Spy",
                    label_hi = "हमला श्रृंखला: शांत जासूसी",
                    points = 15,
                    evidence = "Notification intercept + Accessibility control + Internet"
                )
            )
        }

        // Chain 4: PERSIST_ADMIN
        if (deviceAdminActive && internetGranted &&
            (app.installerTrust == InstallerTrust.SIDELOADED || app.installerTrust == InstallerTrust.UNKNOWN)
        ) {
            findings.add(
                ScoringFinding(
                    ruleId = "CHAIN_PERSIST_ADMIN",
                    label_en = "Attack Chain: Persistent Admin",
                    label_hi = "हमला श्रृंखला: जिद्दी एडमिन",
                    points = 15,
                    evidence = "Device Administrator + Internet on non-store application"
                )
            )
        }

        // Chain 5: DORMANT_THREAT
        val isDormantApp = (app.daysUnused ?: 0) > 30
        if (isDormantApp && (smsAnyGranted || accessibilityEnabled || overlayDeclaredOrGranted)) {
            findings.add(
                ScoringFinding(
                    ruleId = "CHAIN_DORMANT_THREAT",
                    label_en = "Attack Chain: Dormant Threat",
                    label_hi = "हमला श्रृंखला: सुप्त खतरा",
                    points = 10,
                    evidence = "Unused for ${app.daysUnused} days with high-risk active privileges"
                )
            )
        }

        val rawTotal = findings.sumOf { it.points }
        val finalScore = minOf(100, rawTotal)

        // Adjust findings to precisely match final score waterfall if capped at 100
        val waterfallFindings = if (rawTotal <= 100) {
            findings
        } else {
            var accumulated = 0
            val cappedList = mutableListOf<ScoringFinding>()
            for (f in findings) {
                if (accumulated + f.points <= 100) {
                    cappedList.add(f)
                    accumulated += f.points
                } else {
                    val remaining = 100 - accumulated
                    if (remaining > 0) {
                        cappedList.add(f.copy(points = remaining))
                        accumulated += remaining
                    }
                    break
                }
            }
            cappedList
        }

        return Pair(finalScore, waterfallFindings)
    }

    fun getRiskBand(score: Int): RiskBand {
        return when {
            score >= 75 -> RiskBand.CRITICAL
            score >= 50 -> RiskBand.HIGH
            score >= 25 -> RiskBand.MEDIUM
            else -> RiskBand.LOW
        }
    }

    fun checkContextMismatch(label: String, grantedPermissions: List<com.appanuman.data.model.PermissionInfo>): Boolean {
        val labelLower = label.lowercase()
        val hasKeyword = MISMATCH_KEYWORDS.any { labelLower.contains(it) }
        if (!hasKeyword) return false

        return grantedPermissions.any {
            it.state == PermissionState.GRANTED && DANGEROUS_CATEGORIES.contains(it.category)
        }
    }

    fun detectAttackChains(
        app: AppRiskProfile
    ): List<AttackChain> {
        val chains = mutableListOf<AttackChain>()

        fun isPermGranted(permName: String): Boolean {
            return app.grantedPermissions.any {
                it.permissionName.equals(permName, ignoreCase = true) && it.state == PermissionState.GRANTED
            }
        }

        fun isCategoryGranted(cat: PermissionCategory): Boolean {
            val catResult = app.categoryMap[cat]
            if (catResult != null && catResult.state == PermissionState.GRANTED) return true
            return app.grantedPermissions.any { it.category == cat && it.state == PermissionState.GRANTED }
        }

        val accessibility = app.specialAccess.accessibilityEnabled == PermissionState.GRANTED
        val notificationListener = app.specialAccess.notificationListenerEnabled == PermissionState.GRANTED
        val deviceAdmin = app.specialAccess.deviceAdminActive == PermissionState.GRANTED
        val overlay = app.specialAccess.overlayState == PermissionState.GRANTED ||
                app.specialAccess.overlayState == PermissionState.DECLARED_ONLY
        val sms = isCategoryGranted(PermissionCategory.SMS)
        val internet = isPermGranted("android.permission.INTERNET")
        val boot = isPermGranted("android.permission.RECEIVE_BOOT_COMPLETED")
        val days = app.daysUnused ?: 0

        // 1. OTP_STEALER
        val otpMatch = sms && internet && boot
        chains.add(
            AttackChain(
                chainId = AttackChainId.OTP_STEALER,
                matched = otpMatch,
                evidence = if (otpMatch) listOf("SMS access", "Internet access", "Boot start") else emptyList()
            )
        )

        // 2. OVERLAY_TRAP
        val overlayMatch = accessibility && overlay
        chains.add(
            AttackChain(
                chainId = AttackChainId.OVERLAY_TRAP,
                matched = overlayMatch,
                evidence = if (overlayMatch) listOf("Accessibility active", "Screen overlay capability") else emptyList()
            )
        )

        // 3. SILENT_SPY
        val spyMatch = notificationListener && accessibility && internet
        chains.add(
            AttackChain(
                chainId = AttackChainId.SILENT_SPY,
                matched = spyMatch,
                evidence = if (spyMatch) listOf("Notification reader", "Accessibility service", "Internet transmission") else emptyList()
            )
        )

        // 4. PERSIST_ADMIN
        val adminMatch = deviceAdmin && internet &&
                (app.installerTrust == InstallerTrust.SIDELOADED || app.installerTrust == InstallerTrust.UNKNOWN)
        chains.add(
            AttackChain(
                chainId = AttackChainId.PERSIST_ADMIN,
                matched = adminMatch,
                evidence = if (adminMatch) listOf("Device administrator", "Internet access", "Non-store installer: ${app.installerTrust}") else emptyList()
            )
        )

        // 5. DORMANT_THREAT
        val dormantMatch = days > 30 && (sms || accessibility || overlay)
        chains.add(
            AttackChain(
                chainId = AttackChainId.DORMANT_THREAT,
                matched = dormantMatch,
                evidence = if (dormantMatch) listOf("Unused for $days days", "Holds sensitive active access") else emptyList()
            )
        )

        return chains
    }

    fun generateWhyFlagged(findings: List<ScoringFinding>): List<String> {
        val sortedFindings = findings.sortedByDescending { it.points }
        val sentences = mutableListOf<String>()

        for (finding in sortedFindings) {
            val sentence = when (finding.ruleId) {
                "accessibility_enabled", "CHAIN_OVERLAY_TRAP", "CHAIN_SILENT_SPY" ->
                    "This app has special screen control and can interact with other apps on your behalf."
                "notification_listener_enabled" ->
                    "This app can read all incoming messages, codes, and notifications."
                "device_admin_active", "CHAIN_PERSIST_ADMIN" ->
                    "This app has deep system administrator powers and cannot be easily uninstalled."
                "overlay_declared_or_granted" ->
                    "This app can display fake screens over your banking apps to capture passwords."
                "sms_any_granted", "CHAIN_OTP_STEALER" ->
                    "This app can read incoming text messages, including verification codes and bank alerts."
                "background_location_granted" ->
                    "This app can track your exact location in the background even when you are not using it."
                "broad_storage_granted" ->
                    "This app can access and view your private photos, videos, and stored documents."
                "contacts_or_calllog_granted" ->
                    "This app can see your full contact list and past phone call history."
                "camera_or_mic_granted" ->
                    "This app can turn on your microphone or camera without visual indication."
                "boot_persistence_granted" ->
                    "This app automatically starts running in the background as soon as your phone turns on."
                "internet_granted" ->
                    "This app can send information from your phone over the internet."
                "targetSdkVersion_below_28" ->
                    "This app is built using older technology that bypasses modern privacy protections."
                "sideloaded_installer" ->
                    "This app was downloaded from an unverified source outside the official store."
                "permission_creep" ->
                    "A recent update added new sensitive permissions that the app did not originally require."
                "CHAIN_DORMANT_THREAT" ->
                    "This app has not been used for over a month but continues to hold sensitive access."
                else -> finding.evidence
            }

            if (!sentences.contains(sentence)) {
                sentences.add(sentence)
            }
            if (sentences.size >= 3) break
        }

        if (sentences.isEmpty()) {
            sentences.add("No critical security concerns detected.")
            sentences.add("App operates within safe device boundaries.")
            sentences.add("Installed from a verified source.")
        } else {
            while (sentences.size < 3) {
                sentences.add("App maintains regular operational capabilities.")
            }
        }

        return sentences.take(3)
    }
}
