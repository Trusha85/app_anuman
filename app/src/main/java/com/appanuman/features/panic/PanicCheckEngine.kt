package com.appanuman.features.panic

import com.appanuman.data.model.AppRiskProfile
import com.appanuman.data.model.AttackChainId
import com.appanuman.data.model.InstallerTrust
import com.appanuman.data.model.PermissionState

enum class PanicVerdict {
    SAFE,
    WARNING,
    DANGER
}

data class PanicCheckResult(
    val remoteAccessApps: List<String>,
    val recentSpecialAccessApps: List<String>,
    val recentSideloads: List<String>,
    val activeOverlayRisk: Boolean,
    val upiRiskApps: List<String>,
    val verdict: PanicVerdict
)

object PanicCheckEngine {

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

    private val UPI_KEYWORDS = listOf("bank", "pay", "upi", "money", "wallet", "finance", "rupee")

    fun runCheck(apps: List<AppRiskProfile>): PanicCheckResult {
        val now = System.currentTimeMillis()

        // Check 1: Remote access apps
        val remoteAccessApps = apps
            .filter { it.packageName in REMOTE_ACCESS_PACKAGES }
            .map { it.label }

        // Check 2: Recent special access on new installs (< 48h)
        val recentSpecialAccessApps = apps.filter { app ->
            val installedWithin48h = (now - app.firstInstallTime) < 48L * 60 * 60 * 1000
            val hasAccessibility = app.specialAccess.accessibilityEnabled == PermissionState.GRANTED
            val hasOverlay = app.specialAccess.overlayState == PermissionState.GRANTED ||
                    app.specialAccess.overlayState == PermissionState.DECLARED_ONLY
            installedWithin48h && (hasAccessibility || hasOverlay)
        }.map { it.label }

        // Check 3: Apps sideloaded in last 24h
        val recentSideloads = apps.filter { app ->
            val isSideloaded = app.installerTrust == InstallerTrust.SIDELOADED ||
                    app.installerTrust == InstallerTrust.UNKNOWN
            val installedWithin24h = (now - app.firstInstallTime) < 24L * 60 * 60 * 1000
            isSideloaded && installedWithin24h
        }.map { it.label }

        // Check 4: Active screen control risk
        val activeOverlayRisk = apps.any { app ->
            app.threatNarratives.any { narrative ->
                narrative.chainId == AttackChainId.OTP_STEALER ||
                        narrative.chainId == AttackChainId.OVERLAY_TRAP ||
                        narrative.chainId == AttackChainId.SILENT_SPY ||
                        narrative.chainIdStr == "OTP_STEALER" ||
                        narrative.chainIdStr == "OVERLAY_TRAP" ||
                        narrative.chainIdStr == "SILENT_SPY"
            }
        }

        // Check 5: UPI fraud pattern
        val upiRiskApps = apps.filter { app ->
            val smsGranted = app.grantedPermissions.any { it.permissionName.contains("SMS", ignoreCase = true) }
            val notifListener = app.specialAccess.notificationListenerEnabled == PermissionState.GRANTED
            val lowerLabel = app.label.lowercase()
            val matchesFinance = UPI_KEYWORDS.any { lowerLabel.contains(it) }
            smsGranted && notifListener && matchesFinance
        }.map { it.label }

        // Verdict logic
        val isDanger = remoteAccessApps.isNotEmpty() ||
                (recentSpecialAccessApps.isNotEmpty() && recentSideloads.isNotEmpty()) ||
                activeOverlayRisk

        val anySingleTriggered = remoteAccessApps.isNotEmpty() ||
                recentSpecialAccessApps.isNotEmpty() ||
                recentSideloads.isNotEmpty() ||
                activeOverlayRisk ||
                upiRiskApps.isNotEmpty()

        val verdict = when {
            isDanger -> PanicVerdict.DANGER
            anySingleTriggered -> PanicVerdict.WARNING
            else -> PanicVerdict.SAFE
        }

        return PanicCheckResult(
            remoteAccessApps = remoteAccessApps,
            recentSpecialAccessApps = recentSpecialAccessApps,
            recentSideloads = recentSideloads,
            activeOverlayRisk = activeOverlayRisk,
            upiRiskApps = upiRiskApps,
            verdict = verdict
        )
    }
}
